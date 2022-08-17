package tech.calebdunn.webscraper

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import common.*
import org.openqa.selenium.By.ById
import org.openqa.selenium.chrome.{ChromeDriver, ChromeOptions}
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.logging.{LogType, LoggingPreferences}
import org.openqa.selenium.remote.http.{HttpClient, HttpClientName}
import org.openqa.selenium.remote.{CapabilityType, DesiredCapabilities}
import org.openqa.selenium.support.ui.{FluentWait, WebDriverWait}
import org.openqa.selenium.{By, Cookie, Keys, WebElement}
import org.slf4j.Logger

import java.lang.Runnable
import java.time.Duration
import java.util
import java.util.ServiceLoader
import java.util.logging.Level
import java.util.stream.{Collectors, StreamSupport}
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.jdk.CollectionConverters.*

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
enum ScrapeExitStatus {
  case SUCCESS
  case ERROR
}
sealed trait ScrapeCallback(user: String, compId: Int)
case class ScrapeUpdate(user: String, compId: Int, event: ScrapeEvent) extends ScrapeCallback(user, compId)
case class ScrapeResult(user: String, compId: Int, result: Array[Round], status: ScrapeExitStatus)
    extends ScrapeCallback(user, compId)

object WebScraper {

  /**
   * Scrape a competition
   *
   * @param request
   *   Details used for the scrape.
   * @param logger
   *   An implicit logger.
   * @return
   *   Results of the rounds up to the current round.
   */
  def scrape(request: ScrapeRequest, range: Option[Range] = None, cbHandle: Option[ScrapeCallback => Unit] = None)(
    implicit
    logger: Logger,
    ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  ): Array[Round] =

    val fullSet: ArrayBuffer[Round] = ArrayBuffer()

    given implicitRequest: ScrapeRequest = request

    val options = ChromeOptions()
      .addArguments(Config.default.chromeFlags.asJava)
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

      val mRange = range match {
        case Some(value) => value
        case None        => 1 to currentRound
      }

      for (i <- mRange) {
        logger.info(s"${request.user} scraping round $i in comp# ${request.competition}")
        driver
          .navigate()
          .to(
            s"https://www.footytips.com.au/competitions/afl/ladders/?competitionId=${request.competition}&round=$i&view=ladderTips&gameCompId=317695&sort=1"
          )

        val fullTable =
          wait.until { driver =>
            driver.findElement(
              By.xpath(
                "//*[@class=\"table ladder-main table-horizontal ladder-mini comps-ladder scrollable-table ng-scope who-tipped-what\"]"
              )
            )
          }

        val gameResults =
          fullTable
            .findElements(By.tagName("th"))
            .asScala
            .drop(2)
            .dropRight(2) // Clean the table elements we dont care about.
            .zipWithIndex
            .map { (e, i) =>
              val teamsRaw = e
                .getText
                .split("\n")
                .take(2)
                .map(Club.fromText)
              val (home, away) = (teamsRaw(0), teamsRaw(1))
              val winner       = Club.fromText(e.findElement(By.className("winner")).getText)
              Game(i + 1, home, away, winner)
            }
            .toArray

        val headerLength =
          fullTable.getText.split("\n").takeWhile(!_.contains("ROUND")).length + 1
        val gamesPlayed =
          (headerLength - 3) / 2 + 4 // Remove score and name and divide by two as each game takes 2 cols in memory.
        val tableBodyContents = fullTable.getText.split("\n").drop(headerLength)

        // Read the table into a round entry.
        case class ScoresLocal(rScore: ScorePair, tScore: ScorePair)
        def asScorePair(score: (String, String)): ScoresLocal =
          ScoresLocal(ScorePair.fromTableString(score._1), ScorePair.fromTableString(score._2))
        type PosUser = (Int, String)
        val roundMap = tableBodyContents
          .grouped(gamesPlayed)
          .map { row =>
            val tips =
              row
                .drop(2)
                .dropRight(2)
                .zipWithIndex
                .map { tip =>
                  tip._1.toUpperCase match {
                    case "CORRECT"   => gameResults(tip._2).winner
                    case "INCORRECT" => gameResults(tip._2).loser
                    case "NO TIP"    => gameResults(tip._2).home
                    case _           => gameResults(tip._2).home
                  }
                }
            val user: PosUser    = (row(0).toInt, row(1))
            val score            = asScorePair(row.takeRight(2).head, row.last)
            val scoreStatsNoName = ScoreStats(user._1, score.rScore, score.tScore)
            user._2 -> ScoreWithTips(scoreStatsNoName, tips)
          }
          .toMap

        val round       = Round(i, roundMap, gameResults)
        val roundUpdate = ScrapeUpdate(request.user, request.competition, RoundUpdate(i))

        cbHandle.foreach(cb => cb(roundUpdate))
        fullSet.addOne(round)
      }
    } catch {
      case _: Exception =>
        cbHandle.foreach(cb =>
          cb(ScrapeResult(request.user, request.competition, fullSet.toArray, ScrapeExitStatus.ERROR))
        )
    } finally {
      driver.quit()
    }
    cbHandle.foreach(cb =>
      cb(ScrapeResult(request.user, request.competition, fullSet.toArray, ScrapeExitStatus.SUCCESS))
    )
    fullSet.toArray

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
      driver.findElement(By.id("oneid-iframe"))
    }
    logger.debug(s"${request.user} selected login frame")
    driver.switchTo().frame(loginIFrame)
    val emailField = waitRule.until { driver =>
      driver.findElement(By.ByTagName("input"))
    }
    logger.debug(s"${request.user} found email input")
    Actions(driver)
      .moveToElement(emailField)
      .sendKeys(s"${request.user}${Keys.RETURN}")
      .perform()

    val passwordField = waitRule.until { driver =>
      driver.findElement(By.ById("InputPassword"))
    }
    logger.debug(s"${request.user} found password input")

    Actions(driver)
      .moveToElement(passwordField)
      .click
      .sendKeys(s"${request.password}${Keys.RETURN}")
      .perform()

    waitRule.until { driver =>
      driver.manage().getCookies.size() >= 24 // TODO: If program breaks it may be this.
    }
    logger.info(s"${request.user} successfully signed in")
  }
}
