package com.coursescheduling.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedule_requests")
data class ScheduleRequestEntity(
    @PrimaryKey(autoGenerate = true) val requestId: Int = 0,
    val lecturerId: String,
    val lecturerName: String,
    val weekday: Int, // dayIndex
    val timeSlot: Int, // slotIndex
    val note: String,
    val requestType: String, // "Schedule Change" or "Override"
    val status: String = "PENDING",
    val createdAt: Long = System.currentTimeMillis()
)
