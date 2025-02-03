package com.k33.platform.app.vault.pdf

import com.k33.platform.app.vault.VaultAsset
import com.lowagie.text.Chunk
import com.lowagie.text.Document
import com.lowagie.text.Element
import com.lowagie.text.Font
import com.lowagie.text.Image
import com.lowagie.text.PageSize
import com.lowagie.text.Paragraph
import com.lowagie.text.Phrase
import com.lowagie.text.Rectangle
import com.lowagie.text.pdf.PdfPCell
import com.lowagie.text.pdf.PdfPTable
import com.lowagie.text.pdf.PdfWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Color
import java.io.ByteArrayOutputStream
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

suspend fun getBalanceReport(
    name: String,
    address: List<String>,
    date: LocalDate,
    vaultAssets: List<VaultAsset>,
): ByteArray {
    return withContext(Dispatchers.IO) {
        ByteArrayOutputStream().use { byteArrayOutputStream ->

            // A4 = 595 × 842 pts
            val document = Document(
                PageSize.A4,
                72f, // 1 inch
                72f, // 1 inch
                72f, // 1 inch
                72f, // 1 inch
            )

            PdfWriter.getInstance(document, byteArrayOutputStream).use { writer ->
                writer.pageEvent = Footer()

                val locale = Locale.of("no", "NO")
                val currencyFormatter = NumberFormat.getCurrencyInstance(locale)
                val numberFormatter = NumberFormat.getInstance(locale)
                numberFormatter.maximumFractionDigits = 17

                document.use { document ->
                    with(document) {
                        this.open()
                        this.image("pdf/header-logo.png") {
                            this.scalePercent(2f)
                        }
                        this.text("Balance Report", size = 18, style = Font.BOLD) {
                            this.spacingBefore = 10f
                        }

                        this.text(name.uppercase(), size = 14) {
                            this.spacingBefore = 20f
                        }
                        address.forEach { line ->
                            this.text(line, size = 11)
                        }

                        val formattedDate = date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                        this.text("Overview of assets and values on your account at K33 Vault as of $formattedDate") {
                            this.spacingBefore = 20f
                        }

                        this.table(
                            headers = listOf(
                                "Cryptocurrency" to Element.ALIGN_LEFT,
                                "Units\n(A)" to Element.ALIGN_RIGHT,
                                "Rate per unit\n(B)" to Element.ALIGN_RIGHT,
                                "Net Asset Value\n(A × B)" to Element.ALIGN_RIGHT,
                            ),
                            data = vaultAssets
                                .filter { it.available > 0.0 }
                                .flatMap {
                                    listOf(
                                        it.id,
                                        numberFormatter.format(it.available),
                                        currencyFormatter.format(it.rate?.value),
                                        currencyFormatter.format(it.fiatValue?.value),
                                    )
                                },
                            footer = listOf(
                                "",
                                "",
                                "Total",
                                currencyFormatter.format(vaultAssets.sumOf { it.fiatValue?.value ?: 0.0 })
                            )
                        )

                        this.text("Your account balances are not reported to the tax authorities automatically.", size = 11) {
                            this.spacingBefore = 20f
                        }
                        this.text("If you have any questions, please do not hesitate to contact us at ", size = 11) {
                            this.add(
                                Chunk("vault@k33.com", Font(Font.HELVETICA, 10f, Font.NORMAL, Color.BLUE))
                            )
                        }
                    }
                } // document.close()
            } // writer.close()
            byteArrayOutputStream.toByteArray()
        } // byteArrayOutputStream.close()
    }
}

fun Document.image(
    resourceFile: String,
    block: Image.() -> Unit = {},
) {
    add(
        Image
            .getInstanceFromClasspath(resourceFile)
            .apply(block)
    )

}

fun Document.text(
    text: String,
    size: Int = 12,
    style: Int = Font.NORMAL,
    color: Color = Color.BLACK,
    block: Paragraph.() -> Unit = {}
) {
    add(
        Paragraph(
            text,
            Font(Font.HELVETICA, size.toFloat(), style, color),
        ).apply(block)
    )
}

fun Document.table(
    headers: List<Pair<String, Int>>,
    data: List<String>,
    footer: List<String>,
) {
    add(
        PdfPTable(headers.size).apply {
            this.setSpacingBefore(20f)
            this.widthPercentage = 100f
            headers.forEach { (header, align) ->
                this.addCell(
                    PdfPCell(
                        Phrase(header, Font(Font.HELVETICA, 12f, Font.BOLD))
                    ).apply {
                        this.border = Rectangle.BOTTOM
                        this.paddingBottom = 7f
                        this.horizontalAlignment = align
                    }
                )
            }
            data.forEachIndexed { index, text ->
                this.addCell(
                    PdfPCell(
                        Phrase(text, Font(Font.HELVETICA, 12f, Font.NORMAL))
                    ).apply {
                        this.border = Rectangle.BOTTOM
                        this.paddingBottom = 7f
                        this.horizontalAlignment = headers[index % headers.size].second
                    }
                )
            }
            footer.forEachIndexed { index, footer ->
                this.addCell(
                    PdfPCell(
                        Phrase(footer, Font(Font.HELVETICA, 12f, Font.BOLD))
                    ).apply {
                        this.border = Rectangle.NO_BORDER
                        this.paddingBottom = 7f
                        this.horizontalAlignment = headers[index % headers.size].second
                    }
                )
            }
        }
    )
}