package common

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.{CodecMakerConfig, *}
import common.Club.reversed

import java.io.InputStream
import java.time.{Instant, LocalDateTime}
import scala.collection.mutable.ArrayBuffer

import Implicits.*
//given CodecMakerConfig.PrintCodec with {}

case class ScorePair(score: Int, margin: Int) extends Ordered[ScorePair] {
  override def compare(that: ScorePair): Int = if (score < that.score) - 1
  else if (score > that.score) 1
  else {
    if (margin == that.margin) 0
    else if (margin < that.margin) 1
    else -1
  }
  // (that.score * 10_000 + margin) - (score * 10_000 + margin)
}
object ScorePair {
  def fromTableString(s: String): ScorePair =
    val pair = s
      .trim
      .split(' ')
      .take(2)
    ScorePair(pair(0).toInt, pair(1).drop(1).dropRight(1).toInt)
}
case class ScoreStats(pos: Int, roundScore: ScorePair, totalScore: ScorePair)
case class ScoreWithTips(scoreStats: ScoreStats, tips: Array[Club])

enum Club {
  case ADELAIDE
  case BRISBANE_LIONS
  case CARLTON
  case COLLINGWOOD
  case ESSENDON
  case FREMANTLE
  case GEELONG
  case GOLD_COAST
  case GWS
  case HAWTHORN
  case MELBOURNE
  case NORTH_MELBOURNE
  case PORT_ADELAIDE
  case RICHMOND
  case ST_KILDA
  case SYDNEY
  case WEST_COAST
  case WESTERN_BULLDOGS
  case NO_TIP

  def toShortForm: String = reversed(this)

}

//given CodecMakerConfig.PrintCodec with {}


object Club {
  private val textMap: Map[String, Club] = Map(
    "ADEL" -> ADELAIDE,
    "BL"   -> BRISBANE_LIONS,
    "CARL" -> CARLTON,
    "COLL" -> COLLINGWOOD,
    "ESS"  -> ESSENDON,
    "FRE"  -> FREMANTLE,
    "GEEL" -> GEELONG,
    "GCFC" -> GOLD_COAST,
    "GWS"  -> GWS,
    "HAW"  -> HAWTHORN,
    "MELB" -> MELBOURNE,
    "NMFC" -> NORTH_MELBOURNE,
    "PORT" -> PORT_ADELAIDE,
    "RICH" -> RICHMOND,
    "STK"  -> ST_KILDA,
    "SYD"  -> SYDNEY,
    "WCE"  -> WEST_COAST,
    "WB"   -> WESTERN_BULLDOGS,
    "-"    -> NO_TIP
  )
  protected val reversed: Map[Club, String] = textMap.map((k, v) => v -> k)

  def fromText(string: String): Club  = textMap(string)
  def isValidTeam(s: String): Boolean = textMap.contains(s)
}
case class Game(roundOrder: Int, home: Club, away: Club, winner: Club) {
  require(home == winner || away == winner, "The winner must be in the game...")
  def loser: Club = if (home == winner) away else home
}

type ZippedPlayers = Array[((String, ScoreWithTips), Int)]
case class Round(round: Int, players: Map[String, ScoreWithTips], games: Array[Game]) {
  def sortByRoundScore: ZippedPlayers = players.toArray.sortBy(_._2.scoreStats.roundScore).reverse.zipWithIndex
  def sortByTotalScore: ZippedPlayers = players.toArray.sortBy(_._2.scoreStats.totalScore).reverse.zipWithIndex
}


case class ScrapeResultData(competitionID: Int, rounds: Array[Round], lastModifiedUTC: Instant = Instant.now()):
  private lazy val writerConfig: WriterConfig                    = WriterConfig.withIndentionStep(2)
  def arrayToJson(): String             = writeToString(this)
  def arrayToJsonPretty(): String       = writeToString(this, writerConfig)
  def appendRounds(newRounds: Array[Round]): ScrapeResultData =
    ScrapeResultData(this.competitionID, rounds ++ newRounds)

end ScrapeResultData

object ScrapeResultData {
  def loadFromJsonStream(stream: InputStream): ScrapeResultData = readFromStream(stream)
}

object Implicits {
  implicit val clubCodec: JsonValueCodec[Club] =
    JsonCodecMaker.make[Club](CodecMakerConfig.withDiscriminatorFieldName(None))

  implicit val scrapeResultDataCodec: JsonValueCodec[ScrapeResultData] = JsonCodecMaker.make
}
