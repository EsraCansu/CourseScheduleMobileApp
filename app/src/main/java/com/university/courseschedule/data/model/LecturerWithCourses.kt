package com.university.courseschedule.data.model

/**
 * Domain model that groups a Lecturer with all their assigned Courses.
 * Used to display the lecturer-centric UI in DataFragment.
 *
 * Each lecturer appears only once, even if they teach multiple courses
 * (e.g., Assist. Prof. Rezan Bakır teaching CNG101 and CNG342).
 */
data class LecturerWithCourses(
    val lecturer: Lecturer,
    val courses: List<Course>
)
