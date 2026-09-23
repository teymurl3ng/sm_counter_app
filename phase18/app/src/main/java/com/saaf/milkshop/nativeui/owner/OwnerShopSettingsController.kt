package com.saaf.milkshop.nativeui.owner

import com.saaf.milkshop.application.owner.NativeOwnerShopSettingsFacade
import com.saaf.milkshop.domain.BusinessSettings
import com.saaf.milkshop.domain.OpeningStateInput
import com.saaf.milkshop.domain.SecurityFailure
import com.saaf.milkshop.domain.StoreIdentityInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Owns shop identity, rates, categories, opening state, and opening re-auth. */
internal class OwnerShopSettingsController(
    private val facade: NativeOwnerShopSettingsFacade,
    private val runtime: OwnerMoreRuntime,
) {
    private var pendingOpeningInput: OpeningStateInput? = null
    private var pendingOpeningCorrection: Boolean = false

    fun openShopSettings() {
        val token = runtime.captureSession() ?: return
        val session = token.handle
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        runtime.state.value = current.copy(
            destination = NativeOwnerMoreDestination.ShopSettings,
            shopSettingsLoading = true,
            shopSettings = null,
            shopSettingsIdentityEditOpen = false,
            shopSettingsIdentitySaving = false,
            shopSettingsRatesEditOpen = false,
            shopSettingsRatesSaving = false,
            shopSettingsCategoryCreateOpen = false,
            shopSettingsCategoryCreating = false,
            shopSettingsCategoryDetail = null,
            shopSettingsCategoryArchiving = false,
            shopSettingsError = null,
            shopSettingsOpeningEditOpen = false,
            shopSettingsOpeningCorrection = false,
            shopSettingsOpeningSaving = false,
            shopSettingsOpeningReauthOpen = false,
            shopSettingsOpeningReauthBusy = false,
            productsLoading = false,
            productDetail = null,
            productCreateOpen = false,
            productCreating = false,
            productEditOpen = false,
            productUpdating = false,
            productArchiving = false,
            productUnarchiving = false,
            productsError = null,
            customersLoading = false,
            customerDetail = null,
            customerCreateOpen = false,
            customerCreating = false,
            customerEditOpen = false,
            customerUpdating = false,
            customersError = null,
            suppliersLoading = false,
            supplierDetail = null,
            supplierCreateOpen = false,
            supplierCreating = false,
            supplierEditOpen = false,
            supplierUpdating = false,
            suppliersError = null,
            employeesLoading = false,
            employeeDetail = null,
            employeeCreateOpen = false,
            employeeCreating = false,
            employeeEditOpen = false,
            employeeUpdating = false,
            employeesError = null,
            costingPoliciesLoading = false,
            costingPolicyDetail = null,
            costingPoliciesError = null,
            costingCurrentMonthSummary = null,
            costingApproveOpen = false,
            costingApproveSeed = null,
            costingApproving = false,
            costingReauthOpen = false,
            costingReauthBusy = false,
            usersLoading = false,
            userDetail = null,
            usersError = null,
            securityHistoryLoading = false,
            securityAuditDetail = null,
            securityHistoryError = null,
            backupLoading = false,
            backup = null,
            backupError = null,
            backupExportOpen = false,
            backupRestoreOpen = false,
            backupConnectOpen = false,
            backupInspection = null,
            backupBusy = false,
            backupReauthOpen = false,
            backupReauthBusy = false,
            backupDisconnectConfirmOpen = false,
            backupCloudOpen = false,
            confirmRemoveUnlock = false,
            error = null,
            saving = false,
        )
        runtime.scope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) { facade.load(session) }
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                if (latest.destination != NativeOwnerMoreDestination.ShopSettings) return@launch
                runtime.state.value = latest.copy(
                    shopSettings = snapshot,
                    shopSettingsLoading = false,
                    shopSettingsError = null,
                )
            } catch (error: Throwable) {
                handleFailure(token, error)
            }
        }
    }

    fun refreshShopSettings() {
        val token = runtime.captureSession() ?: return
        val session = token.handle
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.ShopSettings) return
        runtime.state.value = current.copy(shopSettingsLoading = true, shopSettingsError = null)
        runtime.scope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) { facade.load(session) }
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                if (latest.destination != NativeOwnerMoreDestination.ShopSettings) return@launch
                runtime.state.value = latest.copy(
                    shopSettings = snapshot,
                    shopSettingsLoading = false,
                    shopSettingsError = null,
                )
            } catch (error: Throwable) {
                handleFailure(token, error)
            }
        }
    }

    fun openShopIdentityEdit() {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.ShopSettings) return
        if (current.shopSettings == null) return
        runtime.state.value = current.copy(
            shopSettingsIdentityEditOpen = true,
            shopSettingsIdentitySaving = false,
            shopSettingsRatesEditOpen = false,
            shopSettingsRatesSaving = false,
            shopSettingsCategoryCreateOpen = false,
            shopSettingsCategoryCreating = false,
            shopSettingsCategoryDetail = null,
            shopSettingsCategoryArchiving = false,
            shopSettingsError = null,
            shopSettingsOpeningEditOpen = false,
            shopSettingsOpeningCorrection = false,
            shopSettingsOpeningSaving = false,
            shopSettingsOpeningReauthOpen = false,
            shopSettingsOpeningReauthBusy = false,
            shopSettingsLoading = false,
        )
    }

    fun clearShopIdentityEdit() {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        runtime.state.value = current.copy(
            shopSettingsIdentityEditOpen = false,
            shopSettingsIdentitySaving = false,
            shopSettingsError = null,
        )
    }

    fun saveShopIdentity(input: StoreIdentityInput) {
        val token = runtime.captureSession() ?: return
        val session = token.handle
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.ShopSettings ||
            !current.shopSettingsIdentityEditOpen
        ) {
            return
        }
        if (current.shopSettingsIdentitySaving) return
        runtime.state.value = current.copy(shopSettingsIdentitySaving = true, shopSettingsError = null)
        runtime.scope.launch {
            try {
                withContext(Dispatchers.IO) { facade.saveIdentity(session, input) }
                val snapshot = withContext(Dispatchers.IO) { facade.load(session) }
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                if (latest.destination != NativeOwnerMoreDestination.ShopSettings) return@launch
                runtime.state.value = latest.copy(
                    shopSettings = snapshot,
                    shopSettingsIdentityEditOpen = false,
                    shopSettingsIdentitySaving = false,
                    shopSettingsError = null,
                    shopSettingsLoading = false,
                )
            } catch (error: Throwable) {
                handleFailure(token, error)
            }
        }
    }

    fun openShopRatesEdit() {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.ShopSettings) return
        if (current.shopSettings == null) return
        runtime.state.value = current.copy(
            shopSettingsRatesEditOpen = true,
            shopSettingsRatesSaving = false,
            shopSettingsIdentityEditOpen = false,
            shopSettingsIdentitySaving = false,
            shopSettingsCategoryCreateOpen = false,
            shopSettingsCategoryCreating = false,
            shopSettingsCategoryDetail = null,
            shopSettingsCategoryArchiving = false,
            shopSettingsError = null,
            shopSettingsOpeningEditOpen = false,
            shopSettingsOpeningCorrection = false,
            shopSettingsOpeningSaving = false,
            shopSettingsOpeningReauthOpen = false,
            shopSettingsOpeningReauthBusy = false,
            shopSettingsLoading = false,
        )
    }

    fun clearShopRatesEdit() {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        runtime.state.value = current.copy(
            shopSettingsRatesEditOpen = false,
            shopSettingsRatesSaving = false,
            shopSettingsError = null,
        )
    }

    fun updateShopRates(settings: BusinessSettings) {
        val token = runtime.captureSession() ?: return
        val session = token.handle
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.ShopSettings ||
            !current.shopSettingsRatesEditOpen
        ) {
            return
        }
        if (current.shopSettingsRatesSaving) return
        runtime.state.value = current.copy(shopSettingsRatesSaving = true, shopSettingsError = null)
        runtime.scope.launch {
            try {
                withContext(Dispatchers.IO) { facade.updateRates(session, settings) }
                val snapshot = withContext(Dispatchers.IO) { facade.load(session) }
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                if (latest.destination != NativeOwnerMoreDestination.ShopSettings) return@launch
                runtime.state.value = latest.copy(
                    shopSettings = snapshot,
                    shopSettingsRatesEditOpen = false,
                    shopSettingsRatesSaving = false,
                    shopSettingsError = null,
                    shopSettingsLoading = false,
                )
            } catch (error: Throwable) {
                handleFailure(token, error)
            }
        }
    }

    fun openShopCategoryCreate() {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.ShopSettings) return
        runtime.state.value = current.copy(
            shopSettingsCategoryCreateOpen = true,
            shopSettingsCategoryCreating = false,
            shopSettingsIdentityEditOpen = false,
            shopSettingsIdentitySaving = false,
            shopSettingsRatesEditOpen = false,
            shopSettingsRatesSaving = false,
            shopSettingsCategoryDetail = null,
            shopSettingsCategoryArchiving = false,
            shopSettingsError = null,
            shopSettingsOpeningEditOpen = false,
            shopSettingsOpeningCorrection = false,
            shopSettingsOpeningSaving = false,
            shopSettingsOpeningReauthOpen = false,
            shopSettingsOpeningReauthBusy = false,
            shopSettingsLoading = false,
        )
    }

    fun clearShopCategoryCreate() {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        runtime.state.value = current.copy(
            shopSettingsCategoryCreateOpen = false,
            shopSettingsCategoryCreating = false,
            shopSettingsError = null,
        )
    }

    fun createShopExpenseCategory(name: String) {
        val token = runtime.captureSession() ?: return
        val session = token.handle
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.ShopSettings ||
            !current.shopSettingsCategoryCreateOpen
        ) {
            return
        }
        if (current.shopSettingsCategoryCreating) return
        runtime.state.value = current.copy(shopSettingsCategoryCreating = true, shopSettingsError = null)
        runtime.scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    facade.createExpenseCategory(session, name)
                }
                val snapshot = withContext(Dispatchers.IO) { facade.load(session) }
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                if (latest.destination != NativeOwnerMoreDestination.ShopSettings) return@launch
                runtime.state.value = latest.copy(
                    shopSettings = snapshot,
                    shopSettingsCategoryCreateOpen = false,
                    shopSettingsCategoryCreating = false,
                    shopSettingsError = null,
                    shopSettingsLoading = false,
                )
            } catch (error: Throwable) {
                handleFailure(token, error)
            }
        }
    }

    fun openShopCategoryDetail(accountId: Long) {
        val token = runtime.captureSession() ?: return
        val session = token.handle
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.ShopSettings) return
        runtime.state.value = current.copy(
            shopSettingsLoading = true,
            shopSettingsCategoryDetail = null,
            shopSettingsCategoryArchiving = false,
            shopSettingsCategoryCreateOpen = false,
            shopSettingsCategoryCreating = false,
            shopSettingsIdentityEditOpen = false,
            shopSettingsIdentitySaving = false,
            shopSettingsRatesEditOpen = false,
            shopSettingsRatesSaving = false,
            shopSettingsError = null,
        )
        runtime.scope.launch {
            try {
                val detail = withContext(Dispatchers.IO) {
                    facade.loadCategoryDetail(session, accountId)
                }
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                if (latest.destination != NativeOwnerMoreDestination.ShopSettings) return@launch
                runtime.state.value = latest.copy(
                    shopSettingsCategoryDetail = detail,
                    shopSettingsLoading = false,
                    shopSettingsError = null,
                )
            } catch (error: Throwable) {
                handleFailure(token, error)
            }
        }
    }

    fun clearShopCategoryDetail() {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        runtime.state.value = current.copy(
            shopSettingsCategoryDetail = null,
            shopSettingsCategoryArchiving = false,
            shopSettingsError = null,
            shopSettingsOpeningEditOpen = false,
            shopSettingsOpeningCorrection = false,
            shopSettingsOpeningSaving = false,
            shopSettingsOpeningReauthOpen = false,
            shopSettingsOpeningReauthBusy = false,
            shopSettingsLoading = false,
        )
    }

    fun setShopExpenseCategoryArchived(archived: Boolean) {
        val token = runtime.captureSession() ?: return
        val session = token.handle
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        val accountId = current.shopSettingsCategoryDetail?.accountId ?: return
        if (current.destination != NativeOwnerMoreDestination.ShopSettings) return
        if (current.shopSettingsCategoryArchiving) return
        runtime.state.value = current.copy(shopSettingsCategoryArchiving = true, shopSettingsError = null)
        runtime.scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    facade.setExpenseCategoryArchived(session, accountId, archived)
                }
                val detail = withContext(Dispatchers.IO) {
                    facade.loadCategoryDetail(session, accountId)
                }
                val snapshot = withContext(Dispatchers.IO) { facade.load(session) }
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                if (latest.destination != NativeOwnerMoreDestination.ShopSettings) return@launch
                runtime.state.value = latest.copy(
                    shopSettings = snapshot,
                    shopSettingsCategoryDetail = detail,
                    shopSettingsCategoryArchiving = false,
                    shopSettingsError = null,
                    shopSettingsLoading = false,
                )
            } catch (error: Throwable) {
                handleFailure(token, error)
            }
        }
    }


    fun openOpeningEdit(correction: Boolean) {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.ShopSettings) return
        if (current.shopSettings == null) return
        pendingOpeningInput = null
        pendingOpeningCorrection = correction
        runtime.state.value = current.copy(
            shopSettingsOpeningEditOpen = true,
            shopSettingsOpeningCorrection = correction,
            shopSettingsOpeningSaving = false,
            shopSettingsOpeningReauthOpen = false,
            shopSettingsOpeningReauthBusy = false,
            shopSettingsIdentityEditOpen = false,
            shopSettingsRatesEditOpen = false,
            shopSettingsCategoryCreateOpen = false,
            shopSettingsCategoryDetail = null,
            shopSettingsError = null,
        )
    }

    fun backFromOpeningEdit() {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        pendingOpeningInput = null
        pendingOpeningCorrection = false
        runtime.state.value = current.copy(
            shopSettingsOpeningEditOpen = false,
            shopSettingsOpeningCorrection = false,
            shopSettingsOpeningSaving = false,
            shopSettingsOpeningReauthOpen = false,
            shopSettingsOpeningReauthBusy = false,
            shopSettingsError = null,
        )
    }

    fun saveOpening(input: OpeningStateInput) {
        val token = runtime.captureSession() ?: return
        val session = token.handle
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.ShopSettings) return
        if (!current.shopSettingsOpeningEditOpen || current.shopSettingsOpeningSaving) return
        val correction = current.shopSettingsOpeningCorrection
        runtime.state.value = current.copy(shopSettingsOpeningSaving = true, shopSettingsError = null)
        runtime.scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    if (correction) {
                        facade.correctOpening(session, input)
                    } else {
                        facade.recordOpening(session, input)
                    }
                }
                val snapshot = withContext(Dispatchers.IO) { facade.load(session) }
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                if (latest.destination != NativeOwnerMoreDestination.ShopSettings) return@launch
                pendingOpeningInput = null
                pendingOpeningCorrection = false
                runtime.state.value = latest.copy(
                    shopSettings = snapshot,
                    shopSettingsOpeningEditOpen = false,
                    shopSettingsOpeningCorrection = false,
                    shopSettingsOpeningSaving = false,
                    shopSettingsOpeningReauthOpen = false,
                    shopSettingsOpeningReauthBusy = false,
                    shopSettingsError = null,
                    shopSettingsLoading = false,
                )
            } catch (error: Throwable) {
                if (error is SecurityFailure.RecentAuthenticationRequired) {
                    pendingOpeningInput = input
                    pendingOpeningCorrection = correction
                    val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                    if (!runtime.isCurrent(token)) return@launch
                    runtime.state.value = latest.copy(
                        shopSettingsOpeningSaving = false,
                        shopSettingsOpeningReauthOpen = true,
                        shopSettingsOpeningReauthBusy = false,
                        shopSettingsError = null,
                    )
                } else {
                    handleFailure(token, error)
                }
            }
        }
    }

    fun submitOpeningReauth(passphrase: CharArray) {
        val token = runtime.captureSession()
        val current = runtime.state.value as? NativeOwnerMoreState.Ready
        if (token == null || current == null) {
            passphrase.fill('\u0000')
            return
        }
        val session = token.handle
        if (!current.shopSettingsOpeningReauthOpen || current.shopSettingsOpeningReauthBusy) {
            passphrase.fill('\u0000')
            return
        }
        val pending = pendingOpeningInput
        if (pending == null) {
            passphrase.fill('\u0000')
            runtime.state.value = current.copy(
                shopSettingsOpeningReauthOpen = false,
                shopSettingsOpeningReauthBusy = false,
            )
            return
        }
        val correction = pendingOpeningCorrection
        runtime.state.value = current.copy(shopSettingsOpeningReauthBusy = true, shopSettingsError = null)
        runtime.scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    facade.reauthenticate(session, passphrase)
                }
                withContext(Dispatchers.IO) {
                    if (correction) {
                        facade.correctOpening(session, pending)
                    } else {
                        facade.recordOpening(session, pending)
                    }
                }
                val snapshot = withContext(Dispatchers.IO) { facade.load(session) }
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                pendingOpeningInput = null
                pendingOpeningCorrection = false
                runtime.state.value = latest.copy(
                    shopSettings = snapshot,
                    shopSettingsOpeningEditOpen = false,
                    shopSettingsOpeningCorrection = false,
                    shopSettingsOpeningSaving = false,
                    shopSettingsOpeningReauthOpen = false,
                    shopSettingsOpeningReauthBusy = false,
                    shopSettingsError = null,
                    shopSettingsLoading = false,
                )
            } catch (error: Throwable) {
                handleFailure(token, error)
            } finally {
                passphrase.fill('\u0000')
            }
        }
    }

    fun cancelOpeningReauth() {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        pendingOpeningInput = null
        runtime.state.value = current.copy(
            shopSettingsOpeningReauthOpen = false,
            shopSettingsOpeningReauthBusy = false,
            shopSettingsOpeningSaving = false,
            shopSettingsError = null,
        )
    }


    fun resetPending() {
        pendingOpeningInput = null
        pendingOpeningCorrection = false
    }

    private fun handleFailure(token: OwnerSessionToken, error: Throwable) {
        runtime.handleFailure(token, NativeOwnerMoreDestination.ShopSettings, error) { current, message ->
            current.copy(
                shopSettingsLoading = false,
                shopSettingsIdentitySaving = false,
                shopSettingsRatesSaving = false,
                shopSettingsCategoryCreating = false,
                shopSettingsCategoryArchiving = false,
                shopSettingsOpeningSaving = false,
                shopSettingsOpeningReauthBusy = false,
                shopSettingsError = message,
            )
        }
    }
}
