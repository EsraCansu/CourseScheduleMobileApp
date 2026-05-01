package com.coursescheduling.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedule_requests")
data class ScheduleRequestEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val lecturerId: String,
    val lecturerName: String,
    val dayIndex: Int,
    val slotIndex: Int,
    val originalCourseId: Int?,
    val reason: String,
    val desiredChange: String,
    val status: String = "Pending", // Pending, Approved, Rejected
    val createdAt: Long = System.currentTimeMillis()
)
