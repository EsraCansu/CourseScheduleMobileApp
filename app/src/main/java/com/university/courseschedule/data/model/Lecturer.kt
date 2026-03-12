package com.university.courseschedule.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a Lecturer.
 * Stores lecturer information including credentials for authentication.
 * The email is derived from the name if not provided in the Excel file.
 * Passwords can be imported via Excel or auto-generated.
 *
 * @property id             Auto-generated primary key.
 * @property lecturerID     UUID of the lecturer — matches User.id for authentication.
 * @property lecturerName   Display name of the lecturer.
 * @property email          Email address (derived from name if not in file).
 * @property password       Password for authentication (imported or auto-generated).
 * @property departmentIndex Department index (0-4) for filtering.
 */
@Entity(tableName = "lecturers")
data class Lecturer(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val lecturerID: String,
    val lecturerName: String,
    val email: String = "",
    val password: String = "",
    val departmentIndex: Int = 0
)
