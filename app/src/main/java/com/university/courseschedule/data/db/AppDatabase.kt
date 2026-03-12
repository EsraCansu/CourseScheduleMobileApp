package com.university.courseschedule.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.university.courseschedule.data.model.Course
import com.university.courseschedule.data.model.Lecturer

@Database(
    entities = [Course::class, Lecturer::class],
    version = 1,
    exportSchema = false        // set true and provide schemaLocation when ready for migrations
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun courseDao(): CourseDao
    abstract fun lecturerDao(): LecturerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "course_schedule.db"
                ).build().also { INSTANCE = it }
            }
    }
}
