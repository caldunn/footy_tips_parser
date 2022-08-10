import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory
import tech.calebdunn.webscraper.*
import tech.calebdunn.webscraper.Main.getClass
import zio.*
import zio.Console.*
import zio.Duration.*
import zio.stream.*

object Main extends ZIOAppDefault {
  given logger: org.slf4j.Logger = Logger(LoggerFactory.getLogger(getClass.getName)).underlying
  val request: ScrapeRequest     = ScrapeRequest("uname", "pword", 00000) // Test for now -- Stripped in commit.
  val run: ZIO[Any, Any, Unit] =
    for {
      stream2 <- TempInterface.scrapeZ(request).runCollect
      _       <- printLine(stream2)
    } yield ()

  // val run: ZIO[Any, IOException, String] = printLine("Hello World") *> readLine
}
