# Project: University Course Scheduler (Android/Kotlin)

## 1. Objective
Develop an Android application that manages university course schedules using a role-based access system (Admin vs. Lecturer) and a multidimensional data structure for scheduling.

## 2. Core Architecture
### A. Identity & Role Management
- **User Object:** `id` (UUID), `name`, `surname`, `department`, `role` (ADMIN or LECTURER), `isRegistered` (Boolean).
- **Persistence:** Use `SharedPreferences` or `DataStore` for the local user profile.
- **Login Generation:** - Username: `lowercase_name_surname` (extracted from lecturer list, omitting academic titles).
    - Password: Unique 6-digit numeric string.

### B. Data Structure (The Schedule Matrix)
The schedule is represented as a **Multidimensional Array (Matrix)** for O(1) efficiency and UI grid mapping.
- **Matrix Dimensions:** `[Department][Day][TimeSlot]`
    - **Departments (5):** Computer, Electrical, Mechanical, Aeronautical, Agricultural.
    - **Days (5):** Monday - Friday.
    - **TimeSlots (Initially 2):** 0 = Morning, 1 = Afternoon.
- **Cell Data:** A `Course` object containing `courseCode`, `courseName`, `lecturerName`, and `lecturerID`.



## 3. Navigation & Fragment Rules
1. **Home Fragment:** - Check `isRegistered`. If false, show a `MaterialAlertDialog` and force redirect to **Settings**.
    - Display: "Welcome [Name]", "[Department]", "[Role]".
2. **Calendar Fragment:**
    - Display a 5x2 grid.
    - **Lecturer Role:** Filter matrix to show only courses where `lecturerID == currentUser.id`.
    - **Admin Role:** Show the full schedule for the selected department.
3. **Data Fragment (Admin Only):**
    - Initial state: Centered "+" button.
    - Function: Import Excel/TXT.
    - Action: Hide "+" and show a list (Accordion or Dropdown) after import.
4. **Settings Fragment:**
    - Form: Name/Surname input, Department dropdown, Position dropdown, Save button.

## 4. Technical Constraints
- **Language:** Kotlin
- **UI:** Jetpack Navigation Component (Single Activity Architecture), BottomNavigationView.
- **Database:** Room (for persisting the imported Course/Lecturer list). Can use Poi library
- **Matrix Manager:** A Singleton repository class to handle the multidimensional array logic.

## 5. Implementation Log
- [x] Project initialized with Kotlin DSL (`build.gradle.kts`).
- [x] Package structure created (`data`, `ui`).
- [x] `PROJECT_SPEC.md` integrated for context.
- [x] **Gradle** — `viewBinding`, Navigation Component, Material Components, Safe Args plugin configured in `app/build.gradle.kts` and root `build.gradle.kts`.
- [x] **Navigation** — `res/navigation/nav_graph.xml` created with destinations: `homeFragment`, `calendarFragment`, `dataFragment`, `settingsFragment`. Named actions: `home→settings` (registration redirect), `settings→home` (post-save return).
- [x] **MainActivity** — `BottomNavigationView` wired to `NavController` via `setupWithNavController`; role-based Data-tab visibility applied on every destination change.

- [x] **ScheduleMatrixManager** — `object` singleton; `ScheduleMatrix` / `DaySchedule` / `TimeSlotRow` typealiases declared; `Array[5][5][2]` initialized; O(1) get/set; `getCoursesByLecturer()` helper for Calendar filter.
- [x] **User data class** — `id` (UUID), `name`, `surname`, `department`, `role`, `isRegistered`; persisted via `SharedPreferences`.
- [x] **HomeFragment** — `isRegistered` guard with non-cancellable `MaterialAlertDialogBuilder`; redirects to Settings.
- [x] **SettingsFragment** — Name/Surname inputs, Department/Role spinners, Save to `SharedPreferences`.
- [x] **Build Fix (2026-03-10)** — Resolved `Configuration.fileCollection(Spec)` build error caused by `kapt 1.9.22` calling a Gradle API removed in Gradle 8.3+. Changes: Gradle wrapper → 8.11.1; AGP → 8.8.0; Kotlin → 2.0.21; replaced `kapt` with `ksp` (`com.google.devtools.ksp:2.0.21-1.0.28`); Navigation + Safe Args → 2.8.5; `compileSdk`/`targetSdk` → 35; Java target → 11.
- [x] **Launcher Icons (2026-03-10)** — Created `mipmap-anydpi-v26/` (adaptive icon) and `mipmap-anydpi/` (legacy layer-list fallback) using `ic_launcher_background.xml` and `ic_launcher_foreground.xml` vector drawables. Resolves AAPT `mipmap/ic_launcher not found` error.
- [x] **OneDrive File-Lock Fix (2026-03-10)** — Resolved `Unable to delete directory app/build/intermediates/data_binding_dependency_artifacts` caused by OneDrive syncing Gradle build outputs. Fix: stopped Gradle daemons (`gradlew --stop`), deleted `app/build/`, added root `.gitignore` and `app/.gitignore` excluding `/build/` so OneDrive no longer tracks incremental build artefacts.
- [ ] CalendarFragment — 5×2 grid, role-filtered matrix display.
- [ ] DataFragment — Import Excel/TXT via Apache POI; accordion list after import.
- [ ] Room database — `Course` and `Lecturer` entities, DAOs, migrations.

## 6. File Organization & Architecture
- **Dependency Management:** Use `build.gradle.kts` (Kotlin DSL).
- **Navigation:** Use `nav_graph.xml` located in `res/navigation`.
- **View Binding:** Enable `viewBinding` in Gradle for type-safe UI access.
- **Resource Naming:** - Layouts: `fragment_home.xml`, `fragment_settings.xml`, etc.
    - Icons: Use Material Design Vector Assets.
- **Strict Role Logic:** All UI visibility logic (hiding 'Data' for Lecturers) must be handled in `MainActivity` using the `User` object from `SharedPreferences`.