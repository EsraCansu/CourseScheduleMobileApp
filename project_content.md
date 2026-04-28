# Course Scheduling App – Master Implementation Plan

## Project Goal

Build a fully functional Android mobile application for university course scheduling.

The app must:

* Be written fully in Kotlin
* Use Jetpack Compose for UI
* Use Firebase backend
* Support offline viewing and caching
* Support Admin and Lecturer roles
* Be scalable for future scheduling automation

---

# Current Project Context

The project originally existed with:

* TSX + Tailwind UI structure
* Components folder
* Screens folder
* Admin and Lecturer screen separation

The project has been partially migrated into Kotlin Jetpack Compose.

Current priority:

> Make the app fully runnable and stable in Android Studio before adding advanced features.

UI perfection can come later.

---

# Current Technical Stack

## Mobile

* Kotlin
* Jetpack Compose
* Material Design 3

## Backend

* Firebase Authentication
* Firebase Firestore
* Firebase Storage

## Offline

* Room Database
* Repository pattern

## Architecture

* MVVM
* Navigation Component
* StateFlow
* Modular package structure

---

# Required User Roles

## Admin

Full access.

Capabilities:

* Upload CSV / Excel files
* Manage lecturers
* Manage classrooms
* Manage departments
* Manage courses
* View lecturer availability
* Create schedules
* Resolve conflicts
* View analytics
* Manage settings
* Allow department visibility permissions
* View recent activity

---

## Lecturer

Limited access.

Capabilities:

* View personal schedule
* Mark available/unavailable times
* Submit preferred schedule requests
* Request department schedule visibility
* View notifications
* Change password
* Access settings

---

# Core Scheduling Rules

The scheduling system must obey:

* Lecturer availability
* Classroom availability
* Department conflict prevention
* Lecturer overlap prevention
* Course duration matching
* Weekday-only scheduling
* Weekly timetable repeat

Weekdays:

* Monday
* Tuesday
* Wednesday
* Thursday
* Friday

No weekend scheduling.

---

# Data Upload Requirements

Admin uploads:

* CSV
* XLSX
* XLS

Required fields:

* Department
* Lecturer Name
* Course Name
* Course Code
* Course Class
* Course Time
* Course Duration
* Classroom Type

Validation:

* Missing fields → ask admin OR skip row
* Validate before Firebase insertion

---

# Lecturer Availability Grid

Visual timetable.

Colors:

* Green → Available
* Red → Not Available
* Gray → Occupied / Assigned

Interaction:

* Tap grid cell to toggle state
* Weekly calendar structure

---

# Main Screens

## Authentication

* Login
* Password reset
* First login password change

---

## Admin Screens

* Dashboard
* Schedule Management
* Data Upload
* Lecturer List
* Department List
* Classroom List
* Course List
* Conflict Management
* Analytics
* Settings

---

## Lecturer Screens

* Dashboard
* Personal Schedule
* Availability Selection
* Requests
* Settings

---

# Current Priority

## Phase 1 — Stabilization

Goal:

> Make project compile, run, and show UI on emulator.

Must verify:

* AndroidManifest.xml exists
* MainActivity launches
* Compose renders
* Navigation works
* Gradle sync succeeds
* Emulator launch succeeds

---

## Phase 2 — Navigation Foundation

Build:

* Login flow
* Admin dashboard navigation
* Lecturer dashboard navigation
* Bottom navigation
* Role-based routing

---

## Phase 3 — UI Integration

Move generated UI into:

* Compose screens
* Reusable components
* Material 3 theme

---

## Phase 4 — Firebase Integration

Implement:

* Authentication
* Firestore collections
* User roles
* Schedule persistence

---

## Phase 5 — Offline Layer

Implement:

* Room DB cache
* Sync strategy
* Repository pattern

---

# Firebase Collections

## users

Fields:

* userId
* username
* role
* email
* mustChangePassword

---

## lecturers

Fields:

* lecturerId
* departmentId
* availabilityGrid
* postgraduateStatus

---

## departments

Fields:

* departmentId
* departmentName

---

## classrooms

Fields:

* classroomId
* capacity
* type

---

## courses

Fields:

* courseId
* courseName
* courseCode
* duration
* lecturerId
* departmentId
* classroomType

---

## schedules

Fields:

* scheduleId
* lecturerId
* courseId
* classroomId
* weekday
* timeSlot

---

# Folder Structure Target

```text
com.coursescheduling
│
├── data
│   ├── remote
│   ├── local
│   ├── repository
│
├── domain
│   ├── model
│   ├── usecase
│
├── presentation
│   ├── auth
│   ├── admin
│   ├── lecturer
│   ├── components
│
├── navigation
├── theme
├── utils
├── firebase
```

---

# Primary Design Identity

Primary Color:

* #5b6bf5

UI Style:

* Modern university dashboard
* Minimal UI
* Material Design 3
* Rounded cards
* Soft shadows
* Professional scheduling aesthetic

---

# Antigravity Instructions

Antigravity must use this document as the project source of truth.

Before implementing anything:

1. Read this full document
2. Understand architecture
3. Understand roles
4. Understand schedule logic
5. Understand Firebase structure
6. Understand current stabilization goal

---

# Critical Rule For Antigravity

If any ambiguity exists, Antigravity MUST ask questions before implementation.

Examples:

* Missing screen logic
* Missing navigation rule
* Missing Firebase structure
* Missing interaction logic
* Unknown admin permissions
* Unknown lecturer workflow

Antigravity must NOT guess.

---

# Immediate Objective

Current objective:

> Make the project compile, sync, run, and display UI in Android Studio emulator.

Only after stabilization should advanced features continue.

---

# Suggested Immediate Implementation Order

1. Verify Android structure
2. Fix Gradle
3. Fix MainActivity
4. Fix Manifest
5. Confirm emulator launch
6. Confirm login screen visible
7. Add navigation
8. Add dashboard
9. Add Firebase auth
10. Add scheduling system

---

# Expected Result

A stable Android app that:

* Launches successfully
* Displays UI
* Supports Admin and Lecturer flow
* Is ready for Firebase integration
* Can be safely extended later
