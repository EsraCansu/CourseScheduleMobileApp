package com.coursescheduling.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a Lecturer for the local-only architecture.
 * This entity stores authentication credentials securely.
 */
@Entity(tableName = "lecturers")
data class LecturerEntity(
    @PrimaryKey val lecturerId: String,
    val lecturerName: String,
    val lecturerTitle: String? = null,
    val email: String,
    val department: String,
    val officeRoom: String? = null,
    val passwordHash: String,
    val mustChangePassword: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
