package common

import os.Path
import zio.*

object FileIO {
  def fileExists(path: Path): UIO[Boolean] =
    ZIO.attempt(os.exists(path)).orElse(ZIO.succeed(false))

  def readRoundsFromFile(path: Path): Task[Array[Round]] =
    ZIO.attempt {
      Round.loadFromJsonStream {
        os.read.inputStream(path)
      }
    }

  def writeToFile(path: Path, s: String): Task[Unit] =
    ZIO.attempt(os.write.over(path, s))
}
