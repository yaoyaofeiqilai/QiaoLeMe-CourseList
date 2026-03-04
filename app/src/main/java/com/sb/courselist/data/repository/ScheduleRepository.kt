package com.sb.courselist.data.repository

import android.net.Uri
import com.sb.courselist.domain.model.CourseEntry
import com.sb.courselist.domain.model.ParsedSchedule
import com.sb.courselist.parser.ParseResult
import kotlinx.coroutines.flow.Flow

interface ScheduleRepository {
    fun observeCurrentSchedule(): Flow<ParsedSchedule?>
    suspend fun importAndParse(uri: Uri): ParseResult
    suspend fun saveSchedule(schedule: ParsedSchedule)
    suspend fun updateCourse(entry: CourseEntry)
}

