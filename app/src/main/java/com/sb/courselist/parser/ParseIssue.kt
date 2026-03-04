package com.sb.courselist.parser

data class ParseIssue(
    val level: IssueLevel,
    val message: String,
    val page: Int? = null,
)

enum class IssueLevel {
    INFO,
    WARNING,
    ERROR,
}

