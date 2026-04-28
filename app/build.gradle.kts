plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // KSP: replaces kapt for Room annotation processing.
    // kapt used the removed Configuration.fileCollection(Spec) API in Kotlin 1.9.x.
    id("com.google.devtools.ksp")
    // Safe Args: generates type-safe Kotlin classes for nav graph actions.
    id("androidx.navigation.safeargs.kotlin")
    // Google Services plugin for Firebase
    id("com.google.gms.google-services")
}

android {
    namespace = "com.university.courseschedule"
    compileSdk = 35

    // Redirect build output to a temporary local folder to avoid OneDrive file locking issues.
    // This keeps the heavy I/O of the build process outside of the synced OneDrive folder.
    layout.buildDirectory.set(file(System.getProperty("java.io.tmpdir") + "/android-builds/${project.name}/app"))

    defaultConfig {
        applicationId = "com.university.courseschedule"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Admin credentials from gradle.properties (with fallbacks)
        buildConfigField("String", "ADMIN_EMAIL", "\"${project.findProperty("ADMIN_EMAIL") ?: "admin@university.com"}\"")
        buildConfigField("String", "ADMIN_PASSWORD", "\"${project.findProperty("ADMIN_PASSWORD") ?: "admin123"}\"")
        buildConfigField("String", "ADMIN_NAME", "\"${project.findProperty("ADMIN_NAME") ?: "Admin"}\"")
        buildConfigField("String", "ADMIN_SURNAME", "\"${project.findProperty("ADMIN_SURNAME") ?: "User"}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        // Java 11 is required when using AGP 8.8.0 with Kotlin 2.x.
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    // Core
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")

    // Navigation Component
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.5")

    // Room — annotation processor via KSP instead of kapt
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // RecyclerView & CardView (used in DataFragment list and CalendarFragment grid)
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")

    // Apache POI for Excel (.xlsx) parsing
    implementation("org.apache.poi:poi:5.2.5")
    implementation("org.apache.poi:poi-ooxml:5.2.5")

    // Firebase SDK (BOM - manages versions automatically)
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
