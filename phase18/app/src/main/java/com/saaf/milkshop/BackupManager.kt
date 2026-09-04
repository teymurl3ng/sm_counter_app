package com.saaf.milkshop

import android.content.Context
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.saaf.milkshop.data.*
import com.saaf.milkshop.domain.AppPasswordPolicy
import com.saaf.milkshop.domain.Argon2idPasswordHasher
import com.saaf.milkshop.domain.NativeSessionManager
import androidx.room3.withReadTransaction
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

class BackupManager(
    private val context: Context,
    private val snapshotReadBoundaryHook: suspend () -> Unit = {},
) {
    private val cryptography = BackupCryptography(context)
    private val restoreEngine = BackupRestoreEngine(
        createSafetySnapshot = { database -> createLocalSnapshot(database).getOrThrow() },
        buildVerifiedBytes = { database, includeSecurity, formatVersion ->
            buildBackupBytes(database, includeSecurity, formatVersion)
        },
        restorePreferences = ::restorePreferences,
    )

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
                    cryptography.encryptLocalFormat2(payload)
                } finally {
                    payload.fill(0)
                }
                val dir = java.io.File(context.filesDir, BACKUP_DIR).apply { mkdirs() }
                val destination = uniqueSnapshotDestination(dir, displayName(context))
                val fileName = destination.name
                val tmp = java.io.File(dir, "$fileName.tmp")
                tmp.writeBytes(bytes)
                val verified = cryptography.readLocalAndVerify(tmp.readBytes())
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
                cryptography.encryptFormat2(payload, passphrase)
            } finally {
                payload.fill(0)
            }
            context.contentResolver.openOutputStream(uri)?.use { it.write(encrypted) }
                ?: throw IOException("The selected backup destination could not be opened")
            val verified = cryptography.readEncryptedAndVerify(encrypted, passphrase)
            backupMeta(verified, uri.lastPathSegment ?: "backup")
        }
    }

    suspend fun readEncryptedBackupMeta(uri: Uri, passphrase: CharArray): Result<BackupMeta> =
        withContext(Dispatchers.IO) {
            runCatching {
                val json = cryptography.readEncryptedAndVerify(cryptography.readUri(uri), passphrase)
                val metadata = json.getJSONObject("metadata")
                validateCompatibility(metadata)
                backupMeta(json, uri.lastPathSegment ?: "backup")
            }
        }

    suspend fun inspectEncryptedSnapshot(uri: Uri, passphrase: CharArray): Result<EncryptedBackupInspection> =
        withContext(Dispatchers.IO) {
            runCatching {
                val json = cryptography.readEncryptedAndVerify(cryptography.readUri(uri), passphrase)
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
                    val json = cryptography.readLegacyOrLocalAndVerify(file.readBytes())
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
            val bytes = cryptography.readUri(uri)
            val json = cryptography.readLegacyOrLocalAndVerify(bytes)
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
            val bytes = cryptography.readUri(uri)
            val json = readAndVerify(bytes)
            val metadata = json.getJSONObject("metadata")
            validateCompatibility(metadata)
            restoreEngine.restore(database, json, selection, includeSecurity = false)
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
            val json = cryptography.readEncryptedAndVerify(cryptography.readUri(uri), passphrase)
            val metadata = json.getJSONObject("metadata")
            validateCompatibility(metadata)
            require(metadata.getInt("format_version") == FORMAT_VERSION) { "Encrypted backup format is invalid" }
            restoreEngine.restore(database, json, selection, includeSecurity = true)
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
            val json = cryptography.readLocalAndVerify(cryptography.readUri(uri))
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
            restoreEngine.restore(database, json, selection, includeSecurity = hasUsers)
            sessions.invalidateAll()
            backupMeta(json, uri.lastPathSegment ?: "backup")
        }.recoverCatching { error ->
            throw BackupFailure(
                reason = error.message ?: error.javaClass.simpleName,
                recovery = "The local restore was not completed. The pre-restore safety backup remains available.",
            )
        }
    }

    /**
     * Builds the complete security-bearing payload from one consistent Room
     * read snapshot. Cloud work may overlap normal shop writes, so callers must
     * never serialize the backup tables as unrelated reads.
     */
    internal suspend fun buildCompleteSnapshotBytes(database: MilkShopDatabase): ByteArray =
        buildBackupBytes(database, includeSecurity = true, formatVersion = FORMAT_VERSION)

    internal fun inspectCompleteSnapshotBytes(bytes: ByteArray, fileName: String): EncryptedBackupInspection {
        val json = readAndVerify(bytes)
        val metadata = json.getJSONObject("metadata")
        validateCompatibility(metadata)
        require(metadata.getInt("format_version") == FORMAT_VERSION) { "Encrypted backup format is invalid" }
        return EncryptedBackupInspection(
            meta = backupMeta(json, fileName),
            containsSecurityState = json.getJSONObject("data").optJSONArray("app_users")?.length()?.let { it > 0 } == true,
        )
    }

    suspend fun restoreCompleteSnapshotReplacing(
        database: MilkShopDatabase,
        bytes: ByteArray,
        sessions: NativeSessionManager,
        fileName: String,
    ): Result<BackupMeta> = withContext(Dispatchers.IO) {
        runCatching {
            val json = readAndVerify(bytes)
            val metadata = json.getJSONObject("metadata")
            validateCompatibility(metadata)
            require(metadata.getInt("format_version") == FORMAT_VERSION) { "Encrypted backup format is invalid" }
            require(json.getJSONObject("data").getJSONArray("app_users").length() > 0) {
                "This encrypted backup contains no application users"
            }
            restoreEngine.restore(
                database,
                json,
                RestoreSelection(business = true, planning = true, preferences = true, security = true),
                includeSecurity = true,
            )
            sessions.invalidateAll()
            backupMeta(json, fileName)
        }.recoverCatching { error ->
            throw BackupFailure(
                reason = error.message ?: error.javaClass.simpleName,
                recovery = "The restore was not completed. The pre-restore safety backup remains available for recovery.",
            )
        }
    }

    private suspend fun buildBackupBytes(
        database: MilkShopDatabase,
        includeSecurity: Boolean = false,
        formatVersion: Int = LEGACY_FORMAT_VERSION,
    ): ByteArray = database.withReadTransaction {
        buildBackupBytesInReadTransaction(database, includeSecurity, formatVersion)
    }

    private suspend fun buildBackupBytesInReadTransaction(
        database: MilkShopDatabase,
        includeSecurity: Boolean,
        formatVersion: Int,
    ): ByteArray {
        val dao = database.backupDataDao()
        val costingPolicies = dao.costingPolicyVersions()
        snapshotReadBoundaryHook()
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

    /**
     * Daily-close integrity requires every historical close to reference the
     * latest count visible at that point in its audit chain. Replay the stored
     * chronology instead of bulk-inserting all counts ahead of all actions.
     */
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


}
