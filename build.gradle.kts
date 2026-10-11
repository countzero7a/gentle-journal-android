// Top-level build file
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // AGP 9 has Kotlin built in; this picks the Kotlin version instead of the one AGP bundles
        classpath(libs.kotlin.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
