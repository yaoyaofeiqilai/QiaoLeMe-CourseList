package com.sb.courselist.domain.model

data class ParsedSchedule(
    val meta: ScheduleMeta,
    val courses: List<CourseEntry>,
)

