package legacy

import zio.Console.printLine
import zio.Task
import zio.stream.{ZPipeline, ZStream}

object Kek {
  val writeBody: Task[Unit] =
    ZStream
      .fromFileName("src/res/page.html")
      .via(ZPipeline.utf8Decode >>> ZPipeline.splitLines)
      .foreach(printLine(_))

////  def printResponse(body: ZStream.BinaryStream): Task[Unit] =
////    body
////      .via(ZPipeline.utf8Decode >>> ZPipeline.splitLines)
////      .foreach(printLine(_))
//
//  val footy =
//    uri"https://www.footytips.com.au/competitions/afl/ladders/?competitionId=546660&gameCompId=317695&gameType=tips&view=ladderScores&round=1"
//  val hello =
//    uri"http://localhost:3000"
}
