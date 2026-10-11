package com.saaf.milkshop

import androidx.room3.executeSQL
import androidx.room3.withWriteTransaction
import com.saaf.milkshop.data.*
import com.saaf.milkshop.domain.ServerAccountPolicy
import org.json.JSONObject

internal class BackupRestoreEngine(
    private val createSafetySnapshot: suspend (MilkShopDatabase) -> Unit,
    private val buildVerifiedBytes: suspend (MilkShopDatabase, Boolean, Int) -> ByteArray,
    private val capturePreferences: () -> JSONObject,
    private val restorePreferences: (JSONObject?) -> Unit,
    private val rebaselineStore: com.saaf.milkshop.sync.SyncRebaselineStore,
    private val afterRestore: suspend () -> Unit = {},
) {
    suspend fun restore(
        database: MilkShopDatabase,
        json: JSONObject,
        selection: BackupManager.RestoreSelection,
        includeSecurity: Boolean,
    ) = com.saaf.milkshop.sync.SyncOperationGate.exclusive {
        restoreLocked(database, json, selection, includeSecurity)
    }

    private suspend fun restoreLocked(
        database: MilkShopDatabase,
        json: JSONObject,
        selection: BackupManager.RestoreSelection,
        includeSecurity: Boolean,
    ) {
        // Safety snapshot is created before touching current state.
        createSafetySnapshot(database)
        val previousPreferences = if (selection.preferences) capturePreferences() else null
        // Read recovery metadata before persisting the fail-closed flags and
        // replacing Room; failed transactions restore the previous flags.
        val formatVersion = json.getJSONObject("metadata").optInt("format_version", 0)
        val prevDeviceId = json.getJSONObject("metadata").optString("device_id").takeIf { it.isNotEmpty() }
        val restoredOutbox = readSyncOutboxRows(json.getJSONObject("data").optJSONArray("sync_outbox"))
        val oldFreshIdentity = rebaselineStore.needsFreshIdentity
        val oldPrevDevice = rebaselineStore.prevDeviceId
        val oldRestoredPending = rebaselineStore.restoredRebaseline
        val oldLivePending = rebaselineStore.liveRebaselinePending
        try {
            // Persist the fail-closed gate BEFORE replacing Room. A process
            // death after commit cannot expose the restored queue to pushes.
            rebaselineStore.needsFreshIdentity = true
            rebaselineStore.prevDeviceId = prevDeviceId
            if (selection.business) {
                rebaselineStore.restoredRebaseline = formatVersion >= 5
                rebaselineStore.liveRebaselinePending = false
            }
            database.withWriteTransaction<Unit> {
                val data = json.getJSONObject("data")
                val backupDao = database.backupDataDao()
                if (selection.business) {
                    Phase3CostingMigrationSql.immutableTriggerNames.forEach { executeSQL("DROP TRIGGER IF EXISTS $it") }
                    // Sync capture triggers must not fire while the snapshot's
                    // rows stream in: a restored database starts with an empty
                    // outbox, not a re-push of its own contents.
                    SyncMigrationSql.triggerNames.forEach { executeSQL("DROP TRIGGER IF EXISTS $it") }
                    if (includeSecurity) {
                        executeSQL("DROP TRIGGER IF EXISTS prevent_app_user_delete")
                        executeSQL("DROP TRIGGER IF EXISTS prevent_security_audit_delete")
                        executeSQL("DROP TRIGGER IF EXISTS validate_verified_daily_close_actor")
                    }
                    backupDao.deleteInventoryCostMovements()
                    backupDao.deleteCostProjectionState()
                    backupDao.deleteCostingPolicyComponents()
                    backupDao.deleteProductionLossAssessments()
                    backupDao.deleteYieldPolicyVersions()
                    backupDao.deleteInventoryCostBasisEntries()
                    backupDao.deleteCostingPolicyVersions()
                    backupDao.deleteDailyCloseActions()
                    backupDao.deleteEventActorLinks()
                    if (includeSecurity) {
                        backupDao.deleteSecurityAuditEntries()
                        backupDao.deleteSavedRecoveryCodes()
                        backupDao.deleteUserCredentials()
                        val serverAccountDao = database.serverAccountDao()
                        serverAccountDao.deleteServerShopSnapshots()
                        serverAccountDao.deleteIdentityLinks()
                        serverAccountDao.deleteBindings()
                        backupDao.deleteAppUsers()
                    }
                    backupDao.deleteOpeningAccountEntries()
                    backupDao.deleteOpeningInventoryEntries()
                    backupDao.deleteOpeningBatches()
                    backupDao.deleteStoreProfiles()
                    backupDao.deleteDeliveryStatuses()
                    backupDao.deleteAttendanceRecords()
                    backupDao.deleteDeliveries()
                    backupDao.deleteSpoilageDetails()
                    backupDao.deletePhysicalCashCounts()
                    backupDao.deleteCustomerProfiles()
                    backupDao.deleteVendorProfiles()
                    backupDao.deleteEmployeeProfiles()
                    backupDao.deleteProductProfiles()
                    backupDao.deleteExpenseCategoryProfiles()
                    backupDao.deleteFinancialLogs()
                    backupDao.deleteInventoryLogs()
                    backupDao.deleteEvents()
                    backupDao.deleteAccounts()
                    backupDao.deleteProducts()
                    backupDao.deleteSettings()
                    backupDao.deleteSyncOutbox()
                    backupDao.deleteSyncPullState()
                    backupDao.insertProducts(readProducts(data.optJSONArray("products")))
                    backupDao.insertAccounts(readAccounts(data.optJSONArray("accounts")))
                    backupDao.insertEvents(readEvents(data.optJSONArray("events")))
                    backupDao.insertStoreProfiles(readStoreProfiles(data.optJSONArray("store_profile")))
                    backupDao.insertOpeningBatches(readOpeningBatches(data.optJSONArray("opening_batches")))
                    backupDao.insertOpeningInventoryEntries(readOpeningInventoryEntries(data.optJSONArray("opening_inventory_entries")))
                    backupDao.insertOpeningAccountEntries(readOpeningAccountEntries(data.optJSONArray("opening_account_entries")))
                    backupDao.insertInventoryLogs(readInventoryLogs(data.optJSONArray("inventory_logs")))
                    backupDao.insertFinancialLogs(readFinancialLogs(data.optJSONArray("financial_logs")))
                    backupDao.insertSettings(readSettings(data.optJSONArray("settings")))
                    backupDao.insertProductProfiles(readProductProfiles(data.optJSONArray("product_profiles")))
                    backupDao.insertCustomerProfiles(readCustomerProfiles(data.optJSONArray("customer_profiles")))
                    backupDao.insertVendorProfiles(readVendorProfiles(data.optJSONArray("vendor_profiles")))
                    backupDao.insertEmployeeProfiles(readEmployeeProfiles(data.optJSONArray("employee_profiles")))

                    if (includeSecurity) {
                        val users = readAppUsers(data.getJSONArray("app_users"))
                        require(users.isNotEmpty()) { "Encrypted backup contains no application users" }
                        // Insert FK-neutral identity shells first, then restore their
                        // creator/disable links once every referenced user exists.
                        backupDao.insertAppUsers(users.map {
                            it.copy(
                                status = "active",
                                createdByUserId = null,
                                disabledAt = null,
                                disabledByUserId = null,
                            )
                        })
                        backupDao.updateAppUsers(users)
                        backupDao.insertUserCredentials(readUserCredentials(data.getJSONArray("user_credentials")))
                        backupDao.insertSavedRecoveryCodes(readSavedRecoveryCodes(data.getJSONArray("saved_recovery_codes")))
                        // Account Bridge state (format 3; absent in legacy backups):
                        // stable server IDs are preserved, but every binding/link
                        // becomes restored_unverified regardless of its backed-up
                        // value — re-verification is required before server-derived
                        // entitlements are trusted again.
                        val serverAccountDao = database.serverAccountDao()
                        readInstallationBindings(data.optJSONArray("server_account_binding")).forEach { binding ->
                            serverAccountDao.insertBinding(binding.copy(status = ServerAccountPolicy.BindingStatus.RESTORED_UNVERIFIED))
                        }
                        readIdentityLinks(data.optJSONArray("server_identity_links")).forEach { link ->
                            serverAccountDao.insertIdentityLink(link.copy(status = ServerAccountPolicy.LinkStatus.RESTORED_UNVERIFIED))
                        }
                        serverAccountDao.insertServerShopSnapshots(readServerShopSnapshots(data.optJSONArray("server_shop_snapshots")))
                        backupDao.insertCostingPolicyVersions(readCostingPolicyVersions(data.optJSONArray("costing_policy_versions")))
                        backupDao.insertCostingPolicyComponents(readCostingPolicyComponents(data.optJSONArray("costing_policy_components")))
                        backupDao.insertInventoryCostBasisEntries(readInventoryCostBasisEntries(data.optJSONArray("inventory_cost_basis_entries")))
                        backupDao.insertYieldPolicyVersions(readYieldPolicyVersions(data.optJSONArray("yield_policy_versions")))
                        backupDao.insertProductionLossAssessments(readProductionLossAssessments(data.optJSONArray("production_loss_assessments")))
                    }

                    backupDao.insertAttendanceRecords(readAttendanceRecords(data.optJSONArray("attendance_records")))
                    backupDao.insertDeliveries(readDeliveries(data.optJSONArray("deliveries")))
                    backupDao.insertDeliveryStatuses(readDeliveryStatuses(data.optJSONArray("delivery_status_history")))
                    backupDao.insertSpoilageDetails(readSpoilageDetails(data.optJSONArray("spoilage_details")))
                    val physicalCashCounts = readPhysicalCashCounts(data.optJSONArray("physical_cash_counts"))
                    val dailyCloseActions = readDailyCloseActions(data.optJSONArray("daily_close_actions"))
                    restoreCashCountsAndDailyCloses(backupDao, physicalCashCounts, dailyCloseActions)
                    backupDao.insertExpenseCategoryProfiles(readExpenseCategoryProfiles(data.optJSONArray("expense_category_profiles")))
                    if (includeSecurity) {
                        backupDao.insertEventActorLinks(readEventActorLinks(data.getJSONArray("event_actor_links")))
                        backupDao.insertSecurityAuditEntries(readSecurityAuditEntries(data.getJSONArray("security_audit_entries")))
                    }
                    // Schema-3 backups do not contain the new one-to-one profile arrays.
                    // Backfill neutral profiles so old backups remain usable after restore.
                    backupDao.backfillProductProfiles()
                    backupDao.backfillCustomerProfiles()
                    backupDao.backfillVendorProfiles()
                    backupDao.backfillEmployeeProfiles()
                    backupDao.backfillExpenseCategoryProfiles()
                    // C1: the restore path resets the pull cursor to 0 rather
                    // than resuming from the snapshot's cursor — parked rows
                    // do not ride in backups, so a full idempotent re-pull is
                    // what guarantees nothing was lost, and a stale cursor can
                    // never skip server rows a fresh device has not seen.
                    backupDao.insertSyncPullState(
                        listOf(
                            com.saaf.milkshop.data.SyncPullStateEntity(
                                stateId = 1,
                                lastServerSeq = 0L,
                                updatedAt = java.time.Instant.now().toString(),
                            ),
                        ),
                    )
                    database.syncParkedRowDao().clear()
                    database.syncRebaselineHoldDao().clear()
                    if (formatVersion >= 5) {
                        // go 8: format-5 restores carry the outbox so edits that
                        // were unsynced at backup time survive; the next
                        // enrollment classifies them against the server
                        // (own-origin drops, foreign-origin holds for review).
                        backupDao.insertSyncOutbox(restoredOutbox.map { it.copy(held = true) })
                    }
                    Phase3CostingMigrationSql.immutableTriggers.forEach { executeSQL(it) }
                    SyncMigrationSql.captureTriggers.forEach { executeSQL(it) }
                    if (includeSecurity) {
                        Phase3SecurityMigrationSql.triggerNames.zip(Phase3SecurityMigrationSql.createTriggers)
                            .filter { (name, _) ->
                                name in setOf(
                                    "prevent_app_user_delete",
                                    "prevent_security_audit_delete",
                                    "validate_verified_daily_close_actor",
                                )
                            }
                            .forEach { (_, sql) -> executeSQL(sql) }
                    }
                }
                if (selection.planning) {
                    backupDao.deleteBudgets()
                    backupDao.deleteCollectionsTargets()
                    backupDao.insertBudgets(readBudgets(data.optJSONArray("budgets")))
                    backupDao.insertCollectionsTargets(readCollectionsTargets(data.optJSONArray("collections_targets")))
                }
                if (selection.preferences) restorePreferences(data.optJSONObject("preferences"))

                // Verification occurs before the transaction is allowed to commit.
                readAndVerify(buildVerifiedBytes(database, includeSecurity, if (includeSecurity) BackupManager.FORMAT_VERSION else BackupManager.LEGACY_FORMAT_VERSION))
                Unit
            }
            // Account Bridge spec §8: every restore deletes persisted remote
            // tokens. Cleanup failure never masks a successful restore.
            runCatching { afterRestore() }
            // go 8: re-baseline state for the forced re-enrollment that follows
            // any restore. EVERY restore marks the installation for a fresh
            // device identity at enrollment (a restored device must never push
            // as the id that wrote its backup — same-origin overwrites escape
            // the server's conflict audit). Format-5 restores also carried the
            // outbox + prev id, so the next enrollment runs the classified
            // pull-first flow; formats 3/4 keep the legacy push-first flow.
        } catch (error: Throwable) {
            rebaselineStore.needsFreshIdentity = oldFreshIdentity
            rebaselineStore.prevDeviceId = oldPrevDevice
            rebaselineStore.restoredRebaseline = oldRestoredPending
            rebaselineStore.liveRebaselinePending = oldLivePending
            if (previousPreferences != null) {
                runCatching { restorePreferences(previousPreferences) }
                    .onFailure(error::addSuppressed)
            }
            throw error
        }
    }

    private suspend fun restoreCashCountsAndDailyCloses(
        dao: BackupDataDao,
        counts: List<PhysicalCashCountEntity>,
        actions: List<DailyCloseActionEntity>,
    ) {
        if (actions.isEmpty()) {
            dao.insertPhysicalCashCounts(counts)
            return
        }

        val actionDates = actions.mapTo(mutableSetOf()) { it.businessDate }
        counts.filter { it.businessDate !in actionDates }
            .sortedWith(compareBy<PhysicalCashCountEntity> { it.businessDate }.thenBy { it.countedAt }.thenBy { it.countId })
            .forEach { dao.insertPhysicalCashCount(it) }

        actionDates.sorted().forEach { businessDate ->
            val dateCounts = counts.filter { it.businessDate == businessDate }
                .sortedWith(compareBy<PhysicalCashCountEntity> { it.countedAt }.thenBy { it.countId })
            val dateActions = actions.filter { it.businessDate == businessDate }.sortedBy { it.actionId }
            val insertedCountIds = mutableSetOf<Long>()

            dateActions.forEach { action ->
                action.physicalCashCountId?.let { requiredCountId ->
                    val requiredIndex = dateCounts.indexOfFirst { it.countId == requiredCountId }
                    require(requiredIndex >= 0) { "Daily close references a missing cash count: $requiredCountId" }
                    dateCounts.take(requiredIndex + 1)
                        .filter { insertedCountIds.add(it.countId) }
                        .forEach { dao.insertPhysicalCashCount(it) }
                }
                dao.insertDailyCloseAction(action)
            }

            dateCounts.filter { insertedCountIds.add(it.countId) }
                .forEach { dao.insertPhysicalCashCount(it) }
        }
    }

}
