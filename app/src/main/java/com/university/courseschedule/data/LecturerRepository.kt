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
     * Insert or replace lecturers without clearing existing data.
     */
    suspend fun insertAll(lecturers: List<Lecturer>) {
        dao.insertAll(lecturers)
    }

    /**
     * Replace the entire lecturer table with [lecturers] in one atomic transaction.
     * This performs deleteAll() followed by insertAll(newLecturers) within a transaction.
     */
    suspend fun replaceAll(lecturers: List<Lecturer>) {
        dao.replaceAllInTransaction(lecturers)
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

    /**
     * Get a specific lecturer by name.
     * Used for duplicate detection before import.
     */
    suspend fun getLecturerByName(name: String): Lecturer? =
        dao.getLecturerByName(name)
}
