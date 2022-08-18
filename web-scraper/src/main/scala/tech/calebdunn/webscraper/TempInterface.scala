package tech.calebdunn.webscraper
import common.{Round, ScrapeResultData}
import org.slf4j.Logger
import zio.stream.{Stream, ZStream}
import zio.{Chunk, Task, ZIO}

import java.nio.file.{Files, Paths}
import scala.concurrent.{ExecutionContext, Future}

object TempInterface {

  val writtenArray: Task[ScrapeResultData] =
    ZIO.succeed(
      ScrapeResultData.loadFromJsonStream(
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

  def scrapeZIOSync(
    request: ScrapeRequest,
    range: Option[Range],
    scrapeResultData: Option[ScrapeResultData] =
      None /*Not sure who should handle creating the data. Will here for now*/
  )(implicit
    logger: Logger
  ): Task[ScrapeResultData] =
    ZIO.attempt {
      val rounds = WebScraper.scrape(request, range)
      scrapeResultData match {
        case None    => ScrapeResultData(request.competition, rounds)
        case Some(x) => x.appendRounds(rounds)
      }
    }

}
