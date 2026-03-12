package com.university.courseschedule.data.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.university.courseschedule.data.model.Lecturer

@Dao
interface LecturerDao {

    /**
     * Insert or replace a batch of lecturers.
     * REPLACE strategy means re-importing the same file refreshes rows cleanly.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(lecturers: List<Lecturer>)

    /** All lecturers as a LiveData stream — emits whenever the table changes. */
    @Query("SELECT * FROM lecturers ORDER BY lecturerName")
    fun getAllLecturers(): LiveData<List<Lecturer>>

    /** Get a specific lecturer by lecturerID. */
    @Query("SELECT * FROM lecturers WHERE lecturerID = :lecturerID LIMIT 1")
    suspend fun getLecturerById(lecturerID: String): Lecturer?

    /** Get lecturer by email. */
    @Query("SELECT * FROM lecturers WHERE email = :email LIMIT 1")
    suspend fun getLecturerByEmail(email: String): Lecturer?

    /** Clear the whole table before a fresh import. */
    @Query("DELETE FROM lecturers")
    suspend fun deleteAll()
}
