import java.io.ByteArrayOutputStream
import java.io.File

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Redirect all app build outputs to G: drive to protect C: drive space if mounted (disabled on CI or non-Windows)
val isCI = System.getenv("CI") == "true" || System.getenv("GITHUB_ACTIONS") == "true"
val isWindows = System.getProperty("os.name")?.lowercase()?.contains("win") == true
if (!isCI && isWindows) {
    val gBuildDir = File("G:/gabay_builds/android_builds/app")
    if (gBuildDir.parentFile?.exists() == true) {
        layout.buildDirectory.set(gBuildDir)
    }
}

fun getGitCommitCount(): Int {
    val propCount = (project.findProperty("versionCode") as? String)?.toIntOrNull()
    if (propCount != null && propCount > 0) return propCount

    val envCount = System.getenv("COMMIT_COUNT")?.toIntOrNull()
    if (envCount != null && envCount > 0) return envCount

    return try {
        val stdout = ByteArrayOutputStream()
        project.exec {
            commandLine("git", "rev-list", "--count", "HEAD")
            standardOutput = stdout
            isIgnoreExitValue = true
        }
        val count = stdout.toString().trim().toIntOrNull() ?: 1
        if (count > 0) count else 1
    } catch (e: Exception) {
        1
    }
}

fun getGitVersionName(commitCount: Int): String {
    val propName = project.findProperty("versionName") as? String
    if (!propName.isNullOrBlank()) return propName

    val envName = System.getenv("APP_VERSION_NAME")
    if (!envName.isNullOrBlank()) return envName

    return "1.0.$commitCount"
}

android {
    namespace = "dev.gabay.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.gabay.app"
        minSdk = 26
        targetSdk = 35

        val commitCount = getGitCommitCount()
        versionCode = commitCount
        versionName = getGitVersionName(commitCount)

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters.add("arm64-v8a")
        }
    }

    val releaseKeystoreFile: File? = when {
        !System.getenv("KEYSTORE_FILE").isNullOrBlank() && File(System.getenv("KEYSTORE_FILE")!!).exists() -> File(System.getenv("KEYSTORE_FILE")!!)
        File(rootDir, "release.keystore").exists() -> File(rootDir, "release.keystore")
        File(projectDir, "release.keystore").exists() -> File(projectDir, "release.keystore")
        // newkey is the keystore file itself (PKCS12), not a directory
        isWindows && File("C:/Users/Administrator/Documents/newkey").isFile -> File("C:/Users/Administrator/Documents/newkey")
        else -> null
    }

    signingConfigs {
        create("release") {
            if (releaseKeystoreFile != null) {
                storeFile = releaseKeystoreFile
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "12345678"
                keyAlias = System.getenv("KEY_ALIAS") ?: "key1"
                keyPassword = System.getenv("KEY_PASSWORD") ?: "12345678"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            if (releaseKeystoreFile != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            isMinifyEnabled = false
            ndk {
                abiFilters.add("arm64-v8a")
            }
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = true
            excludes += listOf("lib/armeabi-v7a/**", "lib/x86/**", "lib/x86_64/**", "lib/mips/**", "lib/mips64/**", "lib/armeabi/**")
        }
    }
}

dependencies {
    // MapLibre GL Native SDK (Vector + 3D Extrusions + Raster Topo/Sat)
    implementation("org.maplibre.gl:android-sdk:13.6.1")

    // AndroidX & Jetpack Compose
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-service:2.8.6")

    // JNA for UniFFI FFI bindings
    implementation("net.java.dev.jna:jna:5.14.0@aar")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}

tasks.register("printAppVersion") {
    doLast {
        println("GABAY_APP_VERSION_NAME=${android.defaultConfig.versionName}")
        println("GABAY_APP_VERSION_CODE=${android.defaultConfig.versionCode}")
    }
}
