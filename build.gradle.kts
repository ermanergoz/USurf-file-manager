// The Android, Kotlin, KSP, Realm and Safe Args plugins are loaded here, once, so they share one class loader: Realm's
// plugin works on the Android plugin's classes.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.realm) apply false
    alias(libs.plugins.navigation.safeargs) apply false
}
