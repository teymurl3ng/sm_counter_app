package com.saaf.milkshop.data

import android.content.Context
import android.net.Uri
import com.saaf.milkshop.BackupManager

/**
 * Application-facing boundary for database-backed backup operations.
 * BackupManager remains authoritative for snapshot/restore semantics.
 */
class BackupDataRepository(
    context: Context,
) {
    private val manager = BackupManager(context)

    suspend fun createLocalSnapshot(database: MilkShopDatabase): Result<BackupManager.BackupMeta> =
        manager.createLocalSnapshot(database)

    suspend fun restore(
        database: MilkShopDatabase,
        uri: Uri,
        selection: BackupManager.RestoreSelection,
    ): Result<BackupManager.BackupMeta> = manager.restore(database, uri, selection)
}
