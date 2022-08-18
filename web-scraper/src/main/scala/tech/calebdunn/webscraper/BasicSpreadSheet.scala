package tech.calebdunn.webscraper

import common.{Club, Game, Round, ScoreStats, ScoreWithTips, ZippedPlayers}
import org.apache.commons.io.IOUtils
import org.apache.logging.log4j.*
import org.apache.poi.hssf.record.aggregates.MergedCellsTable
import org.apache.poi.hssf.util.HSSFColor
import org.apache.poi.ss.formula.functions.Offset
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.{CellRangeAddress, PropertyTemplate}
import org.apache.poi.xssf.usermodel
import org.apache.poi.xssf.usermodel.*

import java.io.FileOutputStream
import scala.annotation.tailrec
import scala.collection.View.ZipWithIndex
import scala.jdk.CollectionConverters.*

object BasicSpreadSheet {

  private val TIP_START = 6
  /* I don't know how to restrict the generic into a valid type.
   * This is a very messy implementation that I would love to fix soon.
   */
  type CellValueValid = String | Boolean | Double | java.util.Date | java.util.Calendar | java.time.LocalDateTime |
    RichTextString
  extension (cell: XSSFCell)
    def setValue(value: CellValueValid): XSSFCell =
      value match {
        case x: String                  => cell.setCellValue(x)
        case x: Boolean                 => cell.setCellValue(x)
        case x: java.util.Date          => cell.setCellValue(x)
        case x: java.util.Calendar      => cell.setCellValue(x)
        case x: java.time.LocalDateTime => cell.setCellValue(x)
        case x: Double                  => cell.setCellValue(x)
        case x: RichTextString          => cell.setCellValue(x)
      }
      cell
    def setStyle(style: XSSFCellStyle): XSSFCell =
      cell.setCellStyle(style)
      cell

  protected class StyleContainer(private val wb: XSSFWorkbook):

    lazy val wrongGame: XSSFCellStyle =
      val style = genBaseStyle
      // style.setFillPattern(FillPatternType.SOLID_FOREGROUND)
      style.setFillForegroundColor(HSSFColor.HSSFColorPredefined.ROSE.getIndex)
      style

    lazy val correctGame: XSSFCellStyle =
      val style = genBaseStyle
      // style.setFillPattern(FillPatternType.SOLID_FOREGROUND)
      style.setFillForegroundColor(HSSFColor.HSSFColorPredefined.LIME.getIndex)
      style

    lazy val separator: XSSFCellStyle =
      val style = wb.createCellStyle()
      style.setFillPattern(FillPatternType.SOLID_FOREGROUND)
      style.setFillForegroundColor(IndexedColors.BLACK.getIndex)
      style

    lazy val gameHeaderStyle: XSSFCellStyle =
      val style = genBaseStyle
      style.setFillForegroundColor(IndexedColors.LAVENDER.getIndex)
      style.setLocked(true)
      style.setWrapText(true)
      style

    lazy val playerRowPrimary: XSSFCellStyle =
      val style = genBaseStyle
      style.setFillForegroundColor(IndexedColors.SKY_BLUE.getIndex)
      style

    lazy val playerRowSecondary: XSSFCellStyle =
      val style = genBaseStyle
      style.setFillForegroundColor(IndexedColors.LAVENDER.getIndex)
      style

    lazy val roundCell: XSSFCellStyle =
      val style = genBaseStyle

      style.setFillForegroundColor(IndexedColors.LAVENDER.getIndex)
      style.setAlignment(HorizontalAlignment.CENTER)
      style.setVerticalAlignment(VerticalAlignment.CENTER)

      val font = wb.createFont()
      font.setBold(true)
      style.setFont(font)

      style

    lazy val averages: XSSFCellStyle =
      val style = genBaseStyle
      style.setWrapText(true)
      style.setAlignment(HorizontalAlignment.CENTER)
      style.setFillForegroundColor(IndexedColors.LAVENDER.getIndex)
      style.setBorderRight(BorderStyle.DASHED)
      style

    lazy val fontWinner: XSSFFont =
      gameFont(IndexedColors.GREEN.getIndex)

    lazy val fontLoser: XSSFFont =
      gameFont(IndexedColors.BLACK.getIndex)

    lazy val frozenScoreHeader: XSSFCellStyle =
      val style = wb.createCellStyle()
      style.setFillForegroundColor(IndexedColors.LAVENDER.getIndex)
      style.setFillPattern(FillPatternType.THIN_FORWARD_DIAG)
      style.setLocked(true)

      val font = wb.createFont()
      font.setBold(true)
      style.setFont(font)
      style.setWrapText(true)
      style.setAlignment(HorizontalAlignment.CENTER)
      style

    private def gameFont(colour: Short) =
      val font = wb.createFont()
      font.setBold(true)
      font.setColor(colour)
      font

    private def genBaseStyle: XSSFCellStyle =
      val style = wb.createCellStyle()
      style.setFillPattern(FillPatternType.THIN_FORWARD_DIAG)
      style.setBorderBottom(BorderStyle.THIN)
      style.setBorderLeft(BorderStyle.DASHED)
      style

  end StyleContainer
  private case class ScoreAveragePair(score: Double, margin: Double)
  enum OrderOption {
    case RoundByRound
    case RunningTotal
  }

  def default(rounds: Array[Round], filename: String)(implicit logger: org.slf4j.Logger): Unit = {
    val wb                       = XSSFWorkbook()
    given styles: StyleContainer = StyleContainer(wb)

    val roundByRound: XSSFSheet = wb.createSheet(s"RoundByRound")
    val runningTotal: XSSFSheet = wb.createSheet(s"RunningTotal")

    writeSheet(roundByRound, rounds, OrderOption.RoundByRound)
    writeSheet(runningTotal, rounds, OrderOption.RunningTotal)

    val file = FileOutputStream(s"./$filename")
    wb.write(file)
  }
  private def writeSheet(sheet: XSSFSheet, rounds: Array[Round], orderOption: OrderOption)(implicit
    styles: StyleContainer
  ): Unit = {
    createHeader(rounds(0).games, sheet, styles)
    writeRounds(rounds, orderOption, 1)(sheet, styles)
  }

  private def writeGameRow(round: Round, scoreAveragePair: ScoreAveragePair)(implicit
    sheet: XSSFSheet,
    styles: StyleContainer,
    offset: Int
  ): Unit = {
    val homeRow = sheet.createRow(offset)
    sheet.addMergedRegion(CellRangeAddress(offset, offset, 0, 1))
    homeRow
      .createCell(0)
      .setValue(s"ROUND ${round.round}")
      .setStyle(styles.roundCell)

    val formatter = java.text.DecimalFormat("#.#")

    homeRow
      .createCell(2)
      .setValue(s"Avg Score\n${formatter.format(scoreAveragePair.score)}")
      .setStyle(styles.averages)

    homeRow
      .createCell(3)
      .setValue(s"Avg Margin\n${formatter.format(scoreAveragePair.margin)}")
      .setStyle(styles.averages)

    for ((game, i) <- round.games.zipWithIndex) {
      val rts = XSSFRichTextString(s"${game.home.toShortForm}\n${game.away.toShortForm}")
      val homeAwayFontWin =
        if (game.home == game.winner) (styles.fontWinner, styles.fontLoser)
        else (styles.fontLoser, styles.fontWinner)

      rts.applyFont(0, game.home.toShortForm.length, homeAwayFontWin._1)
      rts.applyFont(game.home.toShortForm.length + 1, rts.getString.length, homeAwayFontWin._2)

      homeRow
        .createCell(i + TIP_START)
        .setValue(rts)
        .setStyle(styles.gameHeaderStyle)
    }
  }

  private def writePlayers(players: ZippedPlayers, games: Array[Game])(implicit
    sheet: XSSFSheet,
    styles: StyleContainer,
    offset: Int
  ): ScoreAveragePair =
    val totals = players.foldLeft((0d, 0d)) { case (a, ((name, score), i)) =>
      val scoreStats = score.scoreStats
      val row        = sheet.createRow(offset + i + 1)

      row.createCell(0).setCellValue(i + 1)
      row.createCell(1).setCellValue(name)
      row.createCell(2).setCellValue(scoreStats.roundScore.score)
      row.createCell(3).setCellValue(scoreStats.roundScore.margin)
      row.createCell(4).setCellValue(scoreStats.totalScore.score)
      row.createCell(5).setCellValue(scoreStats.totalScore.margin)

      score.tips.zipWithIndex.foreach { case (tip, i) => row.createCell(TIP_START + i).setCellValue(tip.toShortForm) }

      val rowStyle =
        if i % 2 == 0 then styles.playerRowPrimary else styles.playerRowSecondary

      row.cellIterator.asScala.take(TIP_START).foreach(_.setCellStyle(rowStyle))

      row
        .cellIterator
        .asScala
        .drop(TIP_START)
        .zipWithIndex
        .foreach { case (cell, i) =>
          // Dirty before for quick impl
          val club  = Club.fromText(cell.getStringCellValue)
          val style = if (games(i).winner == club) styles.correctGame else styles.wrongGame
          cell.setCellStyle(style)
        }
      (a._1 + scoreStats.roundScore.score, a._2 + scoreStats.roundScore.margin)
    }

    ScoreAveragePair(totals._1 / players.length, totals._2 / players.length)

  private def generateSeparator(rowIndex: Int)(implicit sheet: XSSFSheet, styles: StyleContainer): Unit =
    sheet.addMergedRegion(CellRangeAddress(rowIndex, rowIndex, 0, 14))
    val row = sheet.createRow(rowIndex)
    row.setHeight((row.getHeight * 1.2).toShort)
    row.createCell(0).setCellStyle(styles.separator)

  @tailrec
  private def writeRounds(rounds: Array[Round], orderOption: OrderOption, offset: Int)(implicit
    sheet: XSSFSheet,
    styles: StyleContainer
  ): Unit =
    if (rounds.isEmpty) {
      ()
    } else {
      val round          = rounds.head
      given iOffset: Int = offset
      val players = orderOption match {
        case OrderOption.RoundByRound => round.sortByRoundScore
        case OrderOption.RunningTotal => round.sortByTotalScore
      }
      val x: ScoreAveragePair = writePlayers(players, round.games)
      writeGameRow(round, x)
      (1 to 5).foreach(sheet.autoSizeColumn)
      generateSeparator(players.length + 1 + offset)

      // Outside border
      val pt = PropertyTemplate()
      pt.drawBorders(
        CellRangeAddress(offset, players.length + offset, 0, 5),
        BorderStyle.MEDIUM,
        IndexedColors.BLACK.getIndex,
        BorderExtent.OUTSIDE
      )

      pt.drawBorders(
        CellRangeAddress(offset, players.length + offset, 0, TIP_START + 8),
        BorderStyle.MEDIUM,
        IndexedColors.BLACK.getIndex,
        BorderExtent.OUTSIDE
      )

      pt.drawBorders(
        CellRangeAddress(offset, offset, 0, TIP_START + 8),
        BorderStyle.MEDIUM,
        IndexedColors.BLACK.getIndex,
        BorderExtent.OUTSIDE
      )
      pt.applyBorders(sheet)
      writeRounds(rounds.tail, orderOption, offset + players.length + 2)
    }

  def createHeader(games: Array[Game], sheet: XSSFSheet, styleClass: StyleContainer): Unit = {
    sheet.createFreezePane(2, 1)
    val header = sheet.createRow(0)
    header.createCell(0).setCellValue("Position")
    header.createCell(1).setCellValue("Username")
    header.createCell(2).setCellValue("Round Score")
    header.createCell(3).setCellValue("Round Margin")
    header.createCell(4).setCellValue("Total Score")
    header.createCell(5).setCellValue("Total Margin")

    sheet.addMergedRegion(CellRangeAddress(0, 0, 6, 14))
    header.createCell(6).setCellValue("Games")

    header.cellIterator().asScala.foreach(_.setCellStyle(styleClass.frozenScoreHeader))

    val props = PropertyTemplate()
    props.drawBorders(
      CellRangeAddress(0, 1, 0, TIP_START + games.length - 1),
      BorderStyle.MEDIUM,
      IndexedColors.BLACK.getIndex,
      BorderExtent.ALL
    )
    props.applyBorders(sheet)
  }
}
