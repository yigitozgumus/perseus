import com.vanniktech.maven.publish.AndroidSingleVariantLibrary

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.dokka)
    alias(libs.plugins.maven.publish)
}

version = project.property("LIBRARY_VERSION")
group = project.property("LIBRARY_GROUP")


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

}

kotlin {
    explicitApi()
}

mavenPublishing {
    coordinates(group.toString(), "perseus-interop", version.toString())
    publishToMavenCentral()
    signAllPublications()
    configure(
        AndroidSingleVariantLibrary(
            variant = "release",
            sourcesJar = true,
            publishJavadocJar = true,
        )
    )
}

dependencies {
    api(project(":perseus-core"))
    api(libs.androidx.fragment.compose)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
}
