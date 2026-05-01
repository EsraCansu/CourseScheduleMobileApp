package com.coursescheduling.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing an individual schedule slot for a course.
 * One course can have multiple schedule slots (e.g., a 3-hour course has 3 slots).
 */
@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val weekday: Int,        // 0-4 (Monday-Friday)
    val startTime: String,   // HH:mm
    val endTime: String,     // HH:mm
    val timeSlotIndex: Int,  // 0-8 (matches matrix slots)
    val courseId: Int,       // Foreign key to Course (using id)
    val lecturerId: String,  // Foreign key to Lecturer
    val departmentId: Int    // Foreign key to Department
)
