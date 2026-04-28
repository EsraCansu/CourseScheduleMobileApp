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

- [ ] Add Compose BOM + Material3 + activity-compose to `app/build.gradle.kts`
- [ ] Enable `buildFeatures { compose = true }`
- [ ] Set `composeOptions` Kotlin compiler extension version
- [ ] Migrate LoginScreen (Compose)
- [ ] Migrate HomeScreen — Admin dashboard (Compose)
- [ ] Migrate HomeScreen — Lecturer dashboard (Compose)
- [ ] Migrate CalendarScreen / Availability Grid (Compose)
- [ ] Migrate DataScreen / Excel Import (Compose)
- [ ] Migrate SettingsScreen (Compose)
- [ ] Replace XML layouts with Compose screens
- [ ] Replace NavHostFragment with NavHost in Compose
- [ ] Material Design 3 theme applied globally

---

## Phase 4 — Firebase Integration

- [ ] Firebase Auth replaces SharedPrefs auth
- [ ] Firebase Auth: Email/Password sign-in
- [ ] Firebase Auth: Session persistence
- [ ] Firestore: `users` collection CRUD
- [ ] Firestore: `lecturers` collection CRUD
- [ ] Firestore: `departments` collection CRUD
- [ ] Firestore: `classrooms` collection CRUD
- [ ] Firestore: `courses` collection CRUD
- [ ] Firestore: `schedules` collection CRUD
- [ ] Role assignment enforced via Firestore user document
- [ ] First-login password change flow (Lecturer)
- [ ] Admin exempt from first-login password change
- [ ] Push to Cloud button (Admin only, Data screen)

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

## Markdown Docs Status

- [x] `project_content.md` — master specification ✅
- [x] `README.md` — repository overview ✅ (rewritten)
- [x] `checklist.md` — this file ✅ (created)
- [ ] `PROJECT_SPEC.md` — merge into project_content.md then delete
- [ ] `DEVELOPMENT_REPORT.md` — archive or delete
