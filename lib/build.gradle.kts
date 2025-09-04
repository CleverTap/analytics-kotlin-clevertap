import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.library")
    kotlin("android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.vanniktech.maven.publish")
}

// gradle.properties
val publishedGroupId = project.findProperty("clevertapPublishedGroupId") as String
val artifactId = project.findProperty("clevertapArtifactId") as String
val libraryVersion = project.findProperty("VERSION_NAME") as String
val libraryDescription = project.findProperty("clevertapLibraryDescription") as String
val siteUrl = project.findProperty("siteUrl") as String
val gitUrl = project.findProperty("gitUrl") as String
val licenseName = project.findProperty("licenseName") as String
val licenseUrl = project.findProperty("licenseUrl") as String

// local.properties
val localPropsFile = rootProject.file("local.properties")
val localProps = Properties().apply {
    if (localPropsFile.exists()) {
        load(FileInputStream(localPropsFile))
    }
}
val developerId = localProps.getProperty("developerId").orEmpty()
val developerName = localProps.getProperty("developerName").orEmpty()
val developerEmail = localProps.getProperty("developerEmail").orEmpty()

android {
    namespace = "com.segment.analytics.kotlin.destinations.clevertap"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        multiDexEnabled = true
        minSdk = 21

        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("proguard-consumer-rules.pro")

        buildConfigField("String", "VERSION_NAME", "\"${project.property("VERSION_NAME")}\"")
        buildConfigField("int", "VERSION_CODE", "${project.property("VERSION_CODE")}")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation("com.segment.analytics.kotlin:android:1.21.0")
    implementation("androidx.multidex:multidex:2.0.1")
    implementation("androidx.core:core-ktx:1.13.0")

    api("com.clevertap.android:clevertap-android-sdk:7.5.1")

    // Test Dependencies
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.14.2")

    // Add Robolectric dependencies.
    testImplementation("org.robolectric:robolectric:4.12.2")
    testImplementation("androidx.test:core:1.7.0")

    testImplementation("com.google.firebase:firebase-messaging:25.0.0")
}

// Task to rename the library output name
tasks.withType<AbstractArchiveTask>().configureEach {
    if (name.contains("Release", ignoreCase = true)) {
        archiveFileName.set("$artifactId-$libraryVersion.aar")
    } else if (name.contains("Debug", ignoreCase = true)) {
        archiveFileName.set("$artifactId-debug-$libraryVersion.aar")
    }
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates(publishedGroupId, artifactId, libraryVersion)

    pom {
        name.set(artifactId)
        description.set(libraryDescription)
        url.set(siteUrl)

        licenses {
            license {
                name.set(licenseName)
                url.set(licenseUrl)
            }
        }

        developers {
            developer {
                id.set(developerId)
                name.set(developerName)
                email.set(developerEmail)
            }
        }

        scm {
            connection.set("scm:git:$gitUrl")
            developerConnection.set("scm:git:$gitUrl")
            url.set(siteUrl)
        }
    }
}