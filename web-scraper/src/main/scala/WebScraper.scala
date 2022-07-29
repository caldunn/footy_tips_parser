import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import org.openqa.selenium.By.ById
import org.openqa.selenium.chrome.{ChromeDriver, ChromeOptions}
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.logging.{LogType, LoggingPreferences}
import org.openqa.selenium.remote.http.{HttpClient, HttpClientName}
import org.openqa.selenium.remote.{CapabilityType, DesiredCapabilities}
import org.openqa.selenium.support.ui.{FluentWait, WebDriverWait}
import org.openqa.selenium.{By, Cookie, Keys, WebElement}
import org.slf4j.Logger


import java.time.Duration
import java.util
import java.util.ServiceLoader
import java.util.logging.Level
import java.util.stream.{Collectors, StreamSupport}
import scala.collection.mutable.ArrayBuffer
import scala.jdk.CollectionConverters.*

private object Config {
  val chromeFlags: List[String] = List(
//    "--headless",
//    "--window-size=1920,1080",
    "--blink-settings=imagesEnabled=false",
    "--user-data-dir=selenium",
    "--no-sandbox",
    "--disable-gpu",
    "--disable-crash-reporter",
    "--disable-extensions",
    "--disable-in-process-stack-traces",
    "--disable-logging",
    "--disable-dev-shm-usage",
    "--log-level=3",
    "--silent",
    "--output=/dev/null"
  )
}

/**
 * Used to scrape a new competition.
 * @param user
 *   Username that will be used to authenticate
 * @param password
 *   Pass that will be used to authenticate
 * @param competition
 *   The competition ID.
 */
case class ScrapeRequest(user: String, password: String, competition: Int)

sealed trait ScrapeEvent
case class RoundUpdate(round: Int)     extends ScrapeEvent
case class DescribedEvent(msg: String) extends ScrapeEvent

case class ScrapeUpdate(user: String, compId: Int, event: ScrapeEvent)

object WebScraper {

  /**
   * Scrape a competition
   * @param request
   *   Details used for the scrape.
   * @param logger
   *   An implicit logger.
   * @return
   *   Results of the rounds up to the current round.
   */
  def scrape[T](request: ScrapeRequest, cbHandler: Option[(ScrapeUpdate) => T] = None)(implicit
    logger: Logger
  ): Array[Round] = {
    val fullSet: ArrayBuffer[Round]      = ArrayBuffer()
    given implicitRequest: ScrapeRequest = request

    val options = ChromeOptions()
      .addArguments(Config.chromeFlags.asJava)
    // .setBinary("/usr/bin/chromium")
    // .setBinary("/home/caleb/dev/jvm/scala/footy_tips_parser/web-scraper/src/resources/chromedriver")

    val driver: ChromeDriver = ChromeDriver(options)
    given wait: FluentWait[ChromeDriver] = FluentWait[ChromeDriver](driver)
      .withTimeout(Duration.ofSeconds(10))
      .pollingEvery(Duration.ofMillis(100))
      .ignoring(classOf[org.openqa.selenium.NoSuchElementException])

    try {
      driver.get("https://www.footytips.com.au/home")

      if Option(driver.manage().getCookieNamed("ESPN-FOOTYTIPS.WEB-PROD.token")).isEmpty then {
        signIn(driver)
      }

      driver
        .navigate()
        .to(
          "https://www.footytips.com.au/competitions/afl/siteladder"
        )

      val currentRound = wait.until { driver =>
        driver.findElement(
          By.id("round")
        )
      }.getText
        .split("\n")
        .dropRight(1)
        .last
        .split(" ")
        .last
        .toInt

      // return fullSet.toArray
      for (i <- 1 to currentRound) {
        logger.info(s"${request.user} scraping round $i in comp# ${request.competition}")
        driver
          .navigate()
          .to(
            s"https://www.footytips.com.au/competitions/afl/ladders/?competitionId=${request.competition}&gameCompId=317695&gameType=tips&view=ladderScores&round=$i&sort=1&ref=ladder-round-afl"
          )

        case class Layout(grouped: Int, mapFun: Array[String] => Array[String])
        val layout: Layout = if (i != currentRound) {
          Layout(4 + i, (arr: Array[String]) => arr)
        } else {
          Layout(3 + i, _ :+ "")
        }

        val roundStats = wait.until { driver =>
          driver.findElement(
            By.xpath(
              "//*[@class=\"table ladder-main table-horizontal ladder-mini comps-ladder scrollable-table ng-scope tipping\"]"
            )
          )
        }.getText
          .split('\n')
          .drop(1)
          .grouped(layout.grouped)
          .toArray
          .map(layout.mapFun)
          .map(ScoreStats.fromArray)
          .map(_.toKeyPair)
          .toMap

        val round       = Round(i, roundStats)
        val roundUpdate = ScrapeUpdate(request.user, request.competition, RoundUpdate(i))
        cbHandler.map(c => c(roundUpdate))
        fullSet.addOne(round)
      }
    } finally {
      driver.quit()
    }
    fullSet.toArray
  }

  /**
   * Transforms Seleniums cookies to Sttp Cookies.
   * @param cookies
   *   Selenium cookies.
   * @return
   *   Sttp cookies.
   */
//  private def extractCookies(cookies: Array[Cookie]) =
//    extension (cookie: Cookie)
//      def asSttp =
//        val asInstant = Option(cookie.getExpiry) match {
//          case Some(x) => Some(x.toInstant)
//          case None    => None
//        }
//        CookieWithMeta(
//          cookie.getName,
//          cookie.getValue,
//          asInstant,
//          None,
//          Option(cookie.getDomain),
//          Option(cookie.getPath),
//          cookie.isSecure,
//          cookie.isHttpOnly
//          // cookie.getSameSite
//        )
//
//    cookies.map(_.asSttp)

  /**
   * Sign-in and save cookies to current driver.
   * @param driver
   *   The webdriver in use.
   * @param waitRule
   *   How to wait for elements.
   * @param request
   *   Details required to sign in.
   * @param logger
   *   Logger.
   */
  private def signIn(driver: ChromeDriver)(implicit
    waitRule: FluentWait[ChromeDriver],
    request: ScrapeRequest,
    logger: Logger
  ): Unit = {
    logger.info(s"${request.user} is signing in")
    val signupButton: WebElement = driver.findElement(By.className("sign-in"))
    signupButton.click()

    val loginIFrame = waitRule.until { driver =>
      driver.findElement(By.id("disneyid-iframe"))
    }
    driver.switchTo().frame(loginIFrame)
    val emailField = waitRule.until { driver =>
      driver.findElement(By.className("field-username-email"))
    }

    Actions(driver)
      .moveToElement(emailField)
      .sendKeys(s"${request.user}${Keys.RETURN}")
      .perform()

    val passwordField = waitRule.until { driver =>
      driver.findElement(By.className("field-password"))
    }

    Actions(driver)
      .moveToElement(passwordField)
      .sendKeys(s"${request.password}${Keys.RETURN}")
      .perform()

    waitRule.until { driver =>
      driver.manage().getCookies.size() == 28 // TODO: If program breaks it may be this.
    }
    logger.info(s"${request.user} successfully signed in")
  }
}
