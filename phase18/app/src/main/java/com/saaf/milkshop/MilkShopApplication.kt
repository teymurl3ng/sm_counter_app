package com.saaf.milkshop

import android.app.Application
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.cloud.CloudAccessTokenSource
import com.saaf.milkshop.cloud.CloudBackupCoordinator
import com.saaf.milkshop.cloud.CloudBackupCryptography
import com.saaf.milkshop.cloud.CloudBackupOutbox
import com.saaf.milkshop.cloud.CloudBackupScheduler
import com.saaf.milkshop.cloud.CloudBackupSettingsStore
import com.saaf.milkshop.cloud.GoogleDriveCloudBackupProvider
import com.saaf.milkshop.domain.NativeSessionManager
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.sync.BusinessClock
import com.saaf.milkshop.sync.SyncServerConfigStore
import com.saaf.milkshop.sync.SyncIdentity
import com.saaf.milkshop.sync.SyncRuntime
import com.saaf.milkshop.sync.SyncScheduler
import com.saaf.milkshop.sync.TimeTrustMonitor
import com.saaf.milkshop.sync.TrustedTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MilkShopApplication : Application() {
    private val timeTrustScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        runCatching {
            cloudBackupSettings.read()?.let { cloudBackupScheduler.apply(it.enabled, it.allowMetered) }
        }
        runCatching {
            SyncIdentity.warmUp(this)
        }
        runCatching {
            TrustedTime.warmUp(this)
            BusinessClock.warmUp(this)
        }
        runCatching {
            SyncScheduler.warmUp(this)
        }
        timeTrustScope.launch {
            runCatching { com.saaf.milkshop.statements.StatementExportStore(applicationContext).cleanup() }
            runCatching { TimeTrustMonitor.check(applicationContext, database) }
            // Enrolled devices push before pulling: a local edit reaches the
            // server before any pull can overwrite it (C1 decision record,
            // SYNC_GLOBAL_SEQ_C1_MIGRATION.md §5). The scheduler kick below
            // stays for retry scheduling; this drain already cleared the
            // queue, so it is normally a no-op.
            if (SyncServerConfigStore.load(applicationContext) != null) {
                runCatching { syncRuntime.drainOnce() }
                runCatching { syncRuntime.pullOnce() }
            }
        }
        // An enrolled device drains anything queued while it was offline at
        // every start; an unenrolled device schedules nothing (inert).
        SyncScheduler.requestPush()
    }

    val database: MilkShopDatabase by lazy {
        MilkShopDatabase.create(applicationContext)
    }
    val nativeSessions: NativeSessionManager by lazy { NativeSessionManager() }
    val userSecurity: UserSecurityService by lazy {
        UserSecurityService(database, nativeSessions)
    }
    val backupManager: BackupManager by lazy { BackupManager(applicationContext) }
    val syncRuntime: SyncRuntime by lazy { SyncRuntime(applicationContext, database) }
    val cloudBackupSettings: CloudBackupSettingsStore by lazy { CloudBackupSettingsStore(applicationContext) }
    val cloudBackupOutbox: CloudBackupOutbox by lazy { CloudBackupOutbox(applicationContext) }
    val cloudBackupCryptography: CloudBackupCryptography by lazy { CloudBackupCryptography() }
    val cloudBackupScheduler: CloudBackupScheduler by lazy { CloudBackupScheduler(applicationContext) }

    fun cloudBackupCoordinator(tokens: CloudAccessTokenSource): CloudBackupCoordinator = CloudBackupCoordinator(
        database = database,
        backupManager = backupManager,
        settings = cloudBackupSettings,
        outbox = cloudBackupOutbox,
        cryptography = cloudBackupCryptography,
        provider = GoogleDriveCloudBackupProvider(tokens),
    )

    @Volatile var currentSessionId: String? = null
}
