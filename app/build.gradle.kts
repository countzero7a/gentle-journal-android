// Kotlin is built into AGP 9; only the Compose compiler and KSP are added
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.gentlejournal"
    compileSdk = 37

    // CI sets GITHUB_RUN_NUMBER, so every build installs as an update over the previous one
    val buildNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull()

    defaultConfig {
        applicationId = "com.gentlejournal"
        minSdk = 26
        targetSdk = 34
        versionCode = buildNumber ?: 1
        versionName = "1.0.0" + (buildNumber?.let { "-build.$it" } ?: "")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    // Release signing key comes from environment variables set by
    // .github/workflows/build.yml. Without them, release builds are unsigned.
    val releaseKeystore = System.getenv("SIGNING_KEYSTORE_PATH")
    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = System.getenv("SIGNING_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }

    // Kotlin's jvmTarget follows targetCompatibility with AGP's built-in Kotlin
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

ksp {
    // Exported schemas are committed so future migrations can be written and tested
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // Compose (versions from the BOM)
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    // Only Add, Check and ArrowBack are used; the extended icon set is huge and slows builds
    implementation(libs.androidx.compose.material.icons.core)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Room (local-only DB)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // JSON (backup export/import)
    implementation(libs.kotlinx.serialization.json)

    // DataStore (lightweight preferences)
    implementation(libs.androidx.datastore.preferences)

    // SQLCipher (database encryption); 4.x keeps the same database format
    implementation(libs.sqlcipher.android)
    implementation(libs.androidx.sqlite.ktx)

    // AndroidX Security (encrypted key storage)
    implementation(libs.androidx.security.crypto)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(composeBom)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
