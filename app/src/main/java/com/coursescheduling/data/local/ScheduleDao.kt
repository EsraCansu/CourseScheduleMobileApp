package com.coursescheduling.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.coursescheduling.domain.model.ScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(schedules: List<ScheduleEntity>)

    @Query("SELECT * FROM schedules")
    fun getAllSchedulesFlow(): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules WHERE weekday = :day")
    fun getSchedulesByDay(day: Int): Flow<List<ScheduleEntity>>

    @Query("DELETE FROM schedules")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(schedules: List<ScheduleEntity>) {
        deleteAll()
        insertAll(schedules)
    }
}
