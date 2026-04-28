package com.university.courseschedule.data

import com.university.courseschedule.data.model.Course
import com.university.courseschedule.data.model.Lecturer

/**
 * Represents a conflict detected during data import.
 */
data class ImportConflict(
    val type: ConflictType,
    val existingItem: Any,       // Course or Lecturer being replaced
    val newItem: Any,            // Course or Lecturer from import
    val description: String
)

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
        
        // Create a map of existing courses by their schedule key
        val existingByKey = existingCourses.associateBy { 
            "${it.departmentIndex}_${it.dayIndex}_${it.timeSlotIndex}" 
        }
        
        for (newCourse in newCourses) {
            val key = "${newCourse.departmentIndex}_${newCourse.dayIndex}_${newCourse.timeSlotIndex}"
            val existingCourse = existingByKey[key]
            
            if (existingCourse != null) {
                // Check if the course is actually different
                if (existingCourse.courseCode != newCourse.courseCode || 
                    existingCourse.courseName != newCourse.courseName) {
                    conflicts.add(
                        ImportConflict(
                            type = ConflictType.COURSE_CONFLICT,
                            existingItem = existingCourse,
                            newItem = newCourse,
                            description = "Slot ${getDayLabel(newCourse.dayIndex)} ${getTimeSlotLabel(newCourse.timeSlotIndex)}: '${existingCourse.courseCode}' → '${newCourse.courseCode}'"
                        )
                    )
                }
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
        existingLecturers: List<Lecturer>,
        newLecturers: List<Lecturer>
    ): List<ImportConflict> {
        val conflicts = mutableListOf<ImportConflict>()
        
        // Create a map of existing lecturers by name for O(1) lookups
        val existingByName = existingLecturers.associateBy { it.lecturerName.lowercase() }
        
        for (newLecturer in newLecturers) {
            val existingLecturer = existingByName[newLecturer.lecturerName.lowercase()]
            
            if (existingLecturer != null) {
                // Check if any details differ
                if (existingLecturer.email != newLecturer.email ||
                    existingLecturer.departmentIndex != newLecturer.departmentIndex) {
                    conflicts.add(
                        ImportConflict(
                            type = ConflictType.LECTURER_CONFLICT,
                            existingItem = existingLecturer,
                            newItem = newLecturer,
                            description = "Lecturer '${existingLecturer.lecturerName}': ${existingLecturer.email} → ${newLecturer.email}"
                        )
                    )
                }
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
        existingLecturers: List<Lecturer>,
        newLecturers: List<Lecturer>
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
