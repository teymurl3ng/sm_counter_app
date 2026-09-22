package com.saaf.milkshop.application.owner

import com.saaf.milkshop.MilkShopApplication
import com.saaf.milkshop.cloud.CloudBackupFailure
import com.saaf.milkshop.cloud.CloudBackupCryptography
import com.saaf.milkshop.cloud.CloudBackupSettingsStore
import com.saaf.milkshop.cloud.CloudBackupState
import com.saaf.milkshop.cloud.FixedCloudAccessTokenSource
import com.saaf.milkshop.cloud.GoogleDriveCloudBackupProvider
import com.saaf.milkshop.cloud.InteractiveCloudAuthorization
import java.time.Instant
import java.util.UUID

/**
 * Live Drive gateway: Google authorization Activity results + typed
 * [com.saaf.milkshop.cloud.CloudBackupCoordinator] configure / policy / run /
 * disconnect. Mirrors [com.saaf.milkshop.application.bridge.CloudBackupBridgeHandler]
 * without bridge JSON.
 */
class LiveNativeOwnerCloudBackupGateway(
    private val app: MilkShopApplication,
    private val authorization: InteractiveCloudAuthorization,
) : NativeOwnerCloudBackupGateway {
    override suspend fun connect(
        recoveryPassphrase: CharArray,
        allowMetered: Boolean,
        ownerUserId: Long,
        ownerName: String,
        ownerRole: String,
    ) {
        val authorizationResult = authorization.connect()
        val tokens = FixedCloudAccessTokenSource(authorizationResult.accessToken)
        val coordinator = app.cloudBackupCoordinator(tokens)
        val driveIdentity = GoogleDriveCloudBackupProvider(tokens).accountIdentity()
        if (driveIdentity.displayEmail.isNotBlank() &&
            !driveIdentity.displayEmail.equals(authorizationResult.selectedEmail, ignoreCase = true)
        ) {
            throw CloudBackupFailure.AccountMismatch(
                "Google sign-in and Drive authorization selected different accounts",
            )
        }
        val identity = driveIdentity.copy(displayEmail = authorizationResult.selectedEmail)
        val state = coordinator.configure(
            account = identity,
            recoveryPassphrase = recoveryPassphrase,
            allowMetered = allowMetered,
            ownerUserId = ownerUserId,
            ownerName = ownerName,
            ownerRole = ownerRole,
        )
        app.cloudBackupScheduler.apply(state.enabled, state.allowMetered)
        app.cloudBackupScheduler.runNow()
        authorization.requestNotificationPermission()
    }

    override suspend fun updatePolicy(enabled: Boolean, allowMetered: Boolean) {
        val state = localCoordinator().updatePolicy(enabled = enabled, allowMetered = allowMetered)
        app.cloudBackupScheduler.apply(state.enabled, state.allowMetered)
    }

    override suspend fun runNow() {
        val state = app.cloudBackupSettings.read()
            ?: throw CloudBackupFailure.NotConfigured()
        if (!state.enabled) {
            throw IllegalStateException("Enable automatic cloud backup before running it now")
        }
        app.cloudBackupScheduler.runNow()
    }

    override suspend fun disconnect(confirmRevoke: Boolean) {
        require(confirmRevoke) { "Confirm Google authorization revocation before disconnecting" }
        val state = app.cloudBackupSettings.read()
            ?: throw CloudBackupFailure.NotConfigured()
        authorization.revokeSelectedAccount(state.accountPermissionId)
        app.cloudBackupScheduler.cancelAll()
        localCoordinator().disconnectLocal()
    }

    private fun localCoordinator() = app.cloudBackupCoordinator(
        object : com.saaf.milkshop.cloud.CloudAccessTokenSource {
            override suspend fun accessToken(): String =
                throw CloudBackupFailure.AuthorizationRequired()
        },
    )
}

/**
 * Instrumentation / contract fake: mutates [CloudBackupSettingsStore] with a
 * real vault wrap via [CloudBackupCryptography], without Google OAuth.
 */
class FakeNativeOwnerCloudBackupGateway(
    private val settings: CloudBackupSettingsStore,
    private val cryptography: CloudBackupCryptography = CloudBackupCryptography(),
    private val now: () -> Instant = Instant::now,
) : NativeOwnerCloudBackupGateway {
    override suspend fun connect(
        recoveryPassphrase: CharArray,
        allowMetered: Boolean,
        ownerUserId: Long,
        ownerName: String,
        ownerRole: String,
    ) {
        require(settings.read() == null) {
            "Disconnect the current cloud-backup account before connecting another one"
        }
        val vault = cryptography.createVault(recoveryPassphrase)
        val instant = now().toString()
        settings.write(
            CloudBackupState(
                installationId = UUID.randomUUID().toString(),
                configuredAt = instant,
                enabled = true,
                allowMetered = allowMetered,
                accountPermissionId = "fake-permission",
                accountDisplay = "owner@example.com",
                enabledByUserId = ownerUserId,
                enabledByName = ownerName,
                enabledByRole = ownerRole,
                vault = vault,
            ),
        )
    }

    override suspend fun updatePolicy(enabled: Boolean, allowMetered: Boolean) {
        settings.update { it.copy(enabled = enabled, allowMetered = allowMetered) }
    }

    override suspend fun runNow() {
        val state = settings.read() ?: throw CloudBackupFailure.NotConfigured()
        if (!state.enabled) {
            throw IllegalStateException("Enable automatic cloud backup before running it now")
        }
        val attempted = now().toString()
        settings.write(
            state.copy(
                lastAttemptAt = attempted,
                lastSuccessAt = attempted,
                lastErrorCode = null,
                lastErrorMessage = null,
                pending = null,
            ),
        )
    }

    override suspend fun disconnect(confirmRevoke: Boolean) {
        require(confirmRevoke) { "Confirm Google authorization revocation before disconnecting" }
        settings.read() ?: throw CloudBackupFailure.NotConfigured()
        settings.clear()
    }
}
