plugins {
    id("java-library")
    // no version: the Kotlin plugin is already on the build classpath via the
    // Android modules, and Gradle forbids re-declaring a version there
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {
    // Lint API version = AGP version + 23 (AGP 8.9.1 -> 31.9.1)
    compileOnly(libs.lint.api)
}

tasks.jar {
    manifest {
        attributes("Lint-Registry-v2" to "com.cometchat.uikit.lint.UIKitIssueRegistry")
    }
}
