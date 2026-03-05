package com.sb.courselist.parser

import android.content.Context
import android.net.Uri

class CompositeScheduleParser(context: Context) : ScheduleParser {
    private val ruleEngine = TemplateRuleEngine()
    private val textParser = PdfTextParser(context, ruleEngine)
    private val ocrParser = PdfOcrParser(context, ruleEngine)

    override suspend fun parse(uri: Uri): ParseResult {
        val issues = mutableListOf<ParseIssue>()

        val textSchedule = runCatching { textParser.parse(uri) }
            .onFailure {
                issues += ParseIssue(
                    level = IssueLevel.WARNING,
                    message = "文本解析失败，正在尝试 OCR。",
                )
            }
            .getOrNull()

        if (textSchedule != null && textSchedule.courses.isNotEmpty()) {
            issues += ParseIssue(
                level = IssueLevel.INFO,
                message = "文本解析完成。",
            )
            if (textSchedule.courses.any { it.sourceConfidence < 0.55f }) {
                issues += ParseIssue(
                    level = IssueLevel.WARNING,
                    message = "部分课程置信度较低，请人工校对。",
                )
            }
            return ParseResult.Success(
                schedule = textSchedule,
                issues = issues,
            )
        }

        issues += ParseIssue(
            level = IssueLevel.WARNING,
            message = "文本结果不足，正在尝试 OCR。",
        )

        val ocrSchedule = runCatching { ocrParser.parse(uri) }
            .onFailure { throwable ->
                issues += ParseIssue(
                    level = IssueLevel.ERROR,
                    message = "OCR 失败：${throwable.message ?: "未知错误"}",
                )
            }
            .getOrNull()

        if (ocrSchedule != null && ocrSchedule.courses.isNotEmpty()) {
            issues += ParseIssue(
                level = IssueLevel.INFO,
                message = "OCR 解析完成。",
            )
            return ParseResult.Success(
                schedule = ocrSchedule,
                issues = issues,
            )
        }

        return ParseResult.Failure(
            reason = "未识别到有效课程，请更换 PDF 或手动编辑。",
        )
    }
}
