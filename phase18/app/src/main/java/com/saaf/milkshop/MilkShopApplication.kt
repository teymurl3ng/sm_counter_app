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

class MilkShopApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        runCatching {
            cloudBackupSettings.read()?.let { cloudBackupScheduler.apply(it.enabled, it.allowMetered) }
        }
    }

    val database: MilkShopDatabase by lazy {
        MilkShopDatabase.create(applicationContext)
    }
    val nativeSessions: NativeSessionManager by lazy { NativeSessionManager() }
    val userSecurity: UserSecurityService by lazy {
        UserSecurityService(database, nativeSessions)
    }
    val backupManager: BackupManager by lazy { BackupManager(applicationContext) }
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
