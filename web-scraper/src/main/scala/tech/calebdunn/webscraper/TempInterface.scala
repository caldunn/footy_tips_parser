package tech.calebdunn.webscraper
import common.Round
import org.slf4j.Logger
import zio.{Chunk, Task, ZIO}
import zio.stream.{Stream, ZStream}
import scala.concurrent.ExecutionContext
import java.nio.file.{Files, Paths}

object TempInterface {
  val writtenArray: Task[Array[Round]] =
    ZIO.succeed(
      Round.loadFromJsonStream(
        Files.newInputStream(
          Paths.get("/home/caleb/dev/jvm/scala/footy_tips_parser/dev_cache/scores/546660.json")
        )
      )
    )

  def scrapeZ(request: ScrapeRequest)(implicit logger: Logger): Stream[ScrapeResult, ScrapeUpdate] =
    ZStream.async { cb =>
      given ec: ExecutionContext = zio.Runtime.defaultExecutor.asExecutionContext
      WebScraper
        .scrape(
          request,
          Some {
            case update: ScrapeUpdate => cb(ZIO.succeed(Chunk(update)))
            case result: ScrapeResult =>
              result.status match {
                case ScrapeExitStatus.SUCCESS => cb(ZIO.fail(None))
                case ScrapeExitStatus.ERROR   => cb(ZIO.fail(Some(result)))
              }
          }
        )
    }
}
