package com.university.courseschedule.data.model

/**
 * Cell value stored inside the 3D schedule matrix.
 *
 * @property courseCode   Short identifier, e.g. "CS101".
 * @property courseName   Full name of the course.
 * @property lecturerName Display name of the lecturer teaching this course.
 * @property lecturerID   UUID of the lecturer — matches [User.id] for O(1) filtering.
 */
data class Course(
    val courseCode: String,
    val courseName: String,
    val lecturerName: String,
    val lecturerID: String
)
