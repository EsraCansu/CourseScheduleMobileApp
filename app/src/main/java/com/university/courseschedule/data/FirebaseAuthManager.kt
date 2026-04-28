package com.university.courseschedule.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.university.courseschedule.data.model.Role
import com.university.courseschedule.data.model.User
import com.university.courseschedule.data.model.Department
import kotlinx.coroutines.tasks.await

/**
 * FirebaseAuthManager handles Firebase Authentication operations.
 *
 * This class is the Firebase layer for authentication — it works alongside
 * the existing [AuthManager] (SharedPrefs). During Phase 4 migration, this
 * class will gradually replace SharedPrefs-based auth.
 *
 * Current responsibility:
 *  - Firebase sign-in / sign-out
 *  - Firebase user creation
 *  - Syncing user profile to Firestore "users" collection
 *  - Reading user role from Firestore
 */
class FirebaseAuthManager private constructor() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // ───────────────────────────────────────────────────────────────
    // Session state
    // ───────────────────────────────────────────────────────────────

    /** Returns true if a Firebase session is currently active. */
    fun isLoggedIn(): Boolean = auth.currentUser != null

    /** Returns the raw Firebase user, or null if not signed in. */
    fun getFirebaseUser(): FirebaseUser? = auth.currentUser

    /** Returns the Firebase UID of the current user, or null. */
    fun getCurrentUid(): String? = auth.currentUser?.uid

    // ───────────────────────────────────────────────────────────────
    // Authentication
    // ───────────────────────────────────────────────────────────────

    /**
     * Signs in with email and password via Firebase Auth.
     *
     * @return [FirebaseUser] on success, null on failure.
     */
    suspend fun signIn(email: String, password: String): FirebaseUser? {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Creates a new Firebase Auth account and writes the user profile
     * to Firestore "users" collection.
     *
     * @return The UID of the newly created user, or null on failure.
     */
    suspend fun signUp(
        email: String,
        password: String,
        name: String,
        surname: String,
        role: String,
        department: String
    ): String? {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return null

            // Write user profile to Firestore "users" collection
            val userDoc = mapOf(
                "userId"            to uid,
                "name"              to name,
                "surname"           to surname,
                "email"             to email,
                "role"              to role.uppercase(),
                "department"        to department,
                "mustChangePassword" to (role.uppercase() == "LECTURER"),
                "createdAt"         to System.currentTimeMillis()
            )
            db.collection(COLLECTION_USERS).document(uid).set(userDoc).await()
            uid
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Signs out from Firebase Auth.
     */
    fun signOut() {
        auth.signOut()
    }

    // ───────────────────────────────────────────────────────────────
    // Firestore user profile
    // ───────────────────────────────────────────────────────────────

    /**
     * Fetches the full User profile from Firestore for the given UID.
     * Returns null if the document doesn't exist or a network error occurs.
     */
    suspend fun getUserProfile(uid: String): User? {
        return try {
            val doc = db.collection(COLLECTION_USERS).document(uid).get().await()
            if (!doc.exists()) return null

            val roleStr  = doc.getString("role") ?: "LECTURER"
            val deptStr  = doc.getString("department") ?: ""
            val role = try { Role.valueOf(roleStr.uppercase()) } catch (e: Exception) { Role.LECTURER }
            val dept = Department.values().find { it.displayName == deptStr }
                ?: Department.COMPUTER

            User(
                id           = uid,
                name         = doc.getString("name") ?: "",
                surname      = doc.getString("surname") ?: "",
                email        = doc.getString("email") ?: "",
                department   = dept,
                role         = role,
                isFirstLogin = doc.getBoolean("mustChangePassword") ?: false
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Updates the user's profile fields in Firestore.
     */
    suspend fun updateProfile(uid: String, name: String, surname: String, department: String): Boolean {
        return try {
            db.collection(COLLECTION_USERS).document(uid).update(
                mapOf(
                    "name"       to name,
                    "surname"    to surname,
                    "department" to department
                )
            ).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Clears the mustChangePassword flag after the lecturer changes their password.
     */
    suspend fun clearFirstLoginFlag(uid: String): Boolean {
        return try {
            db.collection(COLLECTION_USERS).document(uid)
                .update("mustChangePassword", false)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Changes the password for the currently authenticated user via Firebase Auth.
     * Also clears mustChangePassword in Firestore on success.
     */
    suspend fun changePassword(newPassword: String): Boolean {
        return try {
            val user = auth.currentUser ?: return false
            user.updatePassword(newPassword).await()
            clearFirstLoginFlag(user.uid)
            true
        } catch (e: Exception) {
            false
        }
    }

    // ───────────────────────────────────────────────────────────────
    // Admin seeding
    // ───────────────────────────────────────────────────────────────

    /**
     * Ensures the Admin account exists in Firebase Auth and Firestore.
     * Called once on app startup from Application class.
     *
     * If the admin account does not exist in Firebase, it is created
     * using the credentials from BuildConfig (gradle.properties).
     */
    suspend fun ensureAdminExists(adminEmail: String, adminPassword: String): Boolean {
        return try {
            // Try to sign in as admin to check existence
            val signInResult = auth.signInWithEmailAndPassword(adminEmail, adminPassword).await()
            val uid = signInResult.user?.uid

            if (uid != null) {
                // Check if Firestore doc exists; create if missing
                val doc = db.collection(COLLECTION_USERS).document(uid).get().await()
                if (!doc.exists()) {
                    db.collection(COLLECTION_USERS).document(uid).set(
                        mapOf(
                            "userId"             to uid,
                            "name"               to AuthManager.ADMIN_NAME,
                            "surname"            to AuthManager.ADMIN_SURNAME,
                            "email"              to adminEmail,
                            "role"               to "ADMIN",
                            "department"         to "COMPUTER",
                            "mustChangePassword" to false,
                            "createdAt"          to System.currentTimeMillis()
                        )
                    ).await()
                }
                // Sign back out — we only checked existence
                auth.signOut()
            }
            true
        } catch (e: com.google.firebase.auth.FirebaseAuthInvalidUserException) {
            // Admin doesn't exist → create account
            try {
                val result = auth.createUserWithEmailAndPassword(adminEmail, adminPassword).await()
                val uid = result.user?.uid ?: return false
                db.collection(COLLECTION_USERS).document(uid).set(
                    mapOf(
                        "userId"             to uid,
                        "name"               to AuthManager.ADMIN_NAME,
                        "surname"            to AuthManager.ADMIN_SURNAME,
                        "email"              to adminEmail,
                        "role"               to "ADMIN",
                        "department"         to "COMPUTER",
                        "mustChangePassword" to false,
                        "createdAt"          to System.currentTimeMillis()
                    )
                ).await()
                auth.signOut()
                true
            } catch (inner: Exception) {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        private const val COLLECTION_USERS = "users"

        @Volatile
        private var INSTANCE: FirebaseAuthManager? = null

        fun getInstance(): FirebaseAuthManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebaseAuthManager().also { INSTANCE = it }
            }
        }
    }
}
