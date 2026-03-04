package com.sb.courselist.ui.state

import com.sb.courselist.domain.model.ParsedSchedule
import com.sb.courselist.parser.ParseIssue

data class CourseListUiState(
    val activeTab: BottomTab = BottomTab.TIMETABLE,
    val currentSchedule: ParsedSchedule? = null,
    val previewSchedule: ParsedSchedule? = null,
    val importIssues: List<ParseIssue> = emptyList(),
    val selectedFileName: String? = null,
    val isParsing: Boolean = false,
    val isSaving: Boolean = false,
    val message: String? = null,
)

