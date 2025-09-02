plugins {
    id("com.android.library")
    kotlin("android")

    id("org.jetbrains.kotlin.plugin.serialization")
    id("mvn-publish")
}

val VERSION_NAME: String by project

android {
    namespace = "com.segment.analytics.kotlin.destinations.clevertap"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        multiDexEnabled = true
        minSdk = 21

        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("proguard-consumer-rules.pro")

        buildConfigField("String", "VERSION_NAME", "\"$VERSION_NAME\"")
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
}

// Test Dependencies
dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.14.2")

    // Add Robolectric dependencies.
    testImplementation("org.robolectric:robolectric:4.12.2")
    testImplementation("androidx.test:core:1.7.0")

    testImplementation("com.google.firebase:firebase-messaging:25.0.0")
}

// required for mvn-publish
// too bad we can't move it into mvn-publish plugin because `android`is only accessible here
tasks {
    val sourceFiles = android.sourceSets.getByName("main").java.srcDirs

    register<Javadoc>("withJavadoc") {
        isFailOnError = false

        setSource(sourceFiles)

        // add Android runtime classpath
        android.bootClasspath.forEach { classpath += project.fileTree(it) }

        // add classpath for all dependencies
        android.libraryVariants.forEach { variant ->
            variant.javaCompileProvider.get().classpath.files.forEach { file ->
                classpath += project.fileTree(file)
            }
        }
    }

    register<Jar>("withJavadocJar") {
        archiveClassifier.set("javadoc")
        dependsOn(named("withJavadoc"))
        val destination = named<Javadoc>("withJavadoc").get().destinationDir
        from(destination)
    }

    register<Jar>("withSourcesJar") {
        archiveClassifier.set("sources")
        from(sourceFiles)
    }
}