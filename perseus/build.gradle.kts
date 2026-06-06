plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.yigitozgumus.perseus"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 29
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Navigation3 (core dependency)
    api(libs.androidx.navigation3.runtime)
    api(libs.androidx.navigation3.ui)

    // Fragment Compose (for FragmentEntry interop)
    api(libs.androidx.fragment.compose)

    // Lifecycle ViewModel Nav3 (for scoped ViewModels)
    api(libs.androidx.lifecycle.viewmodel.navigation3)

    // Koin (optional, consumers add if they use Koin)
    compileOnly(libs.koin.compose.viewmodel)
    compileOnly(libs.koin.navigation3)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
}
