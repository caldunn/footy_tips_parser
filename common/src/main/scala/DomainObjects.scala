import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*

import java.io.InputStream
import scala.collection.mutable.ArrayBuffer

//given CodecMakerConfig.PrintCodec with {}
implicit val codec2: JsonValueCodec[Array[Round]] = JsonCodecMaker.make
case class ScoreStats(uname: String, pos: Int, score: Int, margin: Int) {
  def toKeyPair: (String, ScoreStatsNoName) = this.uname -> this.noName
  private def noName: ScoreStatsNoName      = ScoreStatsNoName(this.pos, this.score, this.margin)
}
case class ScoreStatsNoName(pos: Int, score: Int, margin: Int) {
  def asCSV: String = s"${this.pos},${this.score},${this.margin}"
}

object ScoreStats {
  def fromArray(raw: Array[String]): ScoreStats = {
    val uname            = raw(1)
    val pos              = raw(0).toInt
    val scoreMarginCombo = raw.takeRight(2)(0).split(' ')
    val score            = scoreMarginCombo(0).toInt
    val margin           = scoreMarginCombo(1).drop(1).dropRight(1).toInt
    ScoreStats(uname, pos, score, margin)
  }
}
val writerConfig: WriterConfig = WriterConfig.withIndentionStep(2)

case class Round(round: Int, scoreStats: Map[String, ScoreStatsNoName]) {
  def mapAsArray: Array[ScoreStats] =
    this
      .scoreStats
      .map((k, v: ScoreStatsNoName) => ScoreStats(k, v.pos, v.score, v.margin))
      .toArray
//       .sortWith((e, a) => e.pos < a.pos)
}
object Round {
  def arrayToJson(values: Array[Round]): String             = writeToString(values, writerConfig)
  def loadFromJsonStream(stream: InputStream): Array[Round] = readFromStream(stream)
}
