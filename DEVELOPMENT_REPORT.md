# CourseScheduleMobileApp - Development Journey & Technical Report

## Executive Summary

This report details the development of the **CourseScheduleMobileApp**, an Android application designed for university course scheduling. The primary objective was to create a robust, multi-user system with distinct roles for **Admin**, **Lecturer**, and **Guest**, moving from a local-only database to a professional, cloud-synchronized architecture.

The application is built on a modern Android tech stack, including:
- **Language:** Kotlin
- **Architecture:** Model-View-ViewModel (MVVM)
- **Core Technologies:**
    - **Firebase:** For real-time authentication (Auth) and cloud data storage (Firestore).
    - **Room:** As a local database for a high-performance, offline-first cache.
    - **Jetpack Navigation:** For managing all in-app navigation and user flows.

## System Architecture & Data Flow

The app's architecture is centered around a **"Local-First with Cloud Sync"** strategy. This ensures a seamless user experience, even with intermittent network connectivity.

1.  **Local-First Approach:** The app's primary data source is the local **Room database**. All UI components read data directly from Room, ensuring fast, responsive performance.
2.  **Cloud Sync:** **Firebase Firestore** acts as the single source of truth. A repository layer manages the synchronization between Firestore and Room. Data is fetched from Firestore and cached locally. Any changes made by authenticated users are pushed back to Firestore.
3.  **Scheduling Logic (The 5x2 Matrix):** At the core of the scheduling system is a logical **5x2 matrix** representing five weekdays and two time slots (Morning/Afternoon). This structure, indexed by department, allows for highly efficient O(1) time complexity when checking for scheduling conflicts, as the system can instantly look up if a specific time slot is occupied.

## Step-by-Step Development Journey

The project evolved through several key phases, from initial setup to feature implementation.

#### Initial Setup and Gradle Migration
The project began with a standard Android project template. A significant initial step was migrating the Gradle build system to use the **Kotlin DSL (`build.gradle.kts`)**. This improved script readability and type safety. Concurrently, we transitioned from `kapt` to the **Kotlin Symbol Processing (KSP)** tool for annotation processing, which significantly reduced build times, especially for Room.

#### Building the Guest View and BottomSheet Login UI
The initial user-facing feature was the **Guest View**, designed as the app's main entry point. This screen provides unauthenticated users with an overview of university departments and courses. To facilitate a clean and modern user experience, the login/signup functionality was implemented within a **`BottomSheetDialogFragment`**. This non-intrusive UI component slides up from the bottom, allowing users to sign in without navigating to a separate screen.

#### Integrating Admin Seed Data for Testing
To enable immediate testing of administrative features, we embedded seed data for a default **Admin** account (`admin@gmail.com`). This allowed us to develop and validate role-specific logic and UI components before the full Firebase authentication flow was finalized.

## Troubleshooting & Bug Fixes (The Most Important Part)

The development process involved overcoming several critical bugs that were crucial for stabilizing the application.

#### Issue 1: Navigation Start Destination
-   **Problem:** Upon the first launch, the application was incorrectly navigating to the Admin panel instead of the public-facing Guest View.
-   **Root Cause:** The `startDestination` attribute in the main navigation graph (`nav_graph.xml`) was mistakenly pointing to the `homeFragment` (Admin/Lecturer Home) instead of the `guestFragment`.
-   **Fix:** We explicitly set the `startDestination` to the `guestFragment`, ensuring all users, authenticated or not, begin at the correct entry point.

    ```xml
    <!-- In res/navigation/nav_graph.xml -->
    <navigation xmlns:android="http://schemas.android.com/apk/res/android"
        xmlns:app="http://schemas.android.com/apk/res-auto"
        android:id="@+id/nav_graph"
        app:startDestination="@id/guestFragment">
        
        <!-- Other fragments -->
    </navigation>
    ```

#### Issue 2: Bottom Navigation ID Mismatch (The 'Home-Settings Swap' Bug)
-   **Problem:** Tapping the "Settings" icon in the bottom navigation bar would incorrectly navigate to the Home screen, and vice-versa.
-   **Root Cause:** The `id` attributes of the menu items in `bottom_nav_menu.xml` did not match the corresponding fragment `id`s in `nav_graph.xml`. The Navigation Component relies on this exact match to function correctly.
-   **Fix:** We synchronized the `id`s across both files, ensuring each navigation item pointed to the correct fragment destination.

#### Issue 3: Role-Based Redirection Logic
-   **Problem:** After logging in, an Admin user was being incorrectly redirected to the Settings page instead of the main Home dashboard.
-   **Root Cause:** The redirection logic after a successful login did not properly differentiate between user roles. It contained a flawed conditional that defaulted to the Settings page.
-   **Fix:** We implemented role-specific navigation guards. After login, the system now explicitly checks the user's role (ADMIN, LECTURER) and directs them to the appropriate home screen, with fallbacks for unexpected cases.

#### Issue 4: Theme/Color Synchronization
-   **Problem:** The application's UI displayed a mix of old (default) and new brand colors, creating an inconsistent user experience.
-   **Root Cause:** While new color values were added to `colors.xml`, many layout files still referenced legacy or hardcoded color attributes.
-   **Fix:** We performed a project-wide audit, replacing all legacy color references with the new primary brand color, **`#00c4cc` (Teal)**. This ensured a consistent and professional visual identity across the entire app.

## Key Features Implemented

-   **Lecturer Availability Grid:** A dedicated UI for lecturers to view and mark their available teaching slots. This feature directly interacts with the core scheduling matrix.
-   **Admin Excel Conflict Dialog:** When an Admin imports course data from an Excel file, the system now intelligently handles conflicts. If a scheduling clash is detected, a dialog appears, presenting the conflicting information and allowing the Admin to make an informed decision.

## Conclusion

The **CourseScheduleMobileApp** project successfully transitioned from a conceptual local application to a professional, multi-user system with a robust cloud-based architecture. The integration of Firebase and Room, guided by an MVVM pattern, provides a scalable and maintainable foundation. Overcoming critical bugs related to navigation, role-based logic, and UI consistency were pivotal learning experiences that significantly improved the application's stability and user experience. The final product stands as a testament to modern Android development practices, delivering a reliable and efficient tool for university course management.
