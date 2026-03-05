package com.sb.courselist.domain.model

data class ScheduleMeta(
    val id: Long = 0L,
    val termName: String,
    val totalWeeks: Int,
    val importedAt: Long,
    val templateVersion: String,
    val sourceTag: String,
    val termStartEpochDay: Long = 0L,
    val periodTimes: Map<Int, String> = emptyMap(),
)
