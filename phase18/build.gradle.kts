plugins {
    id("com.android.application") version "9.0.1" apply false
    id("com.google.devtools.ksp") version "2.2.10-2.0.2" apply false
}

val verifyBridgeContract by tasks.registering(Exec::class) {
    group = "verification"
    description = "Verifies the canonical bridge manifest and generated Kotlin/TypeScript projections."
    workingDir = rootDir.parentFile
    val pythonCommand = providers.environmentVariable("PYTHON").orElse(
        if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) "python" else "python3",
    )
    commandLine(pythonCommand.get(), "scripts/verify-bridge-contract.py")
}

val verifyUiCleanup by tasks.registering(Exec::class) {
    group = "verification"
    description = "Rejects removed prototype, browser-state, dependency, and misleading UI behavior."
    workingDir = rootDir.parentFile
    val pythonCommand = providers.environmentVariable("PYTHON").orElse(
        if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) "python" else "python3",
    )
    commandLine(pythonCommand.get(), "scripts/verify-ui-cleanup.py")
}

subprojects {
    tasks.matching { it.name == "preBuild" }.configureEach {
        dependsOn(rootProject.tasks.named("verifyBridgeContract"))
        dependsOn(rootProject.tasks.named("verifyUiCleanup"))
    }
}
