package com.sb.courselist.parser

import android.content.Context
import android.net.Uri
import com.sb.courselist.domain.model.ParsedSchedule
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PdfTextParser(
    private val context: Context,
    private val ruleEngine: TemplateRuleEngine,
) {
    suspend fun parse(uri: Uri): ParsedSchedule? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val stream = resolver.openInputStream(uri) ?: return@withContext null

        stream.use { input ->
            val document = PDDocument.load(input)
            document.use { pdDocument ->
                val tokenCollector = PositionAwareStripper()
                for (page in 1..pdDocument.numberOfPages) {
                    tokenCollector.activePage = page
                    tokenCollector.startPage = page
                    tokenCollector.endPage = page
                    tokenCollector.getText(pdDocument)
                }
                ruleEngine.parse(tokenCollector.tokens, sourceTag = "pdf-text")
            }
        }
    }
}

private class PositionAwareStripper : PDFTextStripper() {
    val tokens = mutableListOf<TextToken>()
    var activePage: Int = 1

    init {
        sortByPosition = true
    }

    override fun writeString(text: String, textPositions: MutableList<TextPosition>) {
        if (text.isBlank() || textPositions.isEmpty()) return
        val compact = text.replace(Regex("\\s+"), "")
        if (compact.isBlank()) return

        val first = textPositions.first()
        val last = textPositions.last()
        val width = (last.xDirAdj + last.widthDirAdj - first.xDirAdj)
            .coerceAtLeast(first.widthDirAdj)
        val height = textPositions.maxOf { it.heightDir.coerceAtLeast(1f) }

        tokens += TextToken(
            text = compact,
            x = first.xDirAdj,
            y = first.yDirAdj,
            width = width,
            height = height,
            page = activePage,
        )
    }
}

