package com.saaf.milkshop.nativeui.owner

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.owner.NativeOwnerBudgetRow
import com.saaf.milkshop.application.owner.NativeOwnerCollectionsTargetRow
import com.saaf.milkshop.application.owner.NativeOwnerMoreContext
import com.saaf.milkshop.application.owner.NativeOwnerYieldBatchRow
import com.saaf.milkshop.application.owner.NativeOwnerYieldPolicyRow
import com.saaf.milkshop.domain.BUDGET_CATEGORY_MILK_PURCHASES
import com.saaf.milkshop.domain.BudgetPlanInput
import com.saaf.milkshop.domain.ProductionLossAssessmentInput
import com.saaf.milkshop.domain.YieldPolicyInput
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.EntryLeaveGuard
import com.saaf.milkshop.nativeui.common.HostedScaffold
import com.saaf.milkshop.nativeui.common.rememberEntryLeaveGuard
import com.saaf.milkshop.nativeui.sale.CashierScreenHeader
import com.saaf.milkshop.nativeui.sale.formatCashierRoleLabel
import java.time.LocalDate
import java.math.BigDecimal

@Composable
internal fun OwnerCostingPlansSectionTabs(
    section: NativeOwnerCostingSection,
    onSectionChange: (NativeOwnerCostingSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .testTag(NativeUiTags.OwnerCostingPlansHub),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = section == NativeOwnerCostingSection.Policies,
            onClick = { onSectionChange(NativeOwnerCostingSection.Policies) },
            label = { Text("Policies") },
            modifier = Modifier.testTag(NativeUiTags.OwnerCostingSectionPolicies),
        )
        FilterChip(
            selected = section == NativeOwnerCostingSection.Plans,
            onClick = { onSectionChange(NativeOwnerCostingSection.Plans) },
            label = { Text("Plans") },
            modifier = Modifier.testTag(NativeUiTags.OwnerCostingSectionPlans),
        )
        FilterChip(
            selected = section == NativeOwnerCostingSection.Opening,
            onClick = { onSectionChange(NativeOwnerCostingSection.Opening) },
            label = { Text("Opening") },
            modifier = Modifier.testTag(NativeUiTags.OwnerCostingSectionOpening),
        )
        FilterChip(
            selected = section == NativeOwnerCostingSection.Yield,
            onClick = { onSectionChange(NativeOwnerCostingSection.Yield) },
            label = { Text("Yield") },
            modifier = Modifier.testTag(NativeUiTags.OwnerCostingSectionYield),
        )
    }
}

@Composable
internal fun OwnerCostingPlansBody(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    budgets: List<NativeOwnerBudgetRow>,
    targets: List<NativeOwnerCollectionsTargetRow>,
    error: String?,
    onOpenBudgetEditor: () -> Unit,
    onOpenCollectionsEditor: () -> Unit,
    hostedScaffold: HostedScaffold,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit,
) {
    val visibleBudgets = remember(budgets) { budgets.filter { !it.archived } }
    val visibleTargets = remember(targets) { targets.filter { !it.archived } }
    hostedScaffold(snackbarHostState, modifier, { it() }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            header()
            error?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .testTag(NativeUiTags.OwnerCostingPlansError),
                )
            }
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .testTag(NativeUiTags.OwnerCostingBudgetList),
            ) {
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Budgets",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        TextButton(
                            onClick = onOpenBudgetEditor,
                            modifier = Modifier.testTag(NativeUiTags.OwnerCostingBudgetOpen),
                        ) { Text("New plan") }
                    }
                }
                if (budgets.none { !it.archived }) {
                    item {
                        Text(
                            "No current budgets saved.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .testTag(NativeUiTags.OwnerCostingBudgetEmpty),
                        )
                    }
                } else {
                    items(visibleBudgets, key = { "b-${it.budgetId}" }) { budget ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .testTag(NativeUiTags.ownerCostingBudget(budget.budgetId)),
                        ) {
                            Text(budget.category, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${budget.periodStart} → before ${budget.periodEndExclusive}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                budget.plannedLitersLabel?.let { "$it · ${budget.plannedAmountLabel}" }
                                    ?: budget.plannedAmountLabel,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            budget.salesTargetLabel?.let {
                                Text(
                                    "Sales target $it",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
                item {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .testTag(NativeUiTags.OwnerCostingTargetList),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Collections targets",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        TextButton(
                            onClick = onOpenCollectionsEditor,
                            modifier = Modifier.testTag(NativeUiTags.OwnerCostingTargetOpen),
                        ) { Text("Set target") }
                    }
                }
                if (targets.none { !it.archived }) {
                    item {
                        Text(
                            "No current collections targets.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .testTag(NativeUiTags.OwnerCostingTargetEmpty),
                        )
                    }
                } else {
                    items(visibleTargets, key = { "t-${it.targetId}" }) { target ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .testTag(NativeUiTags.ownerCostingTarget(target.targetId)),
                        ) {
                            Text("Week of ${target.weekStart}", fontWeight = FontWeight.SemiBold)
                            Text(
                                target.targetAmountLabel,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                "Ends before ${target.weekEndExclusive}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
                item {
                    Text(
                        "Plans are for comparison only. Saving one does not create a purchase, expense, sale, or payment.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun OwnerCostingBudgetEditorScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    categories: List<String>,
    defaultMilkWeekStart: String,
    defaultOperatingMonthStart: String,
    periodEndExclusive: (String, LocalDate) -> LocalDate,
    saving: Boolean,
    error: String?,
    onBack: () -> Unit,
    onSave: (BudgetPlanInput) -> Unit,
    onCopyPrevious: (category: String, start: LocalDate, end: LocalDate) -> Unit,
    hostedScaffold: HostedScaffold,
    modifier: Modifier = Modifier,
) {
    val leaveGuard = rememberEntryLeaveGuard()
    val cats = categories.ifEmpty { listOf(BUDGET_CATEGORY_MILK_PURCHASES) }
    var category by rememberSaveable { mutableStateOf(cats.first()) }
    var periodStartText by rememberSaveable {
        mutableStateOf(
            if (category == BUDGET_CATEGORY_MILK_PURCHASES) defaultMilkWeekStart
            else defaultOperatingMonthStart,
        )
    }
    var plannedValue by rememberSaveable { mutableStateOf("") }
    var draftTouched by rememberSaveable { mutableStateOf(false) }
    val periodStart = runCatching { LocalDate.parse(periodStartText) }.getOrNull()
    val periodEnd = periodStart?.let { periodEndExclusive(category, it) }

    val dirty = draftTouched && !saving
    LaunchedEffect(dirty) {
        if (dirty) leaveGuard.markDirty() else leaveGuard.clear()
    }
    fun markDirty() {
        draftTouched = true
    }
    fun requestLeave(action: () -> Unit) {
        if (saving) return
        leaveGuard.requestLeave(action)
    }
    BackHandler { requestLeave(onBack) }
    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
    hostedScaffold(snackbarHostState, Modifier.fillMaxSize(), { action -> requestLeave(action) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag(NativeUiTags.OwnerCostingBudgetForm),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CashierScreenHeader(
                title = "Costing & plans",
                displayName = context.shopName,
                roleLabel = formatCashierRoleLabel(identity.role.wireValue),
                businessDate = context.businessDate,
                postingOpen = context.postingOpen,
            )
            TextButton(
                onClick = { requestLeave(onBack) },
                enabled = !saving,
                modifier = Modifier.testTag(NativeUiTags.OwnerCostingBudgetBack),
            ) { Text("Back to plans") }
            Text(
                "Set a spending or milk plan",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Milk is planned one week at a time in liters. Other shop costs are planned one month at a time in PKR.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text("What are you planning?", style = MaterialTheme.typography.labelLarge)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.testTag(NativeUiTags.OwnerCostingBudgetCategory),
            ) {
                cats.forEach { option ->
                    FilterChip(
                        selected = category == option,
                        onClick = {
                            category = option
                            periodStartText = if (option == BUDGET_CATEGORY_MILK_PURCHASES) {
                                defaultMilkWeekStart
                            } else {
                                defaultOperatingMonthStart
                            }
                            plannedValue = ""
                            markDirty()
                        },
                        label = { Text(option) },
                        modifier = Modifier.testTag(NativeUiTags.ownerCostingBudgetCategoryOption(option)),
                    )
                }
            }
            OutlinedTextField(
                value = periodStartText,
                onValueChange = {
                    periodStartText = it
                    markDirty()
                },
                label = {
                    Text(
                        if (category == BUDGET_CATEGORY_MILK_PURCHASES) {
                            "Week starts on Monday (YYYY-MM-DD)"
                        } else {
                            "Month starts (YYYY-MM-DD)"
                        },
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(NativeUiTags.OwnerCostingBudgetPeriodStart),
            )
            periodEnd?.let {
                Text(
                    "This plan ends before $it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedTextField(
                value = plannedValue,
                onValueChange = {
                    plannedValue = it
                    markDirty()
                },
                label = {
                    Text(
                        if (category == BUDGET_CATEGORY_MILK_PURCHASES) {
                            "Planned milk to buy (liters)"
                        } else {
                            "Planned amount (PKR)"
                        },
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(NativeUiTags.OwnerCostingBudgetPlannedValue),
            )
            error?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag(NativeUiTags.OwnerCostingPlansError),
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    onClick = {
                        val start = periodStart ?: return@TextButton
                        val end = periodEnd ?: return@TextButton
                        onCopyPrevious(category, start, end)
                    },
                    enabled = !saving && periodStart != null && periodEnd != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(NativeUiTags.OwnerCostingBudgetCopy),
                ) { Text("Copy previous") }
            }
            val canSaveBudget = !saving && periodStart != null && periodEnd != null &&
                plannedValue.toDoubleOrNull() != null
            Button(
                onClick = {
                    val start = periodStart ?: return@Button
                    val end = periodEnd ?: return@Button
                    val value = plannedValue.toDoubleOrNull() ?: return@Button
                    val input = if (category == BUDGET_CATEGORY_MILK_PURCHASES) {
                        BudgetPlanInput(category, start, end, plannedLiters = value)
                    } else {
                        BudgetPlanInput(category, start, end, plannedAmount = value)
                    }
                    onSave(input)
                },
                enabled = canSaveBudget,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(NativeUiTags.OwnerCostingBudgetSave),
            ) { Text(if (saving) "Saving…" else "Save plan") }
        }
    }
    OwnerCostingPlansDiscardDialog(
        leaveGuard = leaveGuard,
        title = "Discard budget draft?",
        body = "You have unsaved budget changes.",
        discardTag = NativeUiTags.OwnerCostingBudgetDiscard,
        keepTag = NativeUiTags.OwnerCostingBudgetKeep,
    )
    }
}

@Composable
internal fun OwnerCostingCollectionsEditorScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    defaultWeekStart: String,
    saving: Boolean,
    error: String?,
    onBack: () -> Unit,
    onSave: (weekStart: LocalDate, amount: Double) -> Unit,
    hostedScaffold: HostedScaffold,
    modifier: Modifier = Modifier,
) {
    val leaveGuard = rememberEntryLeaveGuard()
    var weekStartText by rememberSaveable { mutableStateOf(defaultWeekStart) }
    var amountText by rememberSaveable { mutableStateOf("") }
    var draftTouched by rememberSaveable { mutableStateOf(false) }
    val weekStart = runCatching { LocalDate.parse(weekStartText) }.getOrNull()

    val dirty = draftTouched && !saving
    LaunchedEffect(dirty) {
        if (dirty) leaveGuard.markDirty() else leaveGuard.clear()
    }
    fun requestLeave(action: () -> Unit) {
        if (saving) return
        leaveGuard.requestLeave(action)
    }
    BackHandler { requestLeave(onBack) }
    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
    hostedScaffold(snackbarHostState, Modifier.fillMaxSize(), { action -> requestLeave(action) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag(NativeUiTags.OwnerCostingTargetForm),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CashierScreenHeader(
                title = "Costing & plans",
                displayName = context.shopName,
                roleLabel = formatCashierRoleLabel(identity.role.wireValue),
                businessDate = context.businessDate,
                postingOpen = context.postingOpen,
            )
            TextButton(
                onClick = { requestLeave(onBack) },
                enabled = !saving,
                modifier = Modifier.testTag(NativeUiTags.OwnerCostingTargetBack),
            ) { Text("Back to plans") }
            Text(
                "Set a weekly collection target",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "This is how much customer debt you hope to collect. It is separate from the sales target.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = weekStartText,
                onValueChange = {
                    weekStartText = it
                    draftTouched = true
                },
                label = { Text("Week starts on Monday (YYYY-MM-DD)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(NativeUiTags.OwnerCostingTargetWeekStart),
            )
            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    draftTouched = true
                },
                label = { Text("Target to collect (PKR)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(NativeUiTags.OwnerCostingTargetAmount),
            )
            error?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag(NativeUiTags.OwnerCostingPlansError),
                )
            }
            Button(
                onClick = {
                    val start = weekStart ?: return@Button
                    val amount = amountText.toDoubleOrNull() ?: return@Button
                    onSave(start, amount)
                },
                enabled = !saving && weekStart != null && amountText.toDoubleOrNull() != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(NativeUiTags.OwnerCostingTargetSave),
            ) { Text(if (saving) "Saving…" else "Save collection target") }
        }
    }
    OwnerCostingPlansDiscardDialog(
        leaveGuard = leaveGuard,
        title = "Discard collections draft?",
        body = "You have unsaved collections target changes.",
        discardTag = NativeUiTags.OwnerCostingTargetDiscard,
        keepTag = NativeUiTags.OwnerCostingTargetKeep,
    )
    }
}

@Composable
internal fun OwnerCostingYieldBody(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    policies: List<NativeOwnerYieldPolicyRow>,
    batches: List<NativeOwnerYieldBatchRow>,
    summary: String?,
    error: String?,
    onOpenApprove: () -> Unit,
    savingAssessment: Boolean,
    classifications: List<String>,
    reasons: List<String>,
    onSaveAssessment: (ProductionLossAssessmentInput) -> Unit,
    hostedScaffold: HostedScaffold,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit,
) {
    var assessmentBatch by remember { mutableStateOf<NativeOwnerYieldBatchRow?>(null) }
    hostedScaffold(snackbarHostState, modifier, { it() }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            header()
            error?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .testTag(NativeUiTags.OwnerCostingPlansError),
                )
            }
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .testTag(NativeUiTags.OwnerCostingYieldList),
            ) {
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Yield baseline",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        TextButton(
                            onClick = onOpenApprove,
                            modifier = Modifier.testTag(NativeUiTags.OwnerCostingYieldApprove),
                        ) { Text("Approve baseline") }
                    }
                }
                summary?.let {
                    item {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .testTag(NativeUiTags.OwnerCostingYieldSummary),
                        )
                    }
                }
                if (policies.isEmpty()) {
                    item {
                        Text(
                            "No yield baselines yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag(NativeUiTags.OwnerCostingYieldEmpty),
                        )
                    }
                } else {
                    items(policies, key = { it.yieldPolicyId }) { policy ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .testTag(NativeUiTags.ownerCostingYieldPolicy(policy.yieldPolicyId)),
                        ) {
                            Text(
                                "${policy.statusLabel} · from ${policy.effectiveFrom}",
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "${policy.lowerAlertLimit}–${policy.upperAlertLimit} kg/L · " +
                                    "${policy.acceptedBatchCount} batches",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                "Sample ${policy.sampleStartDate} → ${policy.sampleEndDate}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
                item {
                    Text(
                        "Production loss review",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
                    )
                    Text(
                        "Classify each batch loss so abnormal incidents stay separate from normal conversion loss.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (batches.isEmpty()) {
                    item {
                        Text(
                            "No production batches to assess yet.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp),
                        )
                    }
                } else {
                    items(batches, key = { "loss-${it.productionEventId}" }) { batch ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("Batch #${batch.productionEventId}", fontWeight = FontWeight.SemiBold)
                                Text("${batch.milkInputLabel} · ${batch.yieldRatioLabel}")
                                Text(
                                    batch.assessmentLabel,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            TextButton(
                                onClick = { assessmentBatch = batch },
                                enabled = !savingAssessment,
                                modifier = Modifier.testTag(
                                    NativeUiTags.ownerCostingLossAssess(batch.productionEventId),
                                ),
                            ) { Text(if (batch.assessmentId == null) "Assess" else "Revise") }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
    assessmentBatch?.let { batch ->
        var classification by remember(batch.productionEventId) {
            mutableStateOf(batch.classificationCode ?: classifications.firstOrNull() ?: "ordinary")
        }
        var reason by remember(batch.productionEventId) {
            mutableStateOf(batch.reasonCode ?: reasons.firstOrNull() ?: "spill")
        }
        var abnormalLiters by remember(batch.productionEventId) {
            mutableStateOf(batch.abnormalMilkLiters)
        }
        var notes by remember(batch.productionEventId) { mutableStateOf(batch.assessmentNotes.orEmpty()) }
        val abnormalValue = abnormalLiters.toBigDecimalOrNull()
        val canSave = !savingAssessment && classification.isNotBlank() && reason.isNotBlank() &&
            (classification != "abnormal" || (abnormalValue != null && abnormalValue > BigDecimal.ZERO)) &&
            (reason != "other" || notes.isNotBlank())
        AlertDialog(
            onDismissRequest = { if (!savingAssessment) assessmentBatch = null },
            title = { Text("Assess batch #${batch.productionEventId}") },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("${batch.milkInputLabel} · ${batch.yieldRatioLabel}")
                    Text("Classification", fontWeight = FontWeight.SemiBold)
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        classifications.forEach { code ->
                            FilterChip(
                                selected = classification == code,
                                onClick = {
                                    classification = code
                                    if (code == "ordinary") abnormalLiters = "0"
                                },
                                label = { Text(code.replace('_', ' ').replaceFirstChar { it.uppercase() }) },
                            )
                        }
                    }
                    if (classification == "abnormal") {
                        OutlinedTextField(
                            value = abnormalLiters,
                            onValueChange = { abnormalLiters = it },
                            label = { Text("Abnormal Milk quantity (L)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag(NativeUiTags.OwnerCostingLossQuantity),
                        )
                    }
                    Text("What physically happened?", fontWeight = FontWeight.SemiBold)
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        reasons.forEach { code ->
                            FilterChip(
                                selected = reason == code,
                                onClick = { reason = code },
                                label = { Text(code.replace('_', ' ').replaceFirstChar { it.uppercase() }) },
                            )
                        }
                    }
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(if (reason == "other") "Incident explanation" else "Notes (optional)") },
                        modifier = Modifier.fillMaxWidth().testTag(NativeUiTags.OwnerCostingLossNotes),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSaveAssessment(
                            ProductionLossAssessmentInput(
                                productionEventId = batch.productionEventId,
                                classification = classification,
                                abnormalMilkLiters = if (classification == "ordinary") {
                                    BigDecimal.ZERO
                                } else {
                                    abnormalValue ?: BigDecimal.ZERO
                                },
                                reasonCode = reason,
                                notes = notes.trim().takeIf { it.isNotEmpty() },
                                supersedesAssessmentId = batch.assessmentId,
                            ),
                        )
                        assessmentBatch = null
                    },
                    enabled = canSave,
                    modifier = Modifier.testTag(NativeUiTags.OwnerCostingLossSave),
                ) { Text(if (savingAssessment) "Saving…" else "Save assessment") }
            },
            dismissButton = {
                TextButton(
                    onClick = { assessmentBatch = null },
                    enabled = !savingAssessment,
                ) { Text("Cancel") }
            },
        )
    }
}

@Composable
internal fun OwnerCostingYieldApproveScreen(
    snackbarHostState: SnackbarHostState,
    identity: NativeSignedInIdentity,
    context: NativeOwnerMoreContext,
    batches: List<NativeOwnerYieldBatchRow>,
    activePolicyId: Long?,
    saving: Boolean,
    error: String?,
    onBack: () -> Unit,
    onSave: (YieldPolicyInput) -> Unit,
    hostedScaffold: HostedScaffold,
    modifier: Modifier = Modifier,
) {
    val leaveGuard = rememberEntryLeaveGuard()
    var selectedIds by remember { mutableStateOf(emptySet<Long>()) }
    var effectiveFrom by rememberSaveable {
        mutableStateOf(LocalDate.parse(context.businessDate).plusDays(1).toString())
    }
    var draftTouched by rememberSaveable { mutableStateOf(false) }

    val dirty = draftTouched && !saving
    LaunchedEffect(dirty) {
        if (dirty) leaveGuard.markDirty() else leaveGuard.clear()
    }
    fun requestLeave(action: () -> Unit) {
        if (saving) return
        leaveGuard.requestLeave(action)
    }
    BackHandler { requestLeave(onBack) }
    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
    hostedScaffold(snackbarHostState, Modifier.fillMaxSize(), { action -> requestLeave(action) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .testTag(NativeUiTags.OwnerCostingYieldForm),
        ) {
            CashierScreenHeader(
                title = "Costing & plans",
                displayName = context.shopName,
                roleLabel = formatCashierRoleLabel(identity.role.wireValue),
                businessDate = context.businessDate,
                postingOpen = context.postingOpen,
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(
                    onClick = { requestLeave(onBack) },
                    enabled = !saving,
                    modifier = Modifier.testTag(NativeUiTags.OwnerCostingYieldBack),
                ) { Text("Back to yield") }
                Text(
                    "Shop-specific yogurt yield baseline",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Choose at least 30 trustworthy batches covering at least 28 days. The app uses them to learn the shop’s normal yogurt output range.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (activePolicyId != null) {
                    Text(
                        "Approving this replaces baseline #$activePolicyId. The old baseline remains attached to earlier batches.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag(NativeUiTags.OwnerCostingYieldSupersedeHint),
                    )
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Representative batches (${selectedIds.size} selected)",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    TextButton(
                        onClick = {
                            selectedIds = batches.map { it.productionEventId }.toSet()
                            draftTouched = true
                        },
                        enabled = batches.isNotEmpty(),
                        modifier = Modifier.testTag(NativeUiTags.OwnerCostingYieldSelectAll),
                    ) { Text("Select all") }
                }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .testTag(NativeUiTags.OwnerCostingYieldBatchList),
                ) {
                    if (batches.isEmpty()) {
                        Text(
                            "No yogurt batches are available yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        batches.forEach { batch ->
                            val checked = batch.productionEventId in selectedIds
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedIds = if (checked) {
                                            selectedIds - batch.productionEventId
                                        } else {
                                            selectedIds + batch.productionEventId
                                        }
                                        draftTouched = true
                                    }
                                    .padding(vertical = 6.dp)
                                    .testTag(NativeUiTags.ownerCostingYieldBatch(batch.productionEventId)),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { on ->
                                        selectedIds = if (on) {
                                            selectedIds + batch.productionEventId
                                        } else {
                                            selectedIds - batch.productionEventId
                                        }
                                        draftTouched = true
                                    },
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "Batch #${batch.productionEventId}",
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        "Yield: ${batch.yieldRatioLabel}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    batch.statusLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
                OutlinedTextField(
                    value = effectiveFrom,
                    onValueChange = {
                        effectiveFrom = it
                        draftTouched = true
                    },
                    label = { Text("Start using this baseline on (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(NativeUiTags.OwnerCostingYieldEffectiveFrom),
                )
                error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag(NativeUiTags.OwnerCostingPlansError),
                    )
                }
                Button(
                    onClick = {
                        onSave(
                            YieldPolicyInput(
                                acceptedProductionEventIds = selectedIds.sorted(),
                                effectiveFrom = effectiveFrom,
                                supersedesYieldPolicyId = activePolicyId,
                            ),
                        )
                    },
                    enabled = !saving && selectedIds.size >= 30,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(NativeUiTags.OwnerCostingYieldSave),
                ) {
                    Text(
                        when {
                            saving -> "Checking…"
                            activePolicyId != null -> "Approve revised baseline"
                            else -> "Approve baseline"
                        },
                    )
                }
            }
        }
    }
    OwnerCostingPlansDiscardDialog(
        leaveGuard = leaveGuard,
        title = "Discard yield draft?",
        body = "You have unsaved yield baseline selections.",
        discardTag = NativeUiTags.OwnerCostingYieldDiscard,
        keepTag = NativeUiTags.OwnerCostingYieldKeep,
    )
    }
}
