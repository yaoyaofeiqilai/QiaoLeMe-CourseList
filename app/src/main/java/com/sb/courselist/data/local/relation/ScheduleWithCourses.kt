package com.sb.courselist.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.sb.courselist.data.local.entity.CourseEntity
import com.sb.courselist.data.local.entity.ScheduleEntity

data class ScheduleWithCourses(
    @Embedded val schedule: ScheduleEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "scheduleId",
    )
    val courses: List<CourseEntity>,
)

