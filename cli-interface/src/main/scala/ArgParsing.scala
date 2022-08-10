import tech.calebdunn.webscraper.ScrapeRequest
import zio.*
import zio.Console.*
import zio.ZIOAppArgs.getArgs

object ArgParsing {
  val argsAsMap: RIO[ZIOAppArgs, Map[String, String]] =
    def toKeyPair(s: Array[String]) = s(0) -> s(1)

    for {
      args <- getArgs
      asMap = args
                .toArray
                .grouped(2)
                .map(toKeyPair)
                .toMap
    } yield asMap

  def fromUpiFlag(s: String): Task[ScrapeRequest] =
    ZIO.attempt {
      val parts = s.split(":")
      if (parts.length != 3) throw Exception("Invalid input")
      else
        val compID = parts
          .last
          .toInt
        ScrapeRequest(parts(0), parts(1), compID)
    }

  def fromSeparateFlags: ZIO[ZIOAppArgs, Any, ScrapeRequest] =
    for {
      args <- getArgs
    } yield ScrapeRequest("123", "123", 123)

  val scrapeRequestFromUserInput: ZIO[Any, Throwable, ScrapeRequest] =
    for {
      name     <- readLine("Username: ")
      password <- readLine("Password: ")
      compID <- readLine("Competition ID: ")
                  .flatMap(v => ZIO.attempt(v.toInt))
                  .tapError(e => printLine(s"${e.getMessage} is not a valid int"))
                  .retry(zio.Schedule.forever)
      scrapeRequest = ScrapeRequest(name, password, compID)
    } yield scrapeRequest
}
