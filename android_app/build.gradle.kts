plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "2.2.10" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
}

val isCI = System.getenv("CI") == "true" || System.getenv("GITHUB_ACTIONS") == "true"
val gRootBuildDir = file("G:/gabay_builds/android_builds/root")
if (!isCI && gRootBuildDir.parentFile?.exists() == true) {
    layout.buildDirectory.set(gRootBuildDir)
}

