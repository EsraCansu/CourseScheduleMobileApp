package com.university.courseschedule.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

/**
 * Manages lecturer availability time slots.
 * 
 * Each lecturer can toggle available/busy for each slot in the 5×2 grid:
 * - 5 days (Monday-Friday)
 * - 2 time slots (Morning/Afternoon)
 * 
 * This data is stored in SharedPreferences per lecturer.
 */
class LecturerAvailabilityManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Sets the availability for a specific time slot.
     * 
     * @param lecturerId The lecturer's unique ID
     * @param dayIndex Day index (0=Monday, 4=Friday)
     * @param timeSlotIndex Time slot (0=Morning, 1=Afternoon)
     * @param isAvailable true if available, false if busy
     */
    fun setAvailability(lecturerId: String, dayIndex: Int, timeSlotIndex: Int, isAvailable: Boolean) {
        require(dayIndex in 0 until DAY_COUNT) { "Day index $dayIndex out of range" }
        require(timeSlotIndex in 0 until TIME_SLOT_COUNT) { "Time slot index $timeSlotIndex out of range" }

        val key = getKey(lecturerId, dayIndex, timeSlotIndex)
        prefs.edit().putBoolean(key, isAvailable).apply()
    }

    /**
     * Gets the availability for a specific time slot.
     * Default is true (available) if not set.
     */
    fun getAvailability(lecturerId: String, dayIndex: Int, timeSlotIndex: Int): Boolean {
        require(dayIndex in 0 until DAY_COUNT) { "Day index $dayIndex out of range" }
        require(timeSlotIndex in 0 until TIME_SLOT_COUNT) { "Time slot index $timeSlotIndex out of range" }

        val key = getKey(lecturerId, dayIndex, timeSlotIndex)
        return prefs.getBoolean(key, true) // Default to available
    }

    /**
     * Gets all availability slots for a lecturer as a 2D boolean array.
     * Returns [day][timeSlot] where true = available, false = busy.
     */
    fun getAllAvailability(lecturerId: String): Array<Array<Boolean>> {
        return Array(DAY_COUNT) { day ->
            Array(TIME_SLOT_COUNT) { slot ->
                getAvailability(lecturerId, day, slot)
            }
        }
    }

    /**
     * Clears all availability for a lecturer.
     */
    fun clearAll(lecturerId: String) {
        for (day in 0 until DAY_COUNT) {
            for (slot in 0 until TIME_SLOT_COUNT) {
                val key = getKey(lecturerId, day, slot)
                prefs.edit().remove(key).apply()
            }
        }
    }

    /**
     * Checks if a lecturer has any availability data set.
     */
    fun hasAvailabilityData(lecturerId: String): Boolean {
        for (day in 0 until DAY_COUNT) {
            for (slot in 0 until TIME_SLOT_COUNT) {
                val key = getKey(lecturerId, day, slot)
                if (prefs.contains(key)) return true
            }
        }
        return false
    }

    /**
     * Saves all availability data at once (bulk operation).
     */
    fun setAllAvailability(lecturerId: String, availability: Array<Array<Boolean>>) {
        require(availability.size == DAY_COUNT) { "Availability array must have $DAY_COUNT days" }
        require(availability[0].size == TIME_SLOT_COUNT) { "Each day must have $TIME_SLOT_COUNT time slots" }

        val editor = prefs.edit()
        for (day in 0 until DAY_COUNT) {
            for (slot in 0 until TIME_SLOT_COUNT) {
                val key = getKey(lecturerId, day, slot)
                editor.putBoolean(key, availability[day][slot])
            }
        }
        editor.apply()
    }

    /**
     * Generates the SharedPreferences key for a specific slot.
     */
    private fun getKey(lecturerId: String, dayIndex: Int, timeSlotIndex: Int): String {
        return "${KEY_PREFIX}${lecturerId}_${dayIndex}_$timeSlotIndex"
    }

    companion object {
        private const val PREFS_NAME = "lecturer_availability_prefs"
        private const val KEY_PREFIX = "availability_"
        
        const val DAY_COUNT = 5
        const val TIME_SLOT_COUNT = 2

        val dayLabels = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
        val timeSlotLabels = listOf("Morning", "Afternoon")

        @Volatile
        private var INSTANCE: LecturerAvailabilityManager? = null

        fun getInstance(context: Context): LecturerAvailabilityManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LecturerAvailabilityManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
