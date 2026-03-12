package com.university.courseschedule.data.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.university.courseschedule.data.model.Lecturer

@Dao
interface LecturerDao {

    /**
     * Insert or replace a batch of lecturers.
     * REPLACE strategy means re-importing the same file refreshes rows cleanly.
     * This handles duplicates by replacing existing records with the same primary key.
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

    /**
     * Get lecturer by name.
     * Used for duplicate detection before import.
     */
    @Query("SELECT * FROM lecturers WHERE lecturerName = :name LIMIT 1")
    suspend fun getLecturerByName(name: String): Lecturer?

    /** Clear the whole table before a fresh import. */
    @Query("DELETE FROM lecturers")
    suspend fun deleteAll()

    /**
     * Atomic transaction: delete all lecturers then insert new ones.
     * This ensures the database is the single source of truth.
     */
    @Transaction
    suspend fun replaceAllInTransaction(lecturers: List<Lecturer>) {
        deleteAll()
        if (lecturers.isNotEmpty()) {
            insertAll(lecturers)
        }
    }
}
