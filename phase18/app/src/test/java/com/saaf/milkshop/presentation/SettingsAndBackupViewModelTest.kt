package com.saaf.milkshop.presentation

import com.saaf.milkshop.presentation.backup.BackupViewModel
import com.saaf.milkshop.presentation.settings.SettingsViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsAndBackupViewModelTest {
    @Test
    fun settingsDefaultsAndPreferenceDraftAreStable() {
        val vm = SettingsViewModel()
        assertEquals("system", vm.preferencesDraft.theme)
        assertEquals("en", vm.preferencesDraft.language)
        assertEquals("comfortable", vm.preferencesDraft.density)
        assertEquals("month", vm.preferencesDraft.period)
        assertEquals("dashboard", vm.preferencesDraft.openingSection)

        vm.loadPreferences("dark", "ur", "compact", "week", "budgets")
        assertEquals("dark", vm.preferencesDraft.theme)
        assertEquals("ur", vm.preferencesDraft.language)
        assertEquals("compact", vm.preferencesDraft.density)
        assertEquals("week", vm.preferencesDraft.period)
        assertEquals("budgets", vm.preferencesDraft.openingSection)
        assertEquals(vm.state, vm.stateFlow.value)
    }

    @Test
    fun backupRestoreStateTracksBusyAndStatus() {
        val vm = BackupViewModel()
        assertFalse(vm.state.isBusy)
        assertEquals("", vm.state.statusMessage)

        vm.beginRestore()
        assertTrue(vm.state.isBusy)
        assertEquals("Restoring…", vm.state.statusMessage)

        vm.setStatus("Restore complete")
        assertTrue(vm.state.isBusy)
        assertEquals("Restore complete", vm.state.statusMessage)

        vm.finishRestore()
        assertFalse(vm.state.isBusy)
        assertEquals("Restore complete", vm.state.statusMessage)
        assertEquals(vm.state, vm.stateFlow.value)
    }
}
