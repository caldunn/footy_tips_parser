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

  private lazy val cache_dir = {
    val dir = os.pwd / "dev_cache" / "scores"
    if !os.exists(dir) then os.makeDir.all(dir)
    dir
  }

  enum CacheResult {
    case DoesNotExist
    case PartiallyComplete(rounds: ScrapeResultData)
    case UpToDate(rounds: ScrapeResultData)
  }

  def checkCache(compID: Int): Task[CacheResult] =
    val fp = cache_dir / s"$compID.json"
    // Read a cached file into memory.
    val readFile: Task[CacheResult] =
      FileIO.readCachedFile(fp).flatMap { result =>
        if (result.rounds.length == common.BigLazy.CURRENT_ROUND)
          printLine(s"Cache is up to date. No need to scrape again.") *>
            ZIO.succeed(CacheResult.UpToDate(result))
        else
          printLine(s"${result.rounds.length} cached. Grabbing the rest now") *>
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
      result.arrayToJsonPretty()
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
      scrapeRequest <- ArgParsing.fromUpiFlag(args.lastOption)
      cachedResult  <- checkCache(scrapeRequest.competition)
      rounds        <- fetchRounds(cachedResult, scrapeRequest)
      f1            <- saveToCache(cache_dir / s"${scrapeRequest.competition}.json", rounds).fork
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
