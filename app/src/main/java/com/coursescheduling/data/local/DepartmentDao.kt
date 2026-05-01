package com.coursescheduling.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.coursescheduling.domain.model.DepartmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DepartmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(departments: List<DepartmentEntity>)

    @Query("SELECT * FROM departments ORDER BY departmentName ASC")
    fun getAllDepartmentsFlow(): Flow<List<DepartmentEntity>>

    @Query("SELECT * FROM departments WHERE departmentName = :name LIMIT 1")
    suspend fun getDepartmentByName(name: String): DepartmentEntity?

    @Query("DELETE FROM departments")
    suspend fun deleteAll()
}
