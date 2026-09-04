package com.saaf.milkshop.cloud

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

data class CloudAccountIdentity(
    val permissionId: String,
    val displayEmail: String,
    val displayName: String?,
)

interface CloudAccessTokenSource {
    suspend fun accessToken(): String
}

interface CloudBackupProvider {
    suspend fun accountIdentity(): CloudAccountIdentity
    suspend fun listSnapshots(vaultId: String? = null): List<CloudRemoteSnapshot>
    suspend fun findSnapshot(vaultId: String, snapshotId: String): CloudRemoteSnapshot?
    suspend fun uploadSnapshot(
        pending: CloudPendingSnapshot,
        vaultId: String,
        installationId: String,
        bytes: ByteArray,
    ): CloudRemoteSnapshot
    suspend fun downloadSnapshot(remoteFileId: String): ByteArray
    suspend fun deleteSnapshot(remoteFileId: String)
}

class GoogleDriveCloudBackupProvider(
    private val tokens: CloudAccessTokenSource,
) : CloudBackupProvider {
    override suspend fun accountIdentity(): CloudAccountIdentity = withContext(Dispatchers.IO) {
        val response = request(
            method = "GET",
            url = "$DRIVE_API/about?fields=${encodeQuery("user(displayName,emailAddress,permissionId)")}",
        )
        requireSuccess(response)
        val user = JSONObject(response.bodyText()).getJSONObject("user")
        CloudAccountIdentity(
            permissionId = user.getString("permissionId"),
            displayEmail = user.optString("emailAddress"),
            displayName = user.optString("displayName").takeIf(String::isNotBlank),
        )
    }

    override suspend fun listSnapshots(vaultId: String?): List<CloudRemoteSnapshot> = withContext(Dispatchers.IO) {
        val query = buildString {
            append("trashed = false and appProperties has { key='product' and value='$PRODUCT_PROPERTY' }")
            vaultId?.let { append(" and appProperties has { key='vault_id' and value='$it' }") }
        }
        val fields = "nextPageToken,files(id,name,createdTime,size,sha256Checksum,appProperties)"
        val result = mutableListOf<CloudRemoteSnapshot>()
        var pageToken: String? = null
        do {
            val suffix = buildString {
                append("spaces=appDataFolder&pageSize=1000&orderBy=createdTime%20desc")
                append("&q=").append(encodeQuery(query))
                append("&fields=").append(encodeQuery(fields))
                pageToken?.let { append("&pageToken=").append(encodeQuery(it)) }
            }
            val response = request("GET", "$DRIVE_API/files?$suffix")
            requireSuccess(response)
            val json = JSONObject(response.bodyText())
            val files = json.optJSONArray("files") ?: JSONArray()
            repeat(files.length()) { index ->
                parseRemote(files.getJSONObject(index))?.let(result::add)
            }
            pageToken = json.optString("nextPageToken").takeIf(String::isNotBlank)
        } while (pageToken != null)
        result.sortedByDescending { Instant.parse(it.createdAt) }
    }

    override suspend fun findSnapshot(vaultId: String, snapshotId: String): CloudRemoteSnapshot? =
        listSnapshots(vaultId).firstOrNull { it.snapshotId == snapshotId }

    override suspend fun uploadSnapshot(
        pending: CloudPendingSnapshot,
        vaultId: String,
        installationId: String,
        bytes: ByteArray,
    ): CloudRemoteSnapshot = withContext(Dispatchers.IO) {
        require(bytes.size.toLong() == pending.sizeBytes && sha256(bytes) == pending.sha256) {
            "Cloud upload bytes no longer match the immutable outbox record"
        }
        findSnapshot(vaultId, pending.snapshotId)?.let { return@withContext it }
        val metadata = JSONObject().apply {
            put("name", remoteName(pending))
            put("parents", JSONArray().put("appDataFolder"))
            put("appProperties", JSONObject().apply {
                put("product", PRODUCT_PROPERTY)
                put("snapshot_id", pending.snapshotId)
                put("vault_id", vaultId)
                put("installation_id", installationId)
                put("created_at", pending.createdAt)
                put("envelope_version", CloudBackupCryptography.CLOUD_ENVELOPE_VERSION.toString())
                put("sha256", pending.sha256)
                put("size_bytes", pending.sizeBytes.toString())
            })
        }.toString().toByteArray(Charsets.UTF_8)
        val initiation = request(
            method = "POST",
            url = "$DRIVE_UPLOAD/files?uploadType=resumable&fields=${encodeQuery(FILE_FIELDS)}",
            contentType = "application/json; charset=UTF-8",
            body = metadata,
            extraHeaders = mapOf(
                "X-Upload-Content-Type" to CONTENT_TYPE,
                "X-Upload-Content-Length" to bytes.size.toString(),
            ),
        )
        requireSuccess(initiation)
        val uploadUrl = initiation.headers.entries.firstOrNull { it.key?.equals("Location", ignoreCase = true) == true }
            ?.value?.firstOrNull()
            ?: throw CloudBackupFailure.Remote("Google Drive did not create an upload session")
        val upload = request(
            method = "PUT",
            url = uploadUrl,
            contentType = CONTENT_TYPE,
            body = bytes,
            extraHeaders = mapOf("Content-Range" to "bytes 0-${bytes.lastIndex}/${bytes.size}"),
        )
        if (upload.status == 308) {
            return@withContext findSnapshot(vaultId, pending.snapshotId)
                ?: throw CloudBackupFailure.Network("Google Drive upload completion is not yet confirmed")
        }
        requireSuccess(upload)
        parseRemote(JSONObject(upload.bodyText()))
            ?: findSnapshot(vaultId, pending.snapshotId)
            ?: throw CloudBackupFailure.Remote("Google Drive did not return the uploaded recovery point")
    }

    override suspend fun downloadSnapshot(remoteFileId: String): ByteArray = withContext(Dispatchers.IO) {
        validateRemoteId(remoteFileId)
        val response = request("GET", "$DRIVE_API/files/${encodePath(remoteFileId)}?alt=media")
        requireSuccess(response)
        response.body
    }

    override suspend fun deleteSnapshot(remoteFileId: String) = withContext(Dispatchers.IO) {
        validateRemoteId(remoteFileId)
        val response = request("DELETE", "$DRIVE_API/files/${encodePath(remoteFileId)}")
        if (response.status != HttpURLConnection.HTTP_NOT_FOUND) requireSuccess(response)
    }

    private suspend fun request(
        method: String,
        url: String,
        contentType: String? = null,
        body: ByteArray? = null,
        extraHeaders: Map<String, String> = emptyMap(),
    ): HttpResponse {
        val token = tokens.accessToken()
        return try {
            val connection = URI(url).toURL().openConnection() as HttpURLConnection
            connection.requestMethod = method
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Accept", "application/json")
            contentType?.let { connection.setRequestProperty("Content-Type", it) }
            extraHeaders.forEach(connection::setRequestProperty)
            if (body != null) {
                connection.doOutput = true
                connection.setFixedLengthStreamingMode(body.size)
                connection.outputStream.use { it.write(body) }
            }
            val status = connection.responseCode
            val responseBody = (if (status in 200..399) connection.inputStream else connection.errorStream)
                ?.use { it.readBytes() } ?: ByteArray(0)
            val headers = connection.headerFields.filterKeys { it != null }
            connection.disconnect()
            HttpResponse(status, headers, responseBody)
        } catch (error: IOException) {
            throw CloudBackupFailure.Network(error.message ?: "Google Drive could not be reached")
        } finally {
            // AuthorizationClient owns token caching; the token is never persisted here.
        }
    }

    private fun requireSuccess(response: HttpResponse) {
        if (response.status in 200..299) return
        val raw = response.bodyText()
        val reason = runCatching {
            val errors = JSONObject(raw).getJSONObject("error").optJSONArray("errors")
            errors?.optJSONObject(0)?.optString("reason")
        }.getOrNull().orEmpty()
        when {
            response.status == HttpURLConnection.HTTP_UNAUTHORIZED ->
                throw CloudBackupFailure.AuthorizationRequired("Google Drive authorization expired")
            response.status == HttpURLConnection.HTTP_FORBIDDEN && reason.contains("quota", ignoreCase = true) ->
                throw CloudBackupFailure.Quota()
            response.status == 429 || response.status >= 500 || reason.contains("rateLimit", ignoreCase = true) ->
                throw CloudBackupFailure.Network("Google Drive is temporarily unavailable")
            response.status == HttpURLConnection.HTTP_NOT_FOUND ->
                throw CloudBackupFailure.Remote("The selected cloud recovery point no longer exists")
            else -> throw CloudBackupFailure.Remote("Google Drive rejected the backup request (${response.status})")
        }
    }

    private fun parseRemote(json: JSONObject): CloudRemoteSnapshot? {
        val properties = json.optJSONObject("appProperties") ?: return null
        if (properties.optString("product") != PRODUCT_PROPERTY) return null
        val snapshotId = properties.optString("snapshot_id").takeIf(String::isNotBlank) ?: return null
        val vaultId = properties.optString("vault_id").takeIf(String::isNotBlank) ?: return null
        val createdAt = properties.optString("created_at").takeIf(String::isNotBlank)
            ?: json.optString("createdTime").takeIf(String::isNotBlank)
            ?: return null
        return runCatching {
            CloudRemoteSnapshot(
                remoteFileId = json.getString("id"),
                snapshotId = snapshotId,
                vaultId = vaultId,
                createdAt = createdAt,
                sizeBytes = json.optString("size", properties.optString("size_bytes", "0")).toLong(),
                sha256 = json.optString("sha256Checksum").takeIf(String::isNotBlank),
                name = json.optString("name", "saaf-milk_$snapshotId.milkbackup"),
            )
        }.getOrNull()
    }

    private fun remoteName(pending: CloudPendingSnapshot): String {
        val timestamp = FILE_TIME.format(Instant.parse(pending.createdAt))
        return "saaf-milk_${timestamp}_${pending.snapshotId}.milkbackup"
    }

    private fun validateRemoteId(value: String) {
        require(value.matches(Regex("[A-Za-z0-9_-]{10,200}"))) { "Invalid Google Drive file ID" }
    }

    private fun encodeQuery(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
    private fun encodePath(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.toString()).replace("+", "%20")

    private data class HttpResponse(
        val status: Int,
        val headers: Map<String?, List<String>>,
        val body: ByteArray,
    ) {
        fun bodyText(): String = body.toString(Charsets.UTF_8)
    }

    companion object {
        private const val DRIVE_API = "https://www.googleapis.com/drive/v3"
        private const val DRIVE_UPLOAD = "https://www.googleapis.com/upload/drive/v3"
        private const val CONTENT_TYPE = "application/octet-stream"
        private const val PRODUCT_PROPERTY = "saaf_milk_backup_v1"
        private const val FILE_FIELDS = "id,name,createdTime,size,sha256Checksum,appProperties"
        private const val CONNECT_TIMEOUT_MS = 20_000
        private const val READ_TIMEOUT_MS = 120_000
        private val FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC)
    }
}

class FixedCloudAccessTokenSource(private val token: String) : CloudAccessTokenSource {
    override suspend fun accessToken(): String = token
}
