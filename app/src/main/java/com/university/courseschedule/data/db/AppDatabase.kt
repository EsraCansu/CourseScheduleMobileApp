package com.university.courseschedule.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.university.courseschedule.data.model.Course
import com.university.courseschedule.data.model.Lecturer

@Database(
    entities = [Course::class, Lecturer::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun courseDao(): CourseDao
    abstract fun lecturerDao(): LecturerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Use nullable column first for safe migration, then update defaults
                db.execSQL("ALTER TABLE lecturers ADD COLUMN imageUrl TEXT")
                db.execSQL("UPDATE lecturers SET imageUrl = '' WHERE imageUrl IS NULL")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Use nullable column first for safe migration
                db.execSQL("ALTER TABLE lecturers ADD COLUMN courseCode TEXT")
                db.execSQL("UPDATE lecturers SET courseCode = '' WHERE courseCode IS NULL")
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "course_schedule.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build().also { INSTANCE = it }
            }
    }
}
