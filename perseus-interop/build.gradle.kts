import org.gradle.api.publish.maven.MavenPublication

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.dokka)
    `maven-publish`
}

version = "0.1.0-SNAPSHOT"
group = "com.yigitozgumus.perseus"


android {
    namespace = "com.yigitozgumus.perseus.interop"
    compileSdk = 36

    defaultConfig {
        minSdk = 29
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

kotlin {
    explicitApi()
}

publishing {
    publications {
        register<MavenPublication>("release") {
            artifactId = "perseus-interop"
            afterEvaluate {
                from(components["release"])
            }
        }
    }
}

dependencies {
    api(project(":perseus-core"))
    api(libs.androidx.fragment.compose)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
}
