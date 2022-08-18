import com.typesafe.scalalogging.Logger
import common.*
import org.slf4j.LoggerFactory
import os.Path
import tech.calebdunn.webscraper.*
import zio.*
import zio.Console.*
import zio.Duration.*
import zio.stream.*

import java.io.IOException

object Main extends ZIOAppDefault {
  given logger: org.slf4j.Logger = Logger(LoggerFactory.getLogger(getClass.getName)).underlying

  // Currently using a hardcoded location
  val cache_dir = "/home/caleb/dev/jvm/scala/footy_tips_parser/dev_cache/scores/"

  enum CacheResult {
    case DoesNotExist
    case PartiallyComplete(rounds: ScrapeResultData)
    case UpToDate(rounds: ScrapeResultData)
  }

  def checkCache(compID: Int): Task[CacheResult] =
    val fp = os.Path(s"$cache_dir$compID.json")

    // Read a cached file into memory.
    val readFile: Task[CacheResult] =
      FileIO.readCachedFile(fp).flatMap { result =>
        if (result.rounds.length == common.BigLazy.CURRENT_ROUND)
          ZIO.succeed(CacheResult.UpToDate(result))
        else
          ZIO.succeed(CacheResult.PartiallyComplete(result))
      }

    for {
      exists <- common.FileIO.fileExists(fp)
      status <- if (!exists) ZIO.succeed(CacheResult.DoesNotExist)
                else readFile
    } yield status

  def requestFromScraper(
    request: ScrapeRequest,
    range: Option[Range] = None,
    cachedData: Option[ScrapeResultData] = None
  ): Task[ScrapeResultData] =
    for {
      data <- TempInterface.scrapeZIOSync(request, range, cachedData)
    } yield data

  def fetchRounds(status: CacheResult, request: ScrapeRequest): Task[ScrapeResultData] =
    status match {
      case CacheResult.DoesNotExist => requestFromScraper(request)
      case CacheResult.PartiallyComplete(data) =>
        requestFromScraper(request, Some(data.rounds.length + 1 to BigLazy.CURRENT_ROUND), Some(data))
      case CacheResult.UpToDate(rounds) => ZIO.succeed(rounds)
    }

  def saveToCache(path: Path, result: ScrapeResultData): Task[Unit] =
    ZIO.attempt {
      result.arrayToJson()
    }.flatMap { s =>
      common.FileIO.writeToFile(path, s)
    }

  // Not currently used.
  val asStream: IO[Any, Unit] =
    val request = ScrapeRequest("123", "123", 123)
    for {
      stream2 <- TempInterface.scrapeZIO(request, Some(0 to 21)).runCollect
      _       <- printLine(stream2)
    } yield ()

  val app: ZIO[ZIOAppArgs, Any, Unit] =
    for {
      args          <- getArgs
      scrapeRequest <- ArgParsing.fromUpiFlag(args.last)
      cachedResult  <- checkCache(scrapeRequest.competition)
      rounds        <- fetchRounds(cachedResult, scrapeRequest)
      f1            <- saveToCache(os.Path(s"$cache_dir/${scrapeRequest.competition}.json"), rounds).fork
      _             <- ZIO.attempt(BasicSpreadSheet.default(rounds.rounds, s"${scrapeRequest.competition}.xlsx"))
      _             <- f1.join

    } yield ()

  def onUnhandledError(error: Any): IO[IOException, Unit] =
    printLine(s"\u001B[31;5;3;4m Maccies machine broke!\n $error")

  val appWrapper: ZIO[ZIOAppArgs, IOException, Unit] =
    (for {
      _   <- printLine("\u001B[32m-\u001B[0m" * 60)
      app <- app
      _   <- printLine("\u001B[32m-\u001B[0m" * 60)
    } yield app).catchAll(e => onUnhandledError(e))

  val run: ZIO[ZIOAppArgs, Any, ExitCode] = appWrapper.exitCode
}
