package com.saaf.milkshop.hybrid

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.saaf.milkshop.BackupManager
import com.saaf.milkshop.application.bridge.BackupDocumentPicker
import com.saaf.milkshop.application.bridge.BridgeCommandConflict
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class HybridBackupDocumentPicker(
    private val activity: ComponentActivity,
) : BackupDocumentPicker, AutoCloseable {
    private var pendingCreate: CompletableDeferred<Uri?>? = null
    private var pendingOpen: CompletableDeferred<Uri?>? = null
    private val createDocument = activity.registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri -> pendingCreate?.complete(uri) }
    private val openDocument = activity.registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> pendingOpen?.complete(uri) }

    override suspend fun createDocument(suggestedFileName: String?): Uri? = withContext(Dispatchers.Main.immediate) {
        if (pendingCreate != null) throw BridgeCommandConflict("A backup file picker is already open")
        val result = CompletableDeferred<Uri?>()
        pendingCreate = result
        try {
            createDocument.launch(suggestedFileName ?: BackupManager.displayName(activity))
            result.await()
        } finally {
            pendingCreate = null
        }
    }

    override suspend fun openDocument(): Uri? = withContext(Dispatchers.Main.immediate) {
        if (pendingOpen != null) throw BridgeCommandConflict("A backup file picker is already open")
        val result = CompletableDeferred<Uri?>()
        pendingOpen = result
        try {
            openDocument.launch(arrayOf("application/octet-stream", "application/json", "*/*"))
            result.await()
        } finally {
            pendingOpen = null
        }
    }

    override fun close() {
        pendingCreate?.complete(null)
        pendingOpen?.complete(null)
        pendingCreate = null
        pendingOpen = null
    }
}
