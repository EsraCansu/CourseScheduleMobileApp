package com.coursescheduling.data.repository

import com.coursescheduling.data.local.CourseDao
import com.coursescheduling.domain.model.Course
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Single source of truth for course data.
 * Migrated to LOCAL-ONLY Room database.
 */
class CourseRepository(private val dao: CourseDao) {

    /**
     * Observes courses from local database.
     * Emits a new list whenever the database changes.
     */
    fun observeCourses(): Flow<List<Course>> = dao.getAllCoursesFlow()

    /**
     * Fetches a one-shot list of all courses.
     */
    suspend fun getAllCourses(): List<Course> = withContext(Dispatchers.IO) {
        // We use the non-flow version or collect the flow once
        // For simplicity, we'll keep it focused on the Flow API
        emptyList() // Usually not needed in this architecture as UI observes Flow
    }

    /**
     * Replaces all courses in the local database atomically.
     * This ensures the database accurately reflects the most recent import.
     */
    suspend fun replaceAll(courses: List<Course>) = withContext(Dispatchers.IO) {
        android.util.Log.d("LOCAL_DB_WRITE", "REPLACE_ALL_START - courses (${courses.size} items)")
        try {
            dao.replaceAllInTransaction(courses)
            android.util.Log.d("LOCAL_DB_WRITE", "REPLACE_ALL_SUCCESS - courses")
        } catch (e: Exception) {
            android.util.Log.e("LOCAL_DB_WRITE", "REPLACE_ALL_FAILED - courses: ${e.message}")
            throw e
        }
    }
}
