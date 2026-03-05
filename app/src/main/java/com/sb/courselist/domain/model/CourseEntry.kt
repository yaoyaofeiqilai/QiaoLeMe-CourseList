package com.sb.courselist.domain.model

data class CourseEntry(
    val id: Long = 0L,
    val scheduleId: Long = 0L,
    val name: String,
    val teacher: String,
    val location: String,
    val note: String = "",
    val dayOfWeek: Int,
    val startPeriod: Int,
    val endPeriod: Int,
    val weekPattern: String = "all",
    val rawText: String = "",
    val sourceConfidence: Float = 0.5f,
)
