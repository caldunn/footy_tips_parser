package tech.calebdunn.webscraper

case class Config(chromeFlags: List[String] = List()) {}

object Config {
  val default: Config = Config(
    chromeFlags = List(
      "--headless",
      "--window-size=1920,1080",
      "--blink-settings=imagesEnabled=false",
      "--user-data-dir=/home/caleb/dev/jvm/scala/footy_tips_parser/dev_cache/chrome_dir/selenium",
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
  )
}
