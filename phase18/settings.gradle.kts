pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    // Toolchain auto-provisioning: unit tests must run on JDK 17 (Robolectric
    // 4.14.1's ASM cannot read JDK 25 class files during sandbox
    // instrumentation; JDK 17 also matches the CI workflow).
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MilkShop"
include(":app")
