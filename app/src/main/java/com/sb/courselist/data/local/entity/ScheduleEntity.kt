package com.sb.courselist.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedule_meta")
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val termName: String,
    val totalWeeks: Int,
    val importedAt: Long,
    val templateVersion: String,
    val sourceTag: String,
    val termStartEpochDay: Long = 0L,
    val periodTimeMapText: String = "",
)
