package com.saaf.milkshop.domain

import androidx.room3.withWriteTransaction
import com.saaf.milkshop.data.AppUserEntity
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.data.SavedRecoveryCodeEntity
import com.saaf.milkshop.data.SecurityAuditEntryEntity
import com.saaf.milkshop.data.UserCredentialEntity
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.Normalizer
import java.time.Instant
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

enum class AppRole(val wireValue: String) {
    OWNER("owner"),
    ADMIN("admin"),
    MANAGER("manager"),
    CASHIER("cashier"),
    OPERATOR("operator");

    companion object {
        fun parse(value: String): AppRole = entries.singleOrNull { it.wireValue == value.trim().lowercase() }
            ?: throw SecurityFailure.Validation("Invalid application role")
    }
}

enum class AppPermission {
    VIEW_OPERATIONAL,
    VIEW_FULL_FINANCIAL,
    SALE_AND_COLLECTION,
    PURCHASE,
    SUPPLIER_PAYMENT,
    EXPENSE,
    SALARY_PAYMENT,
    PRODUCTION_AND_SPOILAGE,
    DELIVERY,
    MANAGE_MASTER_DATA,
    ATTENDANCE,
    CORRECTION,
    CASH_COUNT_AND_CLOSE,
    OWNER_DRAWING,
    REOPEN_DAY,
    MANAGE_BUSINESS_SETTINGS,
    CREATE_EXPORT_BACKUP,
    RESTORE_BACKUP,
    MANAGE_STAFF_USERS,
    MANAGE_PRIVILEGED_USERS,
    VIEW_SECURITY_AUDIT,
}

object RolePermissions {
    private val all = AppPermission.entries.toSet()
    private val mapping = mapOf(
        AppRole.OWNER to all,
        AppRole.ADMIN to all - AppPermission.RESTORE_BACKUP - AppPermission.MANAGE_PRIVILEGED_USERS,
        AppRole.MANAGER to setOf(
            AppPermission.VIEW_OPERATIONAL,
            AppPermission.VIEW_FULL_FINANCIAL,
            AppPermission.SALE_AND_COLLECTION,
            AppPermission.PURCHASE,
            AppPermission.SUPPLIER_PAYMENT,
            AppPermission.EXPENSE,
            AppPermission.SALARY_PAYMENT,
            AppPermission.PRODUCTION_AND_SPOILAGE,
            AppPermission.DELIVERY,
            AppPermission.MANAGE_MASTER_DATA,
            AppPermission.ATTENDANCE,
            AppPermission.CORRECTION,
            AppPermission.CASH_COUNT_AND_CLOSE,
        ),
        AppRole.CASHIER to setOf(
            AppPermission.VIEW_OPERATIONAL,
            AppPermission.SALE_AND_COLLECTION,
            AppPermission.EXPENSE,
            AppPermission.DELIVERY,
            AppPermission.CASH_COUNT_AND_CLOSE,
        ),
        AppRole.OPERATOR to setOf(
            AppPermission.VIEW_OPERATIONAL,
            AppPermission.PURCHASE,
            AppPermission.PRODUCTION_AND_SPOILAGE,
            AppPermission.DELIVERY,
            AppPermission.ATTENDANCE,
        ),
    )

    fun permissions(role: AppRole): Set<AppPermission> = mapping.getValue(role)
    fun allows(role: AppRole, permission: AppPermission): Boolean = permission in permissions(role)
}

sealed class SecurityFailure(message: String) : IllegalArgumentException(message) {
    class Validation(message: String) : SecurityFailure(message)
    class Authentication : SecurityFailure(GENERIC_AUTH_FAILURE)
    class SessionExpired : SecurityFailure("Your session expired. Sign in again.")
    // H5: optional stable reason code (e.g. ServerAccountPolicy.REASON_*)
    // so callers can distinguish grace expiry from role denial without new
    // failure types; null = plain role denial.
    class PermissionDenied(val reasonCode: String? = null) : SecurityFailure("You do not have permission to perform this action.")
    class RecentAuthenticationRequired : SecurityFailure("Enter your passphrase again to perform this sensitive action.")

    companion object {
        const val GENERIC_AUTH_FAILURE = "The login name or passphrase is incorrect, or this account is unavailable."
    }
}

data class PasswordHash(
    val algorithm: String,
    val salt: ByteArray,
    val verifier: ByteArray,
    val memoryKib: Int,
    val iterations: Int,
    val parallelism: Int,
)

class Argon2idPasswordHasher(
    private val secureRandom: SecureRandom = SecureRandom(),
    private val targetMemoryKib: Int = MEMORY_KIB,
    private val targetIterations: Int = ITERATIONS,
    private val targetParallelism: Int = PARALLELISM,
) {
    fun hash(passphrase: CharArray): PasswordHash {
        val salt = ByteArray(SALT_BYTES).also(secureRandom::nextBytes)
        val verifier = derive(passphrase, salt, targetMemoryKib, targetIterations, targetParallelism)
        return PasswordHash(ALGORITHM, salt, verifier, targetMemoryKib, targetIterations, targetParallelism)
    }

    fun verify(passphrase: CharArray, credential: UserCredentialEntity): Boolean {
        if (credential.algorithm != ALGORITHM) return false
        val candidate = derive(
            passphrase,
            credential.salt,
            credential.memoryKib,
            credential.iterations,
            credential.parallelism,
        )
        return try {
            MessageDigest.isEqual(candidate, credential.verifier)
        } finally {
            candidate.fill(0)
        }
    }

    fun needsUpgrade(credential: UserCredentialEntity): Boolean =
        credential.algorithm == ALGORITHM && (
            credential.memoryKib < targetMemoryKib ||
                credential.iterations < targetIterations ||
                credential.parallelism < targetParallelism ||
                credential.salt.size < SALT_BYTES ||
                credential.verifier.size < VERIFIER_BYTES
            )

    private fun derive(
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
            val output = ByteArray(VERIFIER_BYTES)
            Argon2BytesGenerator().apply { init(parameters) }.generateBytes(normalized, output)
            output
        } finally {
            normalized.fill('\u0000')
        }
    }

    companion object {
        const val ALGORITHM = "argon2id"
        const val MEMORY_KIB = 19_456
        const val ITERATIONS = 2
        const val PARALLELISM = 1
        const val SALT_BYTES = 16
        const val VERIFIER_BYTES = 32
    }
}

object AppPasswordPolicy {
    private val loginPattern = Regex("[a-z0-9._-]{3,32}")
    private val blocked = setOf(
        "passwordpassword",
        "password123456789",
        "123456789012345",
        "qwertyqwertyqwerty",
        "letmeinletmeinletmein",
        "adminadminadminadmin",
        "saafmilksaafmilk",
    )

    fun normalizeLoginName(value: String): String {
        val normalized = value.trim().lowercase()
        if (!loginPattern.matches(normalized)) {
            throw SecurityFailure.Validation("Login name must be 3-32 lowercase letters, numbers, dots, underscores, or hyphens")
        }
        return normalized
    }

    fun validateDisplayName(value: String): String = value.trim().also {
        if (it.isEmpty()) throw SecurityFailure.Validation("User display name is required")
        if (it.codePointCount(0, it.length) > 100) throw SecurityFailure.Validation("User display name is too long")
    }

    fun validatePassphrase(passphrase: CharArray, loginName: String) {
        val normalized = validateCommonPassphrase(passphrase)
        val lower = normalized.lowercase()
        if (lower == loginName || lower == loginName.repeat(5)) {
            throw SecurityFailure.Validation("Choose a less common or predictable passphrase")
        }
    }

    fun validateBackupPassphrase(passphrase: CharArray) {
        validateCommonPassphrase(passphrase)
    }

    private fun validateCommonPassphrase(passphrase: CharArray): String {
        val normalized = Normalizer.normalize(String(passphrase), Normalizer.Form.NFC)
        val length = normalized.codePointCount(0, normalized.length)
        if (length < 15) throw SecurityFailure.Validation("Passphrase must contain at least 15 characters")
        if (length > 128) throw SecurityFailure.Validation("Passphrase cannot exceed 128 characters")
        val lower = normalized.lowercase()
        if (lower in blocked) {
            throw SecurityFailure.Validation("Choose a less common or predictable passphrase")
        }
        return normalized
    }
}

data class NativeSession(
    val sessionId: String,
    val userId: Long,
    val issuedAtEpochMs: Long,
    val lastActivityEpochMs: Long,
    val lastFullAuthenticationEpochMs: Long?,
    val lastQuickAuthenticationEpochMs: Long,
)

class NativeSessionManager(
    private val nowEpochMs: () -> Long = System::currentTimeMillis,
) {
    private val sessions = ConcurrentHashMap<String, NativeSession>()
    @Volatile private var backgroundedAtEpochMs: Long? = null

    fun start(userId: Long, strongAuthentication: Boolean = true): NativeSession {
        val now = nowEpochMs()
        val session = NativeSession(
            sessionId = UUID.randomUUID().toString(),
            userId = userId,
            issuedAtEpochMs = now,
            lastActivityEpochMs = now,
            lastFullAuthenticationEpochMs = now.takeIf { strongAuthentication },
            lastQuickAuthenticationEpochMs = now,
        )
        sessions[session.sessionId] = session
        return session
    }

    fun require(sessionId: String, touch: Boolean = true): NativeSession {
        val session = sessions[sessionId] ?: throw SecurityFailure.SessionExpired()
        val now = nowEpochMs()
        if (now - session.lastActivityEpochMs >= INACTIVITY_TIMEOUT_MS ||
            now - session.issuedAtEpochMs >= OVERALL_TIMEOUT_MS ||
            backgroundedAtEpochMs?.let { now - it >= BACKGROUND_TIMEOUT_MS } == true
        ) {
            sessions.remove(sessionId)
            throw SecurityFailure.SessionExpired()
        }
        if (!touch) return session
        return session.copy(lastActivityEpochMs = now).also { sessions[sessionId] = it }
    }

    fun markFullAuthentication(sessionId: String) {
        val current = require(sessionId, touch = false)
        val now = nowEpochMs()
        sessions[sessionId] = current.copy(
            lastActivityEpochMs = now,
            lastFullAuthenticationEpochMs = now,
            lastQuickAuthenticationEpochMs = now,
        )
    }

    fun markQuickAuthentication(sessionId: String, strongAuthentication: Boolean) {
        val current = require(sessionId, touch = false)
        val now = nowEpochMs()
        sessions[sessionId] = current.copy(
            lastActivityEpochMs = now,
            lastFullAuthenticationEpochMs = if (strongAuthentication) now else current.lastFullAuthenticationEpochMs,
            lastQuickAuthenticationEpochMs = now,
        )
    }

    fun requireRecentAuthentication(sessionId: String, allowQuickPin: Boolean = false) {
        val session = require(sessionId)
        val authenticatedAt = if (allowQuickPin) {
            maxOf(session.lastQuickAuthenticationEpochMs, session.lastFullAuthenticationEpochMs ?: Long.MIN_VALUE)
        } else {
            session.lastFullAuthenticationEpochMs
        }
        if (authenticatedAt == null || nowEpochMs() - authenticatedAt >= RECENT_AUTH_TIMEOUT_MS) {
            throw SecurityFailure.RecentAuthenticationRequired()
        }
    }

    fun logout(sessionId: String) {
        sessions.remove(sessionId)
    }

    fun invalidateUser(userId: Long) {
        sessions.entries.removeIf { it.value.userId == userId }
    }

    fun invalidateAll() = sessions.clear()
    fun onBackground() { backgroundedAtEpochMs = nowEpochMs() }
    fun onForeground() {
        val backgroundedAt = backgroundedAtEpochMs
        if (backgroundedAt != null && nowEpochMs() - backgroundedAt >= BACKGROUND_TIMEOUT_MS) {
            invalidateAll()
        }
        backgroundedAtEpochMs = null
    }

    companion object {
        const val INACTIVITY_TIMEOUT_MS = 15 * 60 * 1000L
        const val OVERALL_TIMEOUT_MS = 12 * 60 * 60 * 1000L
        const val BACKGROUND_TIMEOUT_MS = 5 * 60 * 1000L
        const val RECENT_AUTH_TIMEOUT_MS = 5 * 60 * 1000L
    }
}

data class BootstrapOwnerResult(
    val user: AppUserEntity,
    val recoveryCode: String,
    val session: NativeSession,
)

data class CreateUserInput(
    val loginName: String,
    val displayName: String,
    val role: String,
    val employeeAccountId: Long? = null,
)

data class PendingUserEnrollment(
    val user: AppUserEntity,
    val enrollmentToken: String,
)

class UserSecurityService(
    private val database: MilkShopDatabase,
    private val sessions: NativeSessionManager = NativeSessionManager(),
    private val passwordHasher: Argon2idPasswordHasher = Argon2idPasswordHasher(),
    private val secureRandom: SecureRandom = SecureRandom(),
    private val nowEpochMs: () -> Long = System::currentTimeMillis,
) {
    private val enrollmentTokens = ConcurrentHashMap<String, Long>()
    private val authenticationEqualizer = dummyCredential
    suspend fun needsOwnerBootstrap(): Boolean = database.appUserDao().count() == 0L

    suspend fun bootstrapOwner(
        loginName: String,
        displayName: String,
        passphrase: CharArray,
    ): BootstrapOwnerResult {
        val login = AppPasswordPolicy.normalizeLoginName(loginName)
        val display = AppPasswordPolicy.validateDisplayName(displayName)
        AppPasswordPolicy.validatePassphrase(passphrase, login)
        val password = passwordHasher.hash(passphrase)
        val recoveryCode = generateRecoveryCode()
        val recovery = recoveryHash(recoveryCode)
        val now = timestamp()

        val user = database.withWriteTransaction {
            check(database.appUserDao().count() == 0L) { "First-owner setup has already been completed" }
            val id = database.appUserDao().insert(
                AppUserEntity(
                    loginName = login,
                    displayName = display,
                    role = AppRole.OWNER.wireValue,
                    createdAt = now,
                    updatedAt = now,
                )
            )
            database.userCredentialDao().save(password.toEntity(id, now))
            database.savedRecoveryCodeDao().insert(
                SavedRecoveryCodeEntity(userId = id, salt = recovery.first, verifier = recovery.second, createdAt = now)
            )
            val created = database.appUserDao().findById(id)!!
            audit(created, ACTION_BOOTSTRAP_OWNER, RESULT_SUCCESS, targetUserId = id)
            created
        }
        return BootstrapOwnerResult(user, recoveryCode, sessions.start(user.userId))
    }

    suspend fun login(loginName: String, passphrase: CharArray): NativeSession {
        val login = runCatching { AppPasswordPolicy.normalizeLoginName(loginName) }.getOrNull()
        val user = login?.let { database.appUserDao().findByLoginName(it) }
        val credential = user?.let { database.userCredentialDao().find(it.userId) }
        val now = nowEpochMs()
        val available = user != null && credential != null && user.status == STATUS_ACTIVE &&
            !user.credentialSetupRequired && credential.lockedUntilEpochMs?.let { it > now } != true
        val verified = passwordHasher.verify(passphrase, credential ?: authenticationEqualizer)
        if (!available || !verified) {
            if (available) {
                val activeUser = requireNotNull(user)
                val activeCredential = requireNotNull(credential)
                val count = activeCredential.failedAttemptCount + 1
                val lockedUntil = failureDelayMs(count)?.let { now + it }
                database.userCredentialDao().recordFailure(activeUser.userId, count, now, lockedUntil)
            }
            audit(user, ACTION_LOGIN, RESULT_FAILURE, REASON_AUTH_FAILED, targetUserId = user?.userId)
            throw SecurityFailure.Authentication()
        }

        if (passwordHasher.needsUpgrade(credential)) {
            val upgraded = passwordHasher.hash(passphrase)
            database.userCredentialDao().save(
                upgraded.toEntity(user.userId, timestamp(), mustChange = credential.mustChange)
            )
        } else {
            database.userCredentialDao().clearFailures(user.userId)
        }
        val session = sessions.start(user.userId)
        audit(user, ACTION_LOGIN, RESULT_SUCCESS, targetUserId = user.userId)
        return session
    }

    suspend fun logout(sessionId: String) {
        val actor = runCatching { actor(sessionId) }.getOrNull()
        sessions.logout(sessionId)
        audit(actor, ACTION_LOGOUT, RESULT_SUCCESS, targetUserId = actor?.userId)
    }

    suspend fun reauthenticate(sessionId: String, passphrase: CharArray) {
        val actor = actor(sessionId, touch = false)
        val credential = database.userCredentialDao().find(actor.userId) ?: throw SecurityFailure.Authentication()
        if (!passwordHasher.verify(passphrase, credential)) {
            audit(actor, ACTION_REAUTHENTICATE, RESULT_FAILURE, REASON_AUTH_FAILED, actor.userId)
            throw SecurityFailure.Authentication()
        }
        sessions.markFullAuthentication(sessionId)
        audit(actor, ACTION_REAUTHENTICATE, RESULT_SUCCESS, targetUserId = actor.userId)
    }

    suspend fun requirePermission(sessionId: String, permission: AppPermission): AppUserEntity {
        val actor = actor(sessionId)
        if (!RolePermissions.allows(AppRole.parse(actor.role), permission)) {
            audit(actor, ACTION_PERMISSION_DENIED, RESULT_FAILURE, permission.name, actor.userId)
            throw SecurityFailure.PermissionDenied()
        }
        // Account Bridge offline grace (frozen 7 days; spec §6 "writes
        // blocked, reads allowed"): a linked user whose verified server
        // entitlement has expired may READ (VIEW_* family) but not write.
        // H5 fix: the old gate blocked every permission — login itself died
        // on VIEW_OPERATIONAL. Unbound installations and non-verified link
        // states are unaffected; the grace clock is untouched here.
        val serverLink = database.serverAccountDao().findIdentityLink(actor.userId)
        if (serverLink != null && ServerAccountPolicy.graceWriteBlocked(serverLink, permission, nowEpochMs())) {
            audit(actor, ACTION_SERVER_VERIFICATION_EXPIRED, RESULT_FAILURE, ServerAccountPolicy.REASON_SERVER_GRACE_EXPIRED, actor.userId)
            throw SecurityFailure.PermissionDenied(ServerAccountPolicy.REASON_SERVER_GRACE_EXPIRED)
        }
        return actor
    }

    suspend fun beginUserEnrollment(
        sessionId: String,
        input: CreateUserInput,
    ): PendingUserEnrollment {
        val actor = actor(sessionId)
        val role = AppRole.parse(input.role)
        requireUserManagement(actor, role)
        if (role == AppRole.OWNER || role == AppRole.ADMIN) sessions.requireRecentAuthentication(sessionId)
        val login = AppPasswordPolicy.normalizeLoginName(input.loginName)
        val display = AppPasswordPolicy.validateDisplayName(input.displayName)
        val now = timestamp()
        val created = database.withWriteTransaction {
            val id = database.appUserDao().insert(
                AppUserEntity(
                    loginName = login,
                    displayName = display,
                    role = role.wireValue,
                    employeeAccountId = input.employeeAccountId,
                    credentialSetupRequired = true,
                    createdAt = now,
                    createdByUserId = actor.userId,
                    updatedAt = now,
                )
            )
            val created = database.appUserDao().findById(id)!!
            audit(actor, ACTION_CREATE_USER, RESULT_SUCCESS, targetUserId = id, details = role.wireValue)
            created
        }
        return PendingUserEnrollment(created, issueEnrollmentToken(created.userId))
    }

    suspend fun resumeUserEnrollment(sessionId: String, targetUserId: Long): PendingUserEnrollment {
        val actor = actor(sessionId)
        val target = database.appUserDao().findById(targetUserId)
            ?: throw SecurityFailure.Validation("User does not exist")
        requireUserManagement(actor, AppRole.parse(target.role))
        sessions.requireRecentAuthentication(sessionId)
        require(target.status == STATUS_ACTIVE && target.credentialSetupRequired) {
            "This user is not waiting for credential setup"
        }
        return PendingUserEnrollment(target, issueEnrollmentToken(target.userId))
    }

    suspend fun completeUserEnrollment(enrollmentToken: String, passphrase: CharArray): AppUserEntity {
        val userId = enrollmentTokens[enrollmentToken]
            ?: throw SecurityFailure.Authentication()
        val target = database.appUserDao().findById(userId)
            ?: throw SecurityFailure.Authentication()
        require(target.status == STATUS_ACTIVE && target.credentialSetupRequired) {
            "This credential setup is no longer available"
        }
        AppPasswordPolicy.validatePassphrase(passphrase, target.loginName)
        val hash = passwordHasher.hash(passphrase)
        val now = timestamp()
        val completed = database.withWriteTransaction {
            database.userCredentialDao().save(hash.toEntity(userId, now))
            database.appUserDao().update(target.copy(credentialSetupRequired = false, updatedAt = now))
            val updated = database.appUserDao().findById(userId)!!
            audit(null, ACTION_COMPLETE_ENROLLMENT, RESULT_SUCCESS, targetUserId = userId)
            updated
        }
        enrollmentTokens.remove(enrollmentToken)
        return completed
    }

    suspend fun changeRole(sessionId: String, targetUserId: Long, newRoleValue: String) {
        val actor = actor(sessionId)
        val target = database.appUserDao().findById(targetUserId) ?: throw SecurityFailure.Validation("User does not exist")
        val oldRole = AppRole.parse(target.role)
        val newRole = AppRole.parse(newRoleValue)
        requireUserManagement(actor, if (oldRole == AppRole.OWNER || newRole == AppRole.OWNER || oldRole == AppRole.ADMIN || newRole == AppRole.ADMIN) AppRole.OWNER else newRole)
        sessions.requireRecentAuthentication(sessionId)
        if (oldRole == AppRole.OWNER && newRole != AppRole.OWNER && database.appUserDao().activeOwnerCount() <= 1L) {
            throw SecurityFailure.Validation("The final active owner cannot be demoted")
        }
        val now = timestamp()
        database.withWriteTransaction {
            database.appUserDao().update(target.copy(role = newRole.wireValue, updatedAt = now))
            audit(
                actor,
                ACTION_CHANGE_ROLE,
                RESULT_SUCCESS,
                targetUserId = targetUserId,
                details = "${oldRole.wireValue}->${newRole.wireValue}",
            )
        }
        sessions.invalidateUser(targetUserId)
    }

    suspend fun setUserEnabled(sessionId: String, targetUserId: Long, enabled: Boolean) {
        val actor = actor(sessionId)
        val target = database.appUserDao().findById(targetUserId) ?: throw SecurityFailure.Validation("User does not exist")
        requireUserManagement(actor, AppRole.parse(target.role))
        sessions.requireRecentAuthentication(sessionId)
        if (!enabled && actor.userId == targetUserId) throw SecurityFailure.Validation("You cannot disable your current account")
        if (!enabled && target.role == AppRole.OWNER.wireValue && database.appUserDao().activeOwnerCount() <= 1L) {
            throw SecurityFailure.Validation("The final active owner cannot be disabled")
        }
        val now = timestamp()
        val updated = target.copy(
            status = if (enabled) STATUS_ACTIVE else STATUS_DISABLED,
            updatedAt = now,
            disabledAt = if (enabled) null else now,
            disabledByUserId = if (enabled) null else actor.userId,
        )
        database.withWriteTransaction {
            database.appUserDao().update(updated)
            audit(
                actor,
                if (enabled) ACTION_ENABLE_USER else ACTION_DISABLE_USER,
                RESULT_SUCCESS,
                targetUserId = targetUserId,
            )
        }
        sessions.invalidateUser(targetUserId)
    }

    suspend fun beginCredentialReset(sessionId: String, targetUserId: Long): PendingUserEnrollment {
        val actor = actor(sessionId)
        val target = database.appUserDao().findById(targetUserId) ?: throw SecurityFailure.Validation("User does not exist")
        requireUserManagement(actor, AppRole.parse(target.role))
        sessions.requireRecentAuthentication(sessionId)
        val now = timestamp()
        database.withWriteTransaction {
            database.appUserDao().update(target.copy(credentialSetupRequired = true, updatedAt = now))
            audit(actor, ACTION_RESET_CREDENTIAL, RESULT_SUCCESS, targetUserId = targetUserId)
        }
        sessions.invalidateUser(targetUserId)
        return PendingUserEnrollment(
            database.appUserDao().findById(targetUserId)!!,
            issueEnrollmentToken(targetUserId),
        )
    }

    suspend fun changeOwnPassphrase(sessionId: String, currentPassphrase: CharArray, newPassphrase: CharArray) {
        val actor = actor(sessionId)
        val current = database.userCredentialDao().find(actor.userId) ?: throw SecurityFailure.Authentication()
        if (!passwordHasher.verify(currentPassphrase, current)) throw SecurityFailure.Authentication()
        AppPasswordPolicy.validatePassphrase(newPassphrase, actor.loginName)
        val hash = passwordHasher.hash(newPassphrase)
        val now = timestamp()
        database.withWriteTransaction {
            database.userCredentialDao().save(hash.toEntity(actor.userId, now))
            audit(actor, ACTION_CHANGE_CREDENTIAL, RESULT_SUCCESS, targetUserId = actor.userId)
        }
        sessions.invalidateUser(actor.userId)
    }

    suspend fun recoverOwner(
        loginName: String,
        recoveryCode: String,
        newPassphrase: CharArray,
    ): Pair<NativeSession, String> {
        val login = runCatching { AppPasswordPolicy.normalizeLoginName(loginName) }.getOrNull()
            ?: throw SecurityFailure.Authentication()
        val user = database.appUserDao().findByLoginName(login)
        if (user == null || user.role != AppRole.OWNER.wireValue || user.status != STATUS_ACTIVE) {
            throw SecurityFailure.Authentication()
        }
        val saved = database.savedRecoveryCodeDao().current(user.userId) ?: throw SecurityFailure.Authentication()
        val nowMs = nowEpochMs()
        if (saved.lockedUntilEpochMs?.let { it > nowMs } == true || !verifyRecovery(recoveryCode, saved)) {
            val failures = saved.failedAttemptCount + 1
            database.savedRecoveryCodeDao().update(
                saved.copy(failedAttemptCount = failures, lockedUntilEpochMs = failureDelayMs(failures)?.let { nowMs + it })
            )
            audit(user, ACTION_RECOVER_OWNER, RESULT_FAILURE, REASON_AUTH_FAILED, user.userId)
            throw SecurityFailure.Authentication()
        }
        AppPasswordPolicy.validatePassphrase(newPassphrase, user.loginName)
        val password = passwordHasher.hash(newPassphrase)
        val replacementCode = generateRecoveryCode()
        val replacement = recoveryHash(replacementCode)
        val now = timestamp()
        database.withWriteTransaction {
            database.savedRecoveryCodeDao().update(saved.copy(usedAt = now))
            database.savedRecoveryCodeDao().insert(
                SavedRecoveryCodeEntity(userId = user.userId, salt = replacement.first, verifier = replacement.second, createdAt = now)
            )
            database.userCredentialDao().save(password.toEntity(user.userId, now))
            audit(user, ACTION_RECOVER_OWNER, RESULT_SUCCESS, targetUserId = user.userId)
        }
        sessions.invalidateUser(user.userId)
        return sessions.start(user.userId) to replacementCode
    }

    suspend fun listUsers(sessionId: String): List<AppUserEntity> {
        requirePermission(sessionId, AppPermission.MANAGE_STAFF_USERS)
        return database.appUserDao().all()
    }

    fun sessionManager(): NativeSessionManager = sessions

    private suspend fun actor(sessionId: String, touch: Boolean = true): AppUserEntity {
        val session = sessions.require(sessionId, touch)
        val user = database.appUserDao().findById(session.userId) ?: run {
            sessions.logout(sessionId)
            throw SecurityFailure.SessionExpired()
        }
        if (user.status != STATUS_ACTIVE || user.credentialSetupRequired) {
            sessions.logout(sessionId)
            throw SecurityFailure.SessionExpired()
        }
        return user
    }

    private fun requireUserManagement(actor: AppUserEntity, targetRole: AppRole) {
        val actorRole = AppRole.parse(actor.role)
        val permitted = when {
            targetRole == AppRole.OWNER || targetRole == AppRole.ADMIN -> actorRole == AppRole.OWNER
            else -> RolePermissions.allows(actorRole, AppPermission.MANAGE_STAFF_USERS)
        }
        if (!permitted) throw SecurityFailure.PermissionDenied()
    }

    private suspend fun audit(
        actor: AppUserEntity?,
        action: String,
        result: String,
        reasonCode: String? = null,
        targetUserId: Long? = null,
        details: String? = null,
    ) {
        database.securityAuditDao().insert(
            SecurityAuditEntryEntity(
                occurredAt = timestamp(),
                action = action,
                result = result,
                reasonCode = reasonCode,
                actorUserId = actor?.userId,
                actorNameSnapshot = actor?.displayName,
                actorRoleSnapshot = actor?.role,
                targetUserId = targetUserId,
                details = details,
            )
        )
    }

    private fun PasswordHash.toEntity(userId: Long, now: String, mustChange: Boolean = false) =
        UserCredentialEntity(
            userId = userId,
            algorithm = algorithm,
            salt = salt,
            verifier = verifier,
            memoryKib = memoryKib,
            iterations = iterations,
            parallelism = parallelism,
            changedAt = now,
            mustChange = mustChange,
        )

    private fun generateRecoveryCode(): String {
        val bytes = ByteArray(16).also(secureRandom::nextBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun issueEnrollmentToken(userId: Long): String = UUID.randomUUID().toString().also { token ->
        enrollmentTokens.entries.removeIf { it.value == userId }
        enrollmentTokens[token] = userId
    }

    private fun recoveryHash(code: String): Pair<ByteArray, ByteArray> {
        val salt = ByteArray(16).also(secureRandom::nextBytes)
        return salt to recoveryDigest(code, salt)
    }

    private fun verifyRecovery(code: String, saved: SavedRecoveryCodeEntity): Boolean =
        MessageDigest.isEqual(recoveryDigest(code, saved.salt), saved.verifier)

    private fun recoveryDigest(code: String, salt: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").apply {
            update(salt)
            update(code.toByteArray(StandardCharsets.US_ASCII))
        }.digest()

    private fun failureDelayMs(count: Int): Long? = when (count) {
        in 0..4 -> null
        5 -> 30_000L
        6 -> 60_000L
        7 -> 120_000L
        8 -> 300_000L
        9 -> 900_000L
        else -> 1_800_000L
    }

    private fun timestamp(): String = Instant.ofEpochMilli(nowEpochMs()).toString()

    companion object {
        private val dummyCredential: UserCredentialEntity by lazy {
            val dummyPassphrase = "non-user timing equalizer value".toCharArray()
            try {
                val hash = Argon2idPasswordHasher().hash(dummyPassphrase)
                UserCredentialEntity(
                    userId = Long.MIN_VALUE,
                    algorithm = hash.algorithm,
                    salt = hash.salt,
                    verifier = hash.verifier,
                    memoryKib = hash.memoryKib,
                    iterations = hash.iterations,
                    parallelism = hash.parallelism,
                    changedAt = "1970-01-01T00:00:00Z",
                )
            } finally {
                dummyPassphrase.fill('\u0000')
            }
        }

        const val STATUS_ACTIVE = "active"
        const val STATUS_DISABLED = "disabled"
        const val RESULT_SUCCESS = "success"
        const val RESULT_FAILURE = "failure"
        const val REASON_AUTH_FAILED = "authentication_failed"
        const val ACTION_BOOTSTRAP_OWNER = "bootstrap_owner"
        const val ACTION_LOGIN = "login"
        const val ACTION_LOGOUT = "logout"
        const val ACTION_REAUTHENTICATE = "reauthenticate"
        const val ACTION_PERMISSION_DENIED = "permission_denied"
        const val ACTION_CREATE_USER = "create_user"
        const val ACTION_COMPLETE_ENROLLMENT = "complete_enrollment"
        const val ACTION_CHANGE_ROLE = "change_role"
        const val ACTION_ENABLE_USER = "enable_user"
        const val ACTION_DISABLE_USER = "disable_user"
        const val ACTION_RESET_CREDENTIAL = "reset_credential"
        const val ACTION_CHANGE_CREDENTIAL = "change_credential"
        const val ACTION_RECOVER_OWNER = "recover_owner"
        const val ACTION_SERVER_ENROLLMENT = "server_enrollment"
        const val ACTION_SERVER_LINK = "server_link"
        const val ACTION_SERVER_UNLINK = "server_unlink"
        const val ACTION_SERVER_VERIFICATION = "server_verification"
        const val ACTION_SERVER_VERIFICATION_EXPIRED = "server_verification_expired"
        const val ACTION_SERVER_ENVIRONMENT_MISMATCH = "server_environment_mismatch"
        const val ACTION_SERVER_REMOTE_DISABLED = "server_remote_disabled"
        const val ACTION_SERVER_RESTORED_UNVERIFIED = "server_restored_unverified"
        const val ACTION_SERVER_SESSION_CLEARED = "server_session_cleared"
    }
}
