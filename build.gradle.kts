// Top-level build file: configuration shared across all sub-projects/modules.
plugins {
    id("com.android.application") version "8.8.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    // KSP replaces kapt — kapt calls Configuration.fileCollection(Spec) which
    // was removed in Gradle 8.3+. KSP does not use that API.
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false
    // Safe Args version must match the Navigation library version.
    id("androidx.navigation.safeargs.kotlin") version "2.8.5" apply false
    // Google Services plugin for Firebase
    id("com.google.gms.google-services") version "4.4.2" apply false
}
