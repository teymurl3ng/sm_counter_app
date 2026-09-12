package com.saaf.milkshop.quickunlock

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.security.keystore.UserNotAuthenticatedException
import android.util.AtomicFile
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import java.util.Base64
import java.util.Collections
import java.util.UUID
import java.util.concurrent.Executor
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal data class StoredCiphertext(
    val nonceBase64: String,
    val ciphertextBase64: String,
)

internal interface QuickUnlockRecordProtector {
    fun protect(plaintext: ByteArray): StoredCiphertext
    fun release(protected: StoredCiphertext): ByteArray
    fun clear()
}

internal class AndroidQuickUnlockRecordProtector(
    private val alias: String = KEY_ALIAS,
) : QuickUnlockRecordProtector {
    override fun protect(plaintext: ByteArray): StoredCiphertext {
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        cipher.updateAAD(RECORD_AAD)
        return StoredCiphertext(
            nonceBase64 = encode(cipher.iv),
            ciphertextBase64 = encode(cipher.doFinal(plaintext)),
        )
    }

    override fun release(protected: StoredCiphertext): ByteArray {
        val nonce = decode(protected.nonceBase64)
        require(nonce.size == NONCE_BYTES) { "Quick unlock state is damaged" }
        return try {
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, nonce))
            cipher.updateAAD(RECORD_AAD)
            cipher.doFinal(decode(protected.ciphertextBase64))
        } catch (_: AEADBadTagException) {
            throw QuickUnlockFailure.Invalidated()
        } finally {
            nonce.fill(0)
        }
    }

    override fun clear() {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }.deleteEntry(alias)
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_BITS)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    private fun encode(value: ByteArray): String = Base64.getEncoder().encodeToString(value)
    private fun decode(value: String): ByteArray = Base64.getDecoder().decode(value)

    companion object {
        private const val KEY_ALIAS = "saaf_milk_quick_unlock_records_v1"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_BITS = 256
        private const val NONCE_BYTES = 12
        private const val TAG_BITS = 128
        private val RECORD_AAD = "saaf-milk-quick-unlock-records-v1".toByteArray(Charsets.UTF_8)
    }
}

internal class AndroidQuickUnlockRecordStore(
    context: Context,
    private val protector: QuickUnlockRecordProtector = AndroidQuickUnlockRecordProtector(),
    directoryName: String = DIRECTORY,
) : QuickUnlockRecordStore {
    private val directory = File(context.noBackupFilesDir, directoryName).apply { mkdirs() }
    private val atomicFile = AtomicFile(File(directory, FILE_NAME))

    @Synchronized
    override fun read(): List<QuickUnlockRecord> {
        if (!atomicFile.baseFile.exists()) return emptyList()
        return try {
            val outer = atomicFile.openRead().use { JSONObject(it.readBytes().toString(Charsets.UTF_8)) }
            require(outer.getInt("version") == FILE_VERSION)
            val plaintext = protector.release(
                StoredCiphertext(
                    nonceBase64 = outer.getString("nonce"),
                    ciphertextBase64 = outer.getString("ciphertext"),
                ),
            )
            try {
                val records = JSONObject(plaintext.toString(Charsets.UTF_8)).getJSONArray("records")
                List(records.length()) { index -> record(records.getJSONObject(index)) }
            } finally {
                plaintext.fill(0)
            }
        } catch (_: Throwable) {
            clear()
            emptyList()
        }
    }

    @Synchronized
    override fun write(records: List<QuickUnlockRecord>) {
        if (records.isEmpty()) {
            atomicFile.delete()
            return
        }
        val plaintext = JSONObject().put(
            "records",
            JSONArray().apply { records.forEach { put(json(it)) } },
        ).toString().toByteArray(Charsets.UTF_8)
        val protected = try {
            protector.protect(plaintext)
        } finally {
            plaintext.fill(0)
        }
        val output = atomicFile.startWrite()
        try {
            output.write(
                JSONObject()
                    .put("version", FILE_VERSION)
                    .put("nonce", protected.nonceBase64)
                    .put("ciphertext", protected.ciphertextBase64)
                    .toString()
                    .toByteArray(Charsets.UTF_8),
            )
            output.fd.sync()
            atomicFile.finishWrite(output)
        } catch (error: Throwable) {
            atomicFile.failWrite(output)
            throw error
        }
    }

    @Synchronized
    override fun clear() {
        atomicFile.delete()
        runCatching(protector::clear)
    }

    private fun json(record: QuickUnlockRecord) = JSONObject()
        .put("user_id", record.userId)
        .put("login_name", record.loginName)
        .put("display_name", record.displayName)
        .put("security_binding", record.securityBindingBase64)
        .putNullable("device_key_id", record.deviceKeyId)
        .putNullable("device_nonce", record.deviceNonceBase64)
        .putNullable("device_ciphertext", record.deviceCiphertextBase64)
        .putNullable("device_token_hash", record.deviceTokenHashBase64)
        .put("biometric_canary_created", record.biometricCanaryCreated)
        .putNullable("pin_salt", record.pinSaltBase64)
        .putNullable("pin_verifier", record.pinVerifierBase64)
        .put("pin_failed_attempt_count", record.pinFailedAttemptCount)
        .putNullable("pin_locked_until_epoch_ms", record.pinLockedUntilEpochMs)
        .put("pin_requires_full_passphrase", record.pinRequiresFullPassphrase)

    private fun record(json: JSONObject) = QuickUnlockRecord(
        userId = json.getLong("user_id"),
        loginName = json.getString("login_name"),
        displayName = json.getString("display_name"),
        securityBindingBase64 = json.getString("security_binding"),
        deviceKeyId = json.optNullableString("device_key_id")?.also(UUID::fromString),
        deviceNonceBase64 = json.optNullableString("device_nonce"),
        deviceCiphertextBase64 = json.optNullableString("device_ciphertext"),
        deviceTokenHashBase64 = json.optNullableString("device_token_hash"),
        biometricCanaryCreated = json.getBoolean("biometric_canary_created"),
        pinSaltBase64 = json.optNullableString("pin_salt"),
        pinVerifierBase64 = json.optNullableString("pin_verifier"),
        pinFailedAttemptCount = json.getInt("pin_failed_attempt_count"),
        pinLockedUntilEpochMs = json.optNullableLong("pin_locked_until_epoch_ms"),
        pinRequiresFullPassphrase = json.getBoolean("pin_requires_full_passphrase"),
    )

    private fun JSONObject.putNullable(name: String, value: Any?) = put(name, value ?: JSONObject.NULL)
    private fun JSONObject.optNullableString(name: String): String? = if (isNull(name)) null else getString(name)
    private fun JSONObject.optNullableLong(name: String): Long? = if (isNull(name)) null else getLong(name)

    companion object {
        private const val DIRECTORY = "quick_unlock"
        private const val FILE_NAME = "state.json"
        private const val FILE_VERSION = 1
    }
}

class AndroidQuickUnlockDeviceGate(
    private val activity: FragmentActivity,
) : QuickUnlockDeviceGate {
    private val executor: Executor = ContextCompat.getMainExecutor(activity)
    private var pending: CancellableContinuation<BiometricPrompt.AuthenticationResult>? = null
    private val prompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                takePending()?.resume(result)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                val error = when (errorCode) {
                    BiometricPrompt.ERROR_CANCELED,
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON -> QuickUnlockFailure.Cancelled()
                    BiometricPrompt.ERROR_LOCKOUT,
                    BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> QuickUnlockFailure.RateLimited(
                        "Phone security is temporarily locked. Use your full Saaf passphrase.",
                    )
                    BiometricPrompt.ERROR_HW_NOT_PRESENT,
                    BiometricPrompt.ERROR_HW_UNAVAILABLE,
                    BiometricPrompt.ERROR_NO_BIOMETRICS,
                    BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL -> QuickUnlockFailure.Unavailable()
                    else -> QuickUnlockFailure.Unavailable("Phone security could not complete Quick unlock. Use your Quick PIN or full passphrase.")
                }
                takePending()?.resumeWithException(error)
            }
        },
    )

    override fun status(): DeviceAuthenticationStatus {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return DeviceAuthenticationStatus(false)
        val biometrics = BiometricManager.from(activity)
        val available = biometrics.canAuthenticate(ALLOWED_AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS
        val strongBiometricAvailable = biometrics.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG,
        ) == BiometricManager.BIOMETRIC_SUCCESS
        return DeviceAuthenticationStatus(
            available = available,
            label = if (strongBiometricAvailable) "Biometric or phone security" else "Phone security",
        )
    }

    override suspend fun protect(
        keyId: String,
        userId: Long,
        displayName: String,
        plaintext: ByteArray,
    ): DeviceProtectedQuickToken {
        requireAvailable()
        validateKeyId(keyId)
        delete(keyId)
        val key = createMainKey(keyId)
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, key)
        }
        val canary = createCanaryIfAvailable(keyId)
        return try {
            val authenticated = authenticate(
                title = "Set up Quick unlock",
                subtitle = "Confirm phone security for $displayName",
                cipher = cipher,
            )
            val authenticatedCipher = authenticated.cryptoObject?.cipher ?: throw QuickUnlockFailure.Unavailable()
            authenticatedCipher.updateAAD(aad(keyId, userId))
            DeviceProtectedQuickToken(
                nonceBase64 = encode(authenticatedCipher.iv),
                ciphertextBase64 = encode(authenticatedCipher.doFinal(plaintext)),
                biometricCanaryCreated = canary,
            )
        } catch (error: Throwable) {
            delete(keyId)
            throw error
        }
    }

    override suspend fun release(
        keyId: String,
        userId: Long,
        displayName: String,
        protected: DeviceProtectedQuickToken,
    ): ByteArray {
        requireAvailable()
        validateKeyId(keyId)
        if (!enrollmentStillValid(keyId, protected.biometricCanaryCreated)) throw QuickUnlockFailure.Invalidated()
        val nonce = decode(protected.nonceBase64)
        require(nonce.size == NONCE_BYTES) { "Quick unlock state is damaged" }
        return try {
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, mainKey(keyId), GCMParameterSpec(TAG_BITS, nonce))
            }
            val authenticated = authenticate(
                title = "Quick unlock Saaf Milk",
                subtitle = "Continue as $displayName",
                cipher = cipher,
            )
            val authenticatedCipher = authenticated.cryptoObject?.cipher ?: throw QuickUnlockFailure.Unavailable()
            authenticatedCipher.updateAAD(aad(keyId, userId))
            authenticatedCipher.doFinal(decode(protected.ciphertextBase64))
        } catch (_: KeyPermanentlyInvalidatedException) {
            throw QuickUnlockFailure.Invalidated()
        } catch (_: AEADBadTagException) {
            throw QuickUnlockFailure.Invalidated()
        } finally {
            nonce.fill(0)
        }
    }

    override fun enrollmentStillValid(keyId: String, biometricCanaryCreated: Boolean): Boolean {
        validateKeyId(keyId)
        if (!keyExists(mainAlias(keyId))) return false
        if (!keyCanInitialize(mainAlias(keyId))) return false
        val biometricAvailable = BiometricManager.from(activity).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
        if (!biometricCanaryCreated) return !biometricAvailable
        return keyExists(canaryAlias(keyId)) && keyCanInitialize(canaryAlias(keyId))
    }

    override fun delete(keyId: String) {
        validateKeyId(keyId)
        keyStore().apply {
            deleteEntry(mainAlias(keyId))
            deleteEntry(canaryAlias(keyId))
        }
    }

    override fun clearAll() {
        keyStore().apply {
            Collections.list(aliases()).filter { it.startsWith(KEY_PREFIX) }.forEach(::deleteEntry)
        }
    }

    override fun close() {
        synchronized(this) {
            pending?.cancel()
            pending = null
        }
        prompt.cancelAuthentication()
    }

    private suspend fun authenticate(title: String, subtitle: String, cipher: Cipher): BiometricPrompt.AuthenticationResult =
        withContext(Dispatchers.Main.immediate) {
            suspendCancellableCoroutine { continuation ->
            synchronized(this) {
                if (pending != null) {
                    continuation.resumeWithException(QuickUnlockFailure.Unavailable("Another phone-security check is already open"))
                    return@suspendCancellableCoroutine
                }
                pending = continuation
            }
            continuation.invokeOnCancellation {
                synchronized(this) {
                    if (pending === continuation) pending = null
                }
                executor.execute(prompt::cancelAuthentication)
            }
            prompt.authenticate(
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
                    .setConfirmationRequired(true)
                    .build(),
                BiometricPrompt.CryptoObject(cipher),
            )
        }
    }

    @Synchronized
    private fun takePending(): CancellableContinuation<BiometricPrompt.AuthenticationResult>? = pending.also { pending = null }

    private fun requireAvailable() {
        if (!status().available) throw QuickUnlockFailure.Unavailable()
    }

    private fun createMainKey(keyId: String): SecretKey = createKey(
        alias = mainAlias(keyId),
        authenticationTypes = KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL,
        invalidateForBiometricChange = false,
    )

    private fun createCanaryIfAvailable(keyId: String): Boolean {
        val available = BiometricManager.from(activity).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
        if (!available) return false
        createKey(
            alias = canaryAlias(keyId),
            authenticationTypes = KeyProperties.AUTH_BIOMETRIC_STRONG,
            invalidateForBiometricChange = true,
        )
        return true
    }

    private fun createKey(alias: String, authenticationTypes: Int, invalidateForBiometricChange: Boolean): SecretKey {
        check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_BITS)
                .setRandomizedEncryptionRequired(true)
                .setUserAuthenticationRequired(true)
                .setUserAuthenticationParameters(0, authenticationTypes)
                .setInvalidatedByBiometricEnrollment(invalidateForBiometricChange)
                .build(),
        )
        return generator.generateKey()
    }

    private fun mainKey(keyId: String): SecretKey = keyStore().getKey(mainAlias(keyId), null) as? SecretKey
        ?: throw QuickUnlockFailure.Invalidated()

    private fun keyCanInitialize(alias: String): Boolean = try {
        val key = keyStore().getKey(alias, null) as? SecretKey ?: return false
        Cipher.getInstance(CIPHER_TRANSFORMATION).init(Cipher.ENCRYPT_MODE, key)
        true
    } catch (_: UserNotAuthenticatedException) {
        true
    } catch (_: KeyPermanentlyInvalidatedException) {
        false
    } catch (_: Throwable) {
        false
    }

    private fun keyExists(alias: String): Boolean = runCatching { keyStore().containsAlias(alias) }.getOrDefault(false)
    private fun keyStore(): KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    private fun mainAlias(keyId: String) = "$KEY_PREFIX$keyId"
    private fun canaryAlias(keyId: String) = "$KEY_PREFIX${keyId}_biometric_canary"
    private fun validateKeyId(keyId: String) { UUID.fromString(keyId) }
    private fun aad(keyId: String, userId: Long) = "saaf-milk-quick-unlock-v1:$keyId:$userId".toByteArray(Charsets.UTF_8)
    private fun encode(value: ByteArray): String = Base64.getEncoder().encodeToString(value)
    private fun decode(value: String): ByteArray = Base64.getDecoder().decode(value)

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_PREFIX = "saaf_milk_quick_unlock_v1_"
        private const val KEY_BITS = 256
        private const val NONCE_BYTES = 12
        private const val TAG_BITS = 128
        private const val ALLOWED_AUTHENTICATORS =
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    }
}
