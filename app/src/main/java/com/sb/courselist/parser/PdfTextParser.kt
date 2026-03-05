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
        if (textPositions.isEmpty()) return

        val parts = mutableListOf<TextToken>()
        val builder = StringBuilder()
        var startX = 0f
        var startY = 0f
        var endX = 0f
        var maxHeight = 0f
        var hasActive = false
        var previous: TextPosition? = null

        fun flush() {
            if (!hasActive) return
            val value = builder.toString()
                .replace(Regex("\\s+"), "")
                .trim()
            if (value.isNotBlank()) {
                parts += TextToken(
                    text = value,
                    x = startX,
                    y = startY,
                    width = (endX - startX).coerceAtLeast(1f),
                    height = maxHeight.coerceAtLeast(1f),
                    page = activePage,
                )
            }
            builder.clear()
            hasActive = false
            previous = null
        }

        textPositions.forEach { position ->
            val unicode = position.unicode.orEmpty()
            if (unicode.isBlank()) {
                flush()
                return@forEach
            }

            val prev = previous
            if (!hasActive) {
                hasActive = true
                startX = position.xDirAdj
                startY = position.yDirAdj
                endX = position.xDirAdj + position.widthDirAdj
                maxHeight = position.heightDir
                builder.append(unicode)
                previous = position
                return@forEach
            }

            val gap = position.xDirAdj - (prev!!.xDirAdj + prev.widthDirAdj)
            val lineBreak = kotlin.math.abs(position.yDirAdj - prev.yDirAdj) > LINE_BREAK_TOLERANCE
            val chunkBreak = gap > maxOf(CHUNK_GAP_MIN, prev.widthDirAdj * CHUNK_GAP_FACTOR)

            if (lineBreak || chunkBreak) {
                flush()
                hasActive = true
                startX = position.xDirAdj
                startY = position.yDirAdj
                endX = position.xDirAdj + position.widthDirAdj
                maxHeight = position.heightDir
                builder.append(unicode)
                previous = position
                return@forEach
            }

            builder.append(unicode)
            endX = position.xDirAdj + position.widthDirAdj
            maxHeight = maxOf(maxHeight, position.heightDir)
            previous = position
        }

        flush()
        tokens += parts
    }

    companion object {
        private const val CHUNK_GAP_FACTOR = 1.8f
        private const val CHUNK_GAP_MIN = 6f
        private const val LINE_BREAK_TOLERANCE = 2.2f
    }
}
