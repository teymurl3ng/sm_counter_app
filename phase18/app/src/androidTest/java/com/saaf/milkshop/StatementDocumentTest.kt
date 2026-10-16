package com.saaf.milkshop

import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.core.content.FileProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.saaf.milkshop.application.owner.*
import com.saaf.milkshop.statements.*
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import java.io.File
import java.time.LocalDate

internal fun statementExportFixture(count: Int=1): CustomerStatement {
    val period=CustomerStatementPeriod(LocalDate.parse("2026-10-09"),LocalDate.parse("2026-10-15"))
    return CustomerStatement(7,"synthetic-customer","Amina Khan - امینہ","Northside Dairy",null,"Market Road","PKR",period,
        "2026-10-16T00:00:00Z",10000,10000+count*12500L,(1..count).map {i->
            CustomerStatementLine("2026-10-11","synthetic-reference-$i",if(i%3==0) "Correction / adjustment" else "Sale $i",
                if(i%3==0) null else "${if(i%2==0) "Yogurt 1 kg" else "Milk 2 L"} | Extra catalog product $i",12500,10000+i*12500L,false) })
}

class StatementDocumentTest {
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var engine: StatementDocumentEngine
    private lateinit var store: StatementExportStore
    @Before fun setup() {assertEquals("com.saaf.milkshop.qa.tests",context.packageName);engine=StatementDocumentEngine(context);store=StatementExportStore(context)}
    @Test fun measuredPlanKeepsEveryEntryAndRejectsUnsafeSizeOrTotals() {
        val fixture=statementExportFixture(45);val document=engine.plan(fixture)
        assertTrue(document.pages.size>1)
        val text=document.pages.flatMap {it.texts}.joinToString("\n") {it.text}
        fixture.lines.forEach { assertTrue(text.contains(it.reference)) }
        assertTrue(text.contains("PKR 5,725.00"));assertTrue(text.contains("امینہ"))
        assertTrue(document.pages.all {p->p.texts.all {it.y+it.height<=830 && it.x+it.width<=559}})
        assertTrue(runCatching {engine.plan(fixture.copy(closingPaisa=0))}.isFailure)
        assertTrue(runCatching {engine.plan(statementExportFixture(1500))}.exceptionOrNull()!!.message!!.contains("smaller period"))
        val wrapped=engine.plan(statementExportFixture().copy(customerName="A very long name ".repeat(60)))
        assertTrue(wrapped.pages.all {p->p.texts.all {it.y+it.height<=830}})
        val long=statementExportFixture().copy(customerName="A very long name ".repeat(400))
        assertTrue(runCatching {engine.plan(long)}.isFailure) // Excessive measured header must fail visibly.
    }
    @Test fun pdfAndPngUseIdenticalPagesAndProduceReadableSamples()=runBlocking {
        val out=File(context.getExternalFilesDir(null),"statement-export-samples").apply {mkdirs()}
        for((name,count) in listOf("short" to 1,"multi" to 20)) {
            val doc=engine.plan(statementExportFixture(count))
            val pdf=File(out,"$name.pdf");pdf.outputStream().use {engine.pdf(doc,it)}
            assertEquals("%PDF",pdf.inputStream().use {String(it.readNBytes(4),Charsets.US_ASCII)})
            PdfRenderer(ParcelFileDescriptor.open(pdf,ParcelFileDescriptor.MODE_READ_ONLY)).use {renderer->
                assertEquals(doc.pages.size,renderer.pageCount)
                for(index in setOf(0,doc.pages.lastIndex)) {
                    val png=engine.bitmap(doc,index)
                    File(out,"$name-page-${index+1}.png").outputStream().use {assertTrue(png.compress(Bitmap.CompressFormat.PNG,100,it))}
                    val rendered=Bitmap.createBitmap(png.width,png.height,Bitmap.Config.ARGB_8888)
                    rendered.eraseColor(StatementDocumentEngine.PAPER_COLOR)
                    renderer.openPage(index).use {it.render(rendered,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)}
                    File(out,"$name-pdf-page-${index+1}.png").outputStream().use {rendered.compress(Bitmap.CompressFormat.PNG,100,it)}
                    var difference=0L;var samples=0L
                    for(y in 0 until png.height step 3) for(x in 0 until png.width step 3) {
                        val a=png.getPixel(x,y);val b=rendered.getPixel(x,y)
                        difference+=kotlin.math.abs(android.graphics.Color.red(a)-android.graphics.Color.red(b))
                        difference+=kotlin.math.abs(android.graphics.Color.green(a)-android.graphics.Color.green(b))
                        difference+=kotlin.math.abs(android.graphics.Color.blue(a)-android.graphics.Color.blue(b));samples+=3
                    }
                    assertTrue("PDF/PNG render mismatch",difference.toDouble()/samples<8)
                    png.recycle();rendered.recycle()
                }
            }
        }
    }
    @Test fun cacheCleanupKeepsFreshAndActiveAndDeletesOnlyAgedOwnedExports()=runBlocking {
        val lease=store.export(engine.plan(statementExportFixture()),StatementExportFormat.PDF,engine)
        val root=File(context.cacheDir,StatementExportStore.DIRECTORY)
        val unrelated=File(root,"unrelated-directory").apply {mkdirs()};File(unrelated,"keep.txt").writeText("Unrelated")
        val future=System.currentTimeMillis()+StatementExportStore.RETENTION_MS+10000
        store.cleanup(future);assertTrue(lease.files.single().isFile)
        lease.close();store.cleanup();assertTrue(lease.files.single().isFile)
        store.cleanup(future);assertFalse(lease.directory.exists());assertTrue(unrelated.exists())
        unrelated.deleteRecursively()
        val full=File(root,java.util.UUID.randomUUID().toString()).apply {mkdirs()}
        try {
            java.io.RandomAccessFile(File(full,"quota-probe"),"rw").use {it.setLength(StatementExportStore.MAX_BYTES+1)}
            assertTrue(runCatching {store.export(engine.plan(statementExportFixture()),StatementExportFormat.PDF,engine)}.isFailure)
        } finally {full.deleteRecursively()}
    }
    @Test fun providerGrantsOnlySelectedFilesAndRejectsSiblingAndOutsidePaths()=runBlocking {
        val lease=store.export(engine.plan(statementExportFixture(20)),StatementExportFormat.Images,engine)
        try {
            val intent=store.shareIntent(lease,StatementExportFormat.Images)
            assertEquals(Intent.ACTION_SEND_MULTIPLE,intent.action);assertEquals("image/png",intent.type)
            assertEquals(lease.files.size,intent.clipData!!.itemCount)
            assertEquals(Intent.FLAG_GRANT_READ_URI_PERMISSION,intent.flags)
            val first=intent.clipData!!.getItemAt(0).uri;val sibling=intent.clipData!!.getItemAt(1).uri
            val device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            fun query(uri: android.net.Uri)=device.executeShellCommand("content query --user 0 --uri $uri")
            assertFalse(query(first).contains("Row: 0"))
            context.grantUriPermission("com.android.shell",first,Intent.FLAG_GRANT_READ_URI_PERMISSION)
            try {assertTrue(query(first).contains("Row: 0"));assertFalse(query(sibling).contains("Row: 0"))}
            finally {context.revokeUriPermission(first,Intent.FLAG_GRANT_READ_URI_PERMISSION)}
            val outside=File(context.cacheDir,"outside-statement.txt").apply {writeText("synthetic")}
            try {assertTrue(runCatching {FileProvider.getUriForFile(context,context.packageName+".statements",outside)}.isFailure)}
            finally {outside.delete()}
            val pdf=store.export(engine.plan(statementExportFixture()),StatementExportFormat.PDF,engine)
            try {
                val pdfIntent=store.shareIntent(pdf,StatementExportFormat.PDF);assertEquals(Intent.ACTION_SEND,pdfIntent.action);assertEquals("application/pdf",pdfIntent.type)
                assertTrue(pdf.files.single().delete())
                assertTrue(runCatching {store.shareIntent(pdf,StatementExportFormat.PDF)}.isFailure)
            }
            finally {pdf.discard()}
        } finally {lease.discard()}
    }
    @Test fun cancelledGenerationReleasesItsLeaseAndDoesNotLeavePartialFiles()=runBlocking {
        val root=File(context.cacheDir,StatementExportStore.DIRECTORY)
        val before=root.listFiles()?.map {it.name}?.toSet() ?: emptySet()
        val job=launch(Dispatchers.Default) {store.export(engine.plan(statementExportFixture(150)),StatementExportFormat.Images,engine)}
        withTimeout(10000) {while((root.listFiles()?.map {it.name}?.toSet() ?: emptySet())==before) delay(5)}
        job.cancelAndJoin()
        assertEquals(before,root.listFiles()?.map {it.name}?.toSet() ?: emptySet<String>())
    }
    @Test fun cancellationWhileReturningCompletedExportDiscardsUnclaimedLease()=runBlocking {
        val queue=java.util.concurrent.LinkedBlockingQueue<Runnable>()
        val dispatcher=object: CoroutineDispatcher() {
            override fun dispatch(context: kotlin.coroutines.CoroutineContext,block: Runnable) {queue.add(block)}
        }
        val root=File(context.cacheDir,StatementExportStore.DIRECTORY)
        val before=root.listFiles()?.map {it.name}?.toSet() ?: emptySet()
        var received=false
        val document=engine.plan(statementExportFixture())
        val job=launch(dispatcher) {store.export(document,StatementExportFormat.PDF,engine).use {received=true}}
        requireNotNull(queue.poll(10,java.util.concurrent.TimeUnit.SECONDS)).run() // Start caller; it suspends into IO.
        val returning=requireNotNull(queue.poll(10,java.util.concurrent.TimeUnit.SECONDS))
        assertTrue(root.listFiles()!!.any {it.name !in before && File(it,"statement.pdf").isFile})
        job.cancel();returning.run() // Cancel precisely before withContext delivers its completed value.
        withTimeout(10000) {while(!job.isCompleted) {queue.poll()?.run();delay(5)}}
        assertFalse(received)
        assertEquals(before,root.listFiles()?.map {it.name}?.toSet() ?: emptySet<String>())
    }
}
