package com.university.courseschedule.data.model

/**
 * Represents one of the 5 supported university departments.
 * The ordinal value is used as the department index in the schedule matrix.
 */
enum class Department(val displayName: String) {
    COMPUTER("Computer Engineering"),
    ELECTRICAL("Electrical Engineering"),
    MECHANICAL("Mechanical Engineering"),
    AERONAUTICAL("Aeronautical Engineering"),
    AGRICULTURAL("Agricultural Engineering")
}
