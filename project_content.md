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

# Current Priority

## Phase 1 — Stabilization

Goal:

* Project compiles
* Emulator launches
* Compose renders
* Navigation works

---

## Phase 2 — Local Database + Authentication

Goal:

* Room Database
* Lecturer login
* Password hashing
* Session persistence
* XLSX import → Room

---

## Phase 3 — Navigation + UI Integration

Goal:

* Admin navigation
* Lecturer navigation
* Dashboard integration
* Shared components

---

## Phase 4 — Scheduling Engine

Goal:

* Availability grid
* Conflict detection
* Approval workflow
* Schedule generation

---

## Phase 5 — Firebase Sync (Optional)

Goal:

* Cloud backup
* Remote sync
* Multi-device support

---

## Phase 6 — UI Polish & Optimization

Goal:

* Dark mode
* Animations
* Performance
* UX improvements
---

# Current Technical Stack

## Mobile

* Kotlin
* Jetpack Compose
* Material Design 3

## Current Backend Strategy

Primary Backend:

* Room Database (Offline-first)

Current Development Mode:

* Local-first architecture
* Firebase temporarily disabled for active development
* Authentication and scheduling currently operate locally

Future Backend:

* Firebase may be used later for cloud sync and backup

## Offline

* Room Database
* Repository pattern

## Architecture

* MVVM
* Navigation Component
* StateFlow
* Modular package structure

---

## Session Management

The system must persist login state locally.

Use:

* SharedPreferences
* SessionManager

Stored session data:

* userId
* role
* loginState
* mustChangePassword
* lastLogin
-------

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
# Lecturer Authentication Rules

Lecturer accounts are generated from imported XLSX data.

Required lecturer authentication fields:

* Email
* Password
* Lecturer Name
* Department

Rules:

* Password must be hashed before storage
* Plain text password must never be stored
* First login forces password change
* Authentication works locally
* Session persists using SharedPreferences
* Lecturer credentials are stored in Room Database

-------

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
----------

# Schedule Approval Workflow

Lecturers may request schedule preferences.

Workflow:

Lecturer Request
→ Pending
→ Admin Review
→ Approved / Rejected

Only approved schedules become active timetable entries.

Request statuses:

* Pending
* Approved
* Rejected

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
* Email
* Password

Validation:

* Missing fields → ask admin OR skip row
* Validate before Firebase insertion

## Smart XLSX Parsing Rules

The import system must support flexible column names.

Rules:

* Case-insensitive parsing
* Space-insensitive parsing
* Underscore normalization
* Flexible header recognition

Examples:

Lecturer Name → lecturerName
Lecturer → lecturerName
Instructor → lecturerName

Course Code → courseCode
Code → courseCode
CourseCode → courseCode

Unknown columns:

* Must generate warnings
* Must not crash import system

Import must use header mapping instead of column index positions.

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

## Schedule Time Slot Structure

Weekly timetable must use fixed hourly blocks.

Time slots:

* 08:00–09:00
* 09:00–10:00
* 10:00–11:00
* 11:00–12:00
* 13:00–14:00
* 14:00–15:00
* 15:00–16:00
* 16:00–17:00
* 17:00–18:00

Each time block represents one selectable schedule cell.

Grid must support:

* Tap interaction
* Availability coloring
* Dynamic expansion for details

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

---

# Current Development Strategy

Current development prioritizes:

* Stable Android runtime
* Offline-first architecture
* Room Database
* Local authentication
* XLSX import reliability
* Emulator stability

Firebase integration is postponed until local system becomes stable.