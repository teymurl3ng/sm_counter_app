package com.saaf.milkshop.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * Minimal HTTPS JSON seam for the sync transport, in the same zero-dependency
 * style as the Google Drive backup provider. Production uses
 * [UrlConnectionSyncHttp]; tests install fakes. Redaction rule (Account
 * Bridge spec §7): request/response bodies and headers are never logged —
 * callers carry tokens and keys.
 */
data class SyncHttpRequest(
    val method: String,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val body: ByteArray? = null,
) {
    override fun equals(other: Any?): Boolean = other is SyncHttpRequest &&
        other.method == method && other.url == url && other.headers == headers &&
        other.body?.contentEquals(body) == true

    override fun hashCode(): Int = method.hashCode() * 31 + url.hashCode()
}

data class SyncHttpResponse(
    val status: Int,
    val body: ByteArray?,
    /** Standard HTTP Date response header in epoch ms, when present. */
    val dateEpochMs: Long? = null,
) {
    fun bodyText(): String? = body?.toString(StandardCharsets.UTF_8)
}

fun interface SyncHttp {
    /** Throws [IOException] on transport-level failures (DNS, TLS, timeout). */
    suspend fun execute(request: SyncHttpRequest): SyncHttpResponse
}

class UrlConnectionSyncHttp(
    private val connectTimeoutMs: Int = 15_000,
    private val readTimeoutMs: Int = 30_000,
    /** Safety valve for runaway responses; pull batches stay far below this. */
    private val maxBodyBytes: Int = 5 * 1024 * 1024,
) : SyncHttp {

    override suspend fun execute(request: SyncHttpRequest): SyncHttpResponse =
        withContext(Dispatchers.IO) {
            val url = URL(request.url)
            require(url.protocol.equals("https", ignoreCase = true)) {
                "Sync transport is HTTPS-only: ${url.protocol}"
            }
            val connection = url.openConnection() as HttpURLConnection
            try {
                connection.requestMethod = request.method
                connection.connectTimeout = connectTimeoutMs
                connection.readTimeout = readTimeoutMs
                connection.instanceFollowRedirects = false
                request.headers.forEach { (name, value) -> connection.setRequestProperty(name, value) }
                request.body?.let { bytes ->
                    connection.doOutput = true
                    connection.setFixedLengthStreamingMode(bytes.size)
                    connection.outputStream.use { it.write(bytes) }
                }
                val status = connection.responseCode
                val dateEpochMs = connection.getHeaderFieldDate("Date", -1L).takeIf { it > 0 }
                val stream = if (status in 200..399) connection.inputStream else connection.errorStream
                val body = stream?.use { input ->
                    val buffer = java.io.ByteArrayOutputStream()
                    val chunk = ByteArray(8 * 1024)
                    while (true) {
                        val n = input.read(chunk)
                        if (n < 0) break
                        buffer.write(chunk, 0, n)
                        check(buffer.size() <= maxBodyBytes) { "Response body exceeds $maxBodyBytes bytes" }
                    }
                    if (buffer.size() == 0) null else buffer.toByteArray()
                }
                SyncHttpResponse(status, body, dateEpochMs)
            } finally {
                connection.disconnect()
            }
        }
}
