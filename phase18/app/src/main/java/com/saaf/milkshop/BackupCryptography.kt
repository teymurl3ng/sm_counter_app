package com.saaf.milkshop

import android.content.Context
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.saaf.milkshop.domain.Argon2idPasswordHasher
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import org.json.JSONObject
import java.io.IOException
import java.security.KeyStore
import java.security.SecureRandom
import java.text.Normalizer
import java.util.Base64
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

internal class BackupCryptography(private val context: Context) {
    companion object {
        private const val BACKUP_KDF_ALGORITHM = "argon2id"
        private const val BACKUP_CIPHER_ALGORITHM = "AES-256-GCM"
        private const val BACKUP_KEY_BYTES = 32
        private const val BACKUP_NONCE_BYTES = 12
        private const val BACKUP_TAG_BITS = 128
        private const val BACKUP_AAD = "saaf-milk-backup-v2"
        private const val LOCAL_BACKUP_AAD = "saaf-milk-local-backup-v2"
        private const val LOCAL_BACKUP_PROTECTION = "android-keystore"
        private const val LOCAL_BACKUP_KEY_ALIAS = "saaf_milk_local_backup_v2"
    }

    internal fun encryptFormat2(payload: ByteArray, passphrase: CharArray): ByteArray {
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
                put("format_version", BackupManager.FORMAT_VERSION)
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

    internal fun encryptLocalFormat2(payload: ByteArray): ByteArray {
        val key = localBackupKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        cipher.updateAAD(LOCAL_BACKUP_AAD.toByteArray(Charsets.UTF_8))
        val ciphertext = cipher.doFinal(payload)
        return JSONObject().apply {
            put("format_version", BackupManager.FORMAT_VERSION)
            put("protection", LOCAL_BACKUP_PROTECTION)
            put("cipher", JSONObject().apply {
                put("algorithm", BACKUP_CIPHER_ALGORITHM)
                put("nonce", Base64.getEncoder().encodeToString(cipher.iv))
                put("ciphertext", Base64.getEncoder().encodeToString(ciphertext))
            })
        }.toString(2).toByteArray(Charsets.UTF_8)
    }

    internal fun readLegacyOrLocalAndVerify(bytes: ByteArray): JSONObject {
        val outer = JSONObject(bytes.toString(Charsets.UTF_8))
        return if (outer.optString("protection") == LOCAL_BACKUP_PROTECTION) {
            readLocalAndVerify(bytes)
        } else {
            readAndVerify(bytes)
        }
    }

    internal fun readLocalAndVerify(bytes: ByteArray): JSONObject {
        val envelope = JSONObject(bytes.toString(Charsets.UTF_8))
        require(envelope.getInt("format_version") == BackupManager.FORMAT_VERSION) { "Local backup format is invalid" }
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

    internal fun readEncryptedAndVerify(bytes: ByteArray, passphrase: CharArray): JSONObject {
        val envelope = JSONObject(bytes.toString(Charsets.UTF_8))
        require(envelope.getInt("format_version") == BackupManager.FORMAT_VERSION) { "Encrypted backup format is invalid" }
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

    internal fun readUri(uri: Uri): ByteArray {
        if (uri.scheme == "file") {
            return java.io.File(uri.path ?: throw IOException("The backup file path is missing")).readBytes()
        }
        return context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IOException("The backup file could not be opened")
    }

}
