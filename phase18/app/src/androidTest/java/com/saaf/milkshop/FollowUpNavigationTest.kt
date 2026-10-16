package com.saaf.milkshop

import com.saaf.milkshop.application.owner.*
import com.saaf.milkshop.nativeui.owner.customerFollowUpAccess
import com.saaf.milkshop.sync.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*

/** Shares the actual database/runtime fixture, not its test conclusions. Only five named methods are selected. */
class FollowUpNavigationTest: FollowUpHandoffTest() {
    private var opens=0
    private var sharedCalls=0
    private fun fake()=CustomerSharedHandoffAccess(load={sharedCalls++;CustomerSharedHandoffState()},
        open={_,_,_,_,launch->sharedCalls++;launch();CustomerSharedHandoffState()},
        recover={_,_->error("Unexpected recovery")},release={_,_->error("Unexpected release")},resolve={_,_,_,_->error("Unexpected resolution")})
    private fun mode()=CustomerReminderMode(context,db,security,runtime,session,shared=fake()).access
    private suspend fun facts()=CustomerCommunicationService(db,security).reminder(session,customer)
    @Test fun standaloneRetainsManualWithoutRpc(): Unit=runBlocking {
        SyncShopBindingStore.clear(context);SyncTokenStore(context).clear();snapshotUnavailable=true
        val m=mode();assertNull(m.load(customer).blockedReason)
        m.open(customer,facts(),"+923001234567","SMS") {opens++}
        assertEquals(1,opens);assertEquals(0,sharedCalls)
    }
    @Test fun connectedModeNeverFallsBackOnUnknown(): Unit=runBlocking {
        val m=mode();snapshotUnavailable=true
        assertTrue(runCatching {m.load(customer)}.isFailure)
        assertTrue(runCatching {m.open(customer,facts(),"+923001234567","SMS") {opens++}}.isFailure)
        snapshotUnavailable=false;SyncTokenStore(context).clear()
        assertTrue(runCatching {m.open(customer,facts(),"+923001234567","SMS") {opens++}}.isFailure)
        assertEquals(0,opens);assertEquals(0,sharedCalls)
    }
    @Test fun modeIsRecheckedBeforeEveryOpen(): Unit=runBlocking {
        val m=mode();enabled=false;m.load(customer);assertEquals(0,sharedCalls)
        enabled=true;m.open(customer,facts(),"+923001234567","WhatsApp") {opens++};assertEquals(1,sharedCalls)
        enabled=false;m.open(customer,facts(),"+923001234567","SMS") {opens++}
        assertEquals(1,sharedCalls);assertEquals(2,opens)
    }
    @Test fun boundWorkspaceRejectsChangedSession(): Unit=runBlocking {
        var current=true
        val bundle=customerFollowUpAccess(context,db,security,runtime,session) {check(current) {"Changed session"}}
        bundle.workspace.load();current=false
        assertTrue(runCatching {bundle.workspace.load()}.isFailure)
        assertTrue(runCatching {bundle.handoff.open(customer,facts(),"+923001234567","SMS") {opens++}}.isFailure)
        assertEquals(0,opens)
    }
    @Test fun restoredRequestCannotBecomeStandalone(): Unit=runBlocking {
        lose="START";assertTrue(runCatching {open()}.isFailure)
        val row=db.followUpDataDao().commands().single();db.followUpDataDao().complete(row.commandUid)
        db.followUpDataDao().insertCommand(row.copy(restored=true))
        SyncShopBindingStore.clear(context);SyncTokenStore(context).clear()
        val m=mode();assertTrue(runCatching {m.load(customer)}.isFailure)
        assertTrue(runCatching {m.open(customer,facts(),"+923001234567","SMS") {opens++}}.isFailure)
        assertEquals(0,opens);assertEquals(row.operationJson,db.followUpDataDao().command(row.commandUid)!!.operationJson)
    }
}
