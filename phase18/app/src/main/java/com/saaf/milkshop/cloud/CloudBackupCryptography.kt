package com.saaf.milkshop.cloud

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.saaf.milkshop.BackupManager
import com.saaf.milkshop.domain.AppPasswordPolicy
import com.saaf.milkshop.domain.Argon2idPasswordHasher
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import org.json.JSONObject
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.SecureRandom
import java.text.Normalizer
import java.time.Instant
import java.util.Base64
import java.util.UUID
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class WrappedCiphertext(
    val nonceBase64: String,
    val ciphertextBase64: String,
)

data class PortableVaultWrap(
    val saltBase64: String,
    val memoryKib: Int,
    val iterations: Int,
    val parallelism: Int,
    val wrappedKey: WrappedCiphertext,
)

data class CloudVaultConfiguration(
    val vaultId: String,
    val portableWrap: PortableVaultWrap,
    val localWrap: WrappedCiphertext,
)

data class DecryptedCloudSnapshot(
    val snapshotId: String,
    val vaultId: String,
    val createdAt: String,
    val payload: ByteArray,
)

internal data class CloudSnapshotIdentity(
    val snapshotId: String,
    val vaultId: String,
    val createdAt: String,
)

interface CloudKeyProtector {
    fun wrap(plaintext: ByteArray, authenticatedData: ByteArray): WrappedCiphertext
    fun unwrap(wrapped: WrappedCiphertext, authenticatedData: ByteArray): ByteArray
}

class AndroidCloudKeyProtector(
    private val alias: String = KEY_ALIAS,
) : CloudKeyProtector {
    override fun wrap(plaintext: ByteArray, authenticatedData: ByteArray): WrappedCiphertext {
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        cipher.updateAAD(authenticatedData)
        return WrappedCiphertext(
            nonceBase64 = Base64.getEncoder().encodeToString(cipher.iv),
            ciphertextBase64 = Base64.getEncoder().encodeToString(cipher.doFinal(plaintext)),
        )
    }

    override fun unwrap(wrapped: WrappedCiphertext, authenticatedData: ByteArray): ByteArray {
        val nonce = Base64.getDecoder().decode(wrapped.nonceBase64)
        require(nonce.size == NONCE_BYTES) { "Invalid local cloud-key nonce" }
        return try {
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, nonce))
            cipher.updateAAD(authenticatedData)
            cipher.doFinal(Base64.getDecoder().decode(wrapped.ciphertextBase64))
        } catch (_: AEADBadTagException) {
            throw CloudBackupFailure.KeyUnavailable("The device cloud-backup key is unavailable or invalid")
        } catch (_: GeneralSecurityException) {
            throw CloudBackupFailure.KeyUnavailable("The device cloud-backup key is unavailable or invalid")
        } finally {
            nonce.fill(0)
        }
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_BYTES * 8)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    companion object {
        const val KEY_ALIAS = "saaf_milk_cloud_backup_v1"
        private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_BYTES = 32
        private const val NONCE_BYTES = 12
        private const val TAG_BITS = 128
    }
}

class CloudBackupCryptography(
    private val localProtector: CloudKeyProtector = AndroidCloudKeyProtector(),
    private val random: SecureRandom = SecureRandom(),
) {
    fun createVault(passphrase: CharArray): CloudVaultConfiguration {
        AppPasswordPolicy.validateBackupPassphrase(passphrase)
        val vaultId = UUID.randomUUID().toString()
        val vaultKey = randomBytes(KEY_BYTES)
        val salt = randomBytes(Argon2idPasswordHasher.SALT_BYTES)
        val wrappingKey = deriveKey(
            passphrase,
            salt,
            Argon2idPasswordHasher.MEMORY_KIB,
            Argon2idPasswordHasher.ITERATIONS,
            Argon2idPasswordHasher.PARALLELISM,
        )
        return try {
            val wrapAad = vaultWrapAad(vaultId)
            CloudVaultConfiguration(
                vaultId = vaultId,
                portableWrap = PortableVaultWrap(
                    saltBase64 = encode(salt),
                    memoryKib = Argon2idPasswordHasher.MEMORY_KIB,
                    iterations = Argon2idPasswordHasher.ITERATIONS,
                    parallelism = Argon2idPasswordHasher.PARALLELISM,
                    wrappedKey = encrypt(vaultKey, wrappingKey, wrapAad),
                ),
                localWrap = localProtector.wrap(vaultKey, wrapAad),
            )
        } finally {
            vaultKey.fill(0)
            wrappingKey.fill(0)
            salt.fill(0)
        }
    }

    fun encryptSnapshot(
        payload: ByteArray,
        configuration: CloudVaultConfiguration,
        snapshotId: String = UUID.randomUUID().toString(),
        createdAt: String = Instant.now().toString(),
    ): ByteArray {
        UUID.fromString(snapshotId)
        UUID.fromString(configuration.vaultId)
        val vaultKey = localProtector.unwrap(configuration.localWrap, vaultWrapAad(configuration.vaultId))
        return try {
            val ciphertextLength = payload.size + TAG_BYTES
            val aad = snapshotAad(
                snapshotId = snapshotId,
                vaultId = configuration.vaultId,
                createdAt = createdAt,
                ciphertextLength = ciphertextLength,
                portableWrap = configuration.portableWrap,
            )
            val encrypted = encrypt(payload, vaultKey, aad)
            check(Base64.getDecoder().decode(encrypted.ciphertextBase64).size == ciphertextLength)
            JSONObject().apply {
                put("cloud_envelope_version", CLOUD_ENVELOPE_VERSION)
                put("snapshot_id", snapshotId)
                put("vault_id", configuration.vaultId)
                put("created_at", createdAt)
                put("inner_format_version", BackupManager.FORMAT_VERSION)
                put("schema_version", BackupManager.CURRENT_SCHEMA_VERSION)
                put("ciphertext_length", ciphertextLength)
                put("kdf", portableKdfJson(configuration.portableWrap))
                put("wrapped_vault_key", wrappedCiphertextJson(configuration.portableWrap.wrappedKey))
                put("cipher", wrappedCiphertextJson(encrypted))
            }.toString(2).toByteArray(Charsets.UTF_8)
        } finally {
            vaultKey.fill(0)
        }
    }

    fun decryptSnapshot(bytes: ByteArray, passphrase: CharArray): DecryptedCloudSnapshot {
        val parsed = parseSnapshot(bytes)
        val salt = Base64.getDecoder().decode(parsed.portableWrap.saltBase64)
        val wrappingKey = deriveKey(
            passphrase,
            salt,
            parsed.portableWrap.memoryKib,
            parsed.portableWrap.iterations,
            parsed.portableWrap.parallelism,
        )
        val vaultKey = try {
            decrypt(parsed.portableWrap.wrappedKey, wrappingKey, vaultWrapAad(parsed.vaultId))
        } catch (_: AEADBadTagException) {
            throw CloudBackupFailure.WrongPassphraseOrTampered
        } finally {
            wrappingKey.fill(0)
            salt.fill(0)
        }
        val payload = try {
            decrypt(
                parsed.snapshotCipher,
                vaultKey,
                snapshotAad(
                    parsed.snapshotId,
                    parsed.vaultId,
                    parsed.createdAt,
                    parsed.ciphertextLength,
                    parsed.portableWrap,
                ),
            )
        } catch (_: AEADBadTagException) {
            throw CloudBackupFailure.WrongPassphraseOrTampered
        } finally {
            vaultKey.fill(0)
        }
        return DecryptedCloudSnapshot(parsed.snapshotId, parsed.vaultId, parsed.createdAt, payload)
    }

    fun decryptSnapshotWithLocalKey(
        bytes: ByteArray,
        configuration: CloudVaultConfiguration,
    ): DecryptedCloudSnapshot {
        val parsed = parseSnapshot(bytes)
        if (parsed.vaultId != configuration.vaultId) {
            throw CloudBackupFailure.KeyUnavailable(
                "This phone does not hold the local key for this backup vault",
            )
        }
        require(parsed.portableWrap == configuration.portableWrap) {
            "The cloud backup does not match the configured recovery key"
        }
        val vaultKey = localProtector.unwrap(
            configuration.localWrap,
            vaultWrapAad(configuration.vaultId),
        )
        val payload = try {
            decrypt(
                parsed.snapshotCipher,
                vaultKey,
                snapshotAad(
                    parsed.snapshotId,
                    parsed.vaultId,
                    parsed.createdAt,
                    parsed.ciphertextLength,
                    parsed.portableWrap,
                ),
            )
        } catch (_: AEADBadTagException) {
            throw CloudBackupFailure.KeyUnavailable("The device cloud-backup key could not authenticate this snapshot")
        } finally {
            vaultKey.fill(0)
        }
        return DecryptedCloudSnapshot(parsed.snapshotId, parsed.vaultId, parsed.createdAt, payload)
    }

    internal fun verifyLocalSnapshot(
        bytes: ByteArray,
        configuration: CloudVaultConfiguration,
    ): CloudSnapshotIdentity {
        val parsed = parseSnapshot(bytes)
        require(parsed.vaultId == configuration.vaultId) {
            "Pending cloud snapshot belongs to a different backup vault"
        }
        require(parsed.portableWrap == configuration.portableWrap) {
            "Pending cloud snapshot does not match the configured recovery key"
        }
        val vaultKey = localProtector.unwrap(
            configuration.localWrap,
            vaultWrapAad(configuration.vaultId),
        )
        val payload = try {
            decrypt(
                parsed.snapshotCipher,
                vaultKey,
                snapshotAad(
                    parsed.snapshotId,
                    parsed.vaultId,
                    parsed.createdAt,
                    parsed.ciphertextLength,
                    parsed.portableWrap,
                ),
            )
        } catch (_: AEADBadTagException) {
            throw CloudBackupFailure.KeyUnavailable("Pending cloud snapshot failed authentication")
        } finally {
            vaultKey.fill(0)
        }
        payload.fill(0)
        return CloudSnapshotIdentity(parsed.snapshotId, parsed.vaultId, parsed.createdAt)
    }

    private fun parseSnapshot(bytes: ByteArray): ParsedCloudSnapshot {
        val envelope = JSONObject(bytes.toString(Charsets.UTF_8))
        require(envelope.getInt("cloud_envelope_version") == CLOUD_ENVELOPE_VERSION) {
            "Unsupported cloud backup envelope"
        }
        val snapshotId = envelope.getString("snapshot_id").also(UUID::fromString)
        val vaultId = envelope.getString("vault_id").also(UUID::fromString)
        val createdAt = envelope.getString("created_at").also(Instant::parse)
        require(envelope.getInt("inner_format_version") == BackupManager.FORMAT_VERSION) {
            "Unsupported inner backup format"
        }
        require(envelope.getInt("schema_version") <= BackupManager.CURRENT_SCHEMA_VERSION) {
            "This cloud backup requires a newer app version"
        }
        val portableWrap = portableWrap(envelope)
        validateKdf(portableWrap)
        val ciphertextLength = envelope.getInt("ciphertext_length")
        val snapshotCipher = wrappedCiphertext(envelope.getJSONObject("cipher"))
        val snapshotCiphertext = Base64.getDecoder().decode(snapshotCipher.ciphertextBase64)
        try {
            require(ciphertextLength == snapshotCiphertext.size && ciphertextLength > TAG_BYTES) {
                "Cloud backup ciphertext length is invalid"
            }
        } finally {
            snapshotCiphertext.fill(0)
        }
        return ParsedCloudSnapshot(
            snapshotId = snapshotId,
            vaultId = vaultId,
            createdAt = createdAt,
            portableWrap = portableWrap,
            ciphertextLength = ciphertextLength,
            snapshotCipher = snapshotCipher,
        )
    }

    private fun validateKdf(wrap: PortableVaultWrap) {
        val salt = Base64.getDecoder().decode(wrap.saltBase64)
        require(salt.size >= Argon2idPasswordHasher.SALT_BYTES) { "Invalid cloud backup salt" }
        require(wrap.memoryKib in Argon2idPasswordHasher.MEMORY_KIB..262_144) { "Invalid cloud backup memory cost" }
        require(wrap.iterations in Argon2idPasswordHasher.ITERATIONS..10) { "Invalid cloud backup iteration count" }
        require(wrap.parallelism in Argon2idPasswordHasher.PARALLELISM..4) { "Invalid cloud backup parallelism" }
    }

    private fun portableWrap(envelope: JSONObject): PortableVaultWrap {
        val kdf = envelope.getJSONObject("kdf")
        require(kdf.getString("algorithm") == KDF_ALGORITHM) { "Unsupported cloud backup key derivation" }
        return PortableVaultWrap(
            saltBase64 = kdf.getString("salt"),
            memoryKib = kdf.getInt("memory_kib"),
            iterations = kdf.getInt("iterations"),
            parallelism = kdf.getInt("parallelism"),
            wrappedKey = wrappedCiphertext(envelope.getJSONObject("wrapped_vault_key")),
        )
    }

    private fun portableKdfJson(wrap: PortableVaultWrap) = JSONObject()
        .put("algorithm", KDF_ALGORITHM)
        .put("salt", wrap.saltBase64)
        .put("memory_kib", wrap.memoryKib)
        .put("iterations", wrap.iterations)
        .put("parallelism", wrap.parallelism)

    private fun wrappedCiphertextJson(value: WrappedCiphertext) = JSONObject()
        .put("algorithm", CIPHER_ALGORITHM)
        .put("nonce", value.nonceBase64)
        .put("ciphertext", value.ciphertextBase64)

    private fun wrappedCiphertext(json: JSONObject): WrappedCiphertext {
        require(json.getString("algorithm") == CIPHER_ALGORITHM) { "Unsupported cloud backup encryption" }
        val nonce = json.getString("nonce")
        require(Base64.getDecoder().decode(nonce).size == NONCE_BYTES) { "Invalid cloud backup nonce" }
        return WrappedCiphertext(nonce, json.getString("ciphertext"))
    }

    private fun encrypt(plaintext: ByteArray, key: ByteArray, aad: ByteArray): WrappedCiphertext {
        val nonce = randomBytes(NONCE_BYTES)
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
        cipher.updateAAD(aad)
        return WrappedCiphertext(encode(nonce), encode(cipher.doFinal(plaintext)))
    }

    @Throws(AEADBadTagException::class)
    private fun decrypt(value: WrappedCiphertext, key: ByteArray, aad: ByteArray): ByteArray {
        val nonce = Base64.getDecoder().decode(value.nonceBase64)
        val ciphertext = Base64.getDecoder().decode(value.ciphertextBase64)
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
        cipher.updateAAD(aad)
        return cipher.doFinal(ciphertext)
    }

    private fun deriveKey(
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
            ByteArray(KEY_BYTES).also { output ->
                Argon2BytesGenerator().apply { init(parameters) }.generateBytes(normalized, output)
            }
        } finally {
            normalized.fill('\u0000')
        }
    }

    private fun snapshotAad(
        snapshotId: String,
        vaultId: String,
        createdAt: String,
        ciphertextLength: Int,
        portableWrap: PortableVaultWrap,
    ): ByteArray = listOf(
        SNAPSHOT_AAD_PREFIX,
        CLOUD_ENVELOPE_VERSION.toString(),
        snapshotId,
        vaultId,
        createdAt,
        BackupManager.FORMAT_VERSION.toString(),
        BackupManager.CURRENT_SCHEMA_VERSION.toString(),
        ciphertextLength.toString(),
        portableWrap.saltBase64,
        portableWrap.memoryKib.toString(),
        portableWrap.iterations.toString(),
        portableWrap.parallelism.toString(),
        portableWrap.wrappedKey.nonceBase64,
        portableWrap.wrappedKey.ciphertextBase64,
    ).joinToString("|").toByteArray(Charsets.UTF_8)

    private fun vaultWrapAad(vaultId: String): ByteArray =
        "$VAULT_WRAP_AAD_PREFIX|$vaultId".toByteArray(Charsets.UTF_8)

    private fun randomBytes(size: Int) = ByteArray(size).also(random::nextBytes)
    private fun encode(value: ByteArray): String = Base64.getEncoder().encodeToString(value)

    private data class ParsedCloudSnapshot(
        val snapshotId: String,
        val vaultId: String,
        val createdAt: String,
        val portableWrap: PortableVaultWrap,
        val ciphertextLength: Int,
        val snapshotCipher: WrappedCiphertext,
    )

    companion object {
        const val CLOUD_ENVELOPE_VERSION = 1
        private const val KDF_ALGORITHM = "argon2id"
        private const val CIPHER_ALGORITHM = "AES-256-GCM"
        private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val SNAPSHOT_AAD_PREFIX = "saaf-milk-cloud-snapshot-v1"
        private const val VAULT_WRAP_AAD_PREFIX = "saaf-milk-cloud-vault-wrap-v1"
        private const val KEY_BYTES = 32
        private const val NONCE_BYTES = 12
        private const val TAG_BITS = 128
        private const val TAG_BYTES = TAG_BITS / 8
    }
}

sealed class CloudBackupFailure(message: String) : IllegalStateException(message) {
    class UserCancelled(message: String = "Cloud-backup recovery entry was cancelled") : CloudBackupFailure(message)
    data object WrongPassphraseOrTampered : CloudBackupFailure(
        "The cloud recovery passphrase is incorrect, or the backup was altered",
    )

    class KeyUnavailable(message: String) : CloudBackupFailure(message)
    class AuthorizationRequired(message: String = "Google Drive authorization is required") : CloudBackupFailure(message)
    class AccountMismatch(message: String = "The authorized Google account does not match this backup vault") : CloudBackupFailure(message)
    class Quota(message: String = "Google Drive storage quota prevents this backup") : CloudBackupFailure(message)
    class Network(message: String = "Google Drive could not be reached") : CloudBackupFailure(message)
    class Remote(message: String) : CloudBackupFailure(message)
    class NotConfigured(message: String = "Cloud backup has not been configured") : CloudBackupFailure(message)
}
