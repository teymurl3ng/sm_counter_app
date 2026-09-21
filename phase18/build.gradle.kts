plugins {
    id("com.android.application") version "9.0.1" apply false
    id("com.google.devtools.ksp") version "2.2.10-2.0.2" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
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

val repoRoot = rootDir.parentFile
val allowLogic = providers.gradleProperty("allow.logic").orNull.equals("true", ignoreCase = true)
val qaScreenName = providers.gradleProperty("qa.screen").orNull?.trim().orEmpty()
val qaDevice = providers.gradleProperty("qa.device").orNull?.trim().orEmpty()

val screenClassMap = mapOf(
    "auth" to listOf(
        // NativeOwnerBootstrapUiTest requires saafSkipBootstrap=true + empty DB; runner seed
        // makes first-run unreachable in shared connected suites. Covered separately via
        // scripts/qa/run-connected-qa.ps1 auth-bootstrap when needed.
        "com.saaf.milkshop.NativeAuthenticationScreenContractTest",
        "com.saaf.milkshop.NativeAuthenticationFacadeTest",
        "com.saaf.milkshop.NativeFoundationUiTest",
    ),
    "sale" to listOf(
        "com.saaf.milkshop.NativeFoundationUiTest",
        "com.saaf.milkshop.NativeSaleHardeningContractTest",
        "com.saaf.milkshop.NativeSaleScreenContractTest",
        "com.saaf.milkshop.Phase8SaleOperationsTest",
        "com.saaf.milkshop.qa.NativeGoldenLedgerRegressionTest",
    ),
    "collect" to listOf(
        "com.saaf.milkshop.NativeCollectionFacadeTest",
        "com.saaf.milkshop.NativeCollectionScreenContractTest",
        "com.saaf.milkshop.qa.NativeCollectionAcceptanceGapsTest",
        "com.saaf.milkshop.qa.NativeCollectionDeviceEvidenceTest",
        "com.saaf.milkshop.NativeFoundationUiTest#everyBottomDestinationProvidesItsDefinedPreviewResponse",
        "com.saaf.milkshop.NativeFoundationUiTest#collectPaymentWritesRoomShowsReceiptAndOffersTheNextVisibleActions",
        "com.saaf.milkshop.qa.NativeGoldenLedgerRegressionTest",
    ),
    "activity" to listOf(
        "com.saaf.milkshop.NativeActivityFacadeTest",
        "com.saaf.milkshop.NativeActivityScreenContractTest",
        "com.saaf.milkshop.NativeFoundationUiTest#everyBottomDestinationProvidesItsDefinedPreviewResponse",
        "com.saaf.milkshop.NativeFoundationUiTest#collectPaymentWritesRoomShowsReceiptAndOffersTheNextVisibleActions",
    ),
    "more" to listOf(
        "com.saaf.milkshop.NativeMoreFacadeTest",
        "com.saaf.milkshop.NativeMoreScreenContractTest",
        "com.saaf.milkshop.NativeFoundationUiTest#everyBottomDestinationProvidesItsDefinedPreviewResponse",
        "com.saaf.milkshop.NativeFoundationUiTest#moreMenuHardwareBackReturnsToSellAndKeepsSession",
        "com.saaf.milkshop.NativeFoundationUiTest#switchAccountHardwareBackKeepsSessionAndReturnsToMore",
        "com.saaf.milkshop.NativeFoundationUiTest#switchAccountUiBackKeepsSessionAndReturnsToMore",
    ),
    "work" to listOf(
        "com.saaf.milkshop.NativeOperationsWorkFacadeTest",
        "com.saaf.milkshop.NativeOperationsWorkScreenContractTest",
        "com.saaf.milkshop.NativeFoundationUiTest#operationsWorkHomeShowsPhysicalContext",
        "com.saaf.milkshop.NativeFoundationUiTest#operationsNavPreviewDestinationsShowExplicitNotices",
        "com.saaf.milkshop.NativeFoundationUiTest#operationsReceiveMilkOpensConnectedEntry",
        "com.saaf.milkshop.NativeFoundationUiTest#operationsWorkHardwareBackEndsSessionLikeSell",
    ),
    "receive" to listOf(
        "com.saaf.milkshop.NativeReceiveMilkFacadeTest",
        "com.saaf.milkshop.NativeReceiveMilkScreenContractTest",
        "com.saaf.milkshop.NativeFoundationUiTest#operationsReceiveMilkOpensConnectedEntry",
        "com.saaf.milkshop.NativeFoundationUiTest#operationsReceiveMilkRecordsPhysicalReceipt",
    ),
    "yogurt" to listOf(
        "com.saaf.milkshop.NativeMakeYogurtFacadeTest",
        "com.saaf.milkshop.NativeMakeYogurtScreenContractTest",
        "com.saaf.milkshop.NativeFoundationUiTest#operationsMakeYogurtOpensConnectedEntry",
        "com.saaf.milkshop.NativeFoundationUiTest#operationsMakeYogurtRecordsPhysicalBatch",
    ),
    "spoilage" to listOf(
        "com.saaf.milkshop.NativeRecordSpoilageFacadeTest",
        "com.saaf.milkshop.NativeRecordSpoilageScreenContractTest",
        "com.saaf.milkshop.NativeFoundationUiTest#operationsRecordSpoilageOpensConnectedEntry",
        "com.saaf.milkshop.NativeFoundationUiTest#operationsRecordSpoilageRecordsPhysicalLoss",
    ),
    "deliveries" to listOf(
        "com.saaf.milkshop.NativeDeliveriesFacadeTest",
        "com.saaf.milkshop.NativeDeliveriesScreenContractTest",
        "com.saaf.milkshop.NativeFoundationUiTest#operationsDeliveriesOpensConnectedEntry",
        "com.saaf.milkshop.NativeFoundationUiTest#operationsDeliveriesMarksPhysicalDelivery",
    ),
    "attendance" to listOf(
        "com.saaf.milkshop.NativeAttendanceFacadeTest",
        "com.saaf.milkshop.NativeAttendanceScreenContractTest",
        "com.saaf.milkshop.NativeFoundationUiTest#operationsAttendanceOpensConnectedEntry",
        "com.saaf.milkshop.NativeFoundationUiTest#operationsAttendanceMarksPresent",
    ),
    "opsmore" to listOf(
        "com.saaf.milkshop.NativeOperationsMoreFacadeTest",
        "com.saaf.milkshop.NativeOperationsMoreScreenContractTest",
        "com.saaf.milkshop.NativeFoundationUiTest#operationsMoreOpensConnectedEntry",
        "com.saaf.milkshop.NativeFoundationUiTest#operationsMoreSignsOutToWelcome",
    ),
    "owner" to listOf(
        "com.saaf.milkshop.NativeOwnerOverviewFacadeTest",
        "com.saaf.milkshop.NativeOwnerOverviewScreenContractTest",
        "com.saaf.milkshop.NativeFoundationUiTest#ownerOverviewOpensConnectedEntry",
        "com.saaf.milkshop.NativeFoundationUiTest#ownerOverviewPreviewNavShowsExplicitNotices",
    ),
    "ownerwork" to listOf(
        "com.saaf.milkshop.NativeOwnerWorkFacadeTest",
        "com.saaf.milkshop.NativeOwnerWorkScreenContractTest",
        "com.saaf.milkshop.NativeOwnerSaleEntryScreenContractTest",
        "com.saaf.milkshop.NativeOwnerCollectionEntryScreenContractTest",
        "com.saaf.milkshop.NativeOwnerDeliveriesEntryScreenContractTest",
        "com.saaf.milkshop.NativeFoundationUiTest#ownerWorkOpensConnectedEntry",
        "com.saaf.milkshop.NativeFoundationUiTest#ownerWorkExpenseShowsExplicitPreview",
        "com.saaf.milkshop.NativeFoundationUiTest#ownerOverviewPreviewNavShowsExplicitNotices",
        "com.saaf.milkshop.NativeFoundationUiTest#ownerWorkRecordSaleOpensUnderOwnerChrome",
        "com.saaf.milkshop.NativeFoundationUiTest#ownerWorkRecordSaleHardwareBackReturnsToWork",
        "com.saaf.milkshop.NativeFoundationUiTest#ownerWorkRecordSaleDirtyBackRequiresDiscard",
        "com.saaf.milkshop.NativeFoundationUiTest#ownerWorkRecordPaymentOpensUnderOwnerChrome",
        "com.saaf.milkshop.NativeFoundationUiTest#ownerWorkRecordPaymentHardwareBackReturnsToWork",
        "com.saaf.milkshop.NativeFoundationUiTest#ownerWorkReceiveMilkOpensUnderOwnerChrome",
        "com.saaf.milkshop.NativeFoundationUiTest#ownerWorkReceiveMilkHardwareBackReturnsToWork",
        "com.saaf.milkshop.NativeFoundationUiTest#ownerWorkMakeYogurtOpensUnderOwnerChrome",
        "com.saaf.milkshop.NativeFoundationUiTest#ownerWorkMakeYogurtHardwareBackReturnsToWork",
        "com.saaf.milkshop.NativeFoundationUiTest#ownerWorkRecordSpoilageOpensUnderOwnerChrome",
        "com.saaf.milkshop.NativeFoundationUiTest#ownerWorkRecordSpoilageHardwareBackReturnsToWork",
        "com.saaf.milkshop.NativeFoundationUiTest#ownerWorkDeliveriesOpensUnderOwnerChrome",
        "com.saaf.milkshop.NativeFoundationUiTest#ownerWorkDeliveriesHardwareBackReturnsToWork",
    ),
    "ownerrecords" to listOf(
        "com.saaf.milkshop.NativeOwnerRecordsFacadeTest",
        "com.saaf.milkshop.NativeOwnerRecordsScreenContractTest",
        "com.saaf.milkshop.NativeFoundationUiTest#ownerRecordsOpensUnderOwnerChrome",
        "com.saaf.milkshop.NativeFoundationUiTest#ownerOverviewPreviewNavShowsExplicitNotices",
    ),
)

// Apply connected-test filter/sandbox via rootProject.extra (setProjectProperties is not
// reliably visible to subproject providers.gradleProperty).
val requestedTasks = gradle.startParameter.taskNames.map { it.substringAfterLast(':') }
val wantsQaScreen = requestedTasks.any { it.equals("qaScreen", ignoreCase = true) }
val wantsQaCheckpoint = requestedTasks.any { it.equals("qaCheckpoint", ignoreCase = true) }
if (wantsQaScreen || wantsQaCheckpoint) {
    val classes = if (wantsQaCheckpoint) {
        (screenClassMap.getValue("auth") + screenClassMap.getValue("sale") +
            screenClassMap.getValue("collect") + screenClassMap.getValue("activity") +
            screenClassMap.getValue("more") + screenClassMap.getValue("work") +
            screenClassMap.getValue("receive") + screenClassMap.getValue("yogurt") +
            screenClassMap.getValue("spoilage") + screenClassMap.getValue("deliveries") +
            screenClassMap.getValue("attendance") + screenClassMap.getValue("opsmore") +
            screenClassMap.getValue("owner") + screenClassMap.getValue("ownerwork") +
            screenClassMap.getValue("ownerrecords"))
            .distinct()
    } else {
        val screen = qaScreenName.lowercase()
        require(screen.isNotEmpty()) { "qaScreen requires -Pqa.screen=auth|sale|collect|activity|more|work|receive|yogurt|spoilage|deliveries|attendance|opsmore|owner|ownerwork|ownerrecords" }
        require(screenClassMap.containsKey(screen)) {
            "Unknown qa.screen='$qaScreenName'. Valid: ${screenClassMap.keys.sorted()}"
        }
        screenClassMap.getValue(screen)
    }
    require(classes.isNotEmpty()) { "Selected QA screen has zero registered tests" }
    val serial = qaDevice.ifEmpty { System.getenv("ANDROID_SERIAL").orEmpty() }
        .ifEmpty { "R8VXA02SNKH" }
    rootProject.extra["qa.forceInstrumentationSandbox"] = true
    rootProject.extra["qa.connected.classFilter"] = classes.joinToString(",")
    println("QA connected filter classCount=${classes.size} preferredSerial=$serial")
    println("QA tip: export ANDROID_SERIAL=$serial before gradlew if multiple adb devices are connected")
}

fun Exec.powershellFile(scriptRelative: String, extraArgs: List<String> = emptyList()) {
    workingDir = repoRoot
    commandLine(
        listOf(
            "powershell",
            "-NoProfile",
            "-ExecutionPolicy",
            "Bypass",
            "-File",
            scriptRelative,
        ) + extraArgs,
    )
}

val checkUiOnly by tasks.registering(Exec::class) {
    group = "qa"
    description = "Fails when protected domain/SQL files change unless -Pallow.logic=true."
    powershellFile(
        "scripts/qa/check-ui-only.ps1",
        if (allowLogic) listOf("-AllowLogic") else emptyList(),
    )
}

val checkFrozenBaselines by tasks.registering(Exec::class) {
    group = "qa"
    description = "Verifies PHASE0 frozen logic-spec and SQL blueprint hashes."
    powershellFile("scripts/qa/check-frozen-baselines.ps1")
}

val checkNativeInventory by tasks.registering(Exec::class) {
    group = "qa"
    description = "Machine inventory orphan/duplicate/missing-test coverage check."
    powershellFile("scripts/qa/check-native-inventory.ps1")
}

val checkNativeInventoryCheckpoint by tasks.registering(Exec::class) {
    group = "qa"
    description = "Inventory check requiring untested=0."
    powershellFile("scripts/qa/check-native-inventory.ps1", listOf("-RequireZeroUntested"))
}

val checkGoldenLedgerContract by tasks.registering(Exec::class) {
    group = "qa"
    description = "Host contract for the instrumented golden-ledger regression."
    powershellFile("scripts/qa/check-golden-ledger-contract.ps1")
}

val devicePreflight by tasks.registering(Exec::class) {
    group = "qa"
    description = "Selects A06 device of record; never silently picks an arbitrary device."
    powershellFile(
        "scripts/qa/device-preflight.ps1",
        buildList {
            if (qaDevice.isNotEmpty()) {
                add("-RequestedSerial")
                add(qaDevice)
            }
        },
    )
}

val qaFast by tasks.registering {
    group = "qa"
    description = "Cheap host-side native UI gate (no physical device)."
    dependsOn(
        checkUiOnly,
        checkFrozenBaselines,
        checkNativeInventory,
        checkGoldenLedgerContract,
        ":app:testDebugUnitTest",
        ":app:compileDebugKotlin",
        ":app:compileDebugAndroidTestKotlin",
    )
    doLast {
        println("QA_FAST: PASS")
    }
}

val qaScreen by tasks.registering {
    group = "qa"
    description = "Device acceptance for one native screen: -Pqa.screen=auth|sale|collect|activity|more|work|receive|yogurt|spoilage|deliveries|attendance|opsmore|owner|ownerwork (no nested Gradle)."
    dependsOn(checkUiOnly, checkNativeInventory, devicePreflight, ":app:connectedDebugAndroidTest")
    doFirst {
        val screen = qaScreenName.lowercase()
        if (screen.isEmpty()) {
            println("QA_SCREEN[]: FAIL")
            throw GradleException("qaScreen requires -Pqa.screen=auth|sale|collect|activity|more|work|receive|yogurt|spoilage|deliveries|attendance|opsmore|owner|ownerwork")
        }
        if (!screenClassMap.containsKey(screen)) {
            println("QA_SCREEN[$screen]: FAIL")
            throw GradleException("Unknown qa.screen='$qaScreenName'. Valid: ${screenClassMap.keys.sorted()}")
        }
        if (screenClassMap.getValue(screen).isEmpty()) {
            println("QA_SCREEN[$screen]: FAIL")
            throw GradleException("Screen '$screen' has zero registered tests")
        }
    }
    doLast {
        println("QA_SCREEN[${qaScreenName.lowercase()}]: PASS")
    }
}

val qaCheckpointPullEvidence by tasks.registering(Exec::class) {
    group = "qa"
    description = "Pull native evidence PNGs for the current checkpoint run only."
    mustRunAfter(":app:connectedDebugAndroidTest")
    powershellFile("scripts/qa/pull-native-evidence.ps1")
}

val qaCheckpointCheckEvidence by tasks.registering(Exec::class) {
    group = "qa"
    description = "Fail when current-run evidence is missing review sidecars or still UNREVIEWED/FAIL."
    mustRunAfter(qaCheckpointPullEvidence)
    powershellFile("scripts/qa/check-evidence-review.ps1", listOf("-RequireReviewed"))
}

val qaCheckpoint by tasks.registering {
    group = "qa"
    description = "Heavyweight Auth+Sale+Collect native acceptance gate with inventory and evidence."
    dependsOn(
        checkUiOnly,
        checkFrozenBaselines,
        checkNativeInventoryCheckpoint,
        checkGoldenLedgerContract,
        devicePreflight,
        ":app:testDebugUnitTest",
        ":app:connectedDebugAndroidTest",
        qaCheckpointPullEvidence,
        qaCheckpointCheckEvidence,
    )
    doLast {
        val summaryFile = repoRoot.resolve("phase14-evidence/checkpoint-evidence-summary.json")
        var evidence = 0
        var unreviewed = 0
        var visualAcceptance = "NONE"
        if (summaryFile.isFile) {
            val text = summaryFile.readText()
            fun intField(name: String): Int {
                val match = Regex("\"$name\"\\s*:\\s*(\\d+)").find(text)
                return match?.groupValues?.get(1)?.toIntOrNull() ?: 0
            }
            fun stringField(name: String): String {
                val match = Regex("\"$name\"\\s*:\\s*\"([^\"]+)\"").find(text)
                return match?.groupValues?.get(1) ?: "NONE"
            }
            evidence = intField("evidence")
            unreviewed = intField("unreviewed")
            visualAcceptance = stringField("visualAcceptance")
        }
        val logicException = if (allowLogic) "YES" else "NO"
        println(
            "QA_CHECKPOINT: PASS | auth=PASS | sale=PASS | collect=PASS | untested=0 | evidence=$evidence | unreviewed=$unreviewed | visualAcceptance=$visualAcceptance | logicException=$logicException",
        )
    }
}
