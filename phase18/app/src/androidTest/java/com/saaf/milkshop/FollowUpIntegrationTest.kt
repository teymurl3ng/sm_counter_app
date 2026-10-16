package com.saaf.milkshop

import android.content.Intent
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.nativeui.*
import com.saaf.milkshop.nativeui.owner.*
import com.saaf.milkshop.sync.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*

/** Actual isolated connection stores/runtime; scheduling callbacks replace background execution only. */
internal class FollowUpIntegrationTest: FollowUpHandoffTest() {
    private fun key()=runBlocking {runtime.withFollowUpSession(session) {it.binding.key}}
    @Test fun workPlanning(): Unit=runBlocking {
        var kicks=0;var stops=0
        FollowUpIntegration.reconcile(context,{kicks++},{stops++})
        assertEquals(1,kicks);assertEquals(0,stops)
        val current=SyncShopBindingStore.load(context)!!
        SyncShopBindingStore.save(context,current.copy(role="cashier"))
        FollowUpIntegration.reconcile(context,{kicks++},{stops++})
        assertEquals(1,kicks);assertEquals(1,stops)
    }
    @Test fun invalidation(): Unit=runBlocking {
        val key=key();val store=FollowUpAttentionStore(context)
        store.update(key) {FollowUpAttention(3,false,"2026-10-18","2026-10-18")}
        FollowUpIntegration.reconcile(context,{}, {})
        val stale=store.current(key)
        assertEquals(3,stale.count);assertTrue(stale.stale);assertEquals("2026-10-18",stale.attemptedOn)
        store.forgetMemoryForTest(key);assertTrue(store.current(key).stale)
        // A second binding must not make a previous binding's count current.
        val other="a".repeat(64);store.update(other) {FollowUpAttention(7,false)}
        FollowUpIntegration.reconcile(context,{}, {})
        assertTrue(store.current(other).stale);assertEquals(7,store.current(other).count)
    }
    @Test fun disconnectCleanup(): Unit=runBlocking {
        val key=key();val store=FollowUpAttentionStore(context)
        store.update(key) {FollowUpAttention(3,false)}
        SyncTokenStore(context).clear()
        var kicks=0;var stops=0
        FollowUpIntegration.reconcile(context,{kicks++},{stops++})
        assertEquals(0,kicks);assertEquals(1,stops);assertNull(store.current(key).count)
        assertTrue(store.current(key).stale)
        // Partial restored configuration cannot start work or make a local session.
        SyncShopBindingStore.clear(context)
        FollowUpIntegration.reconcile(context,{kicks++},{stops++})
        assertEquals(0,kicks);assertEquals(2,stops)
    }
    @Test fun checkedPolicy() {
        val applied=mutableListOf<Boolean>()
        FollowUpIntegration.afterDetection(FollowUpDetection.CHECKED,applied::add)
        FollowUpIntegration.afterDetection(FollowUpDetection.RETRY,applied::add)
        FollowUpIntegration.afterDetection(FollowUpDetection.STOPPED,applied::add)
        assertEquals(listOf(true,false),applied)
    }
    @Test fun destinationValidation() {
        assertFalse(isFollowUpDestination(null));assertFalse(isFollowUpDestination(Intent()))
        assertFalse(isFollowUpDestination(Intent().putExtra(FollowUpNotifier.EXTRA_DESTINATION,"sale")))
        assertTrue(isFollowUpDestination(FollowUpNotifier(context).destinationIntent()))
        assertEquals(setOf(AppRole.OWNER,AppRole.ADMIN,AppRole.MANAGER),AppRole.entries.filter(::canOpenFollowUpDestination).toSet())
        assertFalse(canOpenFollowUpDestination(null))
    }
    @Test fun permissionDecisions() {
        assertEquals(FollowUpPermissionAction.REQUEST,followUpPermissionAction(37,false,false))
        assertEquals(FollowUpPermissionAction.SETTINGS,followUpPermissionAction(37,false,true))
        assertEquals(FollowUpPermissionAction.SETTINGS,followUpPermissionAction(37,true,false))
        assertEquals(FollowUpPermissionAction.SETTINGS,followUpPermissionAction(26,true,false))
    }
}
