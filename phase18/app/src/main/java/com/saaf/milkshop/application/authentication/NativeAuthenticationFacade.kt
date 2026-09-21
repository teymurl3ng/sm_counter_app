package com.saaf.milkshop.application.authentication

import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.RolePermissions
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.quickunlock.QuickUnlockMethod
import com.saaf.milkshop.quickunlock.QuickUnlockService

data class NativeAccountSummary(
    val userId: Long,
    val loginName: String,
    val displayName: String,
    val role: AppRole,
    val quickPinEnabled: Boolean = false,
    val phoneSecurityEnabled: Boolean = false,
    val phoneSecurityLabel: String = "Phone security",
) {
    val hasQuickUnlock: Boolean get() = quickPinEnabled || phoneSecurityEnabled
}

data class NativeSignedInIdentity(
    val userId: Long,
    val loginName: String,
    val displayName: String,
    val role: AppRole,
    val effectivePermissions: Set<AppPermission>,
)

data class NativeAuthenticationStartup(
    val needsOwnerBootstrap: Boolean,
    val accounts: List<NativeAccountSummary>,
)

data class NativeAuthenticatedSession(
    val sessionHandle: String,
    val identity: NativeSignedInIdentity,
)

data class NativeOwnerBootstrap(
    val session: NativeAuthenticatedSession,
    val oneTimeRecoveryCode: String,
)

/** Typed native entry point around the existing security and Room authority. */
class NativeAuthenticationFacade(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
) {
    suspend fun startup(): NativeAuthenticationStartup {
        val needsOwner = security.needsOwnerBootstrap()
        return NativeAuthenticationStartup(
            needsOwnerBootstrap = needsOwner,
            accounts = if (needsOwner) emptyList() else activeAccounts(),
        )
    }

    suspend fun bootstrapOwner(
        loginName: String,
        displayName: String,
        passphrase: CharArray,
    ): NativeOwnerBootstrap = try {
        val result = security.bootstrapOwner(loginName, displayName, passphrase)
        NativeOwnerBootstrap(
            session = NativeAuthenticatedSession(
                sessionHandle = result.session.sessionId,
                identity = identity(result.session.sessionId),
            ),
            oneTimeRecoveryCode = result.recoveryCode,
        )
    } finally {
        passphrase.fill('\u0000')
    }

    suspend fun signIn(
        account: NativeAccountSummary,
        passphrase: CharArray,
    ): NativeAuthenticatedSession = try {
        val session = security.login(account.loginName, passphrase)
        NativeAuthenticatedSession(
            sessionHandle = session.sessionId,
            identity = identity(session.sessionId),
        )
    } finally {
        passphrase.fill('\u0000')
    }

    suspend fun restoreSession(sessionHandle: String): NativeAuthenticatedSession =
        NativeAuthenticatedSession(
            sessionHandle = sessionHandle,
            identity = identity(sessionHandle),
        )

    suspend fun signOut(sessionHandle: String) {
        security.logout(sessionHandle)
    }

    /**
     * Creates debug preview Admin / Cashier / Operator when missing.
     * Existing active accounts are never passphrase-reset.
     * Requires a session that can manage staff users.
     */
    suspend fun ensurePreviewOperator(sessionHandle: String): Boolean =
        ensurePreviewStaffAccounts(sessionHandle) > 0

    suspend fun ensurePreviewStaffAccounts(sessionHandle: String): Int =
        com.saaf.milkshop.application.preview.NativePreviewSampleData.ensurePreviewStaffAccounts(
            database = database,
            security = security,
            sessionHandle = sessionHandle,
        )

    suspend fun alignPreviewPassphrases() {
        com.saaf.milkshop.application.preview.NativePreviewSampleData.alignPreviewPassphrases(security)
    }

    suspend fun ensurePreviewVendor(): Boolean =
        com.saaf.milkshop.application.preview.NativePreviewSampleData.ensureSampleVendor(database)

    suspend fun ensurePreviewOperatorEmployeeLink(): Boolean =
        com.saaf.milkshop.application.preview.NativePreviewSampleData
            .ensurePreviewOperatorEmployeeLink(database)

    suspend fun signInPreviewOperator(): NativeAuthenticatedSession {
        alignPreviewPassphrases()
        val account = activeAccounts().firstOrNull {
            it.loginName == com.saaf.milkshop.application.preview.NativePreviewSampleData.PREVIEW_OPERATOR_LOGIN
        } ?: error(
            "Preview Operator is missing. Sign in as Owner once so the preview can create it, then try again.",
        )
        val passphrase = com.saaf.milkshop.application.preview.NativePreviewSampleData
            .PREVIEW_SHARED_PASSPHRASE.toCharArray()
        return signIn(account, passphrase)
    }

    suspend fun signInPreviewCashier(): NativeAuthenticatedSession {
        alignPreviewPassphrases()
        val sample = com.saaf.milkshop.application.preview.NativePreviewSampleData
        val account = activeAccounts().firstOrNull { it.loginName == sample.PREVIEW_CASHIER_LOGIN }
            ?: error(
                "Preview Cashier is missing. Sign in as Owner once so the preview can create it, then try again.",
            )
        return signIn(account, sample.PREVIEW_SHARED_PASSPHRASE.toCharArray())
    }

    suspend fun signInPreviewAdmin(): NativeAuthenticatedSession {
        alignPreviewPassphrases()
        val sample = com.saaf.milkshop.application.preview.NativePreviewSampleData
        val account = activeAccounts().firstOrNull { it.loginName == sample.PREVIEW_ADMIN_LOGIN }
            ?: error(
                "Preview Admin is missing. Sign in as Owner once so the preview can create it, then try again.",
            )
        return signIn(account, sample.PREVIEW_SHARED_PASSPHRASE.toCharArray())
    }

    /**
     * Debug/preview helper: bootstrap or sign in as Preview Owner for Overview.
     */
    suspend fun signInPreviewOwner(): NativeAuthenticatedSession {
        val sample = com.saaf.milkshop.application.preview.NativePreviewSampleData
        if (security.needsOwnerBootstrap()) {
            return bootstrapOwner(
                loginName = sample.PREVIEW_OWNER_LOGIN,
                displayName = sample.PREVIEW_OWNER_DISPLAY_NAME,
                passphrase = sample.PREVIEW_SHARED_PASSPHRASE.toCharArray(),
            ).session
        }
        alignPreviewPassphrases()
        val account = activeAccounts().firstOrNull { it.loginName == sample.PREVIEW_OWNER_LOGIN }
            ?: error(
                "Preview Owner (${sample.PREVIEW_OWNER_LOGIN}) is missing. " +
                    "Sign in as Owner from the account picker, or clear app data to bootstrap preview.owner.",
            )
        return signIn(account, sample.PREVIEW_SHARED_PASSPHRASE.toCharArray())
    }

    private suspend fun activeAccounts(): List<NativeAccountSummary> =
        database.appUserDao().all()
            .asSequence()
            .filter { it.status == UserSecurityService.STATUS_ACTIVE && !it.credentialSetupRequired }
            .map {
                NativeAccountSummary(
                    userId = it.userId,
                    loginName = it.loginName,
                    displayName = it.displayName,
                    role = AppRole.parse(it.role),
                )
            }
            .toList()

    private suspend fun identity(sessionHandle: String): NativeSignedInIdentity {
        val user = security.requirePermission(sessionHandle, AppPermission.VIEW_OPERATIONAL)
        val role = AppRole.parse(user.role)
        return NativeSignedInIdentity(
            userId = user.userId,
            loginName = user.loginName,
            displayName = user.displayName,
            role = role,
            effectivePermissions = RolePermissions.permissions(role),
        )
    }
}

enum class NativeQuickUnlockMethod {
    PHONE_SECURITY,
    PIN,
}

/** Activity-scoped adapter because Android phone security requires the current FragmentActivity. */
class NativeQuickUnlockFacade(
    private val quickUnlock: QuickUnlockService,
    private val authentication: NativeAuthenticationFacade,
) {
    suspend fun decorate(accounts: List<NativeAccountSummary>): List<NativeAccountSummary> {
        val status = quickUnlock.status()
        val enrollmentByLogin = status.accounts.associateBy { it.loginName }
        return accounts.map { account ->
            val enrollment = enrollmentByLogin[account.loginName]
            account.copy(
                quickPinEnabled = enrollment?.pinEnabled == true,
                phoneSecurityEnabled = enrollment?.deviceAuthenticationEnabled == true,
                phoneSecurityLabel = status.deviceAuthenticationLabel,
            )
        }
    }

    suspend fun signIn(
        account: NativeAccountSummary,
        method: NativeQuickUnlockMethod,
        pin: CharArray?,
    ): NativeAuthenticatedSession = try {
        val nativeSession = quickUnlock.login(
            loginName = account.loginName,
            method = when (method) {
                NativeQuickUnlockMethod.PHONE_SECURITY -> QuickUnlockMethod.DEVICE
                NativeQuickUnlockMethod.PIN -> QuickUnlockMethod.PIN
            },
            pin = pin,
        )
        try {
            authentication.restoreSession(nativeSession.sessionId)
        } catch (error: Throwable) {
            authentication.signOut(nativeSession.sessionId)
            throw error
        }
    } finally {
        pin?.fill('\u0000')
    }

    suspend fun fullPassphraseAccepted(userId: Long) {
        quickUnlock.fullPassphraseAccepted(userId)
    }

    suspend fun clearAll() {
        quickUnlock.clearAll()
    }
}
