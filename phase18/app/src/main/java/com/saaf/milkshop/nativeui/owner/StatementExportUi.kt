package com.saaf.milkshop.nativeui.owner

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import com.saaf.milkshop.nativeui.common.*
import com.saaf.milkshop.nativeui.theme.*
import com.saaf.milkshop.statements.*
import com.saaf.milkshop.application.owner.*
import kotlinx.coroutines.*

@Composable
internal fun StatementPageView(document: StatementDocument,page: Int,engine: StatementDocumentEngine,modifier: Modifier=Modifier) {
    var bitmap by remember(document,page) {mutableStateOf<Bitmap?>(null)}
    var failure by remember(document,page) {mutableStateOf(false)}
    var textOpen by rememberSaveable(page) {mutableStateOf(false)}
    LaunchedEffect(document,page) {
        bitmap=null;failure=false
        try {bitmap=withContext(Dispatchers.Default) {engine.bitmap(document,page)}}
        catch(cancelled: CancellationException) {throw cancelled}
        catch(error: Exception) {failure=true;textOpen=true}
    }
    Column(modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(StatementDocumentEngine.WIDTH.toFloat()/StatementDocumentEngine.HEIGHT)
            .border(1.dp,Color(0xFFC9C3B3)).testTag("statement-paper")) {
            bitmap?.let {Image(it.asImageBitmap(),"Statement page ${page+1} of ${document.pages.size}",Modifier.fillMaxSize())}
            if(bitmap==null) Text(if(failure) "Image preview could not load. Read the statement text below." else "Rendering page…",Modifier.padding(16.dp))
        }
        TextButton(onClick=hapticAction {textOpen=!textOpen},modifier=Modifier.testTag("statement-read-text")) {Text(if(textOpen) "Close statement text" else "Read statement text")}
        if(textOpen) NativeInformationSection(Modifier.testTag("statement-page-text")) {
            document.pages[page].texts.forEach {block->
                Text(block.text,style=when(block.role) {
                    StatementTextRole.Brand,StatementTextRole.Heading->nativeSectionTextStyle()
                    StatementTextRole.Metric->nativeMetricTextStyle()
                    else->MaterialTheme.typography.bodyLarge
                },color=if(block.role==StatementTextRole.Metric) nativeVisualColors.metric else MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

/** No send/delivery inference: success means only that Android accepted the chooser launch. */
@Composable
internal fun StatementShareControls(document: StatementDocument,engine: StatementDocumentEngine,enabled: Boolean,
    load: CustomerStatementLoader,onRefresh: ()->Unit,onBusy: (Boolean)->Unit={},launcher: ((Intent)->Unit)?=null,
    record: suspend (CustomerCommunicationAction)->Unit={}) {
    val context=LocalContext.current
    val store=remember(context.applicationContext) {StatementExportStore(context)}
    val scope=rememberCoroutineScope()
    var dialog by remember {mutableStateOf(false)}
    var format by rememberSaveable {mutableStateOf(StatementExportFormat.PDF)}
    var busy by remember {mutableStateOf(false)}
    var notice by remember {mutableStateOf<String?>(null)}
    var job by remember {mutableStateOf<Job?>(null)}
    var generation by remember {mutableIntStateOf(0)}
    LaunchedEffect(Unit) {withContext(Dispatchers.IO) {runCatching {store.cleanup()}}}
    fun cancel() {generation++;job?.cancel();busy=false;onBusy(false);dialog=false;notice="Sharing cancelled. No share menu opened."}
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Button(onClick=hapticAction {notice=null;dialog=true},enabled=enabled && !busy,
            modifier=Modifier.fillMaxWidth().testTag("statement-share")) {Text("Share statement")}
        Box(Modifier.fillMaxWidth().height(4.dp)) {if(busy) LinearProgressIndicator(Modifier.fillMaxWidth().testTag("statement-export-busy"))}
        notice?.let {Text(it,style=MaterialTheme.typography.bodyMedium,modifier=Modifier.testTag("statement-share-notice").semantics {liveRegion=LiveRegionMode.Polite})}
    }
    if(dialog) AlertDialog(onDismissRequest={if(busy) cancel() else dialog=false},title={Text("Share statement")},text={
        Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("${document.pages.size} ${if(document.pages.size==1) "page" else "pages"} · Complete statement")
            StatementExportFormat.entries.forEach {choice->
                Row(Modifier.fillMaxWidth().heightIn(min=56.dp).clickable(enabled=!busy,onClick=hapticAction {format=choice}),
                    verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                    RadioButton(selected=format==choice,onClick=if(busy) null else hapticAction {format=choice})
                    Text(choice.label,Modifier.weight(1f))
                }
            }
            Text("Choose the recipient in Android's share menu. Opening it does not confirm sending.",style=MaterialTheme.typography.bodyMedium)
            Box(Modifier.fillMaxWidth().height(4.dp)) {if(busy) LinearProgressIndicator()}
            if(busy) Text("Preparing ${format.label.lowercase()}…",style=MaterialTheme.typography.bodyMedium)
        }
    },confirmButton={TextButton(enabled=enabled && !busy,onClick=hapticAction {
        if(busy || !enabled) return@hapticAction
        val selectedFormat=format;val selectedDocument=document;val serial=++generation
        val operation=java.util.UUID.randomUUID().toString()
        fun action(event: CommunicationEvent)=CustomerCommunicationAction(operation,selectedDocument.statement.customerUid,"statement",selectedFormat.label,
            "${selectedDocument.statement.period.start}/${selectedDocument.statement.period.endInclusive}",event)
        busy=true;onBusy(true);notice=null
        job=scope.launch {
            var export: StatementExport?=null;var opened=false
            fun sameFacts(fresh: com.saaf.milkshop.application.owner.CustomerStatement)=fresh.copy(generatedAt=selectedDocument.statement.generatedAt)==selectedDocument.statement
            try {
                val fresh=load(selectedDocument.statement.customerId,selectedDocument.statement.period)
                if(!sameFacts(fresh)) {
                    notice="Records changed. Review the refreshed statement before sharing.";dialog=false;onRefresh();return@launch
                }
                export=store.export(selectedDocument,selectedFormat,engine)
                ensureActive()
                record(action(CommunicationEvent.PREPARED))
                val current=load(selectedDocument.statement.customerId,selectedDocument.statement.period)
                if(!sameFacts(current)) {
                    notice="Records changed. Review the refreshed statement before sharing.";dialog=false;onRefresh();return@launch
                }
                ensureActive()
                val send=store.shareIntent(export!!,selectedFormat)
                val chooser=Intent.createChooser(send,"Share statement").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                (launcher ?: {intent: Intent->context.startActivity(intent)})(chooser)
                opened=true;dialog=false;notice="Share menu opened. Sending is managed in the other app."
                try {record(action(CommunicationEvent.CHOOSER_OPENED))}
                catch(cancelled: CancellationException) {throw cancelled}
                catch(historyError: Exception) {notice+=" Local history could not be saved."}
            } catch(cancelled: CancellationException) {throw cancelled}
            catch(error: Exception) {
                dialog=false
                notice=if(error is ActivityNotFoundException) "No sharing app is available for this format. Enable a suitable app and try again."
                    else error.message?.takeIf {it.isNotBlank()} ?: "Statement sharing failed. Refresh and try again."
                if(!opened) try {record(action(CommunicationEvent.HANDOFF_FAILED))}
                catch(cancelled: CancellationException) {throw cancelled}
                catch(historyError: Exception) {notice+=" Local history could not be saved."}
            }
            finally {
                withContext(NonCancellable+Dispatchers.IO) {export?.let {if(opened) it.close() else it.discard()}}
                if(generation==serial) {busy=false;onBusy(false)}
            }
        }
    },modifier=Modifier.testTag("statement-open-share-menu")) {Text("Open share menu")} },
        dismissButton={TextButton(onClick=hapticAction {if(busy) cancel() else dialog=false},modifier=Modifier.testTag("statement-cancel-share")) {Text("Cancel")}})
}
