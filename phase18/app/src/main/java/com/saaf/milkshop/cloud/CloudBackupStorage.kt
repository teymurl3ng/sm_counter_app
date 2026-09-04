package com.saaf.milkshop.cloud

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters
import java.util.UUID

data class CloudPendingSnapshot(
    val snapshotId: String,
    val createdAt: String,
    val fileName: String,
    val sha256: String,
    val sizeBytes: Long,
)

data class CloudRemoteSnapshot(
    val remoteFileId: String,
    val snapshotId: String,
    val vaultId: String,
    val createdAt: String,
    val sizeBytes: Long,
    val sha256: String?,
    val name: String,
)

data class CloudBackupState(
    val installationId: String,
    val configuredAt: String,
    val enabled: Boolean,
    val allowMetered: Boolean,
    val accountPermissionId: String,
    val accountDisplay: String,
    val enabledByUserId: Long,
    val enabledByName: String,
    val enabledByRole: String,
    val vault: CloudVaultConfiguration,
    val pending: CloudPendingSnapshot? = null,
    val lastAttemptAt: String? = null,
    val lastSuccessAt: String? = null,
    val lastErrorCode: String? = null,
    val lastErrorMessage: String? = null,
    val snapshots: List<CloudRemoteSnapshot> = emptyList(),
)

class CloudBackupSettingsStore(
    context: Context,
    directoryName: String = DIRECTORY,
) {
    private val directory = File(context.noBackupFilesDir, directoryName).apply { mkdirs() }
    private val atomicFile = AtomicFile(File(directory, SETTINGS_FILE))

    @Synchronized
    fun read(): CloudBackupState? {
        if (!atomicFile.baseFile.exists()) return null
        return runCatching {
            atomicFile.openRead().use { input -> decode(JSONObject(input.readBytes().toString(Charsets.UTF_8))) }
        }.getOrElse { throw CloudBackupFailure.KeyUnavailable("Cloud backup settings are damaged") }
    }

    @Synchronized
    fun write(state: CloudBackupState) {
        val output = atomicFile.startWrite()
        try {
            output.write(encode(state).toString(2).toByteArray(Charsets.UTF_8))
            output.fd.sync()
            atomicFile.finishWrite(output)
        } catch (error: Throwable) {
            atomicFile.failWrite(output)
            throw error
        }
    }

    @Synchronized
    fun update(transform: (CloudBackupState) -> CloudBackupState): CloudBackupState {
        val updated = transform(read() ?: throw CloudBackupFailure.NotConfigured())
        write(updated)
        return updated
    }

    @Synchronized
    fun clear() {
        atomicFile.delete()
    }

    private fun encode(state: CloudBackupState) = JSONObject().apply {
        put("installation_id", state.installationId)
        put("configured_at", state.configuredAt)
        put("enabled", state.enabled)
        put("allow_metered", state.allowMetered)
        put("account_permission_id", state.accountPermissionId)
        put("account_display", state.accountDisplay)
        put("enabled_by_user_id", state.enabledByUserId)
        put("enabled_by_name", state.enabledByName)
        put("enabled_by_role", state.enabledByRole)
        put("vault", vaultJson(state.vault))
        put("pending", state.pending?.let(::pendingJson) ?: JSONObject.NULL)
        put("last_attempt_at", state.lastAttemptAt ?: JSONObject.NULL)
        put("last_success_at", state.lastSuccessAt ?: JSONObject.NULL)
        put("last_error_code", state.lastErrorCode ?: JSONObject.NULL)
        put("last_error_message", state.lastErrorMessage ?: JSONObject.NULL)
        put("snapshots", JSONArray().apply { state.snapshots.take(MAX_CACHED_SNAPSHOTS).forEach { put(snapshotJson(it)) } })
    }

    private fun decode(json: JSONObject): CloudBackupState = CloudBackupState(
        installationId = json.getString("installation_id").also(UUID::fromString),
        configuredAt = json.getString("configured_at").also(Instant::parse),
        enabled = json.getBoolean("enabled"),
        allowMetered = json.getBoolean("allow_metered"),
        accountPermissionId = json.getString("account_permission_id"),
        accountDisplay = json.getString("account_display"),
        enabledByUserId = json.getLong("enabled_by_user_id"),
        enabledByName = json.getString("enabled_by_name"),
        enabledByRole = json.getString("enabled_by_role"),
        vault = vault(json.getJSONObject("vault")),
        pending = json.optJSONObject("pending")?.let(::pending),
        lastAttemptAt = json.optNullableString("last_attempt_at"),
        lastSuccessAt = json.optNullableString("last_success_at"),
        lastErrorCode = json.optNullableString("last_error_code"),
        lastErrorMessage = json.optNullableString("last_error_message"),
        snapshots = json.optJSONArray("snapshots")?.let { array ->
            List(array.length()) { index -> snapshot(array.getJSONObject(index)) }
        } ?: emptyList(),
    )

    private fun vaultJson(value: CloudVaultConfiguration) = JSONObject().apply {
        put("vault_id", value.vaultId)
        put("portable_wrap", JSONObject().apply {
            put("salt", value.portableWrap.saltBase64)
            put("memory_kib", value.portableWrap.memoryKib)
            put("iterations", value.portableWrap.iterations)
            put("parallelism", value.portableWrap.parallelism)
            put("nonce", value.portableWrap.wrappedKey.nonceBase64)
            put("ciphertext", value.portableWrap.wrappedKey.ciphertextBase64)
        })
        put("local_wrap", JSONObject().apply {
            put("nonce", value.localWrap.nonceBase64)
            put("ciphertext", value.localWrap.ciphertextBase64)
        })
    }

    private fun vault(json: JSONObject): CloudVaultConfiguration {
        val portable = json.getJSONObject("portable_wrap")
        val local = json.getJSONObject("local_wrap")
        return CloudVaultConfiguration(
            vaultId = json.getString("vault_id").also(UUID::fromString),
            portableWrap = PortableVaultWrap(
                saltBase64 = portable.getString("salt"),
                memoryKib = portable.getInt("memory_kib"),
                iterations = portable.getInt("iterations"),
                parallelism = portable.getInt("parallelism"),
                wrappedKey = WrappedCiphertext(
                    nonceBase64 = portable.getString("nonce"),
                    ciphertextBase64 = portable.getString("ciphertext"),
                ),
            ),
            localWrap = WrappedCiphertext(
                nonceBase64 = local.getString("nonce"),
                ciphertextBase64 = local.getString("ciphertext"),
            ),
        )
    }

    private fun pendingJson(value: CloudPendingSnapshot) = JSONObject()
        .put("snapshot_id", value.snapshotId)
        .put("created_at", value.createdAt)
        .put("file_name", value.fileName)
        .put("sha256", value.sha256)
        .put("size_bytes", value.sizeBytes)

    private fun pending(json: JSONObject) = CloudPendingSnapshot(
        snapshotId = json.getString("snapshot_id").also(UUID::fromString),
        createdAt = json.getString("created_at").also(Instant::parse),
        fileName = json.getString("file_name"),
        sha256 = json.getString("sha256"),
        sizeBytes = json.getLong("size_bytes"),
    )

    private fun snapshotJson(value: CloudRemoteSnapshot) = JSONObject()
        .put("remote_file_id", value.remoteFileId)
        .put("snapshot_id", value.snapshotId)
        .put("vault_id", value.vaultId)
        .put("created_at", value.createdAt)
        .put("size_bytes", value.sizeBytes)
        .put("sha256", value.sha256 ?: JSONObject.NULL)
        .put("name", value.name)

    private fun snapshot(json: JSONObject) = CloudRemoteSnapshot(
        remoteFileId = json.getString("remote_file_id"),
        snapshotId = json.getString("snapshot_id").also(UUID::fromString),
        vaultId = json.getString("vault_id").also(UUID::fromString),
        createdAt = json.getString("created_at").also(Instant::parse),
        sizeBytes = json.getLong("size_bytes"),
        sha256 = json.optNullableString("sha256"),
        name = json.getString("name"),
    )

    private fun JSONObject.optNullableString(name: String): String? =
        if (!has(name) || isNull(name)) null else getString(name)

    companion object {
        private const val DIRECTORY = "cloud_backup"
        private const val SETTINGS_FILE = "settings.json"
        private const val MAX_CACHED_SNAPSHOTS = 200
    }
}

class CloudBackupOutbox(
    context: Context,
    directoryName: String = DIRECTORY,
) {
    private val directory = File(context.noBackupFilesDir, directoryName).apply { mkdirs() }

    fun create(snapshotId: String, createdAt: String, bytes: ByteArray): CloudPendingSnapshot {
        UUID.fromString(snapshotId)
        val fileName = "$snapshotId$EXTENSION"
        val destination = file(fileName)
        val temporary = file("$fileName.tmp")
        FileOutputStream(temporary).use { output ->
            output.write(bytes)
            output.fd.sync()
        }
        check(!destination.exists()) { "Pending cloud snapshots are immutable" }
        check(temporary.renameTo(destination)) { "Could not finalize pending cloud snapshot" }
        return CloudPendingSnapshot(
            snapshotId = snapshotId,
            createdAt = createdAt,
            fileName = fileName,
            sha256 = sha256(bytes),
            sizeBytes = bytes.size.toLong(),
        )
    }

    fun read(pending: CloudPendingSnapshot): ByteArray {
        validateFileName(pending)
        val bytes = file(pending.fileName).readBytes()
        require(bytes.size.toLong() == pending.sizeBytes && sha256(bytes) == pending.sha256) {
            "Pending cloud snapshot failed its local integrity check"
        }
        return bytes
    }

    fun remove(pending: CloudPendingSnapshot) {
        validateFileName(pending)
        val target = file(pending.fileName)
        if (target.exists() && !target.delete()) throw IllegalStateException("Could not clear the uploaded cloud snapshot")
    }

    fun clearAll() {
        directory.listFiles()?.filter { it.isFile && (it.name.endsWith(EXTENSION) || it.name.endsWith("$EXTENSION.tmp")) }
            ?.forEach { it.delete() }
    }

    private fun validateFileName(pending: CloudPendingSnapshot) {
        require(pending.fileName == "${pending.snapshotId}$EXTENSION") { "Invalid pending cloud snapshot path" }
    }

    private fun file(name: String): File {
        val target = File(directory, name)
        require(target.parentFile?.canonicalFile == directory.canonicalFile) { "Invalid cloud outbox path" }
        return target
    }

    companion object {
        private const val DIRECTORY = "cloud_backup/outbox"
        private const val EXTENSION = ".milkcloud"
    }
}

object CloudBackupRetention {
    fun removals(snapshots: List<CloudRemoteSnapshot>): List<CloudRemoteSnapshot> {
        if (snapshots.size <= MINIMUM_NEWEST) return emptyList()
        val ordered = snapshots.sortedWith(compareByDescending<CloudRemoteSnapshot> { Instant.parse(it.createdAt) }.thenByDescending { it.snapshotId })
        val keep = ordered.take(MINIMUM_NEWEST).mapTo(mutableSetOf()) { it.remoteFileId }
        val dates = ordered.associateWith { Instant.parse(it.createdAt).atZone(ZoneOffset.UTC).toLocalDate() }

        dates.entries.groupBy { it.value }.toSortedMap(compareByDescending { it }).values
            .take(DAILY_POINTS)
            .forEach { group -> keep += group.first().key.remoteFileId }

        val referenceDate = dates.getValue(ordered.first())
        val currentWeek = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        dates.entries.groupBy { it.value.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }
            .filterKeys { it < currentWeek }
            .toSortedMap(compareByDescending { it }).values
            .take(WEEKLY_POINTS)
            .forEach { group -> keep += group.first().key.remoteFileId }

        val currentMonth = referenceDate.withDayOfMonth(1)
        dates.entries.groupBy { it.value.withDayOfMonth(1) }
            .filterKeys { it < currentMonth }
            .toSortedMap(compareByDescending { it }).values
            .take(MONTHLY_POINTS)
            .forEach { group -> keep += group.first().key.remoteFileId }

        return ordered.filterNot { it.remoteFileId in keep }
    }

    const val DAILY_POINTS = 14
    const val WEEKLY_POINTS = 8
    const val MONTHLY_POINTS = 12
    const val MINIMUM_NEWEST = 3
}

internal fun sha256(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
