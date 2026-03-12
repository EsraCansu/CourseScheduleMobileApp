package com.university.courseschedule.data

import androidx.lifecycle.LiveData
import com.university.courseschedule.data.db.CourseDao
import com.university.courseschedule.data.model.Course

/**
 * Single source of truth for course data.
 * Fragments and ViewModels interact exclusively through this class.
 */
class CourseRepository(private val dao: CourseDao) {

    val allCourses: LiveData<List<Course>> = dao.getAllCourses()

    fun getCoursesByLecturer(lecturerID: String): LiveData<List<Course>> =
        dao.getCoursesByLecturer(lecturerID)

    fun getCoursesByDepartment(deptIndex: Int): LiveData<List<Course>> =
        dao.getCoursesByDepartment(deptIndex)

    /**
     * Replace the entire course table with [courses] in one atomic transaction.
     * This performs deleteAll() followed by insertAll(newCourses) within a transaction.
     */
    suspend fun replaceAll(courses: List<Course>) {
        dao.replaceAllInTransaction(courses)
    }
}
