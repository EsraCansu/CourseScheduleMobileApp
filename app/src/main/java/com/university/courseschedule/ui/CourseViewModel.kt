package com.university.courseschedule.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.university.courseschedule.data.CourseRepository
import com.university.courseschedule.data.LecturerRepository
import com.university.courseschedule.data.ScheduleMatrixManager
import com.university.courseschedule.data.db.AppDatabase
import com.university.courseschedule.data.model.Course
import com.university.courseschedule.data.model.Lecturer
import com.university.courseschedule.data.model.LecturerWithCourses
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

    /**
     * Grouped view: each Lecturer with their assigned Courses.
     * Merges allLecturers + allCourses reactively via MediatorLiveData.
     * Each lecturer appears only once, even if they teach multiple courses.
     */
    val lecturersWithCourses: LiveData<List<LecturerWithCourses>> = MediatorLiveData<List<LecturerWithCourses>>().apply {
        fun merge() {
            val lecturers = allLecturers.value ?: emptyList()
            val courses = allCourses.value ?: emptyList()
            val coursesByLecturer = courses.groupBy { it.lecturerName }
            value = lecturers.map { lecturer ->
                LecturerWithCourses(
                    lecturer = lecturer,
                    courses = coursesByLecturer[lecturer.lecturerName] ?: emptyList()
                )
            }
        }
        addSource(allLecturers) { merge() }
        addSource(allCourses) { merge() }
    }

    /** Tracks whether data has been imported and processed. */
    private val _isDataImported = MutableLiveData(false)
    val isDataImported: LiveData<Boolean> = _isDataImported

    fun getCoursesByLecturer(lecturerID: String): LiveData<List<Course>> =
        courseRepository.getCoursesByLecturer(lecturerID)

    fun getCoursesByDepartment(deptIndex: Int): LiveData<List<Course>> =
        courseRepository.getCoursesByDepartment(deptIndex)

    /**
     * Called by DataFragment after parsing a file.
     * Clears the table and inserts the new list atomically.
     */
    fun replaceAllCourses(courses: List<Course>) {
        viewModelScope.launch { 
            courseRepository.replaceAll(courses)
            syncMatrixWithDatabase(courses)
        }
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
     * This also synchronizes the ScheduleMatrixManager with the Room Database.
     */
    fun replaceAll(courses: List<Course>, lecturers: List<Lecturer>) {
        viewModelScope.launch {
            courseRepository.replaceAll(courses)
            lecturerRepository.replaceAll(lecturers)
            // Sync matrix after database update
            syncMatrixWithDatabase(courses)
            _isDataImported.postValue(true)
        }
    }

    /**
     * Synchronizes the ScheduleMatrixManager with the Room Database.
     * This is called whenever the database is updated to ensure CalendarFragment
     * renders the correct schedule.
     */
    private fun syncMatrixWithDatabase(courses: List<Course>) {
        ScheduleMatrixManager.clearAll()
        courses.forEach { course ->
            ScheduleMatrixManager.setCourse(
                course.departmentIndex,
                course.dayIndex,
                course.timeSlotIndex,
                course
            )
        }
    }

    /**
     * Loads matrix from database - used on app startup to restore matrix state.
     * This ensures navigation to CalendarFragment shows data immediately.
     */
    fun loadMatrixFromDatabase() {
        viewModelScope.launch {
            val courses = allCourses.value
            if (!courses.isNullOrEmpty()) {
                syncMatrixWithDatabase(courses)
            }
        }
    }

    /**
     * Insert lecturers without clearing existing records.
     * Use when you want to append rather than replace.
     */
    fun insertLecturers(lecturers: List<Lecturer>) {
        viewModelScope.launch {
            lecturerRepository.insertAll(lecturers)
            _isDataImported.postValue(true)
        }
    }

    /**
     * Resets the data import state.
     */
    fun resetDataImportState() {
        _isDataImported.value = false
    }

    /**
     * Get a lecturer by name for duplicate detection.
     */
    suspend fun getLecturerByName(name: String): Lecturer? {
        return lecturerRepository.getLecturerByName(name)
    }
}
