package com.saaf.milkshop

import android.content.Context
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.saaf.milkshop.data.*
import com.saaf.milkshop.domain.AppPasswordPolicy
import com.saaf.milkshop.domain.Argon2idPasswordHasher
import com.saaf.milkshop.domain.NativeSessionManager
import androidx.room3.withWriteTransaction
import androidx.room3.executeSQL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.KeyStore
import java.text.Normalizer
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.Locale
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class BackupManager(private val context: Context) {
    companion object {
        const val LEGACY_FORMAT_VERSION = 1
        const val FORMAT_VERSION = 2
        const val CURRENT_SCHEMA_VERSION = 8
        const val FULL_GROUP = "business"
        const val PLANNING_GROUP = "planning"
        const val PREFERENCES_GROUP = "preferences"
        const val BACKUP_EXTENSION = ".milkbackup"
        const val BACKUP_DIR = "backup_snapshots"
        const val CONFIRMATION_PHRASE = "I understood the consequences and I agree to proceed forward"
        private const val BACKUP_KDF_ALGORITHM = "argon2id"
        private const val BACKUP_CIPHER_ALGORITHM = "AES-256-GCM"
        private const val BACKUP_KEY_BYTES = 32
        private const val BACKUP_NONCE_BYTES = 12
        private const val BACKUP_TAG_BITS = 128
        private const val BACKUP_AAD = "saaf-milk-backup-v2"
        private const val LOCAL_BACKUP_AAD = "saaf-milk-local-backup-v2"
        private const val LOCAL_BACKUP_PROTECTION = "android-keystore"
        private const val LOCAL_BACKUP_KEY_ALIAS = "saaf_milk_local_backup_v2"
        private val fileFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss", Locale.US)

        fun displayName(context: Context, now: LocalDateTime = LocalDateTime.now()): String {
            val raw = context.getString(com.saaf.milkshop.R.string.app_name)
                .replace(Regex("[^A-Za-z0-9_-]+"), "_")
                .trim('_')
                .ifEmpty { "MilkShop" }
            return "${raw}_${now.format(fileFormatter)}$BACKUP_EXTENSION"
        }
    }

    data class BackupMeta(
        val createdAt: String,
        val appVersion: String,
        val schemaVersion: Int,
        val formatVersion: Int,
        val fileName: String,
    )

    data class EncryptedBackupInspection(
        val meta: BackupMeta,
        val containsSecurityState: Boolean,
    )

    data class RestoreSelection(
        val business: Boolean,
        val planning: Boolean,
        val preferences: Boolean,
        val security: Boolean = false,
    )

    data class BackupFailure(
        val reason: String,
        val recovery: String,
    ) : Exception(reason)

    suspend fun createLocalSnapshot(database: MilkShopDatabase): Result<BackupMeta> =
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = buildBackupBytes(database, includeSecurity = true, formatVersion = FORMAT_VERSION)
                val bytes = try {
                    encryptLocalFormat2(payload)
                } finally {
                    payload.fill(0)
                }
                val dir = java.io.File(context.filesDir, BACKUP_DIR).apply { mkdirs() }
                val destination = uniqueSnapshotDestination(dir, displayName(context))
                val fileName = destination.name
                val tmp = java.io.File(dir, "$fileName.tmp")
                tmp.writeBytes(bytes)
                val verified = readLocalAndVerify(tmp.readBytes())
                check(tmp.renameTo(destination)) { "Could not finalize backup file" }
                backupMeta(verified, destination.name)
            }.recoverCatching { error ->
                throw BackupFailure(
                    reason = error.message ?: error.javaClass.simpleName,
                    recovery = "The previous valid local backup was kept unchanged. The failed snapshot was not marked usable.",
                )
            }
        }

    private fun uniqueSnapshotDestination(dir: java.io.File, preferredName: String): java.io.File {
        val preferred = java.io.File(dir, preferredName)
        if (!preferred.exists()) return preferred
        val stem = preferredName.removeSuffix(BACKUP_EXTENSION)
        var suffix = 1
        while (true) {
            val candidate = java.io.File(dir, "${stem}_$suffix$BACKUP_EXTENSION")
            if (!candidate.exists()) return candidate
            suffix += 1
        }
    }

    suspend fun exportSnapshot(database: MilkShopDatabase, uri: Uri): Result<BackupMeta> =
        withContext(Dispatchers.IO) {
            runCatching {
                val bytes = buildBackupBytes(database)
                context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                    ?: throw IOException("The selected backup destination could not be opened")
                val verified = readAndVerify(bytes)
                BackupMeta(
                    createdAt = verified.getJSONObject("metadata").getString("created_at"),
                    appVersion = verified.getJSONObject("metadata").getString("app_version"),
                    schemaVersion = verified.getJSONObject("metadata").getInt("schema_version"),
                    formatVersion = verified.getJSONObject("metadata").getInt("format_version"),
                    fileName = uri.lastPathSegment ?: "backup",
                )
            }
        }

    /**
     * Creates the portable format-2 backup. The complete payload, including
     * user credential and recovery verifiers, is inside an authenticated
     * AES-256-GCM envelope derived from the owner's backup passphrase.
     */
    suspend fun exportEncryptedSnapshot(
        database: MilkShopDatabase,
        uri: Uri,
        passphrase: CharArray,
    ): Result<BackupMeta> = withContext(Dispatchers.IO) {
        runCatching {
            AppPasswordPolicy.validateBackupPassphrase(passphrase)
            val payload = buildBackupBytes(database, includeSecurity = true, formatVersion = FORMAT_VERSION)
            val encrypted = try {
                encryptFormat2(payload, passphrase)
            } finally {
                payload.fill(0)
            }
            context.contentResolver.openOutputStream(uri)?.use { it.write(encrypted) }
                ?: throw IOException("The selected backup destination could not be opened")
            val verified = readEncryptedAndVerify(encrypted, passphrase)
            backupMeta(verified, uri.lastPathSegment ?: "backup")
        }
    }

    suspend fun readEncryptedBackupMeta(uri: Uri, passphrase: CharArray): Result<BackupMeta> =
        withContext(Dispatchers.IO) {
            runCatching {
                val json = readEncryptedAndVerify(readUri(uri), passphrase)
                val metadata = json.getJSONObject("metadata")
                validateCompatibility(metadata)
                backupMeta(json, uri.lastPathSegment ?: "backup")
            }
        }

    suspend fun inspectEncryptedSnapshot(uri: Uri, passphrase: CharArray): Result<EncryptedBackupInspection> =
        withContext(Dispatchers.IO) {
            runCatching {
                val json = readEncryptedAndVerify(readUri(uri), passphrase)
                val metadata = json.getJSONObject("metadata")
                validateCompatibility(metadata)
                EncryptedBackupInspection(
                    meta = backupMeta(json, uri.lastPathSegment ?: "backup"),
                    containsSecurityState = json.getJSONObject("data").optJSONArray("app_users")?.length()?.let { it > 0 } == true,
                )
            }
        }


    fun localBackups(): List<Pair<java.io.File, BackupMeta>> {
        val dir = java.io.File(context.filesDir, BACKUP_DIR)
        return dir.listFiles { file -> file.isFile && file.name.endsWith(BACKUP_EXTENSION) }
            ?.sortedByDescending { it.lastModified() }
            ?.mapNotNull { file ->
                runCatching {
                    val json = readLegacyOrLocalAndVerify(file.readBytes())
                    val metadata = json.getJSONObject("metadata")
                    validateCompatibility(metadata)
                    file to BackupMeta(
                        createdAt = metadata.getString("created_at"),
                        appVersion = metadata.getString("app_version"),
                        schemaVersion = metadata.getInt("schema_version"),
                        formatVersion = metadata.getInt("format_version"),
                        fileName = file.name,
                    )
                }.getOrNull()
            }
            ?: emptyList()
    }

    suspend fun readBackupMeta(uri: Uri): Result<BackupMeta> = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = readUri(uri)
            val json = readLegacyOrLocalAndVerify(bytes)
            val metadata = json.getJSONObject("metadata")
            validateCompatibility(metadata)
            BackupMeta(
                createdAt = metadata.getString("created_at"),
                appVersion = metadata.getString("app_version"),
                schemaVersion = metadata.getInt("schema_version"),
                formatVersion = metadata.getInt("format_version"),
                fileName = uri.lastPathSegment ?: "backup",
            )
        }
    }

    suspend fun restore(
        database: MilkShopDatabase,
        uri: Uri,
        selection: RestoreSelection,
    ): Result<BackupMeta> = withContext(Dispatchers.IO) {
        runCatching {
            require(!selection.security) { "Security identities can only be restored from an encrypted format-2 backup" }
            val bytes = readUri(uri)
            val json = readAndVerify(bytes)
            val metadata = json.getJSONObject("metadata")
            validateCompatibility(metadata)
            restoreVerifiedJson(database, json, selection, includeSecurity = false)
            backupMeta(json, uri.lastPathSegment ?: "backup")
        }.recoverCatching { error ->
            throw BackupFailure(
                reason = error.message ?: error.javaClass.simpleName,
                recovery = "The restore was not completed. The pre-restore safety backup remains available for recovery.",
            )
        }
    }

    suspend fun restoreEncrypted(
        database: MilkShopDatabase,
        uri: Uri,
        selection: RestoreSelection,
        passphrase: CharArray,
        sessions: NativeSessionManager,
    ): Result<BackupMeta> = restoreEncryptedInternal(database, uri, selection, passphrase, sessions, requireEmptyInstallation = true)

    /**
     * Authenticated hybrid replacement flow. The bridge verifies a current owner,
     * recent full authentication, the selected document, and a one-use ticket
     * before calling this method. The pre-setup restore path remains empty-only.
     */
    suspend fun restoreEncryptedReplacing(
        database: MilkShopDatabase,
        uri: Uri,
        selection: RestoreSelection,
        passphrase: CharArray,
        sessions: NativeSessionManager,
    ): Result<BackupMeta> = restoreEncryptedInternal(database, uri, selection, passphrase, sessions, requireEmptyInstallation = false)

    private suspend fun restoreEncryptedInternal(
        database: MilkShopDatabase,
        uri: Uri,
        selection: RestoreSelection,
        passphrase: CharArray,
        sessions: NativeSessionManager,
        requireEmptyInstallation: Boolean,
    ): Result<BackupMeta> = withContext(Dispatchers.IO) {
        runCatching {
            require(selection.business && selection.security) {
                "Encrypted security restore requires both business and security data"
            }
            require(!requireEmptyInstallation || database.appUserDao().count() == 0L) {
                "Security restore is only allowed before user setup on this installation"
            }
            val json = readEncryptedAndVerify(readUri(uri), passphrase)
            val metadata = json.getJSONObject("metadata")
            validateCompatibility(metadata)
            require(metadata.getInt("format_version") == FORMAT_VERSION) { "Encrypted backup format is invalid" }
            restoreVerifiedJson(database, json, selection, includeSecurity = true)
            sessions.invalidateAll()
            backupMeta(json, uri.lastPathSegment ?: "backup")
        }.recoverCatching { error ->
            throw BackupFailure(
                reason = error.message ?: error.javaClass.simpleName,
                recovery = "The restore was not completed. The pre-restore safety backup remains available for recovery.",
            )
        }
    }

    /** Restores an app-private snapshot on the same installation that owns its Keystore key. */
    suspend fun restoreLocalSnapshot(
        database: MilkShopDatabase,
        uri: Uri,
        selection: RestoreSelection,
        sessions: NativeSessionManager,
    ): Result<BackupMeta> = withContext(Dispatchers.IO) {
        runCatching {
            val json = readLocalAndVerify(readUri(uri))
            val metadata = json.getJSONObject("metadata")
            validateCompatibility(metadata)
            val hasUsers = json.getJSONObject("data").getJSONArray("app_users").length() > 0
            if (hasUsers) {
                require(selection.business && selection.security) {
                    "A security-bearing local snapshot must restore business and security together"
                }
                require(database.appUserDao().count() == 0L) {
                    "Security restore is only allowed before user setup on this installation"
                }
            } else {
                require(!selection.security) { "This pre-user snapshot contains no security identities" }
            }
            restoreVerifiedJson(database, json, selection, includeSecurity = hasUsers)
            sessions.invalidateAll()
            backupMeta(json, uri.lastPathSegment ?: "backup")
        }.recoverCatching { error ->
            throw BackupFailure(
                reason = error.message ?: error.javaClass.simpleName,
                recovery = "The local restore was not completed. The pre-restore safety backup remains available.",
            )
        }
    }

    private suspend fun restoreVerifiedJson(
        database: MilkShopDatabase,
        json: JSONObject,
        selection: RestoreSelection,
        includeSecurity: Boolean,
    ) {
        // Safety snapshot is created before touching current state.
        createLocalSnapshot(database).getOrThrow()

        database.withWriteTransaction {
            val data = json.getJSONObject("data")
            val backupDao = database.backupDataDao()
            if (selection.business) {
                Phase3CostingMigrationSql.immutableTriggerNames.forEach { executeSQL("DROP TRIGGER IF EXISTS $it") }
                if (includeSecurity) {
                    executeSQL("DROP TRIGGER IF EXISTS prevent_app_user_delete")
                    executeSQL("DROP TRIGGER IF EXISTS prevent_security_audit_delete")
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
                Phase3CostingMigrationSql.immutableTriggers.forEach { executeSQL(it) }
                if (includeSecurity) {
                    Phase3SecurityMigrationSql.triggerNames.zip(Phase3SecurityMigrationSql.createTriggers)
                        .filter { (name, _) -> name in setOf("prevent_app_user_delete", "prevent_security_audit_delete") }
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
            readAndVerify(
                buildBackupBytes(
                    database,
                    includeSecurity = includeSecurity,
                    formatVersion = if (includeSecurity) FORMAT_VERSION else LEGACY_FORMAT_VERSION,
                )
            )
        }
    }

    private suspend fun buildBackupBytes(
        database: MilkShopDatabase,
        includeSecurity: Boolean = false,
        formatVersion: Int = LEGACY_FORMAT_VERSION,
    ): ByteArray {
        val dao = database.backupDataDao()
        val costingPolicies = dao.costingPolicyVersions()
        val costingComponents = dao.costingPolicyComponents()
        val costBases = dao.inventoryCostBasisEntries()
        val yieldPolicies = dao.yieldPolicyVersions()
        val lossAssessments = dao.productionLossAssessments()
        require(includeSecurity || (
            costingPolicies.isEmpty() && costingComponents.isEmpty() && costBases.isEmpty() &&
                yieldPolicies.isEmpty() && lossAssessments.isEmpty()
            )) {
            "Costing policy and opening-cost history require an encrypted backup"
        }
        val root = JSONObject()
        val now = LocalDateTime.now()
        root.put("metadata", JSONObject().apply {
            put("format_version", formatVersion)
            put("schema_version", CURRENT_SCHEMA_VERSION)
            put("app_version", BuildConfig.VERSION_NAME)
            put("created_at", now.toString())
            put("file_name", displayName(context, now))
        })
        root.put("data", JSONObject().apply {
            put("products", JSONArray(dao.products().map(::productJson)))
            put("accounts", JSONArray(dao.accounts().map(::accountJson)))
            put("events", JSONArray(dao.events().map(::eventJson)))
            put("inventory_logs", JSONArray(dao.inventoryLogs().map(::inventoryJson)))
            put("financial_logs", JSONArray(dao.financialLogs().map(::financialJson)))
            put("settings", JSONArray(dao.settings().map(::settingJson)))
            put("budgets", JSONArray(dao.budgets().map(::budgetJson)))
            put("collections_targets", JSONArray(dao.collectionsTargets().map(::collectionJson)))
            put("product_profiles", JSONArray(dao.productProfiles().map(::productProfileJson)))
            put("customer_profiles", JSONArray(dao.customerProfiles().map(::customerProfileJson)))
            put("vendor_profiles", JSONArray(dao.vendorProfiles().map(::vendorProfileJson)))
            put("employee_profiles", JSONArray(dao.employeeProfiles().map(::employeeProfileJson)))
            put("attendance_records", JSONArray(dao.attendanceRecords().map(::attendanceJson)))
            put("deliveries", JSONArray(dao.deliveries().map(::deliveryJson)))
            put("delivery_status_history", JSONArray(dao.deliveryStatuses().map(::deliveryStatusJson)))
            put("spoilage_details", JSONArray(dao.spoilageDetails().map(::spoilageDetailJson)))
            put("physical_cash_counts", JSONArray(dao.physicalCashCounts().map(::physicalCashCountJson)))
            put("expense_category_profiles", JSONArray(dao.expenseCategoryProfiles().map(::expenseCategoryProfileJson)))
            put("store_profile", JSONArray(dao.storeProfiles().map(::storeProfileJson)))
            put("opening_batches", JSONArray(dao.openingBatches().map(::openingBatchJson)))
            put("opening_inventory_entries", JSONArray(dao.openingInventoryEntries().map(::openingInventoryEntryJson)))
            put("opening_account_entries", JSONArray(dao.openingAccountEntries().map(::openingAccountEntryJson)))
            put("daily_close_actions", JSONArray(dao.dailyCloseActions().map { dailyCloseActionJson(it, includeSecurity) }))
            put("preferences", preferencesJson())
            if (includeSecurity) {
                put("app_users", JSONArray(dao.appUsers().map(::appUserJson)))
                put("user_credentials", JSONArray(dao.userCredentials().map(::userCredentialJson)))
                put("saved_recovery_codes", JSONArray(dao.savedRecoveryCodes().map(::savedRecoveryCodeJson)))
                put("security_audit_entries", JSONArray(dao.securityAuditEntries().map(::securityAuditEntryJson)))
                put("event_actor_links", JSONArray(dao.eventActorLinks().map(::eventActorLinkJson)))
                put("costing_policy_versions", JSONArray(costingPolicies.map(::costingPolicyVersionJson)))
                put("costing_policy_components", JSONArray(costingComponents.map(::costingPolicyComponentJson)))
                put("inventory_cost_basis_entries", JSONArray(costBases.map(::inventoryCostBasisJson)))
                put("yield_policy_versions", JSONArray(yieldPolicies.map(::yieldPolicyVersionJson)))
                put("production_loss_assessments", JSONArray(lossAssessments.map(::productionLossAssessmentJson)))
            }
        })
        val unsigned = root.toString()
        root.put("integrity_sha256", sha256(unsigned.toByteArray(Charsets.UTF_8)))
        return root.toString(2).toByteArray(Charsets.UTF_8)
    }

    private fun readAndVerify(bytes: ByteArray): JSONObject {
        val root = JSONObject(bytes.toString(Charsets.UTF_8))
        val actual = root.getString("integrity_sha256")
        root.remove("integrity_sha256")
        val expected = sha256(root.toString().toByteArray(Charsets.UTF_8))
        check(actual == expected) { "Backup integrity check failed: checksum mismatch" }
        root.put("integrity_sha256", actual)
        return root
    }

    private fun encryptFormat2(payload: ByteArray, passphrase: CharArray): ByteArray {
        val salt = ByteArray(Argon2idPasswordHasher.SALT_BYTES).also(SecureRandom()::nextBytes)
        val nonce = ByteArray(BACKUP_NONCE_BYTES).also(SecureRandom()::nextBytes)
        val key = deriveBackupKey(
            passphrase,
            salt,
            Argon2idPasswordHasher.MEMORY_KIB,
            Argon2idPasswordHasher.ITERATIONS,
            Argon2idPasswordHasher.PARALLELISM,
        )
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(BACKUP_TAG_BITS, nonce))
            cipher.updateAAD(BACKUP_AAD.toByteArray(Charsets.UTF_8))
            val ciphertext = cipher.doFinal(payload)
            JSONObject().apply {
                put("format_version", FORMAT_VERSION)
                put("kdf", JSONObject().apply {
                    put("algorithm", BACKUP_KDF_ALGORITHM)
                    put("memory_kib", Argon2idPasswordHasher.MEMORY_KIB)
                    put("iterations", Argon2idPasswordHasher.ITERATIONS)
                    put("parallelism", Argon2idPasswordHasher.PARALLELISM)
                    put("salt", Base64.getEncoder().encodeToString(salt))
                })
                put("cipher", JSONObject().apply {
                    put("algorithm", BACKUP_CIPHER_ALGORITHM)
                    put("nonce", Base64.getEncoder().encodeToString(nonce))
                    put("ciphertext", Base64.getEncoder().encodeToString(ciphertext))
                })
            }.toString(2).toByteArray(Charsets.UTF_8)
        } finally {
            key.fill(0)
        }
    }

    private fun encryptLocalFormat2(payload: ByteArray): ByteArray {
        val key = localBackupKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        cipher.updateAAD(LOCAL_BACKUP_AAD.toByteArray(Charsets.UTF_8))
        val ciphertext = cipher.doFinal(payload)
        return JSONObject().apply {
            put("format_version", FORMAT_VERSION)
            put("protection", LOCAL_BACKUP_PROTECTION)
            put("cipher", JSONObject().apply {
                put("algorithm", BACKUP_CIPHER_ALGORITHM)
                put("nonce", Base64.getEncoder().encodeToString(cipher.iv))
                put("ciphertext", Base64.getEncoder().encodeToString(ciphertext))
            })
        }.toString(2).toByteArray(Charsets.UTF_8)
    }

    private fun readLegacyOrLocalAndVerify(bytes: ByteArray): JSONObject {
        val outer = JSONObject(bytes.toString(Charsets.UTF_8))
        return if (outer.optString("protection") == LOCAL_BACKUP_PROTECTION) {
            readLocalAndVerify(bytes)
        } else {
            readAndVerify(bytes)
        }
    }

    private fun readLocalAndVerify(bytes: ByteArray): JSONObject {
        val envelope = JSONObject(bytes.toString(Charsets.UTF_8))
        require(envelope.getInt("format_version") == FORMAT_VERSION) { "Local backup format is invalid" }
        require(envelope.getString("protection") == LOCAL_BACKUP_PROTECTION) { "Backup is not a device-local snapshot" }
        val cipherJson = envelope.getJSONObject("cipher")
        require(cipherJson.getString("algorithm") == BACKUP_CIPHER_ALGORITHM) { "Unsupported local backup encryption" }
        val nonce = Base64.getDecoder().decode(cipherJson.getString("nonce"))
        val ciphertext = Base64.getDecoder().decode(cipherJson.getString("ciphertext"))
        require(nonce.size == BACKUP_NONCE_BYTES) { "Invalid local backup nonce" }
        require(ciphertext.size > BACKUP_TAG_BITS / 8) { "Encrypted local backup payload is empty" }
        val plaintext = try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, localBackupKey(), GCMParameterSpec(BACKUP_TAG_BITS, nonce))
            cipher.updateAAD(LOCAL_BACKUP_AAD.toByteArray(Charsets.UTF_8))
            cipher.doFinal(ciphertext)
        } catch (_: AEADBadTagException) {
            throw IllegalArgumentException("The local backup was altered or its device key is unavailable")
        }
        return try {
            readAndVerify(plaintext)
        } finally {
            plaintext.fill(0)
        }
    }

    private fun localBackupKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(LOCAL_BACKUP_KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                LOCAL_BACKUP_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(BACKUP_KEY_BYTES * 8)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private fun readEncryptedAndVerify(bytes: ByteArray, passphrase: CharArray): JSONObject {
        val envelope = JSONObject(bytes.toString(Charsets.UTF_8))
        require(envelope.getInt("format_version") == FORMAT_VERSION) { "Encrypted backup format is invalid" }
        val kdf = envelope.getJSONObject("kdf")
        val cipherJson = envelope.getJSONObject("cipher")
        require(kdf.getString("algorithm") == BACKUP_KDF_ALGORITHM) { "Unsupported backup key derivation" }
        require(cipherJson.getString("algorithm") == BACKUP_CIPHER_ALGORITHM) { "Unsupported backup encryption" }
        val memoryKib = kdf.getInt("memory_kib")
        val iterations = kdf.getInt("iterations")
        val parallelism = kdf.getInt("parallelism")
        require(memoryKib in Argon2idPasswordHasher.MEMORY_KIB..262_144) { "Invalid backup memory cost" }
        require(iterations in Argon2idPasswordHasher.ITERATIONS..10) { "Invalid backup iteration count" }
        require(parallelism in Argon2idPasswordHasher.PARALLELISM..4) { "Invalid backup parallelism" }
        val salt = Base64.getDecoder().decode(kdf.getString("salt"))
        val nonce = Base64.getDecoder().decode(cipherJson.getString("nonce"))
        val ciphertext = Base64.getDecoder().decode(cipherJson.getString("ciphertext"))
        require(salt.size >= Argon2idPasswordHasher.SALT_BYTES) { "Invalid backup salt" }
        require(nonce.size == BACKUP_NONCE_BYTES) { "Invalid backup nonce" }
        require(ciphertext.size > BACKUP_TAG_BITS / 8) { "Encrypted backup payload is empty" }
        val key = deriveBackupKey(passphrase, salt, memoryKib, iterations, parallelism)
        val plaintext = try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(BACKUP_TAG_BITS, nonce))
            cipher.updateAAD(BACKUP_AAD.toByteArray(Charsets.UTF_8))
            cipher.doFinal(ciphertext)
        } catch (_: AEADBadTagException) {
            throw IllegalArgumentException("The backup passphrase is incorrect or the file was altered")
        } finally {
            key.fill(0)
        }
        return try {
            readAndVerify(plaintext)
        } finally {
            plaintext.fill(0)
        }
    }

    private fun deriveBackupKey(
        passphrase: CharArray,
        salt: ByteArray,
        memoryKib: Int,
        iterations: Int,
        parallelism: Int,
    ): ByteArray {
        val normalized = Normalizer.normalize(String(passphrase), Normalizer.Form.NFC).toCharArray()
        return try {
            val parameters = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withSalt(salt)
                .withMemoryAsKB(memoryKib)
                .withIterations(iterations)
                .withParallelism(parallelism)
                .build()
            ByteArray(BACKUP_KEY_BYTES).also { output ->
                Argon2BytesGenerator().apply { init(parameters) }.generateBytes(normalized, output)
            }
        } finally {
            normalized.fill('\u0000')
        }
    }

    private fun backupMeta(json: JSONObject, fileName: String): BackupMeta {
        val metadata = json.getJSONObject("metadata")
        return BackupMeta(
            createdAt = metadata.getString("created_at"),
            appVersion = metadata.getString("app_version"),
            schemaVersion = metadata.getInt("schema_version"),
            formatVersion = metadata.getInt("format_version"),
            fileName = fileName,
        )
    }

    private fun validateCompatibility(metadata: JSONObject) {
        val format = metadata.getInt("format_version")
        val schema = metadata.getInt("schema_version")
        require(format <= FORMAT_VERSION) {
            "This backup uses a newer backup format ($format). Update the app before restoring it."
        }
        require(schema <= CURRENT_SCHEMA_VERSION) {
            "This backup was created with a newer database schema ($schema). Update the app before restoring it."
        }
    }

    private fun readUri(uri: Uri): ByteArray {
        if (uri.scheme == "file") {
            return java.io.File(uri.path ?: throw IOException("The backup file path is missing")).readBytes()
        }
        return context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IOException("The backup file could not be opened")
    }

    /**
     * Daily-close integrity requires every historical close to reference the
     * latest count visible at that point in its audit chain. Replay the stored
     * chronology instead of bulk-inserting all counts ahead of all actions.
     */
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

    private fun preferencesJson(): JSONObject {
        val prefs = context.getSharedPreferences("milk_shop_preferences", Context.MODE_PRIVATE)
        val out = JSONObject()
        prefs.all.forEach { (key, value) ->
            when (value) {
                is String -> out.put(key, value)
                is Boolean -> out.put(key, value)
                is Int -> out.put(key, value)
                is Long -> out.put(key, value)
                is Float -> out.put(key, value.toDouble())
                is Double -> out.put(key, value)
            }
        }
        return out
    }

    private fun restorePreferences(json: JSONObject?) {
        val editor = context.getSharedPreferences("milk_shop_preferences", Context.MODE_PRIVATE).edit().clear()
        if (json != null) {
            for (key in json.keys()) {
                when (val value = json.get(key)) {
                    is Boolean -> editor.putBoolean(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Number -> editor.putFloat(key, value.toFloat())
                    JSONObject.NULL -> Unit
                    else -> editor.putString(key, value.toString())
                }
            }
        }
        editor.apply()
    }

    private fun productJson(v: ProductEntity) = JSONObject().apply {
        put("product_id", v.productId); put("name", v.name); put("unit", v.unit)
    }
    private fun accountJson(v: AccountEntity) = JSONObject().apply {
        put("account_id", v.accountId); put("name", v.name); put("type", v.type); put("category", v.category)
    }
    private fun eventJson(v: EventEntity) = JSONObject().apply {
        put("event_id", v.eventId); put("created_at", v.createdAt); put("occurred_at", v.occurredAt); put("event_type", v.eventType)
        putNullable("channel", v.channel); putNullable("corrects_event_id", v.correctsEventId); putNullable("raw_liters", v.rawLiters)
        putNullable("fat_percentage", v.fatPercentage); putNullable("lr_reading", v.lrReading); putNullable("snf_percentage", v.snfPercentage)
        putNullable("ts_percentage", v.tsPercentage); putNullable("adjusted_liters", v.adjustedLiters); putNullable("amount_owed", v.amountOwed)
        putNullable("milk_liters_used", v.milkLitersUsed); putNullable("yogurt_kg_produced", v.yogurtKgProduced); putNullable("amount", v.amount)
        putNullable("milk_rate_used", v.milkRateUsed); putNullable("delivery_fee_used", v.deliveryFeeUsed); putNullable("ts_standard_used", v.tsStandardUsed)
        putNullable("sale_quantity", v.saleQuantity); putNullable("sale_value", v.saleValue); putNullable("unit_rate", v.unitRate)
        put("surcharge_applied", v.surchargeApplied); putNullable("notes", v.notes)
    }
    private fun inventoryJson(v: InventoryLogEntity) = JSONObject().apply {
        put("log_id", v.logId); put("event_id", v.eventId); put("product_id", v.productId); put("quantity", v.quantity)
    }
    private fun financialJson(v: FinancialLogEntity) = JSONObject().apply {
        put("log_id", v.logId); put("event_id", v.eventId); put("account_id", v.accountId); put("amount", v.amount)
    }
    private fun settingJson(v: SettingEntity) = JSONObject().apply {
        put("setting_key", v.key); put("setting_value", v.value); put("updated_at", v.updatedAt)
    }
    private fun budgetJson(v: BudgetPlanEntity) = JSONObject().apply {
        put("budget_id", v.budgetId); put("category", v.category); put("period_start", v.periodStart); put("period_end_exclusive", v.periodEndExclusive)
        putNullable("planned_liters", v.plannedLiters); putNullable("planned_amount", v.plannedAmount); putNullable("vendor_rate_snapshot", v.vendorRateSnapshot)
        putNullable("retail_rate_snapshot", v.retailRateSnapshot); put("created_at", v.createdAt); putNullable("supersedes_id", v.supersedesId); put("archived", v.archived)
    }
    private fun collectionJson(v: CollectionsTargetEntity) = JSONObject().apply {
        put("target_id", v.targetId); put("week_start", v.weekStart); put("week_end_exclusive", v.weekEndExclusive); put("target_amount", v.targetAmount)
        put("created_at", v.createdAt); putNullable("supersedes_id", v.supersedesId); put("archived", v.archived)
    }
    private fun productProfileJson(v: ProductProfileEntity) = JSONObject().apply {
        put("product_id", v.productId); putNullable("default_sale_rate", v.defaultSaleRate); put("inventory_tracking", v.inventoryTracking)
        put("archived", v.archived); putNullable("notes", v.notes); put("created_at", v.createdAt)
    }
    private fun customerProfileJson(v: CustomerProfileEntity) = JSONObject().apply {
        put("account_id", v.accountId); putNullable("phone", v.phone); putNullable("area_route", v.areaRoute); putNullable("address", v.address)
        putNullable("notes", v.notes); putNullable("credit_limit", v.creditLimit); putNullable("daily_quota", v.dailyQuota); put("active", v.active); put("created_at", v.createdAt)
    }
    private fun vendorProfileJson(v: VendorProfileEntity) = JSONObject().apply {
        put("account_id", v.accountId); putNullable("phone", v.phone); putNullable("location", v.location); putNullable("notes", v.notes); put("active", v.active); put("created_at", v.createdAt)
    }
    private fun employeeProfileJson(v: EmployeeProfileEntity) = JSONObject().apply {
        put("account_id", v.accountId); putNullable("role", v.role); putNullable("phone", v.phone); putNullable("reference_salary", v.referenceSalary)
        putNullable("salary_period", v.salaryPeriod); putNullable("join_date", v.joinDate); put("active", v.active); put("created_at", v.createdAt)
    }
    private fun attendanceJson(v: AttendanceRecordEntity) = JSONObject().apply {
        put("attendance_id", v.attendanceId); put("employee_account_id", v.employeeAccountId); put("business_date", v.businessDate); put("status", v.status)
        putNullable("notes", v.notes); put("created_at", v.createdAt); putNullable("supersedes_id", v.supersedesId); put("archived", v.archived)
    }
    private fun deliveryJson(v: DeliveryEntity) = JSONObject().apply {
        put("delivery_id", v.deliveryId); put("customer_account_id", v.customerAccountId); put("product_id", v.productId); put("quantity", v.quantity)
        putNullable("route", v.route); put("business_date", v.businessDate); putNullable("sale_event_id", v.saleEventId); putNullable("notes", v.notes)
        put("created_at", v.createdAt); put("archived", v.archived)
    }
    private fun deliveryStatusJson(v: DeliveryStatusEntity) = JSONObject().apply {
        put("status_id", v.statusId); put("delivery_id", v.deliveryId); put("status", v.status); put("occurred_at", v.occurredAt); putNullable("notes", v.notes)
    }
    private fun spoilageDetailJson(v: SpoilageDetailEntity) = JSONObject().apply {
        put("event_id", v.eventId); put("product_id", v.productId); put("quantity", v.quantity); put("reason", v.reason)
    }
    private fun physicalCashCountJson(v: PhysicalCashCountEntity) = JSONObject().apply {
        put("count_id", v.countId); put("account_id", v.accountId); put("business_date", v.businessDate); put("counted_amount", v.countedAmount)
        put("counted_at", v.countedAt); putNullable("notes", v.notes)
    }
    private fun expenseCategoryProfileJson(v: ExpenseCategoryProfileEntity) = JSONObject().apply {
        put("account_id", v.accountId); put("display_name", v.displayName); put("active", v.active); put("created_at", v.createdAt)
    }
    private fun storeProfileJson(v: StoreProfileEntity) = JSONObject().apply {
        put("store_profile_id", v.storeProfileId); put("store_name", v.storeName); put("currency_code", v.currencyCode)
        putNullable("phone", v.phone); putNullable("address", v.address); put("updated_at", v.updatedAt)
    }
    private fun openingBatchJson(v: OpeningBatchEntity) = JSONObject().apply {
        put("event_id", v.eventId)
    }
    private fun openingInventoryEntryJson(v: OpeningInventoryEntryEntity) = JSONObject().apply {
        put("event_id", v.eventId); put("product_id", v.productId); put("quantity", v.quantity)
    }
    private fun openingAccountEntryJson(v: OpeningAccountEntryEntity) = JSONObject().apply {
        put("event_id", v.eventId); put("account_id", v.accountId); put("balance", v.balance)
    }
    private fun dailyCloseActionJson(v: DailyCloseActionEntity, includeSecurity: Boolean) = JSONObject().apply {
        put("action_id", v.actionId); put("business_date", v.businessDate); put("action", v.action)
        putNullable("physical_cash_count_id", v.physicalCashCountId); put("actor_name", v.actorName); put("actor_role", v.actorRole)
        if (includeSecurity) putNullable("actor_user_id", v.actorUserId)
        putNullable("reason", v.reason); put("created_at", v.createdAt); putNullable("supersedes_action_id", v.supersedesActionId)
    }
    private fun appUserJson(v: AppUserEntity) = JSONObject().apply {
        put("user_id", v.userId); put("login_name", v.loginName); put("display_name", v.displayName); put("role", v.role)
        putNullable("employee_account_id", v.employeeAccountId); put("status", v.status); put("credential_setup_required", v.credentialSetupRequired)
        put("created_at", v.createdAt); putNullable("created_by_user_id", v.createdByUserId); put("updated_at", v.updatedAt)
        putNullable("disabled_at", v.disabledAt); putNullable("disabled_by_user_id", v.disabledByUserId)
    }
    private fun userCredentialJson(v: UserCredentialEntity) = JSONObject().apply {
        put("user_id", v.userId); put("algorithm", v.algorithm); put("salt", Base64.getEncoder().encodeToString(v.salt))
        put("verifier", Base64.getEncoder().encodeToString(v.verifier)); put("memory_kib", v.memoryKib); put("iterations", v.iterations)
        put("parallelism", v.parallelism); put("changed_at", v.changedAt); put("must_change", v.mustChange)
    }
    private fun savedRecoveryCodeJson(v: SavedRecoveryCodeEntity) = JSONObject().apply {
        put("recovery_code_id", v.recoveryCodeId); put("user_id", v.userId); put("salt", Base64.getEncoder().encodeToString(v.salt))
        put("verifier", Base64.getEncoder().encodeToString(v.verifier)); put("created_at", v.createdAt); putNullable("used_at", v.usedAt)
    }
    private fun securityAuditEntryJson(v: SecurityAuditEntryEntity) = JSONObject().apply {
        put("audit_id", v.auditId); put("occurred_at", v.occurredAt); put("action", v.action); put("result", v.result)
        putNullable("reason_code", v.reasonCode); putNullable("actor_user_id", v.actorUserId); putNullable("actor_name_snapshot", v.actorNameSnapshot)
        putNullable("actor_role_snapshot", v.actorRoleSnapshot); putNullable("target_user_id", v.targetUserId); putNullable("details", v.details)
    }
    private fun eventActorLinkJson(v: EventActorLinkEntity) = JSONObject().apply {
        put("event_id", v.eventId); put("actor_user_id", v.actorUserId); put("actor_name_snapshot", v.actorNameSnapshot)
        put("actor_role_snapshot", v.actorRoleSnapshot); put("attributed_at", v.attributedAt)
    }
    private fun costingPolicyVersionJson(v: CostingPolicyVersionEntity) = JSONObject().apply {
        put("policy_id", v.policyId); put("calendar_month", v.calendarMonth); put("normal_capacity_kg", v.normalCapacityKg)
        put("review_reason", v.reviewReason); putNullable("notes", v.notes); put("approved_at", v.approvedAt)
        put("approved_by_user_id", v.approvedByUserId); putNullable("supersedes_policy_id", v.supersedesPolicyId)
    }
    private fun costingPolicyComponentJson(v: CostingPolicyComponentEntity) = JSONObject().apply {
        put("policy_id", v.policyId); put("component_code", v.componentCode); put("source_amount_paisa", v.sourceAmountPaisa)
        put("basis_quantity_kg", v.basisQuantityKg)
    }
    private fun inventoryCostBasisJson(v: InventoryCostBasisEntity) = JSONObject().apply {
        put("basis_id", v.basisId); put("opening_event_id", v.openingEventId); put("product_id", v.productId)
        put("quantity", v.quantity); put("total_cost_paisa", v.totalCostPaisa); put("basis_kind", v.basisKind)
        putNullable("evidence_notes", v.evidenceNotes); put("created_at", v.createdAt); put("created_by_user_id", v.createdByUserId)
        putNullable("supersedes_basis_id", v.supersedesBasisId)
    }
    private fun yieldPolicyVersionJson(v: YieldPolicyVersionEntity) = JSONObject().apply {
        put("yield_policy_id", v.yieldPolicyId); put("sample_start_date", v.sampleStartDate); put("sample_end_date", v.sampleEndDate)
        put("accepted_batch_count", v.acceptedBatchCount); put("median_yield_ratio", v.medianYieldRatio); put("mad_yield_ratio", v.madYieldRatio)
        put("lower_alert_limit", v.lowerAlertLimit); put("upper_alert_limit", v.upperAlertLimit); put("effective_from", v.effectiveFrom)
        put("approved_at", v.approvedAt); put("approved_by_user_id", v.approvedByUserId)
        putNullable("supersedes_yield_policy_id", v.supersedesYieldPolicyId)
    }
    private fun productionLossAssessmentJson(v: ProductionLossAssessmentEntity) = JSONObject().apply {
        put("assessment_id", v.assessmentId); put("production_event_id", v.productionEventId); put("classification", v.classification)
        put("abnormal_milk_liters", v.abnormalMilkLiters); put("reason_code", v.reasonCode); putNullable("notes", v.notes)
        put("assessed_at", v.assessedAt); put("assessed_by_user_id", v.assessedByUserId)
        putNullable("supersedes_assessment_id", v.supersedesAssessmentId)
    }

    private fun JSONObject.putNullable(key: String, value: Any?) { put(key, value ?: JSONObject.NULL) }

    private fun readProducts(a: JSONArray?): List<ProductEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let { ProductEntity(it.getLong("product_id"), it.getString("name"), it.getString("unit")) } }
    private fun readAccounts(a: JSONArray?): List<AccountEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let { AccountEntity(it.getLong("account_id"), it.getString("name"), it.getString("type"), it.getString("category")) } }
    private fun readEvents(a: JSONArray?): List<EventEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let { EventEntity(
        eventId = it.getLong("event_id"), createdAt = it.getString("created_at"), occurredAt = it.getString("occurred_at"), eventType = it.getString("event_type"),
        channel = it.optStringOrNull("channel"), correctsEventId = it.optLongOrNull("corrects_event_id"), rawLiters = it.optDoubleOrNull("raw_liters"),
        fatPercentage = it.optDoubleOrNull("fat_percentage"), lrReading = it.optDoubleOrNull("lr_reading"), snfPercentage = it.optDoubleOrNull("snf_percentage"),
        tsPercentage = it.optDoubleOrNull("ts_percentage"), adjustedLiters = it.optDoubleOrNull("adjusted_liters"), amountOwed = it.optDoubleOrNull("amount_owed"),
        milkLitersUsed = it.optDoubleOrNull("milk_liters_used"), yogurtKgProduced = it.optDoubleOrNull("yogurt_kg_produced"), amount = it.optDoubleOrNull("amount"),
        milkRateUsed = it.optDoubleOrNull("milk_rate_used"), deliveryFeeUsed = it.optDoubleOrNull("delivery_fee_used"), tsStandardUsed = it.optDoubleOrNull("ts_standard_used"),
        saleQuantity = it.optDoubleOrNull("sale_quantity"), saleValue = it.optDoubleOrNull("sale_value"), unitRate = it.optDoubleOrNull("unit_rate"),
        surchargeApplied = it.optDouble("surcharge_applied", 0.0), notes = it.optStringOrNull("notes")
    ) } }
    private fun readInventoryLogs(a: JSONArray?): List<InventoryLogEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let { InventoryLogEntity(it.getLong("log_id"), it.getLong("event_id"), it.getLong("product_id"), it.getDouble("quantity")) } }
    private fun readFinancialLogs(a: JSONArray?): List<FinancialLogEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let { FinancialLogEntity(it.getLong("log_id"), it.getLong("event_id"), it.getLong("account_id"), it.getDouble("amount")) } }
    private fun readSettings(a: JSONArray?): List<SettingEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let { SettingEntity(it.getString("setting_key"), it.getDouble("setting_value"), it.getString("updated_at")) } }
    private fun readBudgets(a: JSONArray?): List<BudgetPlanEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let { BudgetPlanEntity(
        budgetId = it.getLong("budget_id"), category = it.getString("category"), periodStart = it.getString("period_start"), periodEndExclusive = it.getString("period_end_exclusive"),
        plannedLiters = it.optDoubleOrNull("planned_liters"), plannedAmount = it.optDoubleOrNull("planned_amount"), vendorRateSnapshot = it.optDoubleOrNull("vendor_rate_snapshot"),
        retailRateSnapshot = it.optDoubleOrNull("retail_rate_snapshot"), createdAt = it.getString("created_at"), supersedesId = it.optLongOrNull("supersedes_id"), archived = it.optBoolean("archived", false)
    ) } }
    private fun readCollectionsTargets(a: JSONArray?): List<CollectionsTargetEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let { CollectionsTargetEntity(
        targetId = it.getLong("target_id"), weekStart = it.getString("week_start"), weekEndExclusive = it.getString("week_end_exclusive"), targetAmount = it.getDouble("target_amount"),
        createdAt = it.getString("created_at"), supersedesId = it.optLongOrNull("supersedes_id"), archived = it.optBoolean("archived", false)
    ) } }
    private fun readProductProfiles(a: JSONArray?): List<ProductProfileEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let {
        ProductProfileEntity(it.getLong("product_id"), it.optDoubleOrNull("default_sale_rate"), it.optBoolean("inventory_tracking", true), it.optBoolean("archived", false), it.optStringOrNull("notes"), it.getString("created_at"))
    } }
    private fun readCustomerProfiles(a: JSONArray?): List<CustomerProfileEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let {
        CustomerProfileEntity(it.getLong("account_id"), it.optStringOrNull("phone"), it.optStringOrNull("area_route"), it.optStringOrNull("address"), it.optStringOrNull("notes"), it.optDoubleOrNull("credit_limit"), it.optDoubleOrNull("daily_quota"), it.optBoolean("active", true), it.getString("created_at"))
    } }
    private fun readVendorProfiles(a: JSONArray?): List<VendorProfileEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let {
        VendorProfileEntity(it.getLong("account_id"), it.optStringOrNull("phone"), it.optStringOrNull("location"), it.optStringOrNull("notes"), it.optBoolean("active", true), it.getString("created_at"))
    } }
    private fun readEmployeeProfiles(a: JSONArray?): List<EmployeeProfileEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let {
        EmployeeProfileEntity(it.getLong("account_id"), it.optStringOrNull("role"), it.optStringOrNull("phone"), it.optDoubleOrNull("reference_salary"), it.optStringOrNull("salary_period"), it.optStringOrNull("join_date"), it.optBoolean("active", true), it.getString("created_at"))
    } }
    private fun readAttendanceRecords(a: JSONArray?): List<AttendanceRecordEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let {
        AttendanceRecordEntity(it.getLong("attendance_id"), it.getLong("employee_account_id"), it.getString("business_date"), it.getString("status"), it.optStringOrNull("notes"), it.getString("created_at"), it.optLongOrNull("supersedes_id"), it.optBoolean("archived", false))
    } }
    private fun readDeliveries(a: JSONArray?): List<DeliveryEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let {
        DeliveryEntity(it.getLong("delivery_id"), it.getLong("customer_account_id"), it.getLong("product_id"), it.getDouble("quantity"), it.optStringOrNull("route"), it.getString("business_date"), it.optLongOrNull("sale_event_id"), it.optStringOrNull("notes"), it.getString("created_at"), it.optBoolean("archived", false))
    } }
    private fun readDeliveryStatuses(a: JSONArray?): List<DeliveryStatusEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let {
        DeliveryStatusEntity(it.getLong("status_id"), it.getLong("delivery_id"), it.getString("status"), it.getString("occurred_at"), it.optStringOrNull("notes"))
    } }
    private fun readSpoilageDetails(a: JSONArray?): List<SpoilageDetailEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let {
        SpoilageDetailEntity(it.getLong("event_id"), it.getLong("product_id"), it.getDouble("quantity"), it.getString("reason"))
    } }
    private fun readPhysicalCashCounts(a: JSONArray?): List<PhysicalCashCountEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let {
        PhysicalCashCountEntity(it.getLong("count_id"), it.getLong("account_id"), it.getString("business_date"), it.getDouble("counted_amount"), it.getString("counted_at"), it.optStringOrNull("notes"))
    } }
    private fun readExpenseCategoryProfiles(a: JSONArray?): List<ExpenseCategoryProfileEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let {
        ExpenseCategoryProfileEntity(it.getLong("account_id"), it.getString("display_name"), it.optBoolean("active", true), it.getString("created_at"))
    } }
    private fun readStoreProfiles(a: JSONArray?): List<StoreProfileEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let {
        StoreProfileEntity(it.getInt("store_profile_id"), it.getString("store_name"), it.getString("currency_code"), it.optStringOrNull("phone"), it.optStringOrNull("address"), it.getString("updated_at"))
    } }
    private fun readOpeningBatches(a: JSONArray?): List<OpeningBatchEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let {
        OpeningBatchEntity(it.getLong("event_id"))
    } }
    private fun readOpeningInventoryEntries(a: JSONArray?): List<OpeningInventoryEntryEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let {
        OpeningInventoryEntryEntity(it.getLong("event_id"), it.getLong("product_id"), it.getDouble("quantity"))
    } }
    private fun readOpeningAccountEntries(a: JSONArray?): List<OpeningAccountEntryEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let {
        OpeningAccountEntryEntity(it.getLong("event_id"), it.getLong("account_id"), it.getDouble("balance"))
    } }
    private fun readDailyCloseActions(a: JSONArray?): List<DailyCloseActionEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let {
        DailyCloseActionEntity(
            actionId = it.getLong("action_id"), businessDate = it.getString("business_date"), action = it.getString("action"),
            physicalCashCountId = it.optLongOrNull("physical_cash_count_id"), actorName = it.getString("actor_name"), actorRole = it.getString("actor_role"),
            actorUserId = it.optLongOrNull("actor_user_id"), reason = it.optStringOrNull("reason"), createdAt = it.getString("created_at"), supersedesActionId = it.optLongOrNull("supersedes_action_id")
        )
    } }
    private fun readAppUsers(a: JSONArray?): List<AppUserEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let {
        AppUserEntity(
            userId = it.getLong("user_id"), loginName = it.getString("login_name"), displayName = it.getString("display_name"), role = it.getString("role"),
            employeeAccountId = it.optLongOrNull("employee_account_id"), status = it.getString("status"), credentialSetupRequired = it.optBoolean("credential_setup_required", false),
            createdAt = it.getString("created_at"), createdByUserId = it.optLongOrNull("created_by_user_id"), updatedAt = it.getString("updated_at"),
            disabledAt = it.optStringOrNull("disabled_at"), disabledByUserId = it.optLongOrNull("disabled_by_user_id")
        )
    } }
    private fun readUserCredentials(a: JSONArray?): List<UserCredentialEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let {
        UserCredentialEntity(
            userId = it.getLong("user_id"), algorithm = it.getString("algorithm"), salt = Base64.getDecoder().decode(it.getString("salt")),
            verifier = Base64.getDecoder().decode(it.getString("verifier")), memoryKib = it.getInt("memory_kib"), iterations = it.getInt("iterations"),
            parallelism = it.getInt("parallelism"), changedAt = it.getString("changed_at"), mustChange = it.optBoolean("must_change", false),
            failedAttemptCount = 0, lastFailedAtEpochMs = null, lockedUntilEpochMs = null
        )
    } }
    private fun readSavedRecoveryCodes(a: JSONArray?): List<SavedRecoveryCodeEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let {
        SavedRecoveryCodeEntity(
            recoveryCodeId = it.getLong("recovery_code_id"), userId = it.getLong("user_id"), salt = Base64.getDecoder().decode(it.getString("salt")),
            verifier = Base64.getDecoder().decode(it.getString("verifier")), createdAt = it.getString("created_at"), usedAt = it.optStringOrNull("used_at"),
            failedAttemptCount = 0, lockedUntilEpochMs = null
        )
    } }
    private fun readSecurityAuditEntries(a: JSONArray?): List<SecurityAuditEntryEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let {
        SecurityAuditEntryEntity(
            auditId = it.getLong("audit_id"), occurredAt = it.getString("occurred_at"), action = it.getString("action"), result = it.getString("result"),
            reasonCode = it.optStringOrNull("reason_code"), actorUserId = it.optLongOrNull("actor_user_id"), actorNameSnapshot = it.optStringOrNull("actor_name_snapshot"),
            actorRoleSnapshot = it.optStringOrNull("actor_role_snapshot"), targetUserId = it.optLongOrNull("target_user_id"), details = it.optStringOrNull("details")
        )
    } }
    private fun readEventActorLinks(a: JSONArray?): List<EventActorLinkEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let {
        EventActorLinkEntity(
            eventId = it.getLong("event_id"), actorUserId = it.getLong("actor_user_id"), actorNameSnapshot = it.getString("actor_name_snapshot"),
            actorRoleSnapshot = it.getString("actor_role_snapshot"), attributedAt = it.getString("attributed_at")
        )
    } }
    private fun readCostingPolicyVersions(a: JSONArray?): List<CostingPolicyVersionEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let {
        CostingPolicyVersionEntity(
            policyId = it.getLong("policy_id"), calendarMonth = it.getString("calendar_month"), normalCapacityKg = it.getString("normal_capacity_kg"),
            reviewReason = it.getString("review_reason"), notes = it.optStringOrNull("notes"), approvedAt = it.getString("approved_at"),
            approvedByUserId = it.getLong("approved_by_user_id"), supersedesPolicyId = it.optLongOrNull("supersedes_policy_id")
        )
    } }
    private fun readCostingPolicyComponents(a: JSONArray?): List<CostingPolicyComponentEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let {
        CostingPolicyComponentEntity(it.getLong("policy_id"), it.getString("component_code"), it.getLong("source_amount_paisa"), it.getString("basis_quantity_kg"))
    } }
    private fun readInventoryCostBasisEntries(a: JSONArray?): List<InventoryCostBasisEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let {
        InventoryCostBasisEntity(
            basisId = it.getLong("basis_id"), openingEventId = it.getLong("opening_event_id"), productId = it.getLong("product_id"),
            quantity = it.getString("quantity"), totalCostPaisa = it.getLong("total_cost_paisa"), basisKind = it.getString("basis_kind"),
            evidenceNotes = it.optStringOrNull("evidence_notes"), createdAt = it.getString("created_at"), createdByUserId = it.getLong("created_by_user_id"),
            supersedesBasisId = it.optLongOrNull("supersedes_basis_id")
        )
    } }
    private fun readYieldPolicyVersions(a: JSONArray?): List<YieldPolicyVersionEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let {
        YieldPolicyVersionEntity(
            yieldPolicyId = it.getLong("yield_policy_id"), sampleStartDate = it.getString("sample_start_date"), sampleEndDate = it.getString("sample_end_date"),
            acceptedBatchCount = it.getInt("accepted_batch_count"), medianYieldRatio = it.getString("median_yield_ratio"), madYieldRatio = it.getString("mad_yield_ratio"),
            lowerAlertLimit = it.getString("lower_alert_limit"), upperAlertLimit = it.getString("upper_alert_limit"), effectiveFrom = it.getString("effective_from"),
            approvedAt = it.getString("approved_at"), approvedByUserId = it.getLong("approved_by_user_id"),
            supersedesYieldPolicyId = it.optLongOrNull("supersedes_yield_policy_id")
        )
    } }
    private fun readProductionLossAssessments(a: JSONArray?): List<ProductionLossAssessmentEntity> = (0 until (a?.length() ?: 0)).map { i -> a!!.getJSONObject(i).let {
        ProductionLossAssessmentEntity(
            assessmentId = it.getLong("assessment_id"), productionEventId = it.getLong("production_event_id"), classification = it.getString("classification"),
            abnormalMilkLiters = it.getString("abnormal_milk_liters"), reasonCode = it.getString("reason_code"), notes = it.optStringOrNull("notes"),
            assessedAt = it.getString("assessed_at"), assessedByUserId = it.getLong("assessed_by_user_id"),
            supersedesAssessmentId = it.optLongOrNull("supersedes_assessment_id")
        )
    } }

    private fun JSONObject.optStringOrNull(key: String): String? = if (!has(key) || isNull(key)) null else getString(key)
    private fun JSONObject.optLongOrNull(key: String): Long? = if (!has(key) || isNull(key)) null else getLong(key)
    private fun JSONObject.optDoubleOrNull(key: String): Double? = if (!has(key) || isNull(key)) null else getDouble(key)
    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
