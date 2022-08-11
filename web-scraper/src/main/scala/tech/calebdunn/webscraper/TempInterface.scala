package tech.calebdunn.webscraper
import common.Round
import org.slf4j.Logger
import zio.{Chunk, Task, ZIO}
import zio.stream.{Stream, ZStream}

import scala.concurrent.{ExecutionContext, Future}
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

  def scrapeZIO(request: ScrapeRequest, range: Option[Range])(implicit
    logger: Logger
  ): Stream[ScrapeResult, ScrapeCallback] =
    ZStream.async { cb =>
      given ec: ExecutionContext = zio.Runtime.defaultExecutor.asExecutionContext

      Future {
        WebScraper
          .scrape(
            request,
            range,
            Some {
              case update: ScrapeUpdate => cb(ZIO.succeed(Chunk(update)))
              case result: ScrapeResult =>
                result.status match {
                  case ScrapeExitStatus.SUCCESS => {
                    //                    cb(ZIO.succeed(Chunk(result)))
                    //                    cb(ZIO.fail(None))
                    cb(ZIO.fail(Some(result)))
                  }
                  case ScrapeExitStatus.ERROR => cb(ZIO.fail(Some(result)))
                }
            }
          )
      }
    }

  def scrapeZIOSync(request: ScrapeRequest, range: Option[Range])(implicit
    logger: Logger
  ): Task[Array[Round]] =
    ZIO.attempt {
      WebScraper.scrape(request, range)
    }
}
