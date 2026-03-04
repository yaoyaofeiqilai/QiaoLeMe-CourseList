package com.sb.courselist.parser

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.sb.courselist.domain.model.ParsedSchedule
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class PdfOcrParser(
    private val context: Context,
    private val ruleEngine: TemplateRuleEngine,
) {
    suspend fun parse(uri: Uri): ParsedSchedule? = withContext(Dispatchers.Default) {
        val resolver = context.contentResolver
        val fileDescriptor = resolver.openFileDescriptor(uri, "r") ?: return@withContext null

        fileDescriptor.use { descriptor ->
            val renderer = PdfRenderer(descriptor)
            val recognizer = TextRecognition.getClient(
                ChineseTextRecognizerOptions.Builder().build(),
            )

            try {
                val allTokens = mutableListOf<TextToken>()
                val pagesToScan = renderer.pageCount.coerceAtMost(3)
                for (index in 0 until pagesToScan) {
                    val page = renderer.openPage(index)
                    try {
                        val bitmap = Bitmap.createBitmap(
                            page.width * 2,
                            page.height * 2,
                            Bitmap.Config.ARGB_8888,
                        )
                        try {
                            page.render(
                                bitmap,
                                null,
                                null,
                                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY,
                            )
                            val image = InputImage.fromBitmap(bitmap, 0)
                            val text = recognizer.process(image).await()
                            text.textBlocks.forEach { block ->
                                block.lines.forEach { line ->
                                    val rect = line.boundingBox ?: return@forEach
                                    val value = line.text.replace(Regex("\\s+"), "")
                                    if (value.isBlank()) return@forEach
                                    allTokens += TextToken(
                                        text = value,
                                        x = rect.left.toFloat(),
                                        y = rect.top.toFloat(),
                                        width = rect.width().toFloat(),
                                        height = rect.height().toFloat().coerceAtLeast(1f),
                                        page = index + 1,
                                    )
                                }
                            }
                        } finally {
                            bitmap.recycle()
                        }
                    } finally {
                        page.close()
                    }
                }
                ruleEngine.parse(allTokens, sourceTag = "ocr")
            } finally {
                recognizer.close()
                renderer.close()
            }
        }
    }
}

