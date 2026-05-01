package com.coursescheduling.data.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.coursescheduling.domain.model.Course
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {

    /**
     * Insert or replace a batch of courses.
     * REPLACE strategy means re-importing the same file refreshes rows cleanly.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(courses: List<Course>)

    /** All courses as a Flow stream — emits whenever the table changes. */
    @Query("SELECT * FROM courses ORDER BY departmentIndex, dayIndex, timeSlotIndex")
    fun getAllCoursesFlow(): Flow<List<Course>>

    /** All courses as a LiveData stream — emits whenever the table changes. */
    @Query("SELECT * FROM courses ORDER BY departmentIndex, dayIndex, timeSlotIndex")
    fun getAllCourses(): LiveData<List<Course>>

    /** Filtered view for Lecturers: only courses they teach. */
    @Query("SELECT * FROM courses WHERE lecturerID = :lecturerID ORDER BY departmentIndex, dayIndex, timeSlotIndex")
    fun getCoursesByLecturer(lecturerID: String): LiveData<List<Course>>

    /** All courses belonging to a specific department. */
    @Query("SELECT * FROM courses WHERE departmentIndex = :deptIndex ORDER BY dayIndex, timeSlotIndex")
    fun getCoursesByDepartment(deptIndex: Int): LiveData<List<Course>>

    /** Clear the whole table before a fresh import. */
    @Query("DELETE FROM courses")
    suspend fun deleteAll()

    /**
     * Atomic transaction: delete all courses then insert new ones.
     * This ensures the database is the single source of truth.
     */
    @Transaction
    suspend fun replaceAllInTransaction(courses: List<Course>) {
        deleteAll()
        if (courses.isNotEmpty()) {
            insertAll(courses)
        }
    }
}
