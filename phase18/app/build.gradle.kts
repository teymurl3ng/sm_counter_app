plugins {
    id("com.android.application")
    id("com.google.devtools.ksp")
}

val isolatedQaData = providers.gradleProperty("saaf.isolatedQa").orNull.equals("true", ignoreCase = true)

android {
    namespace = "com.saaf.milkshop"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.saaf.milkshop"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "com.saaf.milkshop.SaafMilkTestRunner"
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            if (isolatedQaData) {
                applicationIdSuffix = ".qa"
                versionNameSuffix = "-qa"
            }
        }
        release {
            isMinifyEnabled = false
        }
    }

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

ksp {
    arg("room.schemaLocation", "${projectDir}/schemas")
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.lifecycle.viewmodel)
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
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.room.testing)
}
