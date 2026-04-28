# CourseScheduleMobileApp

A professional university course scheduling Android application with role-based access for **Admin**, **Lecturer**, and **Guest** users.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose (migration in progress) |
| Architecture | MVVM + Repository Pattern |
| Navigation | Jetpack Navigation Component |
| Local DB | Room (via KSP) |
| Cloud | Firebase Firestore + Firebase Auth |
| Build | AGP 8.8.0, Kotlin 2.0.21, Gradle Kotlin DSL |

---

## User Roles

- **Admin** — Full access: upload CSV/Excel, manage lecturers, classrooms, departments, courses, view analytics
- **Lecturer** — Limited access: personal schedule, availability grid, settings
- **Guest** — Read-only view of public schedule information

---

## Project Structure

```
app/src/main/java/com/university/courseschedule/
├── data/
│   ├── model/          # Domain models (User, Course, Lecturer, …)
│   ├── db/             # Room database, DAOs
│   ├── AuthManager.kt
│   ├── FirestoreManager.kt
│   └── …
├── ui/
│   ├── auth/           # SignIn, SignUp
│   ├── home/           # Dashboard (Admin / Lecturer)
│   ├── calendar/       # Availability grid
│   ├── data/           # Excel/CSV import
│   └── settings/
└── MainActivity.kt
```

---

## Setup

### Prerequisites
- Android Studio Hedgehog or later
- JDK 11+
- Android emulator or device (API 26+)

### Firebase Setup (required for cloud features)
1. Create a Firebase project at [console.firebase.google.com](https://console.firebase.google.com)
2. Add an Android app with package `com.university.courseschedule`
3. Download `google-services.json` and place it in `app/`
4. Enable **Authentication** (Email/Password) and **Firestore** in Firebase Console

### Local Build (offline mode)
The app runs without `google-services.json` — Firebase features are guarded and fall back to local Room DB.

```bash
./gradlew assembleDebug
```

---

## Admin Seed Account

A default admin account is seeded on first launch. Credentials are configured via `gradle.properties`:

```properties
ADMIN_EMAIL=admin@university.com
ADMIN_PASSWORD=admin123
```

---

## Documentation

| File | Purpose |
|---|---|
| `project_content.md` | Master specification — architecture, roles, features |
| `checklist.md` | Implementation phase tracker |
| `README.md` | This file — project overview |

---

## License

MIT License — see [LICENSE](LICENSE)
