package com.sb.courselist.data.repository

import android.net.Uri
import com.sb.courselist.data.local.dao.ScheduleDao
import com.sb.courselist.data.local.entity.CourseEntity
import com.sb.courselist.data.local.entity.ScheduleEntity
import com.sb.courselist.data.local.relation.ScheduleWithCourses
import com.sb.courselist.domain.model.CourseEntry
import com.sb.courselist.domain.model.ParsedSchedule
import com.sb.courselist.domain.model.ScheduleMeta
import com.sb.courselist.parser.ParseResult
import com.sb.courselist.parser.ScheduleParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomScheduleRepository(
    private val dao: ScheduleDao,
    private val parser: ScheduleParser,
) : ScheduleRepository {
    override fun observeCurrentSchedule(): Flow<ParsedSchedule?> {
        return dao.observeLatestSchedule().map { relation ->
            relation?.toDomain()
        }
    }

    override suspend fun importAndParse(uri: Uri): ParseResult {
        return parser.parse(uri)
    }

    override suspend fun saveSchedule(schedule: ParsedSchedule) {
        val scheduleId = dao.insertSchedule(
            ScheduleEntity(
                termName = schedule.meta.termName,
                totalWeeks = schedule.meta.totalWeeks,
                importedAt = schedule.meta.importedAt,
                templateVersion = schedule.meta.templateVersion,
                sourceTag = schedule.meta.sourceTag,
                termStartEpochDay = schedule.meta.termStartEpochDay,
                periodTimeMapText = encodePeriodTimes(schedule.meta.periodTimes),
            ),
        )
        dao.deleteCoursesForSchedule(scheduleId)
        dao.insertCourses(
            schedule.courses.map { entry ->
                entry.toEntity(scheduleId = scheduleId)
            },
        )
    }

    override suspend fun addCourse(entry: CourseEntry) {
        if (entry.scheduleId <= 0L) return
        dao.insertCourse(entry.toEntity(scheduleId = entry.scheduleId))
    }

    override suspend fun deleteCourse(courseId: Long) {
        if (courseId <= 0L) return
        dao.deleteCourseById(courseId)
    }

    override suspend fun updateCourse(entry: CourseEntry) {
        if (entry.id <= 0L || entry.scheduleId <= 0L) return
        dao.updateCourse(entry.toEntity(scheduleId = entry.scheduleId))
    }
}

private fun ScheduleWithCourses.toDomain(): ParsedSchedule {
    return ParsedSchedule(
        meta = ScheduleMeta(
            id = schedule.id,
            termName = schedule.termName,
            totalWeeks = schedule.totalWeeks,
            importedAt = schedule.importedAt,
            templateVersion = schedule.templateVersion,
            sourceTag = schedule.sourceTag,
            termStartEpochDay = schedule.termStartEpochDay,
            periodTimes = decodePeriodTimes(schedule.periodTimeMapText),
        ),
        courses = courses.map { entity ->
            CourseEntry(
                id = entity.id,
                scheduleId = entity.scheduleId,
                name = entity.name,
                teacher = entity.teacher,
                location = entity.location,
                note = entity.note,
                dayOfWeek = entity.dayOfWeek,
                startPeriod = entity.startPeriod,
                endPeriod = entity.endPeriod,
                weekPattern = entity.weekPattern,
                rawText = entity.rawText,
                sourceConfidence = entity.sourceConfidence,
            )
        }.sortedWith(compareBy<CourseEntry> { it.dayOfWeek }.thenBy { it.startPeriod }),
    )
}

private fun CourseEntry.toEntity(scheduleId: Long): CourseEntity {
    return CourseEntity(
        id = if (id > 0L) id else 0L,
        scheduleId = scheduleId,
        name = name,
        teacher = teacher,
        location = location,
        note = note,
        dayOfWeek = dayOfWeek,
        startPeriod = startPeriod,
        endPeriod = endPeriod,
        weekPattern = weekPattern,
        rawText = rawText,
        sourceConfidence = sourceConfidence,
    )
}

private fun encodePeriodTimes(map: Map<Int, String>): String {
    if (map.isEmpty()) return ""
    return map.entries
        .sortedBy { it.key }
        .joinToString(separator = ";") { (period, time) ->
            "${period}:${time.trim()}"
        }
}

private fun decodePeriodTimes(text: String): Map<Int, String> {
    if (text.isBlank()) return emptyMap()
    return text.split(';')
        .mapNotNull { item ->
            val idx = item.indexOf(':')
            if (idx <= 0 || idx >= item.lastIndex) return@mapNotNull null
            val period = item.substring(0, idx).toIntOrNull() ?: return@mapNotNull null
            val value = item.substring(idx + 1).trim()
            if (value.isBlank()) return@mapNotNull null
            period to value
        }
        .sortedBy { it.first }
        .toMap()
}
