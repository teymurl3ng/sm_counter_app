package com.saaf.milkshop.quickunlock

import androidx.room3.withReadTransaction
import com.saaf.milkshop.data.AppUserEntity
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.data.SecurityAuditEntryEntity
import com.saaf.milkshop.data.UserCredentialEntity
import com.saaf.milkshop.domain.AppPasswordPolicy
import com.saaf.milkshop.domain.SecurityFailure
import com.saaf.milkshop.domain.UserSecurityService
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import java.util.UUID

enum class QuickUnlockMethod(val wireValue: String) {
    DEVICE("device"),
    PIN("pin");

    companion object {
        fun parse(value: String): QuickUnlockMethod = entries.singleOrNull { it.wireValue == value }
            ?: throw SecurityFailure.Validation("Choose phone security or Quick PIN")
    }
}

data class QuickUnlockAccount(
    val loginName: String,
    val displayName: String,
    val deviceAuthenticationEnabled: Boolean,
    val pinEnabled: Boolean,
)

data class QuickUnlockStatus(
    val deviceAuthenticationAvailable: Boolean,
    val deviceAuthenticationLabel: String,
    val accounts: List<QuickUnlockAccount>,
)

data class DeviceAuthenticationStatus(
    val available: Boolean,
    val label: String = "Phone security",
)

data class DeviceProtectedQuickToken(
    val nonceBase64: String,
    val ciphertextBase64: String,
    val biometricCanaryCreated: Boolean,
)

internal data class QuickUnlockRecord(
    val userId: Long,
    val loginName: String,
    val displayName: String,
    val securityBindingBase64: String,
    val deviceKeyId: String?,
    val deviceNonceBase64: String?,
    val deviceCiphertextBase64: String?,
    val deviceTokenHashBase64: String?,
    val biometricCanaryCreated: Boolean,
    val pinSaltBase64: String?,
    val pinVerifierBase64: String?,
    val pinFailedAttemptCount: Int = 0,
    val pinLockedUntilEpochMs: Long? = null,
    val pinRequiresFullPassphrase: Boolean = false,
)

internal interface QuickUnlockRecordStore {
    fun read(): List<QuickUnlockRecord>
    fun write(records: List<QuickUnlockRecord>)
    fun clear()
}

interface QuickUnlockDeviceGate : AutoCloseable {
    fun status(): DeviceAuthenticationStatus
    suspend fun protect(keyId: String, userId: Long, displayName: String, plaintext: ByteArray): DeviceProtectedQuickToken
    suspend fun release(keyId: String, userId: Long, displayName: String, protected: DeviceProtectedQuickToken): ByteArray
    fun enrollmentStillValid(keyId: String, biometricCanaryCreated: Boolean): Boolean
    fun delete(keyId: String)
    fun clearAll()
    override fun close() = Unit
}

sealed class QuickUnlockFailure(message: String) : IllegalArgumentException(message) {
    class Cancelled : QuickUnlockFailure("Quick unlock was cancelled")
    class Unavailable(message: String = "Phone security is unavailable. Use your Quick PIN or full passphrase.") : QuickUnlockFailure(message)
    class Invalidated : QuickUnlockFailure("Quick unlock needs to be set up again. Sign in with your full passphrase.")
    class RateLimited(message: String = "Quick unlock is paused. Sign in with your full passphrase.") : QuickUnlockFailure(message)
    class Authentication : QuickUnlockFailure("Quick unlock could not verify this account")
}

class QuickUnlockService internal constructor(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
    private val store: QuickUnlockRecordStore,
    private val deviceGate: QuickUnlockDeviceGate,
    private val pinHasher: QuickPinHasher = QuickPinHasher(),
    private val random: SecureRandom = SecureRandom(),
    private val nowEpochMs: () -> Long = System::currentTimeMillis,
) : AutoCloseable {
    private data class Identity(
        val user: AppUserEntity,
        val credential: UserCredentialEntity,
        val bindingBase64: String,
    )

    private val mutex = Mutex()

    suspend fun status(): QuickUnlockStatus = mutex.withLock {
        val records = validatedRecords()
        val deviceStatus = deviceGate.status()
        QuickUnlockStatus(
            deviceAuthenticationAvailable = deviceStatus.available,
            deviceAuthenticationLabel = deviceStatus.label,
            accounts = records.sortedWith(compareBy({ it.displayName.lowercase() }, { it.userId })).map {
                QuickUnlockAccount(
                    loginName = it.loginName,
                    displayName = it.displayName,
                    deviceAuthenticationEnabled = it.deviceKeyId != null,
                    pinEnabled = it.pinVerifierBase64 != null,
                )
            },
        )
    }

    suspend fun enroll(
        sessionId: String,
        currentPassphrase: CharArray,
        enableDeviceAuthentication: Boolean,
        pin: CharArray?,
    ): QuickUnlockStatus {
        if (!enableDeviceAuthentication && pin == null) {
            throw SecurityFailure.Validation("Choose phone security, a Quick PIN, or both")
        }
        pin?.let(pinHasher::validate)
        security.reauthenticate(sessionId, currentPassphrase)
        val initialIdentity = identityForSession(sessionId)
        val deviceStatus = deviceGate.status()
        if (!deviceStatus.available) {
            throw QuickUnlockFailure.Unavailable("Set up a secure phone lock before enabling Quick unlock")
        }

        val oldRecord = mutex.withLock { validatedRecords().singleOrNull { it.userId == initialIdentity.user.userId } }
        val deviceKeyId = if (enableDeviceAuthentication) UUID.randomUUID().toString() else null
        var protectedToken: DeviceProtectedQuickToken? = null
        var protectedTokenHash: ByteArray? = null
        var pinHash: PinHash? = null
        return try {
            if (deviceKeyId != null) {
                val token = ByteArray(DEVICE_TOKEN_BYTES).also(random::nextBytes)
                try {
                    protectedToken = deviceGate.protect(
                        keyId = deviceKeyId,
                        userId = initialIdentity.user.userId,
                        displayName = initialIdentity.user.displayName,
                        plaintext = token,
                    )
                    protectedTokenHash = sha256(token)
                } finally {
                    token.fill(0)
                }
            }
            if (pin != null) pinHash = pinHasher.hash(pin)

            val currentIdentity = identityForSession(sessionId)
            if (currentIdentity.bindingBase64 != initialIdentity.bindingBase64) throw QuickUnlockFailure.Invalidated()
            val tokenHash = protectedTokenHash
            val record = QuickUnlockRecord(
                userId = currentIdentity.user.userId,
                loginName = currentIdentity.user.loginName,
                displayName = currentIdentity.user.displayName,
                securityBindingBase64 = currentIdentity.bindingBase64,
                deviceKeyId = deviceKeyId,
                deviceNonceBase64 = protectedToken?.nonceBase64,
                deviceCiphertextBase64 = protectedToken?.ciphertextBase64,
                deviceTokenHashBase64 = tokenHash?.let(::encode),
                biometricCanaryCreated = protectedToken?.biometricCanaryCreated == true,
                pinSaltBase64 = pinHash?.salt?.let(::encode),
                pinVerifierBase64 = pinHash?.verifier?.let(::encode),
            )
            mutex.withLock {
                val records = validatedRecords().filterNot { it.userId == record.userId } + record
                store.write(records)
            }
            oldRecord?.deviceKeyId?.takeIf { it != deviceKeyId }?.let(deviceGate::delete)
            audit(currentIdentity.user, ACTION_ENROLL, RESULT_SUCCESS, "device=${deviceKeyId != null};pin=${pin != null}")
            status()
        } catch (error: Throwable) {
            deviceKeyId?.let(deviceGate::delete)
            throw error
        } finally {
            pinHash?.clear()
            protectedTokenHash?.fill(0)
        }
    }

    suspend fun remove(sessionId: String, currentPassphrase: CharArray): QuickUnlockStatus {
        security.reauthenticate(sessionId, currentPassphrase)
        val identity = identityForSession(sessionId)
        invalidateUser(identity.user.userId)
        audit(identity.user, ACTION_REMOVE, RESULT_SUCCESS, null)
        return status()
    }

    suspend fun login(loginName: String, method: QuickUnlockMethod, pin: CharArray?): com.saaf.milkshop.domain.NativeSession {
        val login = runCatching { AppPasswordPolicy.normalizeLoginName(loginName) }.getOrNull()
            ?: throw QuickUnlockFailure.Authentication()
        val record = mutex.withLock {
            validatedRecords().singleOrNull { it.loginName == login }
        } ?: throw QuickUnlockFailure.Invalidated()
        val identity = activeIdentity(record.userId) ?: run {
            invalidateUser(record.userId)
            throw QuickUnlockFailure.Invalidated()
        }
        val session = try {
            val strong = when (method) {
                QuickUnlockMethod.DEVICE -> authenticateDevice(record, identity)
                QuickUnlockMethod.PIN -> authenticatePin(record, pin)
            }
            mutex.withLock {
                requireCurrentEnrollment(record)
                security.sessionManager().start(identity.user.userId, strongAuthentication = strong)
            }
        } catch (error: Throwable) {
            auditFailure(identity.user, ACTION_LOGIN, method, error)
            throw error
        }
        audit(identity.user, ACTION_LOGIN, RESULT_SUCCESS, "method=${method.wireValue}")
        return session
    }

    suspend fun reauthenticate(sessionId: String, method: QuickUnlockMethod, pin: CharArray?) {
        val identity = identityForSession(sessionId)
        val record = mutex.withLock {
            validatedRecords().singleOrNull { it.userId == identity.user.userId }
        } ?: throw QuickUnlockFailure.Invalidated()
        val strong = try {
            val authenticatedStrongly = when (method) {
                QuickUnlockMethod.DEVICE -> authenticateDevice(record, identity)
                QuickUnlockMethod.PIN -> authenticatePin(record, pin)
            }
            mutex.withLock {
                requireCurrentEnrollment(record)
                security.sessionManager().markQuickAuthentication(sessionId, strongAuthentication = authenticatedStrongly)
            }
            authenticatedStrongly
        } catch (error: Throwable) {
            auditFailure(identity.user, ACTION_REAUTHENTICATE, method, error)
            throw error
        }
        audit(identity.user, ACTION_REAUTHENTICATE, RESULT_SUCCESS, "method=${method.wireValue};strong=$strong")
    }

    suspend fun fullPassphraseAccepted(userId: Long) = mutex.withLock {
        val records = validatedRecords()
        val current = records.singleOrNull { it.userId == userId } ?: return@withLock
        if (current.pinFailedAttemptCount != 0 || current.pinLockedUntilEpochMs != null || current.pinRequiresFullPassphrase) {
            store.write(records.map {
                if (it.userId == userId) it.copy(
                    pinFailedAttemptCount = 0,
                    pinLockedUntilEpochMs = null,
                    pinRequiresFullPassphrase = false,
                ) else it
            })
        }
    }

    suspend fun invalidateUser(userId: Long) = mutex.withLock {
        val records = store.read()
        val removed = records.singleOrNull { it.userId == userId }
        if (removed != null) {
            store.write(records.filterNot { it.userId == userId })
            removed.deviceKeyId?.let { runCatching { deviceGate.delete(it) } }
        }
    }

    suspend fun clearAll() = mutex.withLock {
        store.clear()
        runCatching(deviceGate::clearAll)
    }

    private suspend fun authenticateDevice(record: QuickUnlockRecord, identity: Identity): Boolean {
        val keyId = record.deviceKeyId ?: throw QuickUnlockFailure.Unavailable("Phone security is not enabled for this Saaf account")
        val nonce = record.deviceNonceBase64 ?: throw QuickUnlockFailure.Invalidated()
        val ciphertext = record.deviceCiphertextBase64 ?: throw QuickUnlockFailure.Invalidated()
        val expectedHash = record.deviceTokenHashBase64?.let(::decode) ?: throw QuickUnlockFailure.Invalidated()
        if (!deviceGate.enrollmentStillValid(keyId, record.biometricCanaryCreated)) {
            invalidateUser(record.userId)
            throw QuickUnlockFailure.Invalidated()
        }
        val token = deviceGate.release(
            keyId = keyId,
            userId = record.userId,
            displayName = identity.user.displayName,
            protected = DeviceProtectedQuickToken(nonce, ciphertext, record.biometricCanaryCreated),
        )
        var candidateHash: ByteArray? = null
        return try {
            candidateHash = sha256(token)
            if (!MessageDigest.isEqual(expectedHash, candidateHash)) {
                invalidateUser(record.userId)
                throw QuickUnlockFailure.Invalidated()
            }
            true
        } finally {
            token.fill(0)
            expectedHash.fill(0)
            candidateHash?.fill(0)
        }
    }

    private suspend fun authenticatePin(expectedRecord: QuickUnlockRecord, pin: CharArray?): Boolean = mutex.withLock {
        // Login and reauthentication share one atomic check/update of the latest attempt state.
        val record = requireCurrentEnrollment(expectedRecord)
        val entered = pin ?: throw SecurityFailure.Validation("Enter your Quick PIN")
        pinHasher.validate(entered)
        if (record.pinVerifierBase64 == null || record.pinSaltBase64 == null) {
            throw QuickUnlockFailure.Unavailable("A Quick PIN is not enabled for this Saaf account")
        }
        val now = nowEpochMs()
        if (record.pinRequiresFullPassphrase || record.pinLockedUntilEpochMs?.let { it > now } == true) {
            throw QuickUnlockFailure.RateLimited()
        }
        val salt = decode(record.pinSaltBase64)
        val expected = decode(record.pinVerifierBase64)
        val verified = try {
            pinHasher.verify(entered, salt, expected)
        } finally {
            salt.fill(0)
            expected.fill(0)
        }
        if (!verified) {
            val count = record.pinFailedAttemptCount + 1
            val blocked = count >= FULL_PASSPHRASE_AFTER_FAILURES
            val delay = when (count) {
                3 -> 30_000L
                4 -> 2 * 60_000L
                else -> null
            }
            val records = validatedRecords()
            store.write(records.map {
                if (it.userId == record.userId) it.copy(
                    pinFailedAttemptCount = count,
                    pinLockedUntilEpochMs = delay?.let { duration -> now + duration },
                    pinRequiresFullPassphrase = blocked,
                ) else it
            })
            if (blocked || delay != null) throw QuickUnlockFailure.RateLimited()
            throw QuickUnlockFailure.Authentication()
        }
        val records = validatedRecords()
        store.write(records.map {
            if (it.userId == record.userId) it.copy(
                pinFailedAttemptCount = 0,
                pinLockedUntilEpochMs = null,
                pinRequiresFullPassphrase = false,
            ) else it
        })
        false
    }

    /** Called under [mutex], including after any interactive authentication has completed. */
    private suspend fun requireCurrentEnrollment(expected: QuickUnlockRecord): QuickUnlockRecord {
        val current = validatedRecords().singleOrNull { it.userId == expected.userId }
            ?: throw QuickUnlockFailure.Invalidated()
        if (current.copy(
                pinFailedAttemptCount = expected.pinFailedAttemptCount,
                pinLockedUntilEpochMs = expected.pinLockedUntilEpochMs,
                pinRequiresFullPassphrase = expected.pinRequiresFullPassphrase,
            ) != expected
        ) {
            throw QuickUnlockFailure.Invalidated()
        }
        return current
    }

    private suspend fun identityForSession(sessionId: String): Identity {
        val session = security.sessionManager().require(sessionId, touch = false)
        return activeIdentity(session.userId) ?: run {
            security.sessionManager().logout(sessionId)
            throw SecurityFailure.SessionExpired()
        }
    }

    private suspend fun activeIdentity(userId: Long): Identity? = database.withReadTransaction {
        val user = database.appUserDao().findById(userId) ?: return@withReadTransaction null
        val credential = database.userCredentialDao().find(userId) ?: return@withReadTransaction null
        if (user.status != "active" || user.credentialSetupRequired) return@withReadTransaction null
        Identity(user, credential, securityBinding(user, credential))
    }

    private suspend fun validatedRecords(): List<QuickUnlockRecord> {
        val existing = store.read()
        val phoneSecurityAvailable = deviceGate.status().available
        val valid = mutableListOf<QuickUnlockRecord>()
        existing.forEach { record ->
            val identity = activeIdentity(record.userId)
            val deviceValid = record.deviceKeyId == null || deviceGate.enrollmentStillValid(
                record.deviceKeyId,
                record.biometricCanaryCreated,
            )
            if (phoneSecurityAvailable && identity != null && identity.bindingBase64 == record.securityBindingBase64 && deviceValid) {
                valid += record
            } else {
                record.deviceKeyId?.let(deviceGate::delete)
            }
        }
        if (valid.size != existing.size) store.write(valid)
        return valid
    }

    private fun securityBinding(user: AppUserEntity, credential: UserCredentialEntity): String {
        val digest = MessageDigest.getInstance("SHA-256")
        fun text(value: Any?) {
            digest.update((value?.toString() ?: "<null>").toByteArray(Charsets.UTF_8))
            digest.update(0)
        }
        text(user.userId)
        text(user.loginName)
        text(user.displayName)
        text(user.role)
        text(user.status)
        text(user.credentialSetupRequired)
        text(user.updatedAt)
        text(credential.algorithm)
        digest.update(credential.salt)
        digest.update(credential.verifier)
        text(credential.memoryKib)
        text(credential.iterations)
        text(credential.parallelism)
        text(credential.changedAt)
        text(credential.mustChange)
        return encode(digest.digest())
    }

    private suspend fun audit(user: AppUserEntity, action: String, result: String, details: String?) {
        database.securityAuditDao().insert(
            SecurityAuditEntryEntity(
                occurredAt = Instant.ofEpochMilli(nowEpochMs()).toString(),
                action = action,
                result = result,
                actorUserId = user.userId,
                actorNameSnapshot = user.displayName,
                actorRoleSnapshot = user.role,
                targetUserId = user.userId,
                details = details,
            ),
        )
    }

    private suspend fun auditFailure(user: AppUserEntity, action: String, method: QuickUnlockMethod, error: Throwable) {
        val reason = when (error) {
            is QuickUnlockFailure.Cancelled -> "cancelled"
            is QuickUnlockFailure.Unavailable -> "unavailable"
            is QuickUnlockFailure.Invalidated -> "invalidated"
            is QuickUnlockFailure.RateLimited -> "rate_limited"
            else -> "authentication_failed"
        }
        runCatching { audit(user, action, RESULT_FAILURE, "method=${method.wireValue};reason=$reason") }
    }

    private fun sha256(value: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(value)
    private fun encode(value: ByteArray): String = Base64.getEncoder().encodeToString(value)
    private fun decode(value: String): ByteArray = Base64.getDecoder().decode(value)

    override fun close() = deviceGate.close()

    companion object {
        private const val DEVICE_TOKEN_BYTES = 32
        private const val FULL_PASSPHRASE_AFTER_FAILURES = 5
        const val ACTION_ENROLL = "enroll_quick_unlock"
        const val ACTION_REMOVE = "remove_quick_unlock"
        const val ACTION_LOGIN = "quick_unlock_login"
        const val ACTION_REAUTHENTICATE = "quick_unlock_reauthenticate"
        private const val RESULT_SUCCESS = "success"
        private const val RESULT_FAILURE = "failure"
    }
}

internal data class PinHash(val salt: ByteArray, val verifier: ByteArray) {
    fun clear() {
        salt.fill(0)
        verifier.fill(0)
    }
}

internal class QuickPinHasher(
    private val random: SecureRandom = SecureRandom(),
) {
    fun validate(pin: CharArray) {
        if (pin.size !in MIN_PIN_LENGTH..MAX_PIN_LENGTH || pin.any { it !in '0'..'9' }) {
            throw SecurityFailure.Validation("Quick PIN must contain 4 to 12 digits. Six digits are recommended")
        }
    }

    fun hash(pin: CharArray): PinHash {
        validate(pin)
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        return PinHash(salt, derive(pin, salt))
    }

    fun verify(pin: CharArray, salt: ByteArray, expected: ByteArray): Boolean {
        val candidate = derive(pin, salt)
        return try {
            MessageDigest.isEqual(candidate, expected)
        } finally {
            candidate.fill(0)
        }
    }

    private fun derive(pin: CharArray, salt: ByteArray): ByteArray {
        val parameters = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13)
            .withSalt(salt)
            .withMemoryAsKB(MEMORY_KIB)
            .withIterations(ITERATIONS)
            .withParallelism(PARALLELISM)
            .build()
        return ByteArray(VERIFIER_BYTES).also { output ->
            Argon2BytesGenerator().apply { init(parameters) }.generateBytes(pin, output)
        }
    }

    companion object {
        const val MIN_PIN_LENGTH = 4
        const val MAX_PIN_LENGTH = 12
        private const val SALT_BYTES = 16
        private const val VERIFIER_BYTES = 32
        private const val MEMORY_KIB = 19_456
        private const val ITERATIONS = 2
        private const val PARALLELISM = 1
    }
}
