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

      val style = wb.createCellStyle()

      createHeader(sheet, wb)
      var fillFlip = false
      for (score <- round.mapAsArray) {
        val row = sheet.createRow(score.pos)
        row.createCell(0).setCellValue(score.pos)
        row.createCell(1).setCellValue(score.uname)
        row.createCell(2).setCellValue(score.score)
        row.createCell(3).setCellValue(score.margin)

      }
      sheet.autoSizeColumn(1)
      fillFlip = !fillFlip
      // Add a simple border.

    }

    val file = FileOutputStream(s"./$filename")
    wb.write(file)
  }

  def createHeader(sheet: HSSFSheet, book: HSSFWorkbook): Unit = {
    val header = sheet.createRow(0)
    header.createCell(0).setCellValue("Position")
    header.createCell(1).setCellValue("Username")
    header.createCell(2).setCellValue("Score")
    header.createCell(3).setCellValue("Margin")

    val style = book.createCellStyle()
    style.setFillForegroundColor(IndexedColors.LAVENDER.getIndex)
    style.setFillPattern(FillPatternType.THIN_FORWARD_DIAG)
    style.setLocked(true)
    style.setBorderBottom(BorderStyle.MEDIUM)

    val font = book.createFont()
    font.setBold(true)
    style.setFont(font)

    for (cell <- header.cellIterator().asScala) {
      cell.setCellStyle(style)
    }
    val props = PropertyTemplate()
    props.drawBorders(
      CellRangeAddress(1, 2, 1, 3),
      BorderStyle.MEDIUM,
      IndexedColors.RED.getIndex,
      BorderExtent.ALL
    )
    props.applyBorders(sheet)
  }
}
