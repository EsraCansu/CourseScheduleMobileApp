package com.university.courseschedule.data.model

import java.util.UUID

/**
 * Represents an app user (Admin or Lecturer).
 *
 * @property id          Universally unique identifier, generated once on first registration.
 * @property name        First name.
 * @property surname     Last name.
 * @property department  The user's department.
 * @property role        ADMIN can view/manage all departments; LECTURER sees only their own courses.
 * @property isRegistered  False until the user completes the Settings form for the first time.
 */
data class User(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val surname: String = "",
    val department: Department = Department.COMPUTER,
    val role: Role = Role.LECTURER,
    val isRegistered: Boolean = false
) {
    /** Returns the formatted display name: "Name Surname" */
    val fullName: String get() = "$name $surname".trim()

    /**
     * Generates the login username following the spec rule:
     * lowercase first name + "_" + lowercase surname (academic titles omitted externally).
     */
    val username: String get() = "${name.lowercase()}_${surname.lowercase()}"
}
