package com.saaf.milkshop

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.owner.NativeOwnerBudgetRow
import com.saaf.milkshop.application.owner.NativeOwnerCollectionsTargetRow
import com.saaf.milkshop.application.owner.NativeOwnerCostingComponentRow
import com.saaf.milkshop.application.owner.NativeOwnerCostingPolicyDetail
import com.saaf.milkshop.application.owner.NativeOwnerCostingPolicyRow
import com.saaf.milkshop.application.owner.NativeOwnerMoreContext
import com.saaf.milkshop.application.owner.NativeOwnerOpeningCostRow
import com.saaf.milkshop.application.owner.NativeOwnerYieldBatchRow
import com.saaf.milkshop.application.owner.NativeOwnerYieldPolicyRow
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.BudgetPlanInput
import com.saaf.milkshop.domain.CostingPolicyInput
import com.saaf.milkshop.domain.CostingPolicyService
import com.saaf.milkshop.domain.OpeningCostBasisInput
import com.saaf.milkshop.domain.ProductionLossAssessmentInput
import com.saaf.milkshop.domain.RolePermissions
import com.saaf.milkshop.domain.YieldPolicyInput
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.owner.NativeOwnerCostingSection
import com.saaf.milkshop.nativeui.owner.OwnerCostingPlansScreen
import com.saaf.milkshop.nativeui.owner.OwnerScaffold
import com.saaf.milkshop.nativeui.owner.OwnerSection
import com.saaf.milkshop.nativeui.theme.SaafTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeOwnerCostingPlansScreenContractTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun listShowsPoliciesWithoutCashierChrome() {
        setScreen(policies = listOf(samplePolicy()))
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingPlansRoot).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingPlansList).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.ownerCostingPolicy(7L)).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerNavMore).assertIsSelected()
        composeRule.onNodeWithTag(NativeUiTags.CashierNavMore).assertDoesNotExist()
        composeRule.onNodeWithTag(NativeUiTags.OpsNavMore).assertDoesNotExist()
        composeRule.onNodeWithText("Count cash").assertDoesNotExist()
    }

    @Test
    fun emptyStateIsHonest() {
        setScreen(
            policies = emptyList(),
            currentMonthSummary = "No costing policy for 2026-09 yet.",
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingPlansEmpty).assertIsDisplayed()
        composeRule.onNodeWithText("No costing policies yet.").assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingPlansSummary).assertIsDisplayed()
    }

    @Test
    fun policyRowOpensDetail() {
        var opened: Long? = null
        setScreen(
            policies = listOf(samplePolicy()),
            onOpenPolicy = { opened = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.ownerCostingPolicy(7L)).performClick()
        assertEquals(7L, opened)
    }

    @Test
    fun detailShowsFactsAndSupersedeForActive() {
        var supersede = false
        setScreen(
            policies = listOf(samplePolicy()),
            policyDetail = NativeOwnerCostingPolicyDetail(
                row = samplePolicy(),
                normalCapacityKg = "100",
                notes = "Seeded month",
                approvedAt = "2026-09-01T10:00:00Z",
                components = listOf(
                    NativeOwnerCostingComponentRow(
                        componentCode = "culture",
                        sourceAmountPaisa = 1_000L,
                        sourceAmountLabel = "PKR 10.00",
                        basisQuantityKg = "1",
                    ),
                ),
            ),
            onOpenApproveFromDetail = { supersede = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingPlansDetail).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingPlansDetailMonth).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingPlansDetailSupersede)
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        assertTrue(supersede)
    }

    @Test
    fun supersededDetailShowsDeferredEditNotice() {
        setScreen(
            policies = listOf(samplePolicy().copy(statusLabel = "Superseded")),
            policyDetail = NativeOwnerCostingPolicyDetail(
                row = samplePolicy().copy(statusLabel = "Superseded"),
                normalCapacityKg = "100",
                notes = null,
                approvedAt = "2026-09-01T10:00:00Z",
                components = emptyList(),
            ),
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingPlansDetailDeferredEdit)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingPlansDetailSupersede).assertDoesNotExist()
    }

    @Test
    fun approveOpensFormAndSavesTypedInput() {
        var saved: CostingPolicyInput? = null
        setScreen(
            policies = emptyList(),
            approveOpen = true,
            reviewReasons = CostingPolicyService.REVIEW_REASONS.sorted(),
            componentCodes = listOf(
                "culture",
                "packaging",
                "variable_energy",
                "direct_labor",
                CostingPolicyService.COMPONENT_FIXED_OVERHEAD,
            ),
            onApprovePolicy = { saved = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingPlansApproveForm).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingPlansApproveMonth).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.ownerCostingComponentAmount("culture"))
            .performScrollTo()
            .performTextInput("10")
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingPlansApproveSave)
            .performScrollTo()
            .performClick()
        assertNotNull(saved)
        assertEquals("2026-09", saved!!.calendarMonth)
        assertEquals("monthly", saved!!.reviewReason)
        assertEquals(5, saved!!.components.size)
        assertEquals(1_000L, saved!!.components.first { it.code == "culture" }.sourceAmountPaisa)
        assertEquals(null, saved!!.supersedesPolicyId)
    }

    @Test
    fun loadFailureOffersRetry() {
        var retries = 0
        setScreen(
            policies = emptyList(),
            policiesError = "Costing policies could not be loaded.",
            onRetry = { retries += 1 },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingPlansRetry).performClick()
        assertEquals(1, retries)
    }

    @Test
    fun reauthDialogIsShownWhenRequired() {
        var cancelled = false
        setScreen(
            policies = emptyList(),
            approveOpen = true,
            reauthOpen = true,
            onCancelReauth = { cancelled = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingPlansReauth).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingPlansReauthCancel).performClick()
        assertTrue(cancelled)
    }

    @Test
    fun plansSectionShowsBudgetsAndOpensEditor() {
        var opened = false
        setScreen(
            policies = emptyList(),
            section = NativeOwnerCostingSection.Plans,
            budgets = listOf(
                NativeOwnerBudgetRow(
                    budgetId = 3L,
                    category = "Milk purchases",
                    periodStart = "2026-09-22",
                    periodEndExclusive = "2026-09-29",
                    plannedLitersLabel = "100 L",
                    plannedAmountLabel = "PKR 28,000.00",
                    salesTargetLabel = "PKR 40,000.00",
                    archived = false,
                ),
            ),
            onOpenBudgetEditor = { opened = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingSectionPlans).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.ownerCostingBudget(3L)).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingBudgetOpen).performClick()
        assertTrue(opened)
    }

    @Test
    fun budgetEditorShowsFieldsAndSavesTypedInput() {
        var saved: BudgetPlanInput? = null
        setScreen(
            policies = emptyList(),
            section = NativeOwnerCostingSection.Plans,
            budgetEditorOpen = true,
            budgetCategories = listOf("Milk purchases", "Rent"),
            defaultMilkWeekStart = "2026-09-21",
            defaultOperatingMonthStart = "2026-10-01",
            onSaveBudget = { saved = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingBudgetForm).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingBudgetPeriodStart)
            .performScrollTo()
            .performTextReplacement("2026-09-21")
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingBudgetPlannedValue)
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingBudgetPlannedValue)
            .performTextReplacement("50")
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OwnerCostingBudgetSave)
                    .assertIsEnabled()
            }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingBudgetSave)
            .performScrollTo()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) { saved != null }
        assertEquals("Milk purchases", saved!!.category)
        assertEquals(50.0, saved!!.plannedLiters!!, 1e-9)
    }

    @Test
    fun collectionsEditorShowsFieldsAndSavesTypedInput() {
        var week: String? = null
        var amount: Double? = null
        setScreen(
            policies = emptyList(),
            section = NativeOwnerCostingSection.Plans,
            collectionsEditorOpen = true,
            defaultMilkWeekStart = "2026-09-21",
            onSaveCollectionsTarget = { start, value ->
                week = start.toString()
                amount = value
            },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingTargetForm).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingTargetWeekStart)
            .performScrollTo()
            .performTextReplacement("2026-09-21")
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingTargetAmount)
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingTargetAmount)
            .performTextReplacement("250000")
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithTag(NativeUiTags.OwnerCostingTargetSave)
                    .assertIsEnabled()
            }.isSuccess
        }
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingTargetSave)
            .performScrollTo()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) { week != null }
        assertEquals("2026-09-21", week)
        assertEquals(250000.0, amount!!, 1e-9)
    }

    @Test
    fun openingSectionShowsCoverageAndOpensProductEditor() {
        var opened: Long? = null
        setScreen(
            policies = emptyList(),
            section = NativeOwnerCostingSection.Opening,
            openingCosts = listOf(sampleOpeningCost()),
            openingSummary = "0 of 1 opening stock items have a cost basis.",
            onOpenOpeningEditor = { opened = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingSectionOpening).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingOpeningSummary).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.ownerCostingOpeningRow(4L)).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.ownerCostingOpeningEdit(4L)).performClick()
        assertEquals(4L, opened)
    }

    @Test
    fun openingEmptyStateOpensShopSettings() {
        var opened = false
        setScreen(
            policies = emptyList(),
            section = NativeOwnerCostingSection.Opening,
            openingSummary = "No opening stock exists yet. Add it in Shop settings.",
            onOpenShopSettings = { opened = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingOpeningEmpty).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingOpeningOpenShopSettings)
            .performClick()
        assertTrue(opened)
    }

    @Test
    fun costingSectionsRemainReachableAtLargeFontScale() {
        setScreen(
            policies = listOf(samplePolicy()),
            fontScale = 1.3f,
        )
        listOf(
            NativeUiTags.OwnerCostingSectionPolicies,
            NativeUiTags.OwnerCostingSectionPlans,
            NativeUiTags.OwnerCostingSectionOpening,
            NativeUiTags.OwnerCostingSectionYield,
        ).forEach { tag ->
            composeRule.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun openingEditorRequiresEstimateEvidenceAndSavesCorrection() {
        var saved: OpeningCostBasisInput? = null
        setScreen(
            policies = emptyList(),
            section = NativeOwnerCostingSection.Opening,
            openingEditor = sampleOpeningCost().copy(
                basisId = 9L,
                totalCostPaisa = 150_000L,
                totalCostLabel = "PKR 1,500.00",
                totalCostInput = "1500",
            ),
            openingBasisKinds = listOf("documented_historical", "owner_estimate"),
            onSaveOpeningCost = { saved = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingOpeningForm).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingOpeningQuantity).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingOpeningTotal)
            .performTextReplacement("0")
        composeRule.onNodeWithTag(
            NativeUiTags.OwnerCostingOpeningTotalError,
            useUnmergedTree = true,
        )
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingOpeningTotal)
            .performTextReplacement("1600")
        composeRule.onNodeWithTag(NativeUiTags.ownerCostingOpeningKind("owner_estimate"))
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag(
            NativeUiTags.OwnerCostingOpeningEvidenceError,
            useUnmergedTree = true,
        )
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingOpeningSave)
            .performScrollTo()
            .assertIsNotEnabled()
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingOpeningEvidence)
            .performScrollTo()
            .performTextInput("Reconstructed from the opening-day purchase rate")
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingOpeningSave)
            .performScrollTo()
            .assertIsEnabled()
            .performClick()
        assertNotNull(saved)
        assertEquals(14L, saved!!.openingEventId)
        assertEquals(4L, saved!!.productId)
        assertEquals(160_000L, saved!!.totalCostPaisa)
        assertEquals("owner_estimate", saved!!.basisKind)
        assertEquals(9L, saved!!.supersedesBasisId)
    }

    @Test
    fun yieldSectionShowsApproveForm() {
        var opened = false
        setScreen(
            policies = emptyList(),
            section = NativeOwnerCostingSection.Yield,
            yieldSummary = "No yield baseline approved yet.",
            onOpenYieldApprove = { opened = true },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingYieldSummary).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingYieldApprove).performClick()
        assertTrue(opened)
    }

    @Test
    fun yieldApproveFormRequiresThirtyBatches() {
        var saved: YieldPolicyInput? = null
        setScreen(
            policies = emptyList(),
            section = NativeOwnerCostingSection.Yield,
            yieldApproveOpen = true,
            yieldBatches = (1L..30L).map {
                NativeOwnerYieldBatchRow(
                    productionEventId = it,
                    yieldRatioLabel = "0.85 kg/L",
                    statusCode = "calibration_pending",
                    statusLabel = "Calibration not ready",
                )
            },
            onApproveYieldPolicy = { saved = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingYieldForm).assertIsDisplayed()
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingYieldSelectAll)
            .performScrollTo()
            .assertIsEnabled()
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingYieldSave)
            .performScrollTo()
            .assertIsEnabled()
            .performClick()
        assertNotNull(saved)
        assertEquals(30, saved!!.acceptedProductionEventIds.size)
    }

    @Test
    fun productionLossAssessmentCapturesAbnormalBatch() {
        var saved: ProductionLossAssessmentInput? = null
        setScreen(
            policies = emptyList(),
            section = NativeOwnerCostingSection.Yield,
            yieldBatches = listOf(
                NativeOwnerYieldBatchRow(
                    productionEventId = 44L,
                    milkInputLabel = "20 L Milk",
                    yieldRatioLabel = "0.7 kg/L",
                    statusCode = "below_range",
                    statusLabel = "Below baseline",
                ),
            ),
            lossClassifications = listOf("ordinary", "abnormal"),
            lossReasons = listOf("spill", "other"),
            onRecordProductionLoss = { saved = it },
        )
        composeRule.onNodeWithTag(NativeUiTags.ownerCostingLossAssess(44L)).performClick()
        composeRule.onNodeWithText("Abnormal").performClick()
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingLossQuantity)
            .performTextReplacement("1.5")
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingLossNotes)
            .performTextInput("Observed spill")
        composeRule.onNodeWithTag(NativeUiTags.OwnerCostingLossSave).performClick()
        assertNotNull(saved)
        assertEquals("abnormal", saved!!.classification)
        assertEquals("1.5", saved!!.abnormalMilkLiters.toPlainString())
    }

    private fun setScreen(
        policies: List<NativeOwnerCostingPolicyRow>,
        policiesLoading: Boolean = false,
        policyDetail: NativeOwnerCostingPolicyDetail? = null,
        policiesError: String? = null,
        currentMonthSummary: String? = "Current 2026-09: Active · 1000 paisa/kg",
        section: NativeOwnerCostingSection = NativeOwnerCostingSection.Policies,
        budgets: List<NativeOwnerBudgetRow> = emptyList(),
        targets: List<NativeOwnerCollectionsTargetRow> = emptyList(),
        budgetCategories: List<String> = emptyList(),
        defaultMilkWeekStart: String = "2026-09-22",
        defaultOperatingMonthStart: String = "2026-10-01",
        budgetEditorOpen: Boolean = false,
        collectionsEditorOpen: Boolean = false,
        openingCosts: List<NativeOwnerOpeningCostRow> = emptyList(),
        openingSummary: String? = null,
        openingBasisKinds: List<String> = emptyList(),
        openingEditor: NativeOwnerOpeningCostRow? = null,
        yieldPolicies: List<NativeOwnerYieldPolicyRow> = emptyList(),
        yieldBatches: List<NativeOwnerYieldBatchRow> = emptyList(),
        yieldSummary: String? = null,
        yieldApproveOpen: Boolean = false,
        lossClassifications: List<String> = emptyList(),
        lossReasons: List<String> = emptyList(),
        approveOpen: Boolean = false,
        approveSeed: NativeOwnerCostingPolicyDetail? = null,
        approving: Boolean = false,
        reauthOpen: Boolean = false,
        reauthBusy: Boolean = false,
        reviewReasons: List<String> = emptyList(),
        componentCodes: List<String> = emptyList(),
        onBackToMenu: () -> Unit = {},
        onRetry: () -> Unit = {},
        onSectionChange: (NativeOwnerCostingSection) -> Unit = {},
        onOpenPolicy: (Long) -> Unit = {},
        onBackFromDetail: () -> Unit = {},
        onOpenApprove: () -> Unit = {},
        onOpenApproveFromDetail: () -> Unit = {},
        onBackFromApprove: () -> Unit = {},
        onApprovePolicy: (CostingPolicyInput) -> Unit = {},
        onOpenBudgetEditor: () -> Unit = {},
        onSaveBudget: (BudgetPlanInput) -> Unit = {},
        onSaveCollectionsTarget: (java.time.LocalDate, Double) -> Unit = { _, _ -> },
        onOpenOpeningEditor: (Long) -> Unit = {},
        onOpenShopSettings: () -> Unit = {},
        onBackFromOpeningEditor: () -> Unit = {},
        onSaveOpeningCost: (OpeningCostBasisInput) -> Unit = {},
        onOpenYieldApprove: () -> Unit = {},
        onApproveYieldPolicy: (YieldPolicyInput) -> Unit = {},
        onRecordProductionLoss: (ProductionLossAssessmentInput) -> Unit = {},
        onSubmitReauth: (CharArray) -> Unit = { it.fill('\u0000') },
        onCancelReauth: () -> Unit = {},
        onNavigate: (OwnerSection) -> Unit = {},
        fontScale: Float = 1f,
    ) {
        composeRule.setContent {
            val currentDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(currentDensity.density, fontScale),
            ) {
                SaafTheme(darkTheme = false) {
                    OwnerCostingPlansScreen(
                        snackbarHostState = remember { SnackbarHostState() },
                        identity = NativeSignedInIdentity(
                            userId = 1L,
                            loginName = "owner",
                            displayName = "Owner",
                            role = AppRole.OWNER,
                            effectivePermissions = RolePermissions.permissions(AppRole.OWNER),
                        ),
                        context = NativeOwnerMoreContext(
                            businessDate = "2026-09-21",
                            postingOpen = true,
                            shopName = "Saaf Milk",
                        ),
                        policies = policies,
                        policiesLoading = policiesLoading,
                        policyDetail = policyDetail,
                        policiesError = policiesError,
                        currentMonthSummary = currentMonthSummary,
                        section = section,
                        budgets = budgets,
                        targets = targets,
                        budgetCategories = budgetCategories,
                        defaultMilkWeekStart = defaultMilkWeekStart,
                        defaultOperatingMonthStart = defaultOperatingMonthStart,
                        budgetEditorOpen = budgetEditorOpen,
                        collectionsEditorOpen = collectionsEditorOpen,
                        openingCosts = openingCosts,
                        openingSummary = openingSummary,
                        openingBasisKinds = openingBasisKinds,
                        openingEditor = openingEditor,
                        yieldPolicies = yieldPolicies,
                        yieldBatches = yieldBatches,
                        yieldSummary = yieldSummary,
                        yieldApproveOpen = yieldApproveOpen,
                        lossClassifications = lossClassifications,
                        lossReasons = lossReasons,
                        approveOpen = approveOpen,
                        approveSeed = approveSeed,
                        approving = approving,
                        reauthOpen = reauthOpen,
                        reauthBusy = reauthBusy,
                        reviewReasons = reviewReasons,
                        componentCodes = componentCodes,
                        onBackToMenu = onBackToMenu,
                        onRetry = onRetry,
                        onSectionChange = onSectionChange,
                        onOpenPolicy = onOpenPolicy,
                        onBackFromDetail = onBackFromDetail,
                        onOpenApprove = onOpenApprove,
                        onOpenApproveFromDetail = onOpenApproveFromDetail,
                        onBackFromApprove = onBackFromApprove,
                        onApprovePolicy = onApprovePolicy,
                        onOpenBudgetEditor = onOpenBudgetEditor,
                        onSaveBudget = onSaveBudget,
                        onSaveCollectionsTarget = onSaveCollectionsTarget,
                        onOpenOpeningEditor = onOpenOpeningEditor,
                        onOpenShopSettings = onOpenShopSettings,
                        onBackFromOpeningEditor = onBackFromOpeningEditor,
                        onSaveOpeningCost = onSaveOpeningCost,
                        onOpenYieldApprove = onOpenYieldApprove,
                        onApproveYieldPolicy = onApproveYieldPolicy,
                        onRecordProductionLoss = onRecordProductionLoss,
                        onSubmitReauth = onSubmitReauth,
                        onCancelReauth = onCancelReauth,
                        hostedScaffold = { snackbar, mod, wrapNav, content ->
                            OwnerScaffold(
                                snackbarHostState = snackbar,
                                section = OwnerSection.More,
                                onNavigate = { section -> wrapNav { } },
                                modifier = mod,
                                content = content,
                            )
                        },
                    )
                }
            }
        }
    }

    private fun samplePolicy() = NativeOwnerCostingPolicyRow(
        policyId = 7L,
        calendarMonth = "2026-09",
        statusLabel = "Active",
        conversionCostLabel = "1000 paisa/kg",
        reviewReason = "monthly",
    )

    private fun sampleOpeningCost() = NativeOwnerOpeningCostRow(
        openingEventId = 14L,
        productId = 4L,
        productName = "Milk",
        unit = "L",
        quantity = "10",
        basisId = null,
        totalCostPaisa = null,
        totalCostLabel = null,
        totalCostInput = "",
        basisKindCode = "documented_historical",
        basisKindLabel = "Documented bill or historical record",
        evidenceNotes = null,
    )
}
