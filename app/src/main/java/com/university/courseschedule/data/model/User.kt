package com.university.courseschedule.data.model

import java.util.UUID

/**
 * Represents an app user (Admin or Lecturer).
 *
 * @property id          Universally unique identifier, generated once on first registration.
 * @property name        First name.
 * @property surname     Last name.
 * @property email       Email address for authentication and communication.
 * @property department  The user's department.
 * @property role        ADMIN can view/manage all departments; LECTURER sees only their own courses.
 * @property isFirstLogin True until the user changes their temporary password (spec §2A).
 */
data class User(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val surname: String = "",
    val email: String = "",
    val department: Department = Department.COMPUTER,
    val role: Role = Role.LECTURER,
    val isFirstLogin: Boolean = true
) {
    /** Returns the formatted display name: "Name Surname" */
    val fullName: String get() = "$name $surname".trim()

    /**
     * Generates the login username following the spec rule:
     * lowercase first name + "_" + lowercase surname (academic titles omitted externally).
     */
    val username: String get() = "${name.lowercase()}_${surname.lowercase()}"
}
