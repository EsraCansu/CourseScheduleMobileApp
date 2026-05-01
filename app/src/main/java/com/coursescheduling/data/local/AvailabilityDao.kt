package com.coursescheduling.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.coursescheduling.domain.model.AvailabilityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AvailabilityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(availability: List<AvailabilityEntity>)

    @Query("SELECT * FROM lecturer_availability WHERE lecturerId = :lecturerId")
    fun getAvailabilityByLecturer(lecturerId: String): Flow<List<AvailabilityEntity>>

    @Query("SELECT * FROM lecturer_availability")
    fun getAllAvailability(): Flow<List<AvailabilityEntity>>

    @Query("DELETE FROM lecturer_availability WHERE lecturerId = :lecturerId")
    suspend fun clearLecturerAvailability(lecturerId: String)
}
