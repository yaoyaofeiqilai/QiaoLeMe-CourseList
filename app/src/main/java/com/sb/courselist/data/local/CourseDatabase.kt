package com.sb.courselist.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.sb.courselist.data.local.dao.ScheduleDao
import com.sb.courselist.data.local.entity.CourseEntity
import com.sb.courselist.data.local.entity.ScheduleEntity

@Database(
    entities = [ScheduleEntity::class, CourseEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class CourseDatabase : RoomDatabase() {
    abstract fun scheduleDao(): ScheduleDao

    companion object {
        @Volatile
        private var INSTANCE: CourseDatabase? = null

        fun getInstance(context: Context): CourseDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CourseDatabase::class.java,
                    "course_list.db",
                ).build().also { db ->
                    INSTANCE = db
                }
            }
        }
    }
}

