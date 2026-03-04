package com.sb.courselist.parser

import com.sb.courselist.domain.model.ParsedSchedule

sealed interface ParseResult {
    data class Success(
        val schedule: ParsedSchedule,
        val issues: List<ParseIssue> = emptyList(),
    ) : ParseResult

    data class FallbackRequired(
        val reason: String,
        val issues: List<ParseIssue> = emptyList(),
    ) : ParseResult

    data class Failure(
        val reason: String,
        val cause: Throwable? = null,
    ) : ParseResult
}

