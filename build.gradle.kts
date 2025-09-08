val GROUP: String by project
val VERSION_NAME: String by project

group = GROUP
version = getVersionName()

plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    id("org.jetbrains.kotlin.jvm") version "2.0.10"

    // Apply the java-library plugin for API and implementation separation.
    `java-library`
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"

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
        classpath("com.google.gms:google-services:4.4.3")
    }
}

allprojects {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

nexusPublishing {
    repositories {
        sonatype()
    }
}

fun getVersionName() =
    if (hasProperty("release"))
        VERSION_NAME
    else
        "$VERSION_NAME-SNAPSHOT"
