package com.saaf.milkshop.sync

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Server session tokens (Account Bridge spec §7): Android-Keystore-wrapped
 * AES-GCM file in noBackupFilesDir, never Room, never prefs, never logs,
 * never backups. Tokens are replaceable cache material — per the DEV probe
 * findings they are NOT authorization (a JWT outlives logout until natural
 * expiry); clearing them is a hygiene operation, not a security control.
 */
class SyncTokenStore(context: Context) {

    data class Session(
        val accessToken: String,
        val refreshToken: String?,
        val accessTokenExpiresAtEpochMs: Long?,
    )

    private val appContext = context.applicationContext

    fun save(session: Session) {
        val plain = JSONObject()
            .put("access_token", session.accessToken)
            .apply {
                session.refreshToken?.let { put("refresh_token", it) }
                session.accessTokenExpiresAtEpochMs?.let { put("access_token_expires_at_epoch_ms", it) }
            }
            .toString()
            .toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val ciphertext = cipher.iv + cipher.doFinal(plain)
        val target = file(appContext)
        target.parentFile?.let { dir -> check(dir.isDirectory || dir.mkdirs()) }
        target.writeBytes(ciphertext)
        plain.fill(0)
    }

    fun load(): Session? {
        val bytes = runCatching { file(appContext).readBytes() }.getOrNull() ?: return null
        if (bytes.size <= IV_BYTES) return null
        return runCatching {
            val iv = bytes.copyOfRange(0, IV_BYTES)
            val ciphertext = bytes.copyOfRange(IV_BYTES, bytes.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, iv))
            val plain = cipher.doFinal(ciphertext)
            val json = JSONObject(plain.toString(Charsets.UTF_8))
            plain.fill(0)
            Session(
                accessToken = json.getString("access_token"),
                refreshToken = if (json.has("refresh_token")) json.getString("refresh_token") else null,
                accessTokenExpiresAtEpochMs =
                    if (json.has("access_token_expires_at_epoch_ms")) json.getLong("access_token_expires_at_epoch_ms") else null,
            )
        }.getOrNull()
    }

    fun clear() {
        runCatching { file(appContext).delete() }
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    private fun file(context: Context): File =
        File(File(context.noBackupFilesDir, "sync"), "session.bin")

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "saaf_milk_sync_session_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
    }
}
