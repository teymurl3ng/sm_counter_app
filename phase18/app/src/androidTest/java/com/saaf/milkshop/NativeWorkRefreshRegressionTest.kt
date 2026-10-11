package com.saaf.milkshop

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.owner.*
import com.saaf.milkshop.application.manager.*
import com.saaf.milkshop.application.admin.*
import com.saaf.milkshop.application.operations.*
import com.saaf.milkshop.nativeui.owner.*
import com.saaf.milkshop.nativeui.manager.*
import com.saaf.milkshop.nativeui.admin.*
import com.saaf.milkshop.nativeui.operations.*
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** Hold real IO completions while users open/back; test both success and failure. */
@RunWith(AndroidJUnit4::class)
class NativeWorkRefreshRegressionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun ownerNavigationWinsOverRefreshingSuccessAndFailure() {
        val original = NativeOwnerWorkContext("2026-10-10", true, "Test shop", 0)
        var next: CompletableDeferred<NativeOwnerWorkContext>? = null
        val vm = NativeOwnerWorkViewModel(object : NativeOwnerWorkOperations {
            override suspend fun load(sessionHandle: String, date: LocalDate) = next?.await() ?: original
        }, { fail("Unexpected session expiry") })
        val store = ViewModelStore().apply { put("vm", vm) }
        try {
            compose.runOnIdle { vm.open("owner") }
            compose.waitUntil(5_000) { vm.state.value is NativeOwnerWorkState.Ready }
            next = CompletableDeferred()
            compose.runOnIdle { vm.refresh(); vm.openDestination(NativeOwnerWorkDestination.ReceiveMilk) }
            next!!.complete(original.copy(activeDeliveryCount = 7))
            compose.waitUntil(5_000) { (vm.state.value as NativeOwnerWorkState.Ready).context.activeDeliveryCount == 7 }
            assertEquals(NativeOwnerWorkDestination.ReceiveMilk, (vm.state.value as NativeOwnerWorkState.Ready).destination)
            next = CompletableDeferred()
            compose.runOnIdle { vm.refresh(); vm.backToHome() }
            next!!.completeExceptionally(IllegalStateException("Offline"))
            compose.waitUntil(5_000) { !(vm.state.value as NativeOwnerWorkState.Ready).refreshing }
            assertEquals(NativeOwnerWorkDestination.Home, (vm.state.value as NativeOwnerWorkState.Ready).destination)
            assertEquals("Offline", (vm.state.value as NativeOwnerWorkState.Ready).error)
        } finally { compose.runOnIdle { store.clear() } }
    }

    @Test fun managerNavigationWinsOverWarmRefresh() {
        val original = NativeManagerWorkContext("2026-10-10", true, "Test shop", 0, true)
        var next: CompletableDeferred<NativeManagerWorkContext>? = null
        val vm = NativeManagerWorkViewModel(object : NativeManagerWorkOperations {
            override suspend fun load(sessionHandle: String, date: LocalDate) = next?.await() ?: original
        }, { fail("Unexpected session expiry") })
        val store = ViewModelStore().apply { put("vm", vm) }
        try {
            compose.runOnIdle { vm.open("manager") }
            compose.waitUntil(5_000) { vm.state.value is NativeManagerWorkState.Ready }
            next = CompletableDeferred()
            compose.runOnIdle { vm.refresh(); vm.openDestination(NativeManagerWorkDestination.Deliveries) }
            next!!.complete(original.copy(activeDeliveryCount = 7))
            compose.waitUntil(5_000) { !(vm.state.value as NativeManagerWorkState.Ready).refreshing }
            assertEquals(NativeManagerWorkDestination.Deliveries, (vm.state.value as NativeManagerWorkState.Ready).destination)
        } finally { compose.runOnIdle { store.clear() } }
    }

    @Test fun adminNavigationWinsOverWarmRefreshFailure() {
        val original = NativeAdminWorkContext("2026-10-10", true, "Test shop", 0, null, 0, false, 0)
        var next: CompletableDeferred<NativeAdminWorkContext>? = null
        val vm = NativeAdminWorkViewModel(object : NativeAdminWorkOperations {
            override suspend fun load(sessionHandle: String, date: LocalDate) = next?.await() ?: original
        }, { fail("Unexpected session expiry") })
        val store = ViewModelStore().apply { put("vm", vm) }
        try {
            compose.runOnIdle { vm.open("admin") }
            compose.waitUntil(5_000) { vm.state.value is NativeAdminWorkState.Ready }
            next = CompletableDeferred()
            compose.runOnIdle { vm.refresh(); vm.openDestination(NativeAdminWorkDestination.RecordExpense) }
            next!!.completeExceptionally(IllegalStateException("Offline"))
            compose.waitUntil(5_000) { !(vm.state.value as NativeAdminWorkState.Ready).refreshing }
            assertEquals(NativeAdminWorkDestination.RecordExpense, (vm.state.value as NativeAdminWorkState.Ready).destination)
        } finally { compose.runOnIdle { store.clear() } }
    }

    @Test fun operatorBackWinsOverWarmRefresh() {
        val original = NativeOperationsWorkContext("2026-10-10", true, "10 L", true, null, null, 0, NativeOperationsAttendanceStatus.Unlinked)
        var next: CompletableDeferred<NativeOperationsWorkContext>? = null
        val vm = NativeOperationsWorkViewModel(object : NativeOperationsWorkOperations {
            override suspend fun load(sessionHandle: String, date: LocalDate) = next?.await() ?: original
        }, { fail("Unexpected session expiry") })
        val store = ViewModelStore().apply { put("vm", vm) }
        try {
            compose.runOnIdle { vm.open("operator") }
            compose.waitUntil(5_000) { vm.state.value is NativeOperationsWorkState.Ready }
            compose.runOnIdle { vm.openReceiveMilk() }
            next = CompletableDeferred()
            compose.runOnIdle { vm.refresh(); vm.backToHome() }
            next!!.complete(original.copy(activeDeliveryCount = 7))
            compose.waitUntil(5_000) { !(vm.state.value as NativeOperationsWorkState.Ready).refreshing }
            assertEquals(NativeOperationsWorkDestination.Home, (vm.state.value as NativeOperationsWorkState.Ready).destination)
        } finally { compose.runOnIdle { store.clear() } }
    }
}
