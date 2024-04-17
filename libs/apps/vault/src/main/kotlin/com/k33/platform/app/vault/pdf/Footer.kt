package com.k33.platform.app.vault.pdf

import com.lowagie.text.Document
import com.lowagie.text.Element
import com.lowagie.text.Font
import com.lowagie.text.Image
import com.lowagie.text.Paragraph
import com.lowagie.text.Rectangle
import com.lowagie.text.pdf.PdfPCell
import com.lowagie.text.pdf.PdfPTable
import com.lowagie.text.pdf.PdfPageEventHelper
import com.lowagie.text.pdf.PdfWriter
import java.awt.Color


class Footer : PdfPageEventHelper() {
    override fun onEndPage(writer: PdfWriter, document: Document) {
        // step 3: create a footer

        PdfPTable(2).apply {
            // A4 width (595) - 2 * margin (72)
            this.totalWidth = 595f - 2 * 72f
            // 85% - 15%
            this.setWidths(intArrayOf(85, 15))

            this.addCell(
                PdfPCell(
                    Paragraph(
                        """
                            K33 Markets AS, Munkedamsveien 45 C, 0250 Oslo, Norway.
                            Phone: +(47) 23 96 30 10
                            Company registration no. 919578998
                        """.trimIndent(),
                        Font(Font.HELVETICA, 10f, Font.NORMAL, Color.GRAY)
                    )
                ).apply {
                    this.horizontalAlignment = Element.ALIGN_LEFT
                    this.border = Rectangle.NO_BORDER
                }
            )

            this.addCell(
                PdfPCell(
                    Image.getInstanceFromClasspath("pdf/footer-logo.png")
                        .apply {
                            this.scalePercent(5f)
                        }
                ).apply {
                    this.horizontalAlignment = Element.ALIGN_RIGHT
                    this.border = Rectangle.NO_BORDER
                }
            )

            this.writeSelectedRows(
                0,
                -1,
                72f, // 1 inch
                108f, // 1.5 inch
                writer.directContent
            )
        }
    }
}