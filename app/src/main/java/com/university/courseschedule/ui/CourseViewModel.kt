package com.university.courseschedule.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.university.courseschedule.data.CourseRepository
import com.university.courseschedule.data.LecturerRepository
import com.university.courseschedule.data.db.AppDatabase
import com.university.courseschedule.data.model.Course
import com.university.courseschedule.data.model.Lecturer
import kotlinx.coroutines.launch

/**
 * Shared ViewModel scoped to the Activity.
 *
 * Both DataFragment (writes) and CalendarFragment (reads) use this single
 * instance so the grid always reflects the latest import without extra
 * LiveData wiring between fragments.
 */
class CourseViewModel(app: Application) : AndroidViewModel(app) {

    private val courseRepository: CourseRepository = CourseRepository(
        AppDatabase.getInstance(app).courseDao()
    )

    private val lecturerRepository: LecturerRepository = LecturerRepository(
        AppDatabase.getInstance(app).lecturerDao()
    )

    /** All courses — observed by CalendarFragment for grid population. */
    val allCourses: LiveData<List<Course>> = courseRepository.allCourses

    /** All lecturers — observed by DataFragment for lecturer list. */
    val allLecturers: LiveData<List<Lecturer>> = lecturerRepository.allLecturers

    fun getCoursesByLecturer(lecturerID: String): LiveData<List<Course>> =
        courseRepository.getCoursesByLecturer(lecturerID)

    fun getCoursesByDepartment(deptIndex: Int): LiveData<List<Course>> =
        courseRepository.getCoursesByDepartment(deptIndex)

    /**
     * Called by DataFragment after parsing a file.
     * Clears the table and inserts the new list atomically.
     */
    fun replaceAllCourses(courses: List<Course>) {
        viewModelScope.launch { courseRepository.replaceAll(courses) }
    }

    /**
     * Called by DataFragment after parsing a file with lecturer data.
     * Clears the table and inserts the new list atomically.
     */
    fun replaceAllLecturers(lecturers: List<Lecturer>) {
        viewModelScope.launch { lecturerRepository.replaceAll(lecturers) }
    }

    /**
     * Convenience method to replace both courses and lecturers at once.
     */
    fun replaceAll(courses: List<Course>, lecturers: List<Lecturer>) {
        viewModelScope.launch {
            courseRepository.replaceAll(courses)
            lecturerRepository.replaceAll(lecturers)
        }
    }
}
