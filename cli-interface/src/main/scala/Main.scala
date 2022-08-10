import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory
import tech.calebdunn.webscraper.*
import tech.calebdunn.webscraper.Main.getClass
import zio.*
import zio.Console.*
import zio.Duration.*
import zio.stream.*

import java.io.IOException

object Main extends ZIOAppDefault {
  given logger: org.slf4j.Logger = Logger(LoggerFactory.getLogger(getClass.getName)).underlying
//  val argsRaw: Chunk[String]     = Chunk("abc", "123", "123")
//  val args: ULayer[ZIOAppArgs]   = ZLayer.succeed(zio.ZIOAppArgs(argsRaw))

  val app: ZIO[ZIOAppArgs, Any, Unit] =
    for {
      _             <- printLine("-" * 50)
      argMap        <- ArgParsing.argsAsMap
      _             <- printLine(argMap.mkString)
      scrapeRequest <- ArgParsing.fromSeparateFlags
      _             <- printLine(scrapeRequest)
      _             <- printLine("-" * 50)

      // request <- scrapeRequestFromUserInput
      //      stream2 <- TempInterface.scrapeZ(request).runCollect
//      _       <- printLine(stream2)
    } yield ()
    // .provideLayer(args)

  val run: ZIO[ZIOAppArgs, Nothing, ExitCode] = app.exitCode
}
