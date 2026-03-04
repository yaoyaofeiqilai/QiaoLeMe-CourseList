package com.sb.courselist.di

import android.content.Context
import com.sb.courselist.data.local.CourseDatabase
import com.sb.courselist.data.repository.RoomScheduleRepository
import com.sb.courselist.data.repository.ScheduleRepository
import com.sb.courselist.parser.CompositeScheduleParser

class AppContainer(context: Context) {
    private val database = CourseDatabase.getInstance(context)
    private val parser = CompositeScheduleParser(context)

    val scheduleRepository: ScheduleRepository = RoomScheduleRepository(
        dao = database.scheduleDao(),
        parser = parser,
    )
}

