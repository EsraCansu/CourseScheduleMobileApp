package com.coursescheduling.data.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.coursescheduling.domain.model.LecturerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LecturerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(lecturers: List<LecturerEntity>)

    @androidx.room.Update
    suspend fun updateLecturer(lecturer: LecturerEntity)

    @Query("SELECT * FROM lecturers ORDER BY lecturerName")
    fun getAllLecturersFlow(): Flow<List<LecturerEntity>>

    @Query("SELECT * FROM lecturers ORDER BY lecturerName")
    fun getAllLecturers(): LiveData<List<LecturerEntity>>

    @Query("SELECT * FROM lecturers WHERE lecturerId = :lecturerId LIMIT 1")
    suspend fun getLecturerById(lecturerId: String): LecturerEntity?

    @Query("SELECT * FROM lecturers WHERE email = :email LIMIT 1")
    suspend fun getLecturerByEmail(email: String): LecturerEntity?

    @Query("SELECT * FROM lecturers WHERE lecturerName = :name LIMIT 1")
    suspend fun getLecturerByName(name: String): LecturerEntity?

    @Query("DELETE FROM lecturers")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAllInTransaction(lecturers: List<LecturerEntity>) {
        deleteAll()
        if (lecturers.isNotEmpty()) {
            insertAll(lecturers)
        }
    }
}
