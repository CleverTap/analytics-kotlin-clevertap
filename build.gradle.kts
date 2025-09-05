plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    id("org.jetbrains.kotlin.jvm") version "2.0.10"

    // Apply the java-library plugin for API and implementation separation.
    `java-library`
    id("com.vanniktech.maven.publish") version "0.34.0" apply false
}

buildscript {
    repositories {
        // Use JCenter for resolving dependencies.
        mavenCentral()
        google()
        gradlePluginPortal()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.10")
        classpath("org.jetbrains.kotlin:kotlin-serialization:2.0.10")
        classpath("com.android.tools.build:gradle:8.6.1")
    }
}

allprojects {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}