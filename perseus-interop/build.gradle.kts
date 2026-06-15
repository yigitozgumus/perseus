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
            pom {
                name.set("Perseus Fragment Interop")
                description.set("Fragment interop support for Perseus navigation.")
                url.set("https://github.com/yigitozgumus/Perseus")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("yigitozgumus")
                        name.set("Yigit Ozgumus")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/yigitozgumus/Perseus.git")
                    developerConnection.set("scm:git:ssh://git@github.com/yigitozgumus/Perseus.git")
                    url.set("https://github.com/yigitozgumus/Perseus")
                }
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
