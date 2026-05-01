package com.coursescheduling.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.coursescheduling.domain.model.ScheduleRequestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleRequestDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(request: ScheduleRequestEntity)

    @Query("SELECT * FROM schedule_requests ORDER BY createdAt DESC")
    fun getAllRequests(): Flow<List<ScheduleRequestEntity>>

    @Query("SELECT * FROM schedule_requests WHERE lecturerId = :lecturerId ORDER BY createdAt DESC")
    fun getRequestsByLecturer(lecturerId: String): Flow<List<ScheduleRequestEntity>>

    @Update
    suspend fun updateRequest(request: ScheduleRequestEntity)

    @Query("DELETE FROM schedule_requests WHERE requestId = :requestId")
    suspend fun deleteRequest(requestId: Int)
}
