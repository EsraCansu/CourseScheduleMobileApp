package com.coursescheduling.data

import com.coursescheduling.domain.model.Course
import com.coursescheduling.domain.model.Department

// ── Type aliases ─────────────────────────────────────────────────────────────
// These aliases give the raw Array types meaningful names throughout the codebase.

/** The full schedule matrix: [department 0-4][day 0-4][timeSlot 0-8] */
typealias ScheduleMatrix = Array<Array<Array<Course?>>>

/** One department's week: [day 0-4][timeSlot 0-8] */
typealias DaySchedule = Array<Array<Course?>>

/** One day's 9 time slots: [timeSlot 0-8] */
typealias TimeSlotRow = Array<Course?>

/**
 * Singleton repository that owns the in-memory 3D schedule matrix.
 *
 * Matrix dimensions: [Department (5)][Day (5)][TimeSlot (9)]
 *
 *   Department index — matches [Department.ordinal]:
 *     0 = Computer, 1 = Electrical, 2 = Mechanical,
 *     3 = Aeronautical, 4 = Agricultural
 *
 *   Day index:
 *     0 = Monday, 1 = Tuesday, 2 = Wednesday, 3 = Thursday, 4 = Friday
 *
 *   TimeSlot index:
 *     0 = 08:00, 1 = 09:00, ..., 8 = 17:00
 *
 * All read/write operations are O(1) via direct index access.
 */
object ScheduleMatrixManager {

    const val DEPARTMENT_COUNT = 5
    const val DAY_COUNT = 5
    const val TIME_SLOT_COUNT = 9

    val dayLabels = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
    val timeSlotLabels = listOf(
        "08:00–09:00",
        "09:00–10:00",
        "10:00–11:00",
        "11:00–12:00",
        "13:00–14:00",
        "14:00–15:00",
        "15:00–16:00",
        "16:00–17:00",
        "17:00–18:00"
    )

    // 3D matrix initialised with null (no course assigned).
    // Typed via ScheduleMatrix typealias for readability.
    private val matrix: ScheduleMatrix =
        Array(DEPARTMENT_COUNT) { Array(DAY_COUNT) { arrayOfNulls(TIME_SLOT_COUNT) } }

    // ── Read ──────────────────────────────────────────────────────────────────

    /** Returns the [Course] at the given cell, or null if the slot is empty. */
    fun getCourse(department: Int, day: Int, timeSlot: Int): Course? {
        requireValidIndices(department, day, timeSlot)
        return matrix[department][day][timeSlot]
    }

    /**
     * Returns the full 5×2 schedule for [department] as a [DaySchedule]
     * ([day][timeSlot]). The caller receives a direct reference — mutations
     * will affect the matrix. Use [getCourse]/[setCourse] for safety.
     */
    fun getDepartmentSchedule(department: Int): DaySchedule {
        require(department in 0 until DEPARTMENT_COUNT) {
            "Department index $department out of range [0, $DEPARTMENT_COUNT)"
        }
        return matrix[department]
    }

    /** Convenience overload that accepts a [Department] enum value. */
    fun getDepartmentSchedule(department: Department): DaySchedule =
        getDepartmentSchedule(department.ordinal)

    /**
     * Returns every [Course] in the matrix whose [Course.lecturerID] matches
     * [lecturerID], together with its position as a [ScheduleEntry].
     */
    fun getCoursesByLecturer(lecturerID: String): List<ScheduleEntry> {
        val results = mutableListOf<ScheduleEntry>()
        for (dept in 0 until DEPARTMENT_COUNT) {
            for (day in 0 until DAY_COUNT) {
                for (slot in 0 until TIME_SLOT_COUNT) {
                    val course = matrix[dept][day][slot]
                    if (course != null && course.lecturerID == lecturerID) {
                        results.add(ScheduleEntry(dept, day, slot, course))
                    }
                }
            }
        }
        return results
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    /** Places (or removes when [course] is null) a course in the matrix. */
    fun setCourse(department: Int, day: Int, timeSlot: Int, course: Course?) {
        requireValidIndices(department, day, timeSlot)
        matrix[department][day][timeSlot] = course
    }

    /** Convenience overload that accepts a [Department] enum value. */
    fun setCourse(department: Department, day: Int, timeSlot: Int, course: Course?) =
        setCourse(department.ordinal, day, timeSlot, course)

    /** Clears all slots for a single department. */
    fun clearDepartmentSchedule(department: Int) {
        require(department in 0 until DEPARTMENT_COUNT) {
            "Department index $department out of range [0, $DEPARTMENT_COUNT)"
        }
        for (day in 0 until DAY_COUNT)
            for (slot in 0 until TIME_SLOT_COUNT)
                matrix[department][day][slot] = null
    }

    /** Resets the entire matrix to all-null. */
    fun clearAll() {
        for (dept in 0 until DEPARTMENT_COUNT) clearDepartmentSchedule(dept)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun requireValidIndices(department: Int, day: Int, timeSlot: Int) {
        require(department in 0 until DEPARTMENT_COUNT) {
            "Department index $department out of range [0, $DEPARTMENT_COUNT)"
        }
        require(day in 0 until DAY_COUNT) {
            "Day index $day out of range [0, $DAY_COUNT)"
        }
        require(timeSlot in 0 until TIME_SLOT_COUNT) {
            "TimeSlot index $timeSlot out of range [0, $TIME_SLOT_COUNT)"
        }
    }

    // ── Model ─────────────────────────────────────────────────────────────────

    /** Wraps a [Course] together with its matrix coordinates. */
    data class ScheduleEntry(
        val departmentIndex: Int,
        val dayIndex: Int,
        val timeSlotIndex: Int,
        val course: Course
    ) {
        val department: Department get() = Department.values().getOrElse(departmentIndex) { Department.COMPUTER }
        val dayLabel: String get() = dayLabels[dayIndex]
        val timeSlotLabel: String get() = timeSlotLabels[timeSlotIndex]
    }
}
