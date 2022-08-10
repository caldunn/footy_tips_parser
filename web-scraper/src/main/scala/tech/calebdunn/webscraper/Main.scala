package tech.calebdunn.webscraper

import common.*
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory
import tech.calebdunn.webscraper.Main.getClass
import zio.Console.printLine
import zio.stream.ZStream
import zio.{ExitCode, Task, URIO, ZIO, ZIOAppArgs, ZIOAppDefault}

import java.io.{File, FileInputStream, FileWriter}
import java.lang

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
          array.fold("")((acc, str) => s"$acc$str\n")
        )
    }

  val app: ZIO[Any, Any, Unit] =
    for {
      _ <- printLine("Hello")
      // _      <- stream.interrupt
//      contents <- readFromFile
//       _        <- printLine(contents(0).scoreStats.mkString("Map(", "\n", ")"))
//      asCsvArray <- generateCsv(contents)
//      _          <- writeFile(asCsvArray)
    } yield ()

//  val request: ScrapeRequest = ScrapeRequest("caldunn@iinet.net.au", "pEX8#4j9w8wTK!%2", 546660)
//  def printRound(scrapeUpdate: ScrapeUpdate): Unit =
//    println(scrapeUpdate)
//  val fullSet = WebScraper.scrape(request, Some(printRound))
//  // val fullSet = Round.loadFromJson(Files.readString(Paths.get("src/res/tempStore/score.json")))
//
//  val asString = Round.arrayToJson(fullSet)
//  val fw = FileWriter(
//    File(
//      s"/home/caleb/dev/jvm/scala/footy_tips_parser/dev_cache/scores/${request.competition}.json"
//    )
//  )
//  fw.write(asString)
//  fw.close()
