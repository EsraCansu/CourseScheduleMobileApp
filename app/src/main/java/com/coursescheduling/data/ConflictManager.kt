package com.coursescheduling.data

import com.coursescheduling.domain.model.Course
import com.coursescheduling.domain.model.LecturerEntity

/**
 * Represents a conflict detected during data import.
 */
data class ImportConflict(
    val type: ConflictType,
    val existingItem: Any,       // Course or Lecturer from DB
    val newItem: Any,            // Course or Lecturer from XLSX
    val description: String,
    var resolution: ImportResolution = ImportResolution.PENDING
)

enum class ImportResolution {
    PENDING,
    OVERWRITE,
    SKIP,
    DUPLICATE
}

enum class ConflictType {
    COURSE_CONFLICT,    // Same slot but different course
    LECTURER_CONFLICT  // Same lecturer name but different details
}

/**
 * Manages conflict detection during data import.
 * Detects conflicts between imported data and existing database records.
 */
object ConflictManager {

    /**
     * Detects conflicts between imported courses and existing courses.
     * A conflict occurs when:
     * - Same department, day, and time slot exists with DIFFERENT course code/name
     */
    fun detectCourseConflicts(
        existingCourses: List<Course>,
        newCourses: List<Course>
    ): List<ImportConflict> {
        val conflicts = mutableListOf<ImportConflict>()
        val existingByCode = existingCourses.associateBy { it.courseCode.lowercase() }
        
        for (newCourse in newCourses) {
            val existingCourse = existingByCode[newCourse.courseCode.lowercase()]
            
            if (existingCourse != null) {
                conflicts.add(
                    ImportConflict(
                        type = ConflictType.COURSE_CONFLICT,
                        existingItem = existingCourse,
                        newItem = newCourse,
                        description = "Course Code '${newCourse.courseCode}' already exists."
                    )
                )
            }
        }
        return conflicts
    }

    /**
     * Detects conflicts between imported lecturers and existing lecturers.
     * A conflict occurs when:
     * - Same lecturer name exists with DIFFERENT details (email, department, etc.)
     */
    fun detectLecturerConflicts(
        existingLecturers: List<LecturerEntity>,
        newLecturers: List<LecturerEntity>
    ): List<ImportConflict> {
        val conflicts = mutableListOf<ImportConflict>()
        val existingByEmail = existingLecturers.associateBy { it.email.lowercase() }
        
        for (newLecturer in newLecturers) {
            val existingLecturer = existingByEmail[newLecturer.email.lowercase()]
            
            if (existingLecturer != null) {
                conflicts.add(
                    ImportConflict(
                        type = ConflictType.LECTURER_CONFLICT,
                        existingItem = existingLecturer,
                        newItem = newLecturer,
                        description = "Lecturer with email '${newLecturer.email}' already exists."
                    )
                )
            }
        }
        return conflicts
    }

    /**
     * Gets all conflicts from imported data.
     */
    fun getAllConflicts(
        existingCourses: List<Course>,
        newCourses: List<Course>,
        existingLecturers: List<LecturerEntity>,
        newLecturers: List<LecturerEntity>
    ): List<ImportConflict> {
        val courseConflicts = detectCourseConflicts(existingCourses, newCourses)
        val lecturerConflicts = detectLecturerConflicts(existingLecturers, newLecturers)
        return courseConflicts + lecturerConflicts
    }

    private fun getDayLabel(dayIndex: Int): String {
        return when (dayIndex) {
            0 -> "Monday"
            1 -> "Tuesday"
            2 -> "Wednesday"
            3 -> "Thursday"
            4 -> "Friday"
            else -> "Day $dayIndex"
        }
    }

    private fun getTimeSlotLabel(slotIndex: Int): String {
        return when (slotIndex) {
            0 -> "Morning"
            1 -> "Afternoon"
            else -> "Slot $slotIndex"
        }
    }
}
