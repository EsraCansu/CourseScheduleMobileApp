package com.coursescheduling.data.repository

import com.coursescheduling.data.local.LecturerDao
import com.coursescheduling.domain.model.LecturerEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Single source of truth for lecturer data.
 * Migrated to LOCAL-ONLY Room database.
 */
class LecturerRepository(private val dao: LecturerDao) {

    fun observeLecturers(): Flow<List<LecturerEntity>> = dao.getAllLecturersFlow()

    suspend fun insertAll(lecturers: List<LecturerEntity>) = withContext(Dispatchers.IO) {
        dao.insertAll(lecturers)
    }

    suspend fun replaceAll(lecturers: List<LecturerEntity>) = withContext(Dispatchers.IO) {
        android.util.Log.d("LOCAL_DB_WRITE", "REPLACE_ALL_START - lecturers (${lecturers.size} items)")
        try {
            dao.replaceAllInTransaction(lecturers)
            android.util.Log.d("LOCAL_DB_WRITE", "REPLACE_ALL_SUCCESS - lecturers")
        } catch (e: Exception) {
            android.util.Log.e("LOCAL_DB_WRITE", "REPLACE_ALL_FAILED - lecturers: ${e.message}")
            throw e
        }
    }

    suspend fun getLecturerByEmail(email: String): LecturerEntity? = withContext(Dispatchers.IO) {
        dao.getLecturerByEmail(email)
    }
}
