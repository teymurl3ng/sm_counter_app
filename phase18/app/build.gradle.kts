plugins {
    id("com.android.application")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
}

val isolatedQaData = providers.gradleProperty("saaf.isolatedQa").orNull.equals("true", ignoreCase = true)
val forceInstrumentationSandbox =
    rootProject.extra.has("qa.forceInstrumentationSandbox") &&
        rootProject.extra["qa.forceInstrumentationSandbox"] == true
val instrumentationSandbox =
    forceInstrumentationSandbox ||
        providers.gradleProperty("saaf.instrumentationSandbox").orNull.equals("true", ignoreCase = true)
val qaClassFilter = if (rootProject.extra.has("qa.connected.classFilter")) {
    rootProject.extra["qa.connected.classFilter"] as String?
} else {
    null
}
val performanceTestBuild = providers.gradleProperty("saaf.performanceTestBuild").orNull.equals("true", ignoreCase = true)
val googleWebClientId = providers.gradleProperty("saaf.googleWebClientId").orNull.orEmpty()
val escapedGoogleWebClientId = googleWebClientId.replace("\\", "\\\\").replace("\"", "\\\"")

android {
    namespace = "com.saaf.milkshop"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.saaf.milkshop"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$escapedGoogleWebClientId\"")
        buildConfigField("boolean", "ENABLE_NATIVE_PREVIEW_FIXTURES", "false")
        buildConfigField("String", "NATIVE_PREVIEW_SHARED_PASSPHRASE", "\"\"")

        testInstrumentationRunner = "com.saaf.milkshop.SaafMilkTestRunner"
        if (!qaClassFilter.isNullOrBlank()) {
            testInstrumentationRunnerArguments["class"] = qaClassFilter
            println("QA app class filter applied (${qaClassFilter.split(',').size} entries)")
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    buildTypes {
        debug {
            // A debug/native-preview APK must never share the production package/database.
            // Instrumentation and isolated-QA retain their established identities; an
            // ordinary assembleDebug now installs as a dedicated preview application.
            applicationIdSuffix = when {
                instrumentationSandbox -> ".qa.tests"
                isolatedQaData -> ".qa"
                else -> ".preview"
            }
            versionNameSuffix = when {
                instrumentationSandbox -> "-qa-tests"
                isolatedQaData -> "-qa"
                else -> "-preview"
            }
            buildConfigField("boolean", "ENABLE_NATIVE_PREVIEW_FIXTURES", "true")
            buildConfigField(
                "String",
                "NATIVE_PREVIEW_SHARED_PASSPHRASE",
                "\"1234567890qwert\"",
            )
        }
        release {
            isMinifyEnabled = false
        }
        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            isProfileable = true
            applicationIdSuffix = if (isolatedQaData) ".qa.benchmark" else ".benchmark"
            versionNameSuffix = "-benchmark"
            matchingFallbacks += listOf("release")
        }
    }

    testBuildType = if (performanceTestBuild) "benchmark" else "debug"

    sourceSets {
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

val verifyHybridWebAssets by tasks.registering {
    group = "verification"
    description = "Checks that the separately built Phase 7 web bundle is ready for APK packaging."
    val hybridAssets = layout.projectDirectory.dir("src/main/assets/hybrid")
    inputs.dir(hybridAssets)
    doLast {
        val index = hybridAssets.file("index.html").asFile
        check(index.isFile) {
            "Missing packaged hybrid UI. Run ../scripts/build-web-ui.ps1 before Gradle packaging."
        }
        check(index.readText().contains("type=\"module\"")) {
            "Packaged hybrid index.html does not contain the expected ES module entry."
        }
    }
}

tasks.matching { it.name == "preBuild" }.configureEach {
    dependsOn(verifyHybridWebAssets)
}

tasks.matching { it.name == "connectedDebugAndroidTest" }.configureEach {
    doFirst {
        check(instrumentationSandbox) {
            "Connected debug tests require -Psaaf.instrumentationSandbox=true so instrumentation cannot replace a user-facing app identity."
        }
    }
}

ksp {
    arg("room.schemaLocation", "${projectDir}/schemas")
    arg("room.generateKotlin", "true")
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.12.00")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.webkit:webkit:1.17.0")
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.sqlite.bundled)
    // Room 3.0.1 migration metadata uses serialization-json 1.8.1. Keep
    // serialization-core on the same runtime version so migration validation
    // does not mix the 1.7.3 ABI with 1.8.1 generated serializers.
    implementation(platform("org.jetbrains.kotlinx:kotlinx-serialization-bom:1.8.1"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core")
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.bouncycastle.provider)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.google.identity.googleid)
    implementation(libs.google.play.services.auth)
    implementation(libs.kotlinx.coroutines.play.services)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.work.testing)
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
