package com.saaf.milkshop.nativeui.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saaf.milkshop.application.owner.*
import com.saaf.milkshop.nativeui.common.*
import com.saaf.milkshop.nativeui.theme.*
import com.saaf.milkshop.sync.BusinessClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.saaf.milkshop.statements.StatementDocument
import com.saaf.milkshop.statements.StatementDocumentEngine
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

typealias CustomerStatementLoader = suspend (Long, CustomerStatementPeriod) -> CustomerStatement

@Composable
internal fun CustomerDetailOverview(detail: NativeOwnerCustomerDetail, owner: Boolean, busy: Boolean,
    error: String?, onOpenStatement: () -> Unit, onEdit: () -> Unit, onCreditAction: (CustomerCreditCommand)->Unit) {
    var disclosure by rememberSaveable(detail.row.accountId) { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=16.dp,vertical=12.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text(detail.row.name,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.SemiBold,
            modifier=Modifier.testTag(com.saaf.milkshop.nativeui.NativeUiTags.OwnerCustomersDetailName))
        if(detail.row.credit != null) CustomerCreditBody(detail,owner,busy,CustomerCreditContent.Summary,onCreditAction)
        else NativeInformationSection {
            NativeInformationFact("Balance",detail.row.balanceLabel,emphasized=true)
            detail.creditAge?.let { Text(it.description,style=MaterialTheme.typography.bodyMedium) }
        }
        TextButton(onClick=hapticAction(onOpenStatement),modifier=Modifier.fillMaxWidth()
            .nativeListItem(NativeListStyle.Navigation).testTag("customer-statement-entry"),
            contentPadding=PaddingValues(horizontal=16.dp,vertical=12.dp)) {
            Icon(Icons.Outlined.Description,contentDescription=null,modifier=Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Statements & reminders",style=nativeNavigationTitleTextStyle(),color=MaterialTheme.colorScheme.onSurface)
                Text("Preview a statement or prepare a message",style=nativeSupportingTextStyle(),color=nativeVisualColors.supporting)
            }
            Icon(Icons.Outlined.ChevronRight,contentDescription=null,modifier=Modifier.padding(start=12.dp).size(24.dp))
        }
        detail.row.credit?.pending?.takeIf { it.isNotEmpty() }?.let { changes ->
            NativeInformationSection(Modifier.testTag("customer-credit-attention")) {
                Text("${changes.size} credit changes awaiting sync",style=nativeSectionTextStyle())
                Text(if(changes.any { it.held }) "Held for Owner review. Accepted terms and flags remain in effect." else "Pending server acceptance. Accepted terms and flags remain in effect.")
                TextButton(onClick=hapticAction { disclosure="credit" },enabled=!busy) { Text("Review changes") }
            }
        }
        CustomerDetailDisclosure("Account history","history",disclosure,{disclosure=it}) {
            detail.ledger?.entries?.forEach { entry ->
                Text("${entry.occurredAt} · ${entry.taskLabel} · ${entry.stateLabel}",style=MaterialTheme.typography.labelLarge)
                NativeInformationFact(entry.movementLabel,"Balance ${entry.balanceAfterLabel}")
            }
            if(detail.ledger?.entries.isNullOrEmpty()) Text("No ledger movements yet.")
        }
        CustomerDetailDisclosure("Customer details","profile",disclosure,{disclosure=it}) {
            NativeInformationHeading("Profile",actionLabel="Edit customer",actionTag=com.saaf.milkshop.nativeui.NativeUiTags.OwnerCustomersDetailEdit,onAction=onEdit)
            NativeInformationFact("Status",detail.row.statusLabel)
            detail.row.phone?.let { NativeInformationFact("Phone",it) }
            detail.row.areaRoute?.let { NativeInformationFact("Area / route",it) }
            detail.address?.let { NativeInformationFact("Address",it,prose=true) }
            detail.creditLimitLabel?.let { NativeInformationFact("Credit limit",it,emphasized=true) }
            detail.dailyQuotaLabel?.let { NativeInformationFact("Daily quota",it) }
            detail.notes?.let { NativeInformationFact("Notes",it,prose=true) }
        }
        if(detail.row.credit != null) CustomerDetailDisclosure("Credit settings & review","credit",disclosure,{disclosure=it}) {
            CustomerCreditBody(detail,owner,busy,CustomerCreditContent.Management,onCreditAction)
        }
        error?.let { Text(it,color=MaterialTheme.colorScheme.error,modifier=Modifier.testTag(com.saaf.milkshop.nativeui.NativeUiTags.OwnerCustomersError)) }
    }
}

@Composable
private fun CustomerDetailDisclosure(title: String,key: String,selected: String,onSelect: (String)->Unit,
    content: @Composable ColumnScope.()->Unit) {
    val bring=remember { BringIntoViewRequester() }
    LaunchedEffect(selected) {
        if(selected==key) { withFrameNanos { }; bring.bringIntoView() }
    }
    NativeInformationSection {
        NativeInformationHeading(title,modifier=Modifier.bringIntoViewRequester(bring),actionLabel=if(selected==key) "Hide" else "Show",actionTag="customer-disclosure-$key",
            onAction={onSelect(if(selected==key) "" else key)})
        if(selected==key) Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(12.dp),content=content)
    }
}

/** The snapshot is immutable; period/retry changes cancel older work before it can publish. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CustomerStatementScreen(customerId: Long,customerName: String,load: CustomerStatementLoader,shareLauncher: ((android.content.Intent)->Unit)?=null,
    communication: CustomerCommunicationAccess=CustomerCommunicationAccess(),onReminder: ()->Unit={},onHistory: ()->Unit={},
    backLabel: String="Back to customer",onSchedule: (()->Unit)?=null,onBack: ()->Unit) {
    val context=LocalContext.current
    val engine=remember(context.applicationContext) {StatementDocumentEngine(context)}
    val today=BusinessClock.today()
    var start by rememberSaveable(customerId) { mutableStateOf(today.minusDays(6).toString()) }
    var end by rememberSaveable(customerId) { mutableStateOf(today.toString()) }
    val period=CustomerStatementPeriod(LocalDate.parse(start),LocalDate.parse(end))
    var retry by remember { mutableIntStateOf(0) }
    var snapshot by remember(customerId) { mutableStateOf<CustomerStatement?>(null) }
    var document by remember(customerId) { mutableStateOf<StatementDocument?>(null) }
    var sharing by remember { mutableStateOf(false) }
    var loading by remember(customerId) { mutableStateOf(true) }
    var error by remember(customerId) { mutableStateOf<String?>(null) }
    var choosePeriod by remember { mutableStateOf(false) }
    var custom by remember { mutableStateOf(false) }
    var page by rememberSaveable(customerId,start,end) { mutableIntStateOf(0) }
    LaunchedEffect(customerId,period,retry) {
        loading=true; error=null
        try {
            val next=load(customerId,period)
            require(next.customerId==customerId && next.period==period) { "The statement does not match this customer and period. Try again." }
            val planned=withContext(Dispatchers.Default) {engine.plan(next)}
            snapshot=next;document=planned
            page=page.coerceIn(0,planned.pages.lastIndex)
        }
        catch(cancelled: CancellationException) { throw cancelled }
        catch(failure: Exception) { error=failure.message?.takeIf { it.isNotBlank() } ?: "Statement could not load. Try again." }
        finally { loading=false }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=16.dp,vertical=12.dp)
        .testTag("customer-statement"),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        TextButton(onClick=hapticAction(onBack),contentPadding=PaddingValues(0.dp),modifier=Modifier.heightIn(min=48.dp)) { Text(backLabel) }
        Column { Text("Statement",style=MaterialTheme.typography.titleLarge); Text(customerName,style=nativeSupportingTextStyle()) }
        NativeInformationSection {
            NativeInformationHeading("Period",actionLabel="Change",actionTag="statement-period",onAction={if(!sharing) choosePeriod=true})
            Text(period.label,style=nativeValueTextStyle())
        }
        Box(Modifier.fillMaxWidth().heightIn(min=240.dp)) {
            val current=document
            if(current != null) key(current,page) { StatementPageView(current,page,engine,Modifier.alpha(if(loading || current.statement.period!=period) .5f else 1f)) }
            else Text(if(loading) "Preparing statement…" else "No statement prepared.",Modifier.padding(16.dp))
            if(loading) LinearProgressIndicator(Modifier.fillMaxWidth().testTag("statement-loading"))
        }
        error?.let {
            NativeInformationSection(Modifier.testTag("statement-error")) {
                Text(it,color=MaterialTheme.colorScheme.error)
                if(snapshot!=null) Text("The faded preview is the previous statement, not the selected period.")
                TextButton(onClick=hapticAction { retry++ }) { Text("Try again") }
                snapshot?.takeIf { it.period!=period }?.let { previous ->
                    TextButton(onClick=hapticAction { start=previous.period.start.toString();end=previous.period.endInclusive.toString() }) { Text("Revert period") }
                }
            }
        }
        onSchedule?.let {open->TextButton(onClick=hapticAction(open),enabled=!sharing,modifier=Modifier.testTag("statement-followup-schedule")) {Text("Schedule & pause")}}
        document?.let { currentDocument ->
            val statement=currentDocument.statement
            val usable=!loading && error==null && statement.period==period
            val pages=currentDocument.pages.size
            val controls: @Composable ()->Unit = {
                TextButton(onClick=hapticAction { page-- },enabled=usable && !sharing && page>0,modifier=Modifier.testTag("statement-previous")) { Text("Previous") }
                Text("Page ${page+1} of $pages",style=nativeSupportingTextStyle())
                TextButton(onClick=hapticAction { page++ },enabled=usable && !sharing && page<pages-1,modifier=Modifier.testTag("statement-next")) { Text("Next") }
            }
            if(pages==1) Text("Page 1 of 1",style=nativeSupportingTextStyle())
            else if(LocalDensity.current.fontScale>=1.5f) Column(Modifier.fillMaxWidth(),content={controls()})
            else Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,
                verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,content={controls()})
            val generated=Instant.parse(statement.generatedAt).atZone(java.time.ZoneId.systemDefault())
                .format(java.time.format.DateTimeFormatter.ofPattern("d MMM uuuu, HH:mm"))
            Text("Prepared $generated. This snapshot reflects local records for the selected period. Product names are current catalog labels.",style=nativeSupportingTextStyle())
            StatementShareControls(currentDocument,engine,usable,load,onRefresh={retry++},onBusy={sharing=it},launcher=shareLauncher,record={communication.record(customerId,it)})
            OutlinedButton(onClick=hapticAction(onReminder),enabled=!loading && !sharing,modifier=Modifier.fillMaxWidth().testTag("statement-prepare-reminder")) {Text("Prepare reminder")}
            TextButton(onClick=hapticAction(onHistory),enabled=!sharing,modifier=Modifier.fillMaxWidth().testTag("statement-preparation-history")) {Text("Preparation history")}
            OutlinedButton(onClick=hapticAction { retry++ },enabled=!loading && !sharing,modifier=Modifier.fillMaxWidth().testTag("statement-refresh")) { Text("Refresh statement") }
        }
    }
    if(choosePeriod) AlertDialog(onDismissRequest={choosePeriod=false},title={Text("Statement period")},text={
        Column {
            TextButton(onClick=hapticAction { start=today.minusDays(6).toString();end=today.toString();choosePeriod=false }) { Text("Last 7 days") }
            TextButton(onClick=hapticAction { start=today.withDayOfMonth(1).toString();end=today.toString();choosePeriod=false }) { Text("This month") }
            TextButton(onClick=hapticAction { choosePeriod=false;custom=true }) { Text("Custom dates") }
        }
    },confirmButton={TextButton(onClick=hapticAction {choosePeriod=false}) {Text("Cancel")}})
    if(custom) {
        val range=rememberDateRangePickerState(initialSelectedStartDateMillis=period.start.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            initialSelectedEndDateMillis=period.endInclusive.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates=object: SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long)=Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()<=today
            })
        DatePickerDialog(onDismissRequest={custom=false},confirmButton={ TextButton(onClick=hapticAction {
            start=Instant.ofEpochMilli(range.selectedStartDateMillis!!).atZone(ZoneOffset.UTC).toLocalDate().toString()
            end=Instant.ofEpochMilli(range.selectedEndDateMillis!!).atZone(ZoneOffset.UTC).toLocalDate().toString();custom=false
        },enabled=range.selectedStartDateMillis!=null && range.selectedEndDateMillis!=null) {Text("Use period")} },
            dismissButton={TextButton(onClick=hapticAction {custom=false}) {Text("Cancel")}}) {
            DateRangePicker(range,modifier=Modifier.heightIn(max=480.dp))
        }
    }
}
