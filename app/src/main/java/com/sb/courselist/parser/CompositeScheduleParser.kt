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
                    message = "Text parsing failed. Falling back to OCR.",
                )
            }
            .getOrNull()

        if (textSchedule != null && textSchedule.courses.isNotEmpty()) {
            issues += ParseIssue(
                level = IssueLevel.INFO,
                message = "Text parsing completed.",
            )
            if (textSchedule.courses.any { it.sourceConfidence < 0.55f }) {
                issues += ParseIssue(
                    level = IssueLevel.WARNING,
                    message = "Some courses have low confidence. Please review manually.",
                )
            }
            return ParseResult.Success(
                schedule = textSchedule,
                issues = issues,
            )
        }

        issues += ParseIssue(
            level = IssueLevel.WARNING,
            message = "Text result is insufficient. Trying OCR.",
        )

        val ocrSchedule = runCatching { ocrParser.parse(uri) }
            .onFailure { throwable ->
                issues += ParseIssue(
                    level = IssueLevel.ERROR,
                    message = "OCR failed: ${throwable.message ?: "unknown error"}",
                )
            }
            .getOrNull()

        if (ocrSchedule != null && ocrSchedule.courses.isNotEmpty()) {
            issues += ParseIssue(
                level = IssueLevel.INFO,
                message = "OCR parsing completed.",
            )
            return ParseResult.Success(
                schedule = ocrSchedule,
                issues = issues,
            )
        }

        return ParseResult.Failure(
            reason = "No valid courses recognized. Try another PDF or edit manually.",
        )
    }
}

