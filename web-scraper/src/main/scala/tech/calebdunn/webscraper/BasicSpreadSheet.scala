package tech.calebdunn.webscraper

import common.{Game, Round, ScoreStats, Club}
import org.apache.commons.io.IOUtils
import org.apache.logging.log4j.*
import org.apache.poi.xssf.usermodel
import org.apache.poi.xssf.usermodel.*
import org.apache.poi.hssf.util.HSSFColor
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.{CellRangeAddress, PropertyTemplate}
import java.io.FileOutputStream
import scala.jdk.CollectionConverters.*

object BasicSpreadSheet {

  private val TIP_START = 6
  /* I don't know how to restrict the generic into a valid type.
  type CellValueValid = String | Boolean | java.util.Date | java.util.Calendar | java.time.LocalDateTime |
    java.time.LocalDateTime | Double | org.apache.poi.ss.usermodel.RichTextString
  extension (row: HSSFCell)
    def fSetValue[A <: CellValueValid](value: A): HSSFCell =

      row.setCellValue(value)
      row
   */
  class StyleContainer(private val wb: XSSFWorkbook):

    lazy val wrongGame: XSSFCellStyle =
      val style = genBaseStyle
      style.setFillPattern(FillPatternType.SOLID_FOREGROUND)
      style.setFillForegroundColor(HSSFColor.HSSFColorPredefined.ROSE.getIndex)
      style

    lazy val correctGame: XSSFCellStyle =
      val style = genBaseStyle
      style.setFillPattern(FillPatternType.SOLID_FOREGROUND)
      style.setFillForegroundColor(HSSFColor.HSSFColorPredefined.LIME.getIndex)
      style

    lazy val seperator: XSSFCellStyle =
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
      style.setFillForegroundColor(IndexedColors.LAVENDER.getIndex)
      style

    lazy val playerRowSecondary: XSSFCellStyle =
      val style = genBaseStyle
      style.setFillForegroundColor(IndexedColors.LIME.getIndex)
      style

    lazy val fontWinner: XSSFFont =
      gameFont(IndexedColors.GREEN.getIndex)

    lazy val fontLoser: XSSFFont =
      gameFont(IndexedColors.BLACK.getIndex)
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

  def default(rounds: Array[Round], filename: String)(implicit logger: org.slf4j.Logger): Unit = {
    val wb          = XSSFWorkbook()
    val stylesClass = StyleContainer(wb)

    val sheet = wb.createSheet(s"RoundByRound")
    sheet.createFreezePane(2, 1)

    var OFFSET: Int = 0
    createHeader(sheet, rounds(0).games)

    def writeGameRow(round: Round): Unit = {
      val gameRow = sheet.createRow(OFFSET + 1)
      sheet.addMergedRegion(CellRangeAddress(OFFSET + 1, OFFSET + 1, 0, 1))
      gameRow.createCell(0).setCellValue(s"ROUND ${round.round}")
      for ((game, i) <- round.games.zipWithIndex) {
        val richTextTeam = XSSFRichTextString(s"${game.home.toShortForm}${" " * 9}${game.away.toShortForm}")

        val homeAwayFontWin =
          if (game.home == game.winner) (stylesClass.fontWinner, stylesClass.fontLoser)
          else (stylesClass.fontLoser, stylesClass.fontWinner)

        richTextTeam.applyFont(0, game.home.toShortForm.length, homeAwayFontWin._1)
        richTextTeam.applyFont(
          game.home.toShortForm.length + 1,
          richTextTeam.getString.length,
          homeAwayFontWin._2
        )

        val cell = gameRow.createCell(i + TIP_START)
        cell.setCellValue(richTextTeam)
        cell.setCellStyle(stylesClass.gameHeaderStyle)
      }
    }

    // Main iteration.
    for (round <- rounds) {
      writeGameRow(round)

      val players = round.scoreStats.toArray

      for (((name, score), i) <- players.sortBy(_._2.scoreStats.roundScore).reverse.zipWithIndex) {
        val scoreStats = score.scoreStats
        val row        = sheet.createRow(OFFSET + i + 2)
        val pos        = row.createCell(0)
        pos.setCellValue(i + 1)

        row.createCell(1).setCellValue(name)
        row.createCell(2).setCellValue(scoreStats.roundScore.score)
        row.createCell(3).setCellValue(scoreStats.roundScore.margin)
        row.createCell(4).setCellValue(scoreStats.totalScore.score)
        row.createCell(5).setCellValue(scoreStats.totalScore.margin)

        for ((tip, i) <- score.tips.zipWithIndex) {
          row.createCell(TIP_START + i).setCellValue(tip.toShortForm)
        }
        val rowStyle = if scoreStats.pos % 2 == 0 then stylesClass.playerRowPrimary else stylesClass.playerRowSecondary

        row.cellIterator.asScala.take(TIP_START).foreach(_.setCellStyle(rowStyle))

        row.cellIterator.asScala.drop(TIP_START).zipWithIndex.foreach { cell =>
          // Dirty before for quick impl
          val club  = Club.fromText(cell._1.getStringCellValue)
          val style = if (round.games(cell._2).winner == club) stylesClass.correctGame else stylesClass.wrongGame
          cell._1.setCellStyle(style)
        }
      }

      for (i <- 1 to 5) {
        sheet.autoSizeColumn(i)
      }

      def generateSeparator(offset: Int): Unit =
        val sepCellIndex = players.length + 2
        sheet.addMergedRegion(CellRangeAddress(offset + sepCellIndex, offset + sepCellIndex, 0, 14))
        val row = sheet.createRow(offset + sepCellIndex)
        row.setHeight((row.getHeight * 1.2).toShort)
        val cell = row.createCell(0)
        cell.setCellStyle(stylesClass.seperator)

      generateSeparator(OFFSET)
      // Outside border
      val pt = PropertyTemplate()
      pt.drawBorders(
        CellRangeAddress(1, players.length + 1, 0, 5),
        BorderStyle.MEDIUM,
        IndexedColors.BLACK.getIndex,
        BorderExtent.OUTSIDE
      )

      pt.drawBorders(
        CellRangeAddress(1, players.length + 1, 0, TIP_START + 9 - 1),
        BorderStyle.MEDIUM,
        IndexedColors.BLACK.getIndex,
        BorderExtent.OUTSIDE
      )
      pt.applyBorders(sheet)
      OFFSET = OFFSET + players.length + 2
    }

    val file = FileOutputStream(s"./$filename")
    wb.write(file)
  }

  def createHeader(sheet: XSSFSheet, games: Array[Game]): Unit = {
    val header = sheet.createRow(0)
    header.createCell(0).setCellValue("Position")
    header.createCell(1).setCellValue("Username")
    header.createCell(2).setCellValue("Round Score")
    header.createCell(3).setCellValue("Round Margin")
    header.createCell(4).setCellValue("Total Score")
    header.createCell(5).setCellValue("Total Margin")

    sheet.addMergedRegion(CellRangeAddress(0, 0, 6, 14))
    header.createCell(6).setCellValue("Games")
    val book  = sheet.getWorkbook
    val style = book.createCellStyle()
    style.setFillForegroundColor(IndexedColors.LAVENDER.getIndex)
    style.setFillPattern(FillPatternType.THIN_FORWARD_DIAG)
    style.setLocked(true)
    val font = book.createFont()
    font.setBold(true)
    style.setFont(font)
    style.setWrapText(true)
    style.setAlignment(HorizontalAlignment.CENTER)

    val style2 = book.createCellStyle()
    style2.setAlignment(HorizontalAlignment.CENTER)
    for (cell <- header.cellIterator().asScala) {
      cell.setCellStyle(style)
    }
    val props = PropertyTemplate()
    props.drawBorders(
      CellRangeAddress(0, 1, 0, TIP_START + games.length - 1),
      BorderStyle.MEDIUM,
      IndexedColors.BLACK.getIndex,
      BorderExtent.ALL
    )
    props.applyBorders(sheet)
  }
  object CommonStyling {}
}
