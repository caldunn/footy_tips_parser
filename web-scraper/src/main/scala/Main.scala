import zio.*
import zio.Console.printLine
import zio.stream.*

import java.io.*
import java.nio.file.{Files, Paths}
import java.time.Duration
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

object Main extends ZIOAppDefault:

  def run: ZIO[ZIOAppArgs, Any, ExitCode] = app.exitCode
  given logger: org.slf4j.Logger          = Logger(LoggerFactory.getLogger(getClass.getName)).underlying

  val openFileAsStream: Task[FileInputStream] =
    ZIO.attemptBlockingInterrupt {
      FileInputStream("src/res/tempStore/score.json")
    }

  def closeFileStream(stream: FileInputStream): URIO[Any, Any] =
    ZIO.succeed(stream.close())

  val readFromFile: ZIO[Any, Throwable, Array[Round]] =
    ZIO.acquireReleaseWith(openFileAsStream)(closeFileStream) { file =>
      ZIO.attemptBlockingInterrupt(Round.loadFromJsonStream(file))
    }

  def generateCsv(rounds: Array[Round]) =
    ZIO.attemptBlockingInterrupt {

      rounds
        .flatMap(r =>
          r.scoreStats
            .map(s => s"${r.round},${s._1},${s._2.asCSV}")
            .toArray
        )
    }

  def writeFile(array: Array[String]) =
    ZIO.scoped {
      ZIO
        .writeFile(
          "src/res/csv_out/out.csv",
          array.fold("")((acc, str) => s"${acc}${str}\n")
        )
    }
  val app: ZIO[Any, Throwable, Unit] =
    for {
      _ <- printLine("")
//      contents <- readFromFile
//       _        <- printLine(contents(0).scoreStats.mkString("Map(", "\n", ")"))
//      asCsvArray <- generateCsv(contents)
//      _          <- writeFile(asCsvArray)
//      _          <- ZIO.attemptBlockingInterrupt(BasicSpreadSheet.default(contents))
//      _          <- ZIO.attemptBlockingInterrupt(WebScraper.scrape())
    } yield ()
  val request: ScrapeRequest = ScrapeRequest("email@kek.com", "", 000000)
  def printRound(scrapeUpdate: ScrapeUpdate): Unit =
    println(scrapeUpdate)
  val fullSet = WebScraper.scrape(request, Some(printRound))
  // val fullSet = Round.loadFromJson(Files.readString(Paths.get("src/res/tempStore/score.json")))

  val asString = Round.arrayToJson(fullSet)
  val fw       = FileWriter(File("src/resources/tempStore/score_new.json"))
  fw.write(asString)
  fw.close()
