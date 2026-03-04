package com.sb.courselist.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "courses",
    foreignKeys = [
        ForeignKey(
            entity = ScheduleEntity::class,
            parentColumns = ["id"],
            childColumns = ["scheduleId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["scheduleId"])],
)
data class CourseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val scheduleId: Long,
    val name: String,
    val teacher: String,
    val location: String,
    val dayOfWeek: Int,
    val startPeriod: Int,
    val endPeriod: Int,
    val weekPattern: String,
    val rawText: String,
    val sourceConfidence: Float,
)

