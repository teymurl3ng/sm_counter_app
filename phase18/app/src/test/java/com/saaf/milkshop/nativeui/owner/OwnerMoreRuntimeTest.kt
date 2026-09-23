package com.saaf.milkshop.nativeui.owner

import com.saaf.milkshop.application.owner.NativeOwnerMoreContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OwnerMoreRuntimeTest {
    @Test
    fun staleTokenCannotBecomeCurrentWhenSameHandleIsReopened() {
        val sessions = OwnerSessionRuntime()
        sessions.replace("owner-session")
        val stale = requireNotNull(sessions.capture())

        sessions.replace(null)
        sessions.replace("owner-session")

        assertFalse(sessions.isCurrent(stale))
        assertTrue(sessions.isCurrent(requireNotNull(sessions.capture())))
    }

    @Test
    fun restartingCurrentSessionInvalidatesEarlierRequests() {
        val sessions = OwnerSessionRuntime()
        sessions.replace("owner-session")
        val stale = requireNotNull(sessions.capture())

        val refreshed = requireNotNull(sessions.restart())

        assertFalse(sessions.isCurrent(stale))
        assertTrue(sessions.isCurrent(refreshed))
        assertEquals(stale.handle, refreshed.handle)
    }

    @Test
    fun menuResetClearsTransientAndSensitiveUiState() {
        val ready = NativeOwnerMoreState.Ready(
            context = NativeOwnerMoreContext(
                businessDate = "2026-09-23",
                postingOpen = true,
                shopName = "Saaf Milk",
            ),
            destination = NativeOwnerMoreDestination.Backup,
            personalSecurity = OwnerPersonalSecurityState(
                confirmRemoveUnlock = true,
                saving = true,
                error = "personal error",
            ),
            productsState = OwnerProductsState(
                productCreateOpen = true,
                productCreating = true,
            ),
            costingState = OwnerCostingState(
                costingReauthOpen = true,
                costingReauthBusy = true,
            ),
            shopSettingsState = OwnerShopSettingsState(
                shopSettingsOpeningReauthOpen = true,
                shopSettingsOpeningReauthBusy = true,
            ),
            usersRolesState = OwnerUsersRolesState(
                userReauthOpen = true,
                userReauthBusy = true,
            ),
            backupState = OwnerBackupState(
                backupReauthOpen = true,
                backupReauthBusy = true,
                backupError = "backup error",
            ),
        )

        val reset = ready.resetForMenu()

        assertEquals(NativeOwnerMoreDestination.Menu, reset.destination)
        assertFalse(reset.confirmRemoveUnlock)
        assertFalse(reset.saving)
        assertNull(reset.error)
        assertFalse(reset.productCreateOpen)
        assertFalse(reset.productCreating)
        assertFalse(reset.costingReauthOpen)
        assertFalse(reset.costingReauthBusy)
        assertFalse(reset.shopSettingsOpeningReauthOpen)
        assertFalse(reset.shopSettingsOpeningReauthBusy)
        assertFalse(reset.userReauthOpen)
        assertFalse(reset.userReauthBusy)
        assertFalse(reset.backupReauthOpen)
        assertFalse(reset.backupReauthBusy)
        assertNull(reset.backupError)
    }

    @Test
    fun pendingBackupStoreWipesSecretsOnReplacementAndClear() {
        val store = OwnerPendingBackupActionStore()
        val first = "first secret".toCharArray()
        val second = "second secret".toCharArray()

        store.replace(PendingBackupAction.Connect(first, allowMetered = false))
        store.replace(PendingBackupAction.Connect(second, allowMetered = true))

        assertTrue(first.all { it == '\u0000' })
        assertFalse(second.all { it == '\u0000' })

        store.clear()

        assertTrue(second.all { it == '\u0000' })
        assertNull(store.current)
    }
}
