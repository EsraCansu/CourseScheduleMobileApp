package com.coursescheduling.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing an Academic Department.
 */
@Entity(tableName = "departments")
data class DepartmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val departmentName: String
)
