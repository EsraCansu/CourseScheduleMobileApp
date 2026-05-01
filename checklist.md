# CourseScheduleMobileApp — Implementation Checklist

> Tracks progress against `project_content.md` phases.
> Update this file as each item is completed.

---

## Phase 1 — Stabilization

Goal: Make project compile, run, and show UI on emulator.

- [x] Gradle sync succeeds with zero errors
- [x] `./gradlew assembleDebug` passes — BUILD SUCCESSFUL ✅
- [x] App launches on emulator ✅
- [x] AndroidManifest.xml verified ✅ (exists, correct)
- [x] MainActivity verified ✅ (declared, exported)
- [x] Navigation graph verified ✅ (5 destinations wired)
- [x] Fix nav `startDestination` — HomeFragment handles Guest state ✅
- [x] Add `firebase-auth-ktx` dependency ✅
- [x] Add real `google-services.json` to `app/` ✅
- [x] Enable `com.google.gms.google-services` plugin ✅
- [x] Confirm build with Firebase active — BUILD SUCCESSFUL ✅
- [x] Embedded admin: `admin@example.com` / `admin123` / ADMIN role ✅
- [x] Login screen visible and functional on emulator ✅

> **Phase 1 COMPLETE ✅**

---

## Phase 2 — Navigation Foundation

- [x] Login → Home (Admin) navigation works ✅ (onResume refreshes auth state)
- [x] Login → Home (Lecturer) navigation works ✅
- [x] Guest view accessible without login ✅ (HomeFragment shows guest layout)
- [x] Bottom navigation role-based tab visibility correct ✅
  - Admin: Home, Calendar, Data, Settings
  - Lecturer: Home, Calendar, Settings (Data hidden)
  - Guest: all tabs visible, content gated
- [x] SignIn → SignUp flow ✅ (BottomSheet chain)
- [x] SignIn → Home (after success) ✅ (dismiss + onResume)
- [x] Logout → back to Guest view ✅ (Firebase + SharedPrefs signOut)
- [x] Firebase Auth + SharedPrefs dual-layer auth ✅
- [x] First-login warning banner for Lecturers ✅

> **Phase 2 COMPLETE ✅**

---

## Phase 3 — Jetpack Compose Migration

- [x] Add Compose BOM + Material3 + activity-compose to `app/build.gradle.kts` ✅
- [x] Enable `buildFeatures { compose = true }` ✅
- [x] Set `composeOptions` Kotlin compiler extension version ✅
- [x] Migrate LoginScreen (Compose) ✅
- [x] Migrate HomeScreen — Admin dashboard (Compose) ✅
- [x] Migrate HomeScreen — Lecturer dashboard (Compose) ✅

- [x] Migrate CalendarScreen / Availability Grid (Compose) ✅
- [x] Migrate DataScreen / Excel Import (Compose) ✅
- [x] Migrate SettingsScreen (Compose) ✅
- [x] Replace XML layouts with Compose screens ✅
- [x] Replace NavHostFragment with NavHost in Compose ✅
- [x] Material Design 3 theme applied globally ✅

> **Phase 3 COMPLETE ✅**

---

## Current Session Fixes (UI & Emulator)

- [x] Emulator portrait mode fixed
- [x] Login screen set as start destination
- [x] Home screen hidden before authentication
- [x] Hourly calendar grid implemented
- [x] Emulator rerun verification completed
- [x] Runtime crash diagnosis (Root cause: `popUpTo(0)` in `NavGraph.kt`)
- [x] Firebase validation status (Firebase successfully initialized, Auth is safe)
- [x] Startup flow validation (Login fallback safely triggers Home screen)
- [x] Crash resolved confirmation (App successfully runs without closing)

---

## Phase 4 — UI Polish & Advanced Workflows (Week 10)

- [x] Import schema supports approval workflow
- [x] Data page management structure implemented
- [x] Lecturer calendar accessible from Data page
- [x] Expanded calendar detail panel implemented
- [x] Three-mode theme system implemented
- [ ] Approval state integrated into schedule system
- [x] Import validation workflow implemented
- [x] Duplicate resolution flow added
- [x] Confirmation-based import added
- [x] Missing data handling added
- [x] Import summary preview added
- [ ] Implement `UiState` sealed class (Idle, Loading, Success, Error with retryable flag)
- [ ] Update ViewModels to use `StateFlow` and `viewModelScope` (remove LiveData)
- [ ] Migrate Firestore reads/writes to `Dispatchers.IO` using coroutines
- [ ] Create Repositories (`CourseRepository`, `LecturerRepository`)
- [ ] Implement real-time Firestore listeners using `callbackFlow` and `addSnapshotListener`
- [ ] Handle `CancellationException` properly in all catch blocks
- [ ] Use Firestore `runTransaction` to handle race conditions safely
- [ ] Implement parallel data loading using `async`/`await` in ViewModels
- [x] Map `FirebaseFirestoreException` codes to user-friendly error messages
- [x] Lecturer password column added ✅
- [x] Temporary password import implemented ✅
- [x] Forced password change flow implemented ✅
- [x] Password hashing implemented ✅
- [x] Admin password status visibility added ✅
- [x] First login flow completed ✅

---

## Phase 5 — Offline Layer

- [ ] Room DB as local cache for all Firestore collections
- [ ] Repository pattern: Firestore read → Room write
- [ ] Repository pattern: Room read → UI
- [ ] Sync strategy: push local changes back to Firestore
- [ ] Offline detection and graceful fallback
- [ ] DataStore for user preferences (dark mode, etc.)

---

## Phase 6 — Schedule Module

- [ ] Scheduling matrix: `[Department][Day][TimeSlot]`
- [ ] Weekday-only scheduling (Mon–Fri)
- [ ] Lecturer availability grid (Green/Red/Gray)
- [ ] Tap to toggle availability cell
- [ ] Conflict detection: lecturer overlap prevention
- [ ] Conflict detection: classroom availability
- [ ] Conflict detection: department conflict prevention
- [ ] Conflict dialog for Excel import mismatches
- [ ] CSV import (Department, Lecturer, Course, Code, Class, Time, Duration, Classroom Type)
- [ ] XLSX/XLS import (same fields)
- [ ] Validation: missing fields → skip or ask admin

---

## Phase 7 — Repository Cleanup & Integrity

- [x] Startup route fixed to Sign In ✅
- [x] Sign Up button restored ✅
- [x] ChangePassword blocked from startup ✅
- [x] Navigation root cause fixed ✅ (Root cause: Redundant LaunchedEffect + Persistent Session)
- [x] AuthGraph corrected ✅
- [x] Global Auth Guard implemented ✅
- [x] Debug logging added to NavGraph ✅
- [x] Login TextFields editable ✅ (Root cause: Fixed unconnected state lambdas in MainActivity)
- [x] Keyboard interaction fixed ✅
- [x] Focus handling fixed ✅
- [x] Input blocking layer removed ✅
- [x] Sign In interaction restored ✅
- [x] Firebase role routing implemented ✅
- [x] Local emulator-only assumption added ✅
- [x] Web emulator attempts removed ✅
- [x] Lecturer onboarding flow corrected ✅

---

## Phase 8 — Architecture Pivot (Local-Only Room) ✅
Goal: Remove Firebase and migrate to a high-performance, local-only architecture.

- [x] Remove Firebase Firestore/Auth/Storage dependencies ✅
- [x] Delete `google-services.json` and clean build scripts ✅
- [x] Migrate `CourseRepository` and `LecturerRepository` to Room ✅
- [x] Implement atomic transaction-based `replaceAll` in Room ✅
- [x] Update `CourseViewModel` to read exclusively from local DAOs ✅
- [x] Fix infinite loading issue by removing network-bound tasks ✅
- [x] Synchronous local session management via `AuthManager` (SharedPrefs) ✅
- [x] Verify data persistence across app restarts ✅
- [x] Support flexible Excel import into local database ✅
- [x] Secure LecturerEntity with hashed passwords (SHA-256) ✅
- [x] Robust SessionManager for local persistence ✅
- [x] Smart email/password column mapping in ImportEngine ✅
- [x] First-login flow (mustChangePassword) integrated with Room ✅
- [x] Architecture Reorganized to match project_content.md ✅
- [x] Package Renamed to com.coursescheduling ✅
- [x] Course model expanded (approvalStatus, duration, etc.) ✅
- [x] ImportEngine updated for new fields ✅
- [x] 9-slot timetable grid finalized ✅

---

## Phase 9 — Final Verification & Polish
- [x] Comprehensive UI test for local data flow ✅
- [x] Performance audit for large Excel imports (Room batching) ✅
- [x] Ensure all screens (Home, Calendar, Data) reflect local state instantly ✅
- [x] Final stabilization for build-ready Android package ✅

---

## Markdown Docs Status

- [x] `project_content.md` — Updated for Local-Only logic ✅
- [x] `README.md` — repository overview ✅
- [x] `checklist.md` — this file ✅
