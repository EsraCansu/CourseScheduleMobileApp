package com.coursescheduling.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.coursescheduling.domain.model.Course
import com.coursescheduling.domain.model.LecturerEntity
import com.coursescheduling.domain.model.DepartmentEntity
import com.coursescheduling.domain.model.ScheduleEntity
import com.coursescheduling.domain.model.ScheduleRequestEntity
import com.coursescheduling.domain.model.AvailabilityEntity

@Database(
    entities = [
        Course::class, 
        LecturerEntity::class, 
        DepartmentEntity::class, 
        ScheduleEntity::class,
        ScheduleRequestEntity::class,
        AvailabilityEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun courseDao(): CourseDao
    abstract fun lecturerDao(): LecturerDao
    abstract fun departmentDao(): DepartmentDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun scheduleRequestDao(): ScheduleRequestDao
    abstract fun availabilityDao(): AvailabilityDao

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
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
    }
}
