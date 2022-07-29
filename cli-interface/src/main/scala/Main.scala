import zio._
import zio.Console._
import zio.Duration._
import zio.stream._

import java.io.IOException
import scala.io.StdIn

object Main extends ZIOAppDefault {

  val kek: ZIO[Any, Any, Any] = {
    Console
      .printLine("Caleb")
      .delay(1.seconds)
      .forever
  }
  val countTo10: ZStream[Any, Nothing, Byte] = {
    ZStream
      .fromIterable(1 to 10)
      .map(_.toByte)
      .schedule(Schedule.spaced(500.millis))
  }
  kek.hello()
  val run: ZIO[Any, IOException, Unit] = {
    for {
      stream <- countTo10.foreach(x => printLine(x)).fork
      _      <- readLine
      _      <- stream.interrupt
    } yield ()
  }
  // val run: ZIO[Any, IOException, String] = printLine("Hello World") *> readLine
}
