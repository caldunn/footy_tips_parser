package tech.calebdunn.webscraper
import common.Round
import org.apache.poi.hssf.usermodel.{HSSFSheet, HSSFWorkbook}

import java.io.FileOutputStream

object BasicSpreadSheet {
  def default(rounds: Array[Round]): Unit = {
    val wb = HSSFWorkbook()

    for (round <- rounds) {
      val sheet = wb.createSheet(s"round-${round.round}")
      createHeader(sheet)
      for (score <- round.mapAsArray) {
        val row = sheet.createRow(score.pos)
        row.createCell(0).setCellValue(score.pos)
        row.createCell(1).setCellValue(score.uname)
        row.createCell(2).setCellValue(score.score)
        row.createCell(3).setCellValue(score.margin)
      }
    }

    val file = FileOutputStream("src/res/csv_out/workbook.xls")
    wb.write(file)
  }
  def createHeader(sheet: HSSFSheet): Unit = {
    val header = sheet.createRow(0)
    header.createCell(0).setCellValue("Position")
    header.createCell(1).setCellValue("Username")
    header.createCell(2).setCellValue("Score")
    header.createCell(3).setCellValue("Margin")
  }
}
