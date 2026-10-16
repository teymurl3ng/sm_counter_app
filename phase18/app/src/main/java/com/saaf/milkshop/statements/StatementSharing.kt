package com.saaf.milkshop.statements

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.coroutines.coroutineContext

class StatementFileProvider : FileProvider()

internal enum class StatementExportFormat(val label: String,val mime: String) { PDF("PDF","application/pdf"), Images("Images","image/png") }
internal class StatementExport internal constructor(val directory: File,val files: List<File>,private val store: StatementExportStore) : AutoCloseable {
    fun discard()=store.discard(this)
    override fun close()=store.release(this)
}

/** Private, opaque files. Only generated exports are reachable through the dedicated provider. */
internal class StatementExportStore(context: Context) {
    private val context=context.applicationContext
    private val root=File(this.context.cacheDir,DIRECTORY)
    private fun owned(file: File): Boolean = file.name.matches(UUID_NAME) && file.canonicalFile.parentFile==root.canonicalFile
    fun cleanup(now: Long=System.currentTimeMillis())=synchronized(guard) {
        root.listFiles()?.filter {owned(it) && it.canonicalPath !in active && now-it.lastModified()>=RETENTION_MS}?.forEach {it.deleteRecursively()}
    }
    suspend fun export(document: StatementDocument,format: StatementExportFormat,engine: StatementDocumentEngine): StatementExport {
        var ownedExport: StatementExport?=null
        try {return withContext(Dispatchers.IO) {exportMutex.withLock {
            cleanup()
            val directory=synchronized(guard) {
                check(root.isDirectory || root.mkdirs()) {"Temporary statement storage is unavailable."}
                check(totalBytes()<MAX_BYTES) {"Temporary statement storage is full. Try again later or choose a smaller period."}
                File(root,UUID.randomUUID().toString()).also {check(it.mkdir());active.add(it.canonicalPath)}
            }
            val job=coroutineContext
            val files=mutableListOf<File>()
            val lease=StatementExport(directory,files,this@StatementExportStore)
            ownedExport=lease
                fun budget() {
                    check(totalBytes()<=MAX_BYTES) {"Statement uses too much temporary space. Try PDF or choose a smaller period."}
                    job.ensureActive()
                }
                fun write(name: String,body: (java.io.OutputStream)->Unit) {
                    val partial=File(directory,"$name.partial")
                    partial.outputStream().buffered().use(body)
                    budget()
                    val final=File(directory,name)
                    check(partial.length()>0 && partial.renameTo(final)) {"Statement file could not be saved. Try again."}
                    files.add(final)
                }
                when(format) {
                    StatementExportFormat.PDF -> write("statement.pdf") {engine.pdf(document,it) {job.ensureActive()}}
                    StatementExportFormat.Images -> document.pages.indices.forEach {index->
                        budget();val bitmap=engine.bitmap(document,index) {job.ensureActive()}
                        try {write("page-${(index+1).toString().padStart(4,'0')}.png") {check(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it))}}
                        finally {bitmap.recycle()}
                    }
                }
                directory.setLastModified(System.currentTimeMillis())
                lease
        }}} catch(failure: Throwable) {
            // Cancellation can win while withContext is delivering an already completed export.
            // Keep ownership outside that boundary so the caller never inherits an invisible lease.
            withContext(NonCancellable+Dispatchers.IO) {ownedExport?.discard()}
            throw failure
        }
    }

    fun shareIntent(export: StatementExport,format: StatementExportFormat): Intent {
        require(export.files.isNotEmpty() && owned(export.directory) && export.files.all {
            it.isFile && it.length()>0 && it.canonicalFile.parentFile==export.directory.canonicalFile
        }) {"Statement files are no longer available. Refresh and prepare them again."}
        val uris=ArrayList(export.files.map {FileProvider.getUriForFile(context,context.packageName+".statements",it)})
        return Intent(if(uris.size==1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE).apply {
            type=format.mime
            if(uris.size==1) putExtra(Intent.EXTRA_STREAM,uris.single()) else putParcelableArrayListExtra(Intent.EXTRA_STREAM,uris)
            clipData=ClipData.newUri(context.contentResolver,"Customer statement",uris.first()).also {clip->uris.drop(1).forEach {clip.addItem(ClipData.Item(it))}}
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    fun release(export: StatementExport)=synchronized(guard) {active.remove(export.directory.canonicalPath);Unit}
    fun discard(export: StatementExport)=synchronized(guard) {
        require(owned(export.directory))
        export.directory.deleteRecursively();active.remove(export.directory.canonicalPath);Unit
    }
    private fun totalBytes()=root.listFiles()?.filter {owned(it)}?.sumOf {dir->dir.walkTopDown().filter {it.isFile}.sumOf {it.length()}} ?: 0L
    companion object {
        internal const val DIRECTORY="statement-exports-v1"
        internal const val RETENTION_MS=24*60*60*1000L
        internal const val MAX_BYTES=64*1024*1024L
        private val UUID_NAME=Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
        private val guard=Any()
        private val active=mutableSetOf<String>()
        private val exportMutex=Mutex()
    }
}
