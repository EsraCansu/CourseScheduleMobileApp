# Project: University Course Scheduler (Android/Kotlin)

## 1. Objective
Develop an Android application that manages university course schedules using a role-based access system (Admin vs. Lecturer) and a multidimensional data structure for scheduling.

## 2. Core Architecture
### A. Identity & Role Management
- **User Object:** `id` (UUID), `name`, `surname`, `department`, `role` (ADMIN or LECTURER), `isRegistered` (Boolean).
- **Persistence:** Use `SharedPreferences` or `DataStore` for the local user profile.
- **Login Generation:** - Username: `lowercase_name_surname` (extracted from lecturer list, omitting academic titles).
    - Password: Unique 6-digit numeric string.
- **Lecturer passwords can be provided via Excel import; the system will prioritize imported passwords over auto-generated ones.**

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

## 5. Data Flow
- **Data imported via Excel is immediately persisted to the Room Database and observed by the UI via ViewModel to ensure state preservation across navigation.**
- **Lecturer credentials (username/password) are automatically created in SharedPreferences during Excel import, allowing immediate sign-in with imported passwords.**
- **The CourseViewModel observes both Course and Lecturer tables, providing reactive UI updates when data changes.**

## 6. Implementation Log
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
- [x] **Theme & Colors (2026-03-10)** — Replaced purple/teal palette with professional university navy scale. Primary: Deep Navy `#1A237E`; Secondary: Indigo Accent `#5C6BC0`; Background: Light Grey `#F5F5F5`. Updated `colors.xml`, `themes.xml` (status bar `#0D174E`), and `activity_main.xml` (`backgroundTint`/`itemIconTint`/`itemTextColor` via `bnv_item_color` state list).
- [x] **Navigation Visibility Logic (2026-03-10)** — `MainActivity.applyRoleBasedNavVisibility()` now hides **both** `dataFragment` and `settingsFragment` tabs for `LECTURER` role; all four tabs remain visible for `ADMIN`. Guard skips adjustment until `isRegistered = true`.
- [x] **DataFragment — File Picker (2026-03-10)** — `ActivityResultContracts.OpenDocument` launches system file picker for `.txt` / `.xlsx` / `.xls`. Initial stub replaced with full implementation (see Room DB entry below).
- [x] **Room Database (2026-03-10)** — `Course.kt` upgraded to `@Entity(tableName = "courses")` with auto-generated `id` and matrix-coordinate fields (`departmentIndex`, `dayIndex`, `timeSlotIndex`). `CourseDao` provides `insertAll`, `getAllCourses`, `getCoursesByLecturer`, `getCoursesByDepartment`, `deleteAll`. `AppDatabase` singleton via double-checked locking. `CourseRepository` wraps DAO, exposes `LiveData` queries and atomic `replaceAll` (deleteAll + insertAll in one coroutine). `CourseViewModel` (`AndroidViewModel`, `activityViewModels` pattern) shared between DataFragment and CalendarFragment.
- [x] **DataFragment — Full Implementation (2026-03-10)** — Observes `viewModel.allCourses` LiveData on `onViewCreated` so the list reloads from the database after navigation without re-importing. `.txt` files parsed line-by-line via `parseTxtLine()` (pipe-separated format: `courseCode|courseName|lecturerName|lecturerID|deptIndex|dayIndex|slotIndex`). `syncMatrix()` writes each imported `Course` into `ScheduleMatrixManager`. `viewModel.replaceAll()` atomically persists the new list to Room. `CourseListAdapter` inner `RecyclerView.Adapter` displays `courseCode — courseName (lecturerName)` per row.
- [x] **CalendarFragment — 5×2 Grid (2026-03-10)** — `fragment_calendar.xml` replaced with a `TableLayout` grid: navy header row (Morning / Afternoon), five day-label rows (Monday–Friday), and 10 `TextView` cells (`cell00`–`cell41`). `CalendarFragment.kt` observes Room via `activityViewModels`: Admin role shows a department `Spinner` and calls `getCoursesByDepartment()`, writes into `ScheduleMatrixManager`, then renders from the matrix; Lecturer role hides the spinner and calls `getCoursesByLecturer(userId)`, renders the personal 5×2 schedule directly.
- [x] DataFragment — Apache POI `.xlsx` parsing (plain `.txt` is fully functional).
- [ ] **Authentication System (2026-03-11)** — Implement full Sign In/Sign Up flow with AuthManager for credential verification. SignInFragment (Role, Email, Password), SignUpFragment (Username, Role, Email, Password, Confirm Password). MainActivity checks user session and redirects to SignIn if not logged in.
- [ ] **Role-Based Home Interface (2026-03-11)** — Admin dashboard shows Total Lecturers and Total Courses counts. Lecturer view shows "Your Courses" RecyclerView filtered by lecturerID.

## 7. Summary of System Features

### Authentication System
- **Sign In:** Role dropdown, Email, Password fields with credential verification against SharedPreferences
- **Sign Up:** Username, Role, Department, Email, Password with confirmation field
- **Session Management:** AuthManager singleton tracks login state via SharedPreferences
- **Guest View:** Non-logged-in users see "Welcome to CourseSchedule!" message with Sign In button

### Role-Based Interfaces
- **Admin Home:** Dashboard with Department Name, Total Lecturers count, Total Courses count
- **Lecturer Home:** "Your Courses" RecyclerView showing only courses assigned to the logged-in lecturer
- **Navigation:** Data tab strictly hidden for Lecturer role; visible for Admin

### Data Management
- **Import Formats:** Supports `.txt` (pipe-delimited), `.csv` (comma-delimited), and `.xlsx` (Excel, placeholder)
- **TXT Format:** `courseCode|courseName|lecturerName|lecturerID|deptIndex|dayIndex|slotIndex`
- **CSV Format:** `courseCode,courseName,lecturerName,lecturerID,deptIndex,dayIndex,slotIndex`
- **Processing:** Parsed courses populate both Room database and ScheduleMatrixManager

### Technical Stack
- **Language:** Kotlin
- **UI:** Jetpack Navigation Component, BottomNavigationView, Material Design
- **Database:** Room with KSP annotation processing
- **Data Structure:** Multidimensional Array `[Department][Day][TimeSlot]` for O(1) schedule operations

## 6. File Organization & Architecture
- **Dependency Management:** Use `build.gradle.kts` (Kotlin DSL).
- **Navigation:** Use `nav_graph.xml` located in `res/navigation`.
- **View Binding:** Enable `viewBinding` in Gradle for type-safe UI access.
- **Resource Naming:** - Layouts: `fragment_home.xml`, `fragment_settings.xml`, etc.
    - Icons: Use Material Design Vector Assets.
- **Strict Role Logic:** All UI visibility logic (hiding 'Data' for Lecturers) must be handled in `MainActivity` using the `User` object from `SharedPreferences`.