package com.university.courseschedule.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity **and** in-memory matrix cell.
 *
 * Each row encodes its full position in the 3-D schedule matrix so that
 * the database can be efficiently queried by department, day, or time slot
 * without scanning the entire table.
 *
 * @property id             Auto-generated primary key.
 * @property courseCode     Short identifier, e.g. "CS101".
 * @property courseName     Full name of the course.
 * @property lecturerName   Display name of the lecturer.
 * @property lecturerID     UUID of the lecturer — equals [User.id].
 * @property password      Password for lecturer authentication (from Excel import).
 * @property departmentIndex 0–4, matches [Department.ordinal].
 * @property dayIndex        0–4  (Monday = 0 … Friday = 4).
 * @property timeSlotIndex   0–1  (0 = Morning, 1 = Afternoon).
 */
@Entity(tableName = "courses")
data class Course(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val courseCode: String,
    val courseName: String,
    val lecturerName: String,
    val lecturerID: String,
    val password: String = "",  // Password from Excel import (stored for reference)
    val departmentIndex: Int = 0,
    val dayIndex: Int = 0,
    val timeSlotIndex: Int = 0
)
