package com.university.courseschedule.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.university.courseschedule.data.model.Course
import com.university.courseschedule.data.model.Lecturer
import kotlinx.coroutines.tasks.await

/**
 * FirestoreManager handles all cloud sync operations with Firebase Firestore.
 * 
 * This manager provides:
 * - Push local data to cloud (Admin feature)
 * - Pull data from cloud to local
 * - Real-time sync listeners
 * - Conflict resolution for cloud data
 * 
 * Note: Requires valid google-services.json in app/ directory.
 * Get your config from Firebase Console > Project Settings > Your Apps.
 */
class FirestoreManager private constructor() {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    
    companion object {
        private const val TAG = "FirestoreManager"
        
        // Collection names in Firestore
        private const val COLLECTION_COURSES = "courses"
        private const val COLLECTION_LECTURERS = "lecturers"
        private const val COLLECTION_SCHEDULE = "schedule"
        
        @Volatile
        private var INSTANCE: FirestoreManager? = null
        
        fun getInstance(): FirestoreManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirestoreManager().also { INSTANCE = it }
            }
        }
        
        /**
         * Checks if Firebase is properly configured.
         * Returns true if google-services.json is present and valid.
         */
        fun isFirebaseConfigured(): Boolean {
            return try {
                // Attempt to get Firestore instance - will throw if not configured
                FirebaseFirestore.getInstance()
                true
            } catch (e: Exception) {
                Log.w(TAG, "Firebase not configured: ${e.message}")
                false
            }
        }
    }
    
    // ==================== PUSH TO CLOUD (Admin) ====================
    
    /**
     * Pushes all courses to Firestore.
     * Uses batch writes for efficiency.
     * 
     * @param courses List of courses to upload
     * @return true if successful, false otherwise
     */
    suspend fun pushCoursesToCloud(courses: List<Course>): Boolean {
        if (!isFirebaseConfigured()) {
            Log.w(TAG, "Cannot push courses - Firebase not configured")
            return false
        }
        
        return try {
            val batch = db.batch()
            
            courses.forEach { course ->
                val docRef = db.collection(COLLECTION_COURSES)
                    .document(course.courseCode)
                batch.set(docRef, courseToMap(course), SetOptions.merge())
            }
            
            batch.commit().await()
            Log.d(TAG, "Successfully pushed ${courses.size} courses to cloud")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error pushing courses to cloud", e)
            false
        }
    }
    
    /**
     * Pushes all lecturers to Firestore.
     * 
     * @param lecturers List of lecturers to upload
     * @return true if successful, false otherwise
     */
    suspend fun pushLecturersToCloud(lecturers: List<Lecturer>): Boolean {
        if (!isFirebaseConfigured()) {
            Log.w(TAG, "Cannot push lecturers - Firebase not configured")
            return false
        }
        
        return try {
            val batch = db.batch()
            
            lecturers.forEach { lecturer ->
                val docRef = db.collection(COLLECTION_LECTURERS)
                    .document(lecturer.lecturerID)
                batch.set(docRef, lecturerToMap(lecturer), SetOptions.merge())
            }
            
            batch.commit().await()
            Log.d(TAG, "Successfully pushed ${lecturers.size} lecturers to cloud")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error pushing lecturers to cloud", e)
            false
        }
    }
    
    /**
     * Pushes schedule matrix to Firestore.
     * Stores as nested structure: department > day > timeSlot
     * 
     * @param courses List of courses representing the schedule
     * @return true if successful, false otherwise
     */
    suspend fun pushScheduleToCloud(courses: List<Course>): Boolean {
        if (!isFirebaseConfigured()) {
            Log.w(TAG, "Cannot push schedule - Firebase not configured")
            return false
        }
        
        return try {
            // Group courses by department, day, timeSlot
            val scheduleData = mutableMapOf<String, Any>()
            
            courses.forEach { course ->
                val key = "dept_${course.departmentIndex}_day_${course.dayIndex}_slot_${course.timeSlotIndex}"
                scheduleData[key] = courseToMap(course)
            }
            
            db.collection(COLLECTION_SCHEDULE)
                .document("schedule_matrix")
                .set(scheduleData)
                .await()
            
            Log.d(TAG, "Successfully pushed schedule to cloud")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error pushing schedule to cloud", e)
            false
        }
    }
    
    /**
     * Complete sync: pushes courses, lecturers, and schedule to cloud.
     * Admin use case - imports data from Excel and pushes to cloud.
     * 
     * @param courses List of courses
     * @param lecturers List of lecturers
     * @return true if all operations successful
     */
    suspend fun pushAllToCloud(courses: List<Course>, lecturers: List<Lecturer>): Boolean {
        val coursesResult = pushCoursesToCloud(courses)
        val lecturersResult = pushLecturersToCloud(lecturers)
        val scheduleResult = pushScheduleToCloud(courses)
        
        return coursesResult && lecturersResult && scheduleResult
    }
    
    // ==================== PULL FROM CLOUD ====================
    
    /**
     * Pulls all courses from Firestore.
     * 
     * @return List of courses from cloud, or empty list on error
     */
    suspend fun pullCoursesFromCloud(): List<Course> {
        if (!isFirebaseConfigured()) {
            Log.w(TAG, "Cannot pull courses - Firebase not configured")
            return emptyList()
        }
        
        return try {
            val snapshot = db.collection(COLLECTION_COURSES).get().await()
            snapshot.documents.mapNotNull { doc ->
                try {
                    mapToCourse(doc.data ?: return@mapNotNull null)
                } catch (e: Exception) {
                    Log.w(TAG, "Error parsing course document: ${doc.id}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pulling courses from cloud", e)
            emptyList()
        }
    }
    
    /**
     * Pulls all lecturers from Firestore.
     * 
     * @return List of lecturers from cloud, or empty list on error
     */
    suspend fun pullLecturersFromCloud(): List<Lecturer> {
        if (!isFirebaseConfigured()) {
            Log.w(TAG, "Cannot pull lecturers - Firebase not configured")
            return emptyList()
        }
        
        return try {
            val snapshot = db.collection(COLLECTION_LECTURERS).get().await()
            snapshot.documents.mapNotNull { doc ->
                try {
                    mapToLecturer(doc.data ?: return@mapNotNull null)
                } catch (e: Exception) {
                    Log.w(TAG, "Error parsing lecturer document: ${doc.id}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pulling lecturers from cloud", e)
            emptyList()
        }
    }
    
    /**
     * Pulls schedule matrix from Firestore.
     * 
     * @return List of courses representing the schedule
     */
    suspend fun pullScheduleFromCloud(): List<Course> {
        if (!isFirebaseConfigured()) {
            Log.w(TAG, "Cannot pull schedule - Firebase not configured")
            return emptyList()
        }
        
        return try {
            val doc = db.collection(COLLECTION_SCHEDULE)
                .document("schedule_matrix")
                .get()
                .await()
            
            val data = doc.data ?: return emptyList()
            data.values.mapNotNull { value ->
                try {
                    @Suppress("UNCHECKED_CAST")
                    mapToCourse(value as Map<String, Any>)
                } catch (e: Exception) {
                    Log.w(TAG, "Error parsing schedule entry", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pulling schedule from cloud", e)
            emptyList()
        }
    }
    
    // ==================== HELPER METHODS ====================
    
    private fun courseToMap(course: Course): Map<String, Any> {
        return mapOf(
            "id" to course.id,
            "courseCode" to course.courseCode,
            "courseName" to course.courseName,
            "departmentIndex" to course.departmentIndex,
            "dayIndex" to course.dayIndex,
            "timeSlotIndex" to course.timeSlotIndex,
            "lecturerID" to course.lecturerID,
            "lecturerName" to course.lecturerName
            // Password removed for security - never store passwords in cloud
        )
    }
    
    private fun lecturerToMap(lecturer: Lecturer): Map<String, Any> {
        return mapOf(
            "id" to lecturer.id,
            "lecturerID" to lecturer.lecturerID,
            "lecturerName" to lecturer.lecturerName,
            "email" to lecturer.email,
            "departmentIndex" to lecturer.departmentIndex,
            "imageUrl" to lecturer.imageUrl,
            "courseCode" to lecturer.courseCode
            // Password removed for security - never store passwords in cloud
        )
    }
    
    @Suppress("UNCHECKED_CAST")
    private fun mapToCourse(data: Map<String, Any>): Course {
        return Course(
            id = (data["id"] as? Number)?.toInt() ?: 0,
            courseCode = data["courseCode"] as? String ?: "",
            courseName = data["courseName"] as? String ?: "",
            departmentIndex = (data["departmentIndex"] as? Number)?.toInt() ?: 0,
            dayIndex = (data["dayIndex"] as? Number)?.toInt() ?: 0,
            timeSlotIndex = (data["timeSlotIndex"] as? Number)?.toInt() ?: 0,
            lecturerID = data["lecturerID"] as? String ?: "",
            lecturerName = data["lecturerName"] as? String ?: ""
        )
    }
    
    @Suppress("UNCHECKED_CAST")
    private fun mapToLecturer(data: Map<String, Any>): Lecturer {
        return Lecturer(
            id = (data["id"] as? Number)?.toInt() ?: 0,
            lecturerID = data["lecturerID"] as? String ?: "",
            lecturerName = data["lecturerName"] as? String ?: "",
            email = data["email"] as? String ?: "",
            departmentIndex = (data["departmentIndex"] as? Number)?.toInt() ?: 0,
            imageUrl = data["imageUrl"] as? String ?: "",
            courseCode = data["courseCode"] as? String ?: ""
        )
    }
}
