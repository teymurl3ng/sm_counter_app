package com.saaf.milkshop
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.owner.NativeOwnerMoreContext
import com.saaf.milkshop.domain.*
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.owner.NativeOwnerMoreState
import com.saaf.milkshop.nativeui.admin.AdminMoreScreen
import com.saaf.milkshop.nativeui.manager.ManagerMoreScreen
import com.saaf.milkshop.nativeui.theme.SaafTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class)
class NativeDelegatedLockMenuTest {
    @get:Rule val rule = createComposeRule()
    @Test fun adminMenuExplainsLocalLockAndCallsItsActionOnce() {
        var locked = 0
        rule.setContent { SaafTheme {
            AdminMoreScreen(
                snackbarHostState = remember { SnackbarHostState() },
                identity = NativeSignedInIdentity(5L, "synthetic", "Synthetic person", AppRole.ADMIN, RolePermissions.permissions(AppRole.ADMIN)),
                state = NativeOwnerMoreState.Ready(context = NativeOwnerMoreContext("2026-10-13", true, "Synthetic Shop")),
                onRetry = { },
onOpenPersonalSecurity = { },
onOpenProducts = { },
onOpenCostingPlans = { },
onOpenShopSettings = { },
onOpenBackup = { },
onBackToMenu = { },
onRetryProducts = { },
onOpenProduct = { _ -> },
onBackFromProductDetail = { },
onOpenProductCreate = { },
onBackFromProductCreate = { },
onCreateProduct = { _ -> },
onOpenProductEdit = { },
onBackFromProductEdit = { },
onUpdateProduct = { _ -> },
onArchiveProduct = { },
onUnarchiveProduct = { },
onProductsShowArchivedChange = { _ -> },
onRetryShopSettings = { },
onOpenShopIdentityEdit = { },
onBackFromShopIdentityEdit = { },
onSaveShopIdentity = { _ -> },
onOpenShopRatesEdit = { },
onBackFromShopRatesEdit = { },
onUpdateShopRates = { _ -> },
onOpenShopCategoryCreate = { },
onBackFromShopCategoryCreate = { },
onCreateShopExpenseCategory = { _ -> },
onOpenShopCategory = { _ -> },
onBackFromShopCategoryDetail = { },
onSetShopExpenseCategoryArchived = { _ -> },
onRetryBackup = { },
onOpenBackupExport = { },
onBackFromBackupExport = { },
onExportBackup = { _, _ -> },
onRequestRemoveQuickUnlock = { },
onCancelRemoveQuickUnlock = { },
onRemoveQuickUnlock = { _ -> },
onEnrollQuickPin = { _, _ -> },
onEnrollPhoneSecurity = { _, _ -> },
onSwitchAccount = { },
onSignOut = { locked += 1 },
                admScaffold = { _, _, content -> content(PaddingValues(0.dp)) },
                modifier = Modifier.safeDrawingPadding(),
            )
        } }
        rule.onNodeWithTag(NativeUiTags.AdminMoreSignOut).performScrollTo()
            .assertTextContains("Lock app").assertTextContains("End your session. Business sync is unchanged.").performClick()
        assertEquals(1, locked)
    }
    @Test fun managerMenuExplainsLocalLockAndCallsItsActionOnce() {
        var locked = 0
        rule.setContent { SaafTheme {
            ManagerMoreScreen(
                snackbarHostState = remember { SnackbarHostState() },
                identity = NativeSignedInIdentity(5L, "synthetic", "Synthetic person", AppRole.MANAGER, RolePermissions.permissions(AppRole.MANAGER)),
                state = NativeOwnerMoreState.Ready(context = NativeOwnerMoreContext("2026-10-13", true, "Synthetic Shop")),
                onRetry = { },
onOpenPersonalSecurity = { },
onOpenProducts = { },
onOpenCustomers = { },
onOpenSuppliers = { },
onOpenEmployees = { },
onBackToMenu = { },
onRetryProducts = { },
onOpenProduct = { _ -> },
onBackFromProductDetail = { },
onOpenProductCreate = { },
onBackFromProductCreate = { },
onCreateProduct = { _ -> },
onOpenProductEdit = { },
onBackFromProductEdit = { },
onUpdateProduct = { _ -> },
onArchiveProduct = { },
onUnarchiveProduct = { },
onProductsShowArchivedChange = { _ -> },
onRetryCustomers = { },
onOpenCustomer = { _ -> },
onBackFromCustomerDetail = { },
onOpenCustomerCreate = { },
onBackFromCustomerCreate = { },
onCreateCustomer = { _ -> },
onOpenCustomerEdit = { },
onBackFromCustomerEdit = { },
onUpdateCustomer = { _, _ -> },
onRetrySuppliers = { },
onOpenSupplier = { _ -> },
onBackFromSupplierDetail = { },
onOpenSupplierCreate = { },
onBackFromSupplierCreate = { },
onCreateSupplier = { _ -> },
onOpenSupplierEdit = { },
onBackFromSupplierEdit = { },
onUpdateSupplier = { _, _ -> },
onRetryEmployees = { },
onOpenEmployee = { _ -> },
onBackFromEmployeeDetail = { },
onOpenEmployeeCreate = { },
onBackFromEmployeeCreate = { },
onCreateEmployee = { _ -> },
onOpenEmployeeEdit = { },
onBackFromEmployeeEdit = { },
onUpdateEmployee = { _, _ -> },
onRequestRemoveQuickUnlock = { },
onCancelRemoveQuickUnlock = { },
onRemoveQuickUnlock = { _ -> },
onEnrollQuickPin = { _, _ -> },
onEnrollPhoneSecurity = { _, _ -> },
onSwitchAccount = { },
onSignOut = { locked += 1 },
                mgrScaffold = { _, _, content -> content(PaddingValues(0.dp)) },
                modifier = Modifier.safeDrawingPadding(),
            )
        } }
        rule.onNodeWithTag(NativeUiTags.ManagerMoreSignOut).performScrollTo()
            .assertTextContains("Lock app").assertTextContains("End your session. Business sync is unchanged.").performClick()
        assertEquals(1, locked)
    }
}
