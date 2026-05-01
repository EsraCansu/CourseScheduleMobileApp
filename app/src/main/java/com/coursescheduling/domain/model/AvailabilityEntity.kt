package com.coursescheduling.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lecturer_availability")
data class AvailabilityEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val lecturerId: String,
    val dayIndex: Int,
    val slotIndex: Int,
    val isAvailable: Boolean = true
)
