package com.university.courseschedule.data

import androidx.lifecycle.LiveData
import com.university.courseschedule.data.db.LecturerDao
import com.university.courseschedule.data.model.Lecturer

/**
 * Single source of truth for lecturer data.
 * Fragments and ViewModels interact exclusively through this class.
 */
class LecturerRepository(private val dao: LecturerDao) {

    val allLecturers: LiveData<List<Lecturer>> = dao.getAllLecturers()

    /**
     * Replace the entire lecturer table with [lecturers] in one transaction.
     */
    suspend fun replaceAll(lecturers: List<Lecturer>) {
        dao.deleteAll()
        dao.insertAll(lecturers)
    }

    /**
     * Get a specific lecturer by ID.
     */
    suspend fun getLecturerById(lecturerID: String): Lecturer? =
        dao.getLecturerById(lecturerID)

    /**
     * Get a specific lecturer by email.
     */
    suspend fun getLecturerByEmail(email: String): Lecturer? =
        dao.getLecturerByEmail(email)
}
