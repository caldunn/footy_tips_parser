package tech.calebdunn.webscraper

import common.Round
import org.apache.poi.hssf.usermodel.{HSSFCell, HSSFCellStyle, HSSFRow, HSSFSheet, HSSFWorkbook}
import org.apache.logging.log4j.*
import org.apache.poi.hssf.usermodel
import org.apache.poi.ss.usermodel.{BorderExtent, BorderStyle, FillPatternType, IndexedColors}
import org.apache.poi.ss.util.{CellRangeAddress, PropertyTemplate}

import java.io.FileOutputStream
import scala.jdk.CollectionConverters.*

object BasicSpreadSheet {

  /* I don't know how to restrict the generic into a valid type.
  type CellValueValid = String | Boolean | java.util.Date | java.util.Calendar | java.time.LocalDateTime |
    java.time.LocalDateTime | Double | org.apache.poi.ss.usermodel.RichTextString
  extension (row: HSSFCell)
    def fSetValue[A <: CellValueValid](value: A): HSSFCell =

      row.setCellValue(value)
      row
   */

  def default(rounds: Array[Round], filename: String)(implicit logger: org.slf4j.Logger): Unit = {
    val wb = HSSFWorkbook()

    for (round <- rounds) {
      val sheet = wb.createSheet(s"round-${round.round}")

      // .copy does not seem to work for some reason. Let me just use this.
      def genBaseStyle: HSSFCellStyle =
        val style = wb.createCellStyle()
        style.setFillPattern(FillPatternType.THIN_FORWARD_DIAG)
        style.setBorderBottom(BorderStyle.DASHED)
        style

      val rStyle1 = genBaseStyle
      rStyle1.setFillForegroundColor(IndexedColors.LAVENDER.getIndex)

      val rStyle2 = genBaseStyle
      rStyle2.setFillForegroundColor(IndexedColors.LIME.getIndex)

      val posColStyle = genBaseStyle
      val font        = wb.createFont()
      font.setBold(true)
      posColStyle.setFont(font)

      createHeader(sheet)
      for (score <- round.mapAsArray) {
        val row = sheet.createRow(score.pos)
        val pos = row.createCell(0)
        pos.setCellValue(score.pos)

        row.createCell(1).setCellValue(score.uname)
        row.createCell(2).setCellValue(score.score)
        row.createCell(3).setCellValue(score.margin)
        val rowStyle = if score.pos % 2 == 0 then rStyle1 else rStyle2

        row.cellIterator.asScala.foreach(_.setCellStyle(rowStyle))
      }
      sheet.autoSizeColumn(1)

      // Outside border
      val pt = PropertyTemplate()
      pt.drawBorders(
        CellRangeAddress(1, 15, 0, 3),
        BorderStyle.MEDIUM,
        IndexedColors.BLACK.getIndex,
        BorderExtent.OUTSIDE
      )

      pt.applyBorders(sheet)
    }

    val file = FileOutputStream(s"./$filename")
    wb.write(file)
  }

  def createHeader(sheet: HSSFSheet): Unit = {
    val header = sheet.createRow(0)
    header.createCell(0).setCellValue("Position")
    header.createCell(1).setCellValue("Username")
    header.createCell(2).setCellValue("Score")
    header.createCell(3).setCellValue("Margin")

    val book  = sheet.getWorkbook
    val style = book.createCellStyle()
    style.setFillForegroundColor(IndexedColors.LAVENDER.getIndex)
    style.setFillPattern(FillPatternType.THIN_FORWARD_DIAG)
    style.setLocked(true)
    // style.setBorderBottom(BorderStyle.MEDIUM)

    val font = book.createFont()
    font.setBold(true)
    style.setFont(font)

    for (cell <- header.cellIterator().asScala) {
      cell.setCellStyle(style)
    }
    val props = PropertyTemplate()
    props.drawBorders(
      CellRangeAddress(0, 1, 0, 3),
      BorderStyle.MEDIUM,
      IndexedColors.BLACK.getIndex,
      BorderExtent.ALL
    )
    props.applyBorders(sheet)
  }
}
