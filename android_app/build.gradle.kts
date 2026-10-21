plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "2.2.10" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
}

val gRootBuildDir = file("G:/gabay_builds/android_builds/root")
if (gRootBuildDir.parentFile?.exists() == true) {
    layout.buildDirectory.set(gRootBuildDir)
}

