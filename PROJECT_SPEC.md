# Project: CourseScheduleMobileApp (Android/Kotlin)

## 1. Objective
A professional university course scheduling application with a multi-layered access system (Admin, Lecturer, and Guest). The app uses a **Cloud-Synced system via Firebase**, with Room DB as a high-performance offline cache.

---

## 2. Core Architecture

### A. Identity & Role Management (Firebase & Seed Data)
* **User Object:** `uid` (Firebase ID), `name`, `surname`, `email`, `department`, `role` (ADMIN, LECTURER, or GUEST), `isFirstLogin` (Boolean).
* **Initial Seed Data (Admin Credentials):**
    * **Email:** `admin@gmail.com`
    * **Password:** `admin1234`
* **Authentication Flow:**
    * **SignInSheet (`image_e1c763.png`):** A dismissible `BottomSheetDialogFragment`. 
    * **Validation:** "Sign In" button is enabled only when Email format is correct and Password length >= 6.
    * **Persistence:** Use `AuthManager` to track sessions. If logged in, skip GuestView on next launch.

### B. Data Structure (The Schedule Matrix)
* **Logic:** Multidimensional Array `[Department][Day][TimeSlot]` for O(1) scheduling operations.
* **Storage:** Firebase Firestore (Cloud) + Room DB (Local Cache).
* **Dimensions:** 5 Departments × 5 Days × 2 TimeSlots (Morning/Afternoon).

---

## 3. Navigation & User Flows

### 1. Guest View (Start Destination - `image_f0bf84.png`)
* **Initial State:** This fragment MUST be the `startDestination` in `nav_graph.xml`.
* **UI:** Category cards (Departments, Courses, etc.) and a "Sign In" button in the AppBar.
* **Interaction:** Clicking any card or "Sign In" MUST open the **SignInSheet** (Bottom Sheet).

### 2. Role-Based Navigation (Post-Login)
* **ADMIN:** Access to Home, Calendar, Data (Excel Import), and Settings.
* **LECTURER:** Access to Home, Calendar (Availability Mode), and Settings. **Data tab is HIDDEN.**
* **Security:** IF `user.role == "LECTURER"` AND `isFirstLogin == true`, show a mandatory password change banner on Home. **Admins are exempt from this.**

### 3. Bottom Navigation (Sync Requirement)
* **CRITICAL:** Item IDs in `bottom_nav_menu.xml` MUST match fragment IDs in `nav_graph.xml` (e.g., `@+id/homeFragment`).

---

## 4. Technical Constraints & UI
* **Primary Brand Color:** `#00c4cc` (Teal).
* **Guest Dashboard Navy:** `#1A237E`.
* **Tech Stack:** Kotlin, MVVM, Jetpack Navigation, Firebase Auth/Firestore, Room KSP.
* **Conflict Policy (Excel):** Append new data, Deduplicate identicals, show Conflict Dialog for mismatched info.

---

## 5. Implementation Log (Current State)

- [x] UI Palette updated to `#00c4cc`.
- [x] **Guest Dashboard:** Layout completed (`211543.png`).
- [x] **SignInSheet:** Bottom Sheet UI completed (`211543.png`).
- [x] **Admin Seed Data:** Integrated into Auth logic (`admin@gmail.com` / `admin1234`).
- [ ] **Navigation Fix (URGENT):** Correct `startDestination` to GuestView and fix Home/Settings ID mismatch.
- [ ] **Firestore Migration:** Move local Course/Lecturer data to Firebase collections.
- [ ] **Security Banner:** Implement the `isFirstLogin` UI in Lecturer Home.

---