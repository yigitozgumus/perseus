import org.gradle.api.publish.maven.MavenPublication

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    `maven-publish`
}

version = "0.1.0-SNAPSHOT"
group = "com.yigitozgumus.perseus"

android {
    namespace = "com.yigitozgumus.perseus.koin"
    compileSdk = 36

    defaultConfig {
        minSdk = 29
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
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
            artifactId = "perseus-koin"
            afterEvaluate {
                from(components["release"])
            }
        }
    }
}

dependencies {
    api(project(":perseus-interop"))
    api(libs.koin.core.viewmodel)
}
