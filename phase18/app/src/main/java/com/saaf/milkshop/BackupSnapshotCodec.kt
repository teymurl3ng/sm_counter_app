package com.saaf.milkshop

import com.saaf.milkshop.data.*
import com.saaf.milkshop.sync.LegacyUid
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.Base64

internal fun readAndVerify(bytes: ByteArray): JSONObject {
    val root = JSONObject(bytes.toString(Charsets.UTF_8))
    val actual = root.getString("integrity_sha256")
    root.remove("integrity_sha256")
    val expected = sha256(root.toString().toByteArray(Charsets.UTF_8))
    check(actual == expected) { "Backup integrity check failed: checksum mismatch" }
    root.put("integrity_sha256", actual)
    return root
}

internal fun productJson(v: ProductEntity) = JSONObject().apply {
    put("product_id", v.productId); put("name", v.name); put("unit", v.unit); put("uid", v.uid)
}
internal fun accountJson(v: AccountEntity) = JSONObject().apply {
    put("account_id", v.accountId); put("name", v.name); put("type", v.type); put("category", v.category); put("uid", v.uid)
}
internal fun eventJson(v: EventEntity) = JSONObject().apply {
    put("event_id", v.eventId); put("created_at", v.createdAt); put("occurred_at", v.occurredAt); put("event_type", v.eventType)
    putNullable("channel", v.channel); putNullable("corrects_event_id", v.correctsEventId); putNullable("raw_liters", v.rawLiters)
    putNullable("fat_percentage", v.fatPercentage); putNullable("lr_reading", v.lrReading); putNullable("snf_percentage", v.snfPercentage)
    putNullable("ts_percentage", v.tsPercentage); putNullable("adjusted_liters", v.adjustedLiters); putNullable("amount_owed", v.amountOwed)
    putNullable("milk_liters_used", v.milkLitersUsed); putNullable("yogurt_kg_produced", v.yogurtKgProduced); putNullable("amount", v.amount)
    putNullable("milk_rate_used", v.milkRateUsed); putNullable("delivery_fee_used", v.deliveryFeeUsed); putNullable("ts_standard_used", v.tsStandardUsed)
    putNullable("sale_quantity", v.saleQuantity); putNullable("sale_value", v.saleValue); putNullable("unit_rate", v.unitRate)
    put("surcharge_applied", v.surchargeApplied); putNullable("notes", v.notes)
    put("event_uid", v.eventUid); put("origin_device_id", v.originDeviceId)
}
internal fun inventoryJson(v: InventoryLogEntity) = JSONObject().apply {
    put("log_id", v.logId); put("event_id", v.eventId); put("product_id", v.productId); put("quantity", v.quantity)
}
internal fun financialJson(v: FinancialLogEntity) = JSONObject().apply {
    put("log_id", v.logId); put("event_id", v.eventId); put("account_id", v.accountId); put("amount", v.amount)
}
internal fun settingJson(v: SettingEntity) = JSONObject().apply {
    put("setting_key", v.key); put("setting_value", v.value); put("updated_at", v.updatedAt)
}
internal fun budgetJson(v: BudgetPlanEntity) = JSONObject().apply {
    put("budget_id", v.budgetId); put("category", v.category); put("period_start", v.periodStart); put("period_end_exclusive", v.periodEndExclusive)
    putNullable("planned_liters", v.plannedLiters); putNullable("planned_amount", v.plannedAmount); putNullable("vendor_rate_snapshot", v.vendorRateSnapshot)
    putNullable("retail_rate_snapshot", v.retailRateSnapshot); put("created_at", v.createdAt); putNullable("supersedes_id", v.supersedesId); put("archived", v.archived)
    put("uid", v.uid)
}
internal fun collectionJson(v: CollectionsTargetEntity) = JSONObject().apply {
    put("target_id", v.targetId); put("week_start", v.weekStart); put("week_end_exclusive", v.weekEndExclusive); put("target_amount", v.targetAmount)
    put("created_at", v.createdAt); putNullable("supersedes_id", v.supersedesId); put("archived", v.archived)
    put("uid", v.uid)
}
internal fun productProfileJson(v: ProductProfileEntity) = JSONObject().apply {
    put("product_id", v.productId); putNullable("default_sale_rate", v.defaultSaleRate); put("inventory_tracking", v.inventoryTracking)
    put("archived", v.archived); putNullable("notes", v.notes); put("created_at", v.createdAt)
}
internal fun customerProfileJson(v: CustomerProfileEntity) = JSONObject().apply {
    put("account_id", v.accountId); putNullable("phone", v.phone); putNullable("area_route", v.areaRoute); putNullable("address", v.address)
    putNullable("notes", v.notes); putNullable("credit_limit", v.creditLimit); putNullable("daily_quota", v.dailyQuota); put("active", v.active); put("created_at", v.createdAt)
}
internal fun vendorProfileJson(v: VendorProfileEntity) = JSONObject().apply {
    put("account_id", v.accountId); putNullable("phone", v.phone); putNullable("location", v.location); putNullable("notes", v.notes); put("active", v.active); put("created_at", v.createdAt)
}
internal fun employeeProfileJson(v: EmployeeProfileEntity) = JSONObject().apply {
    put("account_id", v.accountId); putNullable("role", v.role); putNullable("phone", v.phone); putNullable("reference_salary", v.referenceSalary)
    putNullable("salary_period", v.salaryPeriod); putNullable("join_date", v.joinDate); put("active", v.active); put("created_at", v.createdAt)
}
internal fun attendanceJson(v: AttendanceRecordEntity) = JSONObject().apply {
    put("attendance_id", v.attendanceId); put("employee_account_id", v.employeeAccountId); put("business_date", v.businessDate); put("status", v.status)
    putNullable("notes", v.notes); put("created_at", v.createdAt); putNullable("supersedes_id", v.supersedesId); put("archived", v.archived)
    put("uid", v.uid)
}
internal fun deliveryJson(v: DeliveryEntity) = JSONObject().apply {
    put("delivery_id", v.deliveryId); put("customer_account_id", v.customerAccountId); put("product_id", v.productId); put("quantity", v.quantity)
    putNullable("route", v.route); put("business_date", v.businessDate); putNullable("sale_event_id", v.saleEventId); putNullable("notes", v.notes)
    put("created_at", v.createdAt); put("archived", v.archived)
    put("uid", v.uid)
}
internal fun deliveryStatusJson(v: DeliveryStatusEntity) = JSONObject().apply {
    put("status_id", v.statusId); put("delivery_id", v.deliveryId); put("status", v.status); put("occurred_at", v.occurredAt); putNullable("notes", v.notes)
    put("uid", v.uid)
}
internal fun spoilageDetailJson(v: SpoilageDetailEntity) = JSONObject().apply {
    put("event_id", v.eventId); put("product_id", v.productId); put("quantity", v.quantity); put("reason", v.reason)
}
internal fun physicalCashCountJson(v: PhysicalCashCountEntity) = JSONObject().apply {
    put("count_id", v.countId); put("account_id", v.accountId); put("business_date", v.businessDate); put("counted_amount", v.countedAmount)
    put("counted_at", v.countedAt); putNullable("notes", v.notes)
    put("uid", v.uid)
}
internal fun expenseCategoryProfileJson(v: ExpenseCategoryProfileEntity) = JSONObject().apply {
    put("account_id", v.accountId); put("display_name", v.displayName); put("active", v.active); put("created_at", v.createdAt)
}
internal fun storeProfileJson(v: StoreProfileEntity) = JSONObject().apply {
    put("store_profile_id", v.storeProfileId); put("store_name", v.storeName); put("currency_code", v.currencyCode)
    putNullable("phone", v.phone); putNullable("address", v.address); put("updated_at", v.updatedAt)
}
internal fun openingBatchJson(v: OpeningBatchEntity) = JSONObject().apply {
    put("event_id", v.eventId)
}
internal fun openingInventoryEntryJson(v: OpeningInventoryEntryEntity) = JSONObject().apply {
    put("event_id", v.eventId); put("product_id", v.productId); put("quantity", v.quantity)
}
internal fun openingAccountEntryJson(v: OpeningAccountEntryEntity) = JSONObject().apply {
    put("event_id", v.eventId); put("account_id", v.accountId); put("balance", v.balance)
}
internal fun dailyCloseActionJson(v: DailyCloseActionEntity, includeSecurity: Boolean) = JSONObject().apply {
    put("action_id", v.actionId); put("business_date", v.businessDate); put("action", v.action)
    putNullable("physical_cash_count_id", v.physicalCashCountId); put("actor_name", v.actorName); put("actor_role", v.actorRole)
    if (includeSecurity) putNullable("actor_user_id", v.actorUserId)
    putNullable("reason", v.reason); put("created_at", v.createdAt); putNullable("supersedes_action_id", v.supersedesActionId)
    put("uid", v.uid)
}
internal fun appUserJson(v: AppUserEntity) = JSONObject().apply {
    put("user_id", v.userId); put("login_name", v.loginName); put("display_name", v.displayName); put("role", v.role)
    putNullable("employee_account_id", v.employeeAccountId); put("status", v.status); put("credential_setup_required", v.credentialSetupRequired)
    put("created_at", v.createdAt); putNullable("created_by_user_id", v.createdByUserId); put("updated_at", v.updatedAt)
    putNullable("disabled_at", v.disabledAt); putNullable("disabled_by_user_id", v.disabledByUserId)
}
internal fun userCredentialJson(v: UserCredentialEntity) = JSONObject().apply {
    put("user_id", v.userId); put("algorithm", v.algorithm); put("salt", Base64.getEncoder().encodeToString(v.salt))
    put("verifier", Base64.getEncoder().encodeToString(v.verifier)); put("memory_kib", v.memoryKib); put("iterations", v.iterations)
    put("parallelism", v.parallelism); put("changed_at", v.changedAt); put("must_change", v.mustChange)
}
internal fun savedRecoveryCodeJson(v: SavedRecoveryCodeEntity) = JSONObject().apply {
    put("recovery_code_id", v.recoveryCodeId); put("user_id", v.userId); put("salt", Base64.getEncoder().encodeToString(v.salt))
    put("verifier", Base64.getEncoder().encodeToString(v.verifier)); put("created_at", v.createdAt); putNullable("used_at", v.usedAt)
}
internal fun securityAuditEntryJson(v: SecurityAuditEntryEntity) = JSONObject().apply {
    put("audit_id", v.auditId); put("occurred_at", v.occurredAt); put("action", v.action); put("result", v.result)
    putNullable("reason_code", v.reasonCode); putNullable("actor_user_id", v.actorUserId); putNullable("actor_name_snapshot", v.actorNameSnapshot)
    putNullable("actor_role_snapshot", v.actorRoleSnapshot); putNullable("target_user_id", v.targetUserId); putNullable("details", v.details)
}
internal fun eventActorLinkJson(v: EventActorLinkEntity) = JSONObject().apply {
    put("event_id", v.eventId); put("actor_user_id", v.actorUserId); put("actor_name_snapshot", v.actorNameSnapshot)
    put("actor_role_snapshot", v.actorRoleSnapshot); put("attributed_at", v.attributedAt)
}
internal fun costingPolicyVersionJson(v: CostingPolicyVersionEntity) = JSONObject().apply {
    put("policy_id", v.policyId); put("calendar_month", v.calendarMonth); put("normal_capacity_kg", v.normalCapacityKg)
    put("review_reason", v.reviewReason); putNullable("notes", v.notes); put("approved_at", v.approvedAt)
    put("approved_by_user_id", v.approvedByUserId); putNullable("supersedes_policy_id", v.supersedesPolicyId)
}
internal fun costingPolicyComponentJson(v: CostingPolicyComponentEntity) = JSONObject().apply {
    put("policy_id", v.policyId); put("component_code", v.componentCode); put("source_amount_paisa", v.sourceAmountPaisa)
    put("basis_quantity_kg", v.basisQuantityKg)
}
internal fun inventoryCostBasisJson(v: InventoryCostBasisEntity) = JSONObject().apply {
    put("basis_id", v.basisId); put("opening_event_id", v.openingEventId); put("product_id", v.productId)
    put("quantity", v.quantity); put("total_cost_paisa", v.totalCostPaisa); put("basis_kind", v.basisKind)
    putNullable("evidence_notes", v.evidenceNotes); put("created_at", v.createdAt); put("created_by_user_id", v.createdByUserId)
    putNullable("supersedes_basis_id", v.supersedesBasisId)
}
internal fun yieldPolicyVersionJson(v: YieldPolicyVersionEntity) = JSONObject().apply {
    put("yield_policy_id", v.yieldPolicyId); put("sample_start_date", v.sampleStartDate); put("sample_end_date", v.sampleEndDate)
    put("accepted_batch_count", v.acceptedBatchCount); put("median_yield_ratio", v.medianYieldRatio); put("mad_yield_ratio", v.madYieldRatio)
    put("lower_alert_limit", v.lowerAlertLimit); put("upper_alert_limit", v.upperAlertLimit); put("effective_from", v.effectiveFrom)
    put("approved_at", v.approvedAt); put("approved_by_user_id", v.approvedByUserId)
    putNullable("supersedes_yield_policy_id", v.supersedesYieldPolicyId)
}
internal fun productionLossAssessmentJson(v: ProductionLossAssessmentEntity) = JSONObject().apply {
    put("assessment_id", v.assessmentId); put("production_event_id", v.productionEventId); put("classification", v.classification)
    put("abnormal_milk_liters", v.abnormalMilkLiters); put("reason_code", v.reasonCode); putNullable("notes", v.notes)
    put("assessed_at", v.assessedAt); put("assessed_by_user_id", v.assessedByUserId)
    putNullable("supersedes_assessment_id", v.supersedesAssessmentId)
}
internal fun installationBindingJson(v: AccountInstallationBindingEntity) = JSONObject().apply {
    put("binding_id", v.bindingId); put("environment", v.environment); put("business_id", v.businessId)
    put("shop_id", v.shopId); put("shop_name_snapshot", v.shopNameSnapshot); put("status", v.status)
    putNullable("verified_at_epoch_ms", v.verifiedAtEpochMs); putNullable("contract_version", v.contractVersion)
    put("created_at", v.createdAt); put("updated_at", v.updatedAt)
}
internal fun identityLinkJson(v: ServerIdentityLinkEntity) = JSONObject().apply {
    put("local_user_id", v.localUserId); put("server_staff_id", v.serverStaffId); put("business_id", v.businessId)
    putNullable("username_snapshot", v.usernameSnapshot); putNullable("role_snapshot", v.roleSnapshot); put("status", v.status)
    putNullable("verified_at_epoch_ms", v.verifiedAtEpochMs); putNullable("server_changed_at_epoch_ms", v.serverChangedAtEpochMs)
    putNullable("last_error_code", v.lastErrorCode)
}
internal fun serverShopSnapshotJson(v: ServerShopSnapshotEntity) = JSONObject().apply {
    put("snapshot_id", v.snapshotId); put("server_shop_id", v.serverShopId); put("shop_name_snapshot", v.shopNameSnapshot)
    put("observed_at_epoch_ms", v.observedAtEpochMs); put("local_user_id", v.localUserId)
}

internal fun JSONObject.putNullable(key: String, value: Any?) { put(key, value ?: JSONObject.NULL) }

/**
 * Pre-schema-10 backups carry no uid fields. Their rows must still import
 * with the same deterministic identities the schema-10 migration assigns, so
 * restoring an old backup never forks row identity across devices.
 */
private fun JSONObject.restoredUid(key: String): String? = optStringOrNull(key)?.takeIf { it.isNotBlank() }

internal fun readProducts(a: JSONArray?): List<ProductEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let {
    val productId = it.getLong("product_id"); val name = it.getString("name"); val unit = it.getString("unit")
    ProductEntity(productId, name, unit, uid = it.restoredUid("uid") ?: LegacyUid.product(productId, name, unit))
} }
internal fun readAccounts(a: JSONArray?): List<AccountEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let {
    val accountId = it.getLong("account_id"); val name = it.getString("name"); val category = it.getString("category")
    AccountEntity(accountId, name, it.getString("type"), category, uid = it.restoredUid("uid") ?: LegacyUid.account(accountId, name, category))
} }
internal fun readEvents(a: JSONArray?): List<EventEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let { EventEntity(
    eventId = it.getLong("event_id"), createdAt = it.getString("created_at"), occurredAt = it.getString("occurred_at"), eventType = it.getString("event_type"),
    channel = it.optStringOrNull("channel"), correctsEventId = it.optLongOrNull("corrects_event_id"), rawLiters = it.optDoubleOrNull("raw_liters"),
    fatPercentage = it.optDoubleOrNull("fat_percentage"), lrReading = it.optDoubleOrNull("lr_reading"), snfPercentage = it.optDoubleOrNull("snf_percentage"),
    tsPercentage = it.optDoubleOrNull("ts_percentage"), adjustedLiters = it.optDoubleOrNull("adjusted_liters"), amountOwed = it.optDoubleOrNull("amount_owed"),
    milkLitersUsed = it.optDoubleOrNull("milk_liters_used"), yogurtKgProduced = it.optDoubleOrNull("yogurt_kg_produced"), amount = it.optDoubleOrNull("amount"),
    milkRateUsed = it.optDoubleOrNull("milk_rate_used"), deliveryFeeUsed = it.optDoubleOrNull("delivery_fee_used"), tsStandardUsed = it.optDoubleOrNull("ts_standard_used"),
    saleQuantity = it.optDoubleOrNull("sale_quantity"), saleValue = it.optDoubleOrNull("sale_value"), unitRate = it.optDoubleOrNull("unit_rate"),
    surchargeApplied = it.optDouble("surcharge_applied", 0.0), notes = it.optStringOrNull("notes"),
    eventUid = it.restoredUid("event_uid")
        ?: LegacyUid.event(it.getLong("event_id"), it.getString("event_type"), it.getString("created_at")),
    originDeviceId = it.optStringOrNull("origin_device_id") ?: ""
) } }
internal fun readInventoryLogs(a: JSONArray?): List<InventoryLogEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let { InventoryLogEntity(it.getLong("log_id"), it.getLong("event_id"), it.getLong("product_id"), it.getDouble("quantity")) } }
internal fun readFinancialLogs(a: JSONArray?): List<FinancialLogEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let { FinancialLogEntity(it.getLong("log_id"), it.getLong("event_id"), it.getLong("account_id"), it.getDouble("amount")) } }
internal fun readSettings(a: JSONArray?): List<SettingEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let { SettingEntity(it.getString("setting_key"), it.getDouble("setting_value"), it.getString("updated_at")) } }
internal fun readBudgets(a: JSONArray?): List<BudgetPlanEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let { BudgetPlanEntity(
    budgetId = it.getLong("budget_id"), category = it.getString("category"), periodStart = it.getString("period_start"), periodEndExclusive = it.getString("period_end_exclusive"),
    plannedLiters = it.optDoubleOrNull("planned_liters"), plannedAmount = it.optDoubleOrNull("planned_amount"), vendorRateSnapshot = it.optDoubleOrNull("vendor_rate_snapshot"),
    retailRateSnapshot = it.optDoubleOrNull("retail_rate_snapshot"), createdAt = it.getString("created_at"), supersedesId = it.optLongOrNull("supersedes_id"), archived = it.optBoolean("archived", false),
    uid = it.restoredUid("uid")
        ?: LegacyUid.budget(it.getLong("budget_id"), it.getString("category"), it.getString("period_start"), it.getString("created_at"))
) } }
internal fun readCollectionsTargets(a: JSONArray?): List<CollectionsTargetEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let { CollectionsTargetEntity(
    targetId = it.getLong("target_id"), weekStart = it.getString("week_start"), weekEndExclusive = it.getString("week_end_exclusive"), targetAmount = it.getDouble("target_amount"),
    createdAt = it.getString("created_at"), supersedesId = it.optLongOrNull("supersedes_id"), archived = it.optBoolean("archived", false),
    uid = it.restoredUid("uid")
        ?: LegacyUid.collectionsTarget(it.getLong("target_id"), it.getString("week_start"), it.getString("created_at"))
) } }
internal fun readProductProfiles(a: JSONArray?): List<ProductProfileEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let {
    ProductProfileEntity(it.getLong("product_id"), it.optDoubleOrNull("default_sale_rate"), it.optBoolean("inventory_tracking", true), it.optBoolean("archived", false), it.optStringOrNull("notes"), it.getString("created_at"))
} }
internal fun readCustomerProfiles(a: JSONArray?): List<CustomerProfileEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let {
    CustomerProfileEntity(it.getLong("account_id"), it.optStringOrNull("phone"), it.optStringOrNull("area_route"), it.optStringOrNull("address"), it.optStringOrNull("notes"), it.optDoubleOrNull("credit_limit"), it.optDoubleOrNull("daily_quota"), it.optBoolean("active", true), it.getString("created_at"))
} }
internal fun readVendorProfiles(a: JSONArray?): List<VendorProfileEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let {
    VendorProfileEntity(it.getLong("account_id"), it.optStringOrNull("phone"), it.optStringOrNull("location"), it.optStringOrNull("notes"), it.optBoolean("active", true), it.getString("created_at"))
} }
internal fun readEmployeeProfiles(a: JSONArray?): List<EmployeeProfileEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let {
    EmployeeProfileEntity(it.getLong("account_id"), it.optStringOrNull("role"), it.optStringOrNull("phone"), it.optDoubleOrNull("reference_salary"), it.optStringOrNull("salary_period"), it.optStringOrNull("join_date"), it.optBoolean("active", true), it.getString("created_at"))
} }
internal fun readAttendanceRecords(a: JSONArray?): List<AttendanceRecordEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let {
    AttendanceRecordEntity(it.getLong("attendance_id"), it.getLong("employee_account_id"), it.getString("business_date"), it.getString("status"), it.optStringOrNull("notes"), it.getString("created_at"), it.optLongOrNull("supersedes_id"), it.optBoolean("archived", false),
        uid = it.restoredUid("uid")
            ?: LegacyUid.attendance(it.getLong("attendance_id"), it.getString("business_date"), it.getString("created_at")))
} }
internal fun readDeliveries(a: JSONArray?): List<DeliveryEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let {
    DeliveryEntity(it.getLong("delivery_id"), it.getLong("customer_account_id"), it.getLong("product_id"), it.getDouble("quantity"), it.optStringOrNull("route"), it.getString("business_date"), it.optLongOrNull("sale_event_id"), it.optStringOrNull("notes"), it.getString("created_at"), it.optBoolean("archived", false),
        uid = it.restoredUid("uid")
            ?: LegacyUid.delivery(it.getLong("delivery_id"), it.getString("business_date"), it.getString("created_at")))
} }
internal fun readDeliveryStatuses(a: JSONArray?): List<DeliveryStatusEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let {
    DeliveryStatusEntity(it.getLong("status_id"), it.getLong("delivery_id"), it.getString("status"), it.getString("occurred_at"), it.optStringOrNull("notes"),
        uid = it.restoredUid("uid")
            ?: LegacyUid.deliveryStatus(it.getLong("status_id"), it.getLong("delivery_id"), it.getString("occurred_at")))
} }
internal fun readSpoilageDetails(a: JSONArray?): List<SpoilageDetailEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let {
    SpoilageDetailEntity(it.getLong("event_id"), it.getLong("product_id"), it.getDouble("quantity"), it.getString("reason"))
} }
internal fun readPhysicalCashCounts(a: JSONArray?): List<PhysicalCashCountEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let {
    PhysicalCashCountEntity(it.getLong("count_id"), it.getLong("account_id"), it.getString("business_date"), it.getDouble("counted_amount"), it.getString("counted_at"), it.optStringOrNull("notes"),
        uid = it.restoredUid("uid")
            ?: LegacyUid.physicalCashCount(it.getLong("count_id"), it.getString("business_date"), it.getString("counted_at")))
} }
internal fun readExpenseCategoryProfiles(a: JSONArray?): List<ExpenseCategoryProfileEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let {
    ExpenseCategoryProfileEntity(it.getLong("account_id"), it.getString("display_name"), it.optBoolean("active", true), it.getString("created_at"))
} }
internal fun readStoreProfiles(a: JSONArray?): List<StoreProfileEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let {
    StoreProfileEntity(it.getInt("store_profile_id"), it.getString("store_name"), it.getString("currency_code"), it.optStringOrNull("phone"), it.optStringOrNull("address"), it.getString("updated_at"))
} }
internal fun readOpeningBatches(a: JSONArray?): List<OpeningBatchEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let {
    OpeningBatchEntity(it.getLong("event_id"))
} }
internal fun readOpeningInventoryEntries(a: JSONArray?): List<OpeningInventoryEntryEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let {
    OpeningInventoryEntryEntity(it.getLong("event_id"), it.getLong("product_id"), it.getDouble("quantity"))
} }
internal fun readOpeningAccountEntries(a: JSONArray?): List<OpeningAccountEntryEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let {
    OpeningAccountEntryEntity(it.getLong("event_id"), it.getLong("account_id"), it.getDouble("balance"))
} }
internal fun readDailyCloseActions(a: JSONArray?): List<DailyCloseActionEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let {
    DailyCloseActionEntity(
        actionId = it.getLong("action_id"), businessDate = it.getString("business_date"), action = it.getString("action"),
        physicalCashCountId = it.optLongOrNull("physical_cash_count_id"), actorName = it.getString("actor_name"), actorRole = it.getString("actor_role"),
        actorUserId = it.optLongOrNull("actor_user_id"), reason = it.optStringOrNull("reason"), createdAt = it.getString("created_at"), supersedesActionId = it.optLongOrNull("supersedes_action_id"),
        uid = it.restoredUid("uid")
            ?: LegacyUid.dailyCloseAction(it.getLong("action_id"), it.getString("business_date"), it.getString("action"), it.getString("created_at"))
    )
} }
internal fun readAppUsers(a: JSONArray?): List<AppUserEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let {
    AppUserEntity(
        userId = it.getLong("user_id"), loginName = it.getString("login_name"), displayName = it.getString("display_name"), role = it.getString("role"),
        employeeAccountId = it.optLongOrNull("employee_account_id"), status = it.getString("status"), credentialSetupRequired = it.optBoolean("credential_setup_required", false),
        createdAt = it.getString("created_at"), createdByUserId = it.optLongOrNull("created_by_user_id"), updatedAt = it.getString("updated_at"),
        disabledAt = it.optStringOrNull("disabled_at"), disabledByUserId = it.optLongOrNull("disabled_by_user_id")
    )
} }
internal fun readUserCredentials(a: JSONArray?): List<UserCredentialEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let {
    UserCredentialEntity(
        userId = it.getLong("user_id"), algorithm = it.getString("algorithm"), salt = Base64.getDecoder().decode(it.getString("salt")),
        verifier = Base64.getDecoder().decode(it.getString("verifier")), memoryKib = it.getInt("memory_kib"), iterations = it.getInt("iterations"),
        parallelism = it.getInt("parallelism"), changedAt = it.getString("changed_at"), mustChange = it.optBoolean("must_change", false),
        failedAttemptCount = 0, lastFailedAtEpochMs = null, lockedUntilEpochMs = null
    )
} }
internal fun readSavedRecoveryCodes(a: JSONArray?): List<SavedRecoveryCodeEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let {
    SavedRecoveryCodeEntity(
        recoveryCodeId = it.getLong("recovery_code_id"), userId = it.getLong("user_id"), salt = Base64.getDecoder().decode(it.getString("salt")),
        verifier = Base64.getDecoder().decode(it.getString("verifier")), createdAt = it.getString("created_at"), usedAt = it.optStringOrNull("used_at"),
        failedAttemptCount = 0, lockedUntilEpochMs = null
    )
} }
internal fun readSecurityAuditEntries(a: JSONArray?): List<SecurityAuditEntryEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let {
    SecurityAuditEntryEntity(
        auditId = it.getLong("audit_id"), occurredAt = it.getString("occurred_at"), action = it.getString("action"), result = it.getString("result"),
        reasonCode = it.optStringOrNull("reason_code"), actorUserId = it.optLongOrNull("actor_user_id"), actorNameSnapshot = it.optStringOrNull("actor_name_snapshot"),
        actorRoleSnapshot = it.optStringOrNull("actor_role_snapshot"), targetUserId = it.optLongOrNull("target_user_id"), details = it.optStringOrNull("details")
    )
} }
internal fun readEventActorLinks(a: JSONArray?): List<EventActorLinkEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let {
    EventActorLinkEntity(
        eventId = it.getLong("event_id"), actorUserId = it.getLong("actor_user_id"), actorNameSnapshot = it.getString("actor_name_snapshot"),
        actorRoleSnapshot = it.getString("actor_role_snapshot"), attributedAt = it.getString("attributed_at")
    )
} }
internal fun readCostingPolicyVersions(a: JSONArray?): List<CostingPolicyVersionEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let {
    CostingPolicyVersionEntity(
        policyId = it.getLong("policy_id"), calendarMonth = it.getString("calendar_month"), normalCapacityKg = it.getString("normal_capacity_kg"),
        reviewReason = it.getString("review_reason"), notes = it.optStringOrNull("notes"), approvedAt = it.getString("approved_at"),
        approvedByUserId = it.getLong("approved_by_user_id"), supersedesPolicyId = it.optLongOrNull("supersedes_policy_id")
    )
} }
internal fun readCostingPolicyComponents(a: JSONArray?): List<CostingPolicyComponentEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let {
    CostingPolicyComponentEntity(it.getLong("policy_id"), it.getString("component_code"), it.getLong("source_amount_paisa"), it.getString("basis_quantity_kg"))
} }
internal fun readInventoryCostBasisEntries(a: JSONArray?): List<InventoryCostBasisEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let {
    InventoryCostBasisEntity(
        basisId = it.getLong("basis_id"), openingEventId = it.getLong("opening_event_id"), productId = it.getLong("product_id"),
        quantity = it.getString("quantity"), totalCostPaisa = it.getLong("total_cost_paisa"), basisKind = it.getString("basis_kind"),
        evidenceNotes = it.optStringOrNull("evidence_notes"), createdAt = it.getString("created_at"), createdByUserId = it.getLong("created_by_user_id"),
        supersedesBasisId = it.optLongOrNull("supersedes_basis_id")
    )
} }
internal fun readYieldPolicyVersions(a: JSONArray?): List<YieldPolicyVersionEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let {
    YieldPolicyVersionEntity(
        yieldPolicyId = it.getLong("yield_policy_id"), sampleStartDate = it.getString("sample_start_date"), sampleEndDate = it.getString("sample_end_date"),
        acceptedBatchCount = it.getInt("accepted_batch_count"), medianYieldRatio = it.getString("median_yield_ratio"), madYieldRatio = it.getString("mad_yield_ratio"),
        lowerAlertLimit = it.getString("lower_alert_limit"), upperAlertLimit = it.getString("upper_alert_limit"), effectiveFrom = it.getString("effective_from"),
        approvedAt = it.getString("approved_at"), approvedByUserId = it.getLong("approved_by_user_id"),
        supersedesYieldPolicyId = it.optLongOrNull("supersedes_yield_policy_id")
    )
} }
internal fun readProductionLossAssessments(a: JSONArray?): List<ProductionLossAssessmentEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let {
    ProductionLossAssessmentEntity(
        assessmentId = it.getLong("assessment_id"), productionEventId = it.getLong("production_event_id"), classification = it.getString("classification"),
        abnormalMilkLiters = it.getString("abnormal_milk_liters"), reasonCode = it.getString("reason_code"), notes = it.optStringOrNull("notes"),
        assessedAt = it.getString("assessed_at"), assessedByUserId = it.getLong("assessed_by_user_id"),
        supersedesAssessmentId = it.optLongOrNull("supersedes_assessment_id")
    )
} }

internal fun JSONObject.optStringOrNull(key: String): String? = if (!has(key) || isNull(key)) null else getString(key)
internal fun JSONObject.optLongOrNull(key: String): Long? = if (!has(key) || isNull(key)) null else getLong(key)
internal fun JSONObject.optDoubleOrNull(key: String): Double? = if (!has(key) || isNull(key)) null else getDouble(key)

internal fun readInstallationBindings(a: JSONArray?): List<AccountInstallationBindingEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let {
    AccountInstallationBindingEntity(
        bindingId = it.getLong("binding_id"), environment = it.getString("environment"), businessId = it.getString("business_id"),
        shopId = it.getString("shop_id"), shopNameSnapshot = it.getString("shop_name_snapshot"), status = it.getString("status"),
        verifiedAtEpochMs = it.optLongOrNull("verified_at_epoch_ms"), contractVersion = it.optStringOrNull("contract_version"),
        createdAt = it.getString("created_at"), updatedAt = it.getString("updated_at")
    )
} }
internal fun readIdentityLinks(a: JSONArray?): List<ServerIdentityLinkEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let {
    ServerIdentityLinkEntity(
        localUserId = it.getLong("local_user_id"), serverStaffId = it.getString("server_staff_id"), businessId = it.getString("business_id"),
        usernameSnapshot = it.optStringOrNull("username_snapshot"), roleSnapshot = it.optStringOrNull("role_snapshot"), status = it.getString("status"),
        verifiedAtEpochMs = it.optLongOrNull("verified_at_epoch_ms"), serverChangedAtEpochMs = it.optLongOrNull("server_changed_at_epoch_ms"),
        lastErrorCode = it.optStringOrNull("last_error_code")
    )
} }
internal fun readServerShopSnapshots(a: JSONArray?): List<ServerShopSnapshotEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let {
    ServerShopSnapshotEntity(
        snapshotId = it.getLong("snapshot_id"), serverShopId = it.getString("server_shop_id"), shopNameSnapshot = it.getString("shop_name_snapshot"),
        observedAtEpochMs = it.getLong("observed_at_epoch_ms"), localUserId = it.getLong("local_user_id")
    )
} }
internal fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
