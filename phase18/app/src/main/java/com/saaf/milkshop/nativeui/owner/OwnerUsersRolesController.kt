package com.saaf.milkshop.nativeui.owner

import com.saaf.milkshop.application.owner.NativeOwnerUsersRolesFacade
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.CreateUserInput
import com.saaf.milkshop.domain.SecurityFailure
import com.saaf.milkshop.nativeui.DataChangeBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Owns users, enrollment, roles, account status, credential reset, and re-auth. */
internal class OwnerUsersRolesController(
    private val facade: NativeOwnerUsersRolesFacade,
    private val runtime: OwnerMoreRuntime,
) {
    private var pendingUsersAction: PendingUsersRolesAction? = null

    fun openUsersRoles() {
        val token = runtime.captureSession() ?: return
        val session = token.handle
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        pendingUsersAction = null
        runtime.state.value = current.copy(
            destination = NativeOwnerMoreDestination.UsersRoles,
            usersLoading = NativeOwnerMoreDestination.UsersRoles !in current.loadedDestinations,
            userDetail = null,
            userEnrollOpen = false,
            userEnrollPending = null,
            userEnrollBusy = false,
            userRoleChangeOpen = false,
            userRoleChanging = false,
            userEnableConfirmOpen = false,
            userEnableTargetEnabled = false,
            userEnableBusy = false,
            userReauthOpen = false,
            userReauthBusy = false,
            usersError = null,
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
            costingApproveOpen = false,
            costingApproveSeed = null,
            costingApproving = false,
            costingReauthOpen = false,
            costingReauthBusy = false,
            shopSettingsLoading = false,
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
            securityHistoryLoading = false,
            securityAuditDetail = null,
            securityHistoryError = null,
            backupLoading = false,
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
                val snapshot = withContext(Dispatchers.IO) { facade.loadUsers(session) }
                val roles = withContext(Dispatchers.IO) { facade.enrollableRoles(session) }
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                if (latest.destination != NativeOwnerMoreDestination.UsersRoles) return@launch
                runtime.state.value = latest.copy(
                    users = snapshot.users,
                    usersLoading = false,
                    enrollableRoles = roles,
                    usersError = null,
                    loadedDestinations = latest.loadedDestinations + NativeOwnerMoreDestination.UsersRoles,
                )
            } catch (error: Throwable) {
                handleFailure(token, error)
            }
        }
    }

    fun refreshUsersRoles() {
        val token = runtime.captureSession() ?: return
        val session = token.handle
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.UsersRoles) return
        runtime.state.value = current.copy(
            usersLoading = true,
            usersError = null,
            userDetail = null,
            userEnrollOpen = false,
            userEnrollPending = null,
            userEnrollBusy = false,
            userRoleChangeOpen = false,
            userRoleChanging = false,
            userEnableConfirmOpen = false,
            userEnableBusy = false,
            userReauthOpen = false,
            userReauthBusy = false,
        )
        pendingUsersAction = null
        runtime.scope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) { facade.loadUsers(session) }
                val roles = withContext(Dispatchers.IO) { facade.enrollableRoles(session) }
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                if (latest.destination != NativeOwnerMoreDestination.UsersRoles) return@launch
                runtime.state.value = latest.copy(
                    users = snapshot.users,
                    usersLoading = false,
                    enrollableRoles = roles,
                    usersError = null,
                )
            } catch (error: Throwable) {
                handleFailure(token, error)
            }
        }
    }

    fun openUserDetail(userId: Long) {
        val token = runtime.captureSession() ?: return
        val session = token.handle
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.UsersRoles) return
        runtime.state.value = current.copy(
            usersLoading = true,
            usersError = null,
            userEnrollOpen = false,
            userEnrollPending = null,
            userRoleChangeOpen = false,
            userEnableConfirmOpen = false,
            userReauthOpen = false,
        )
        pendingUsersAction = null
        runtime.scope.launch {
            try {
                val detail = withContext(Dispatchers.IO) {
                    facade.loadDetail(session, userId)
                }
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                if (latest.destination != NativeOwnerMoreDestination.UsersRoles) return@launch
                runtime.state.value = latest.copy(
                    userDetail = detail,
                    usersLoading = false,
                    usersError = null,
                )
            } catch (error: Throwable) {
                handleFailure(token, error)
            }
        }
    }

    fun clearUserDetail() {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        pendingUsersAction = null
        runtime.state.value = current.copy(
            userDetail = null,
            userRoleChangeOpen = false,
            userRoleChanging = false,
            userEnableConfirmOpen = false,
            userEnableBusy = false,
            userReauthOpen = false,
            userReauthBusy = false,
            usersError = null,
            usersLoading = false,
        )
    }

    fun openUserEnroll() {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.UsersRoles) return
        pendingUsersAction = null
        runtime.state.value = current.copy(
            userEnrollOpen = true,
            userEnrollPending = null,
            userEnrollBusy = false,
            userDetail = null,
            userRoleChangeOpen = false,
            userEnableConfirmOpen = false,
            userReauthOpen = false,
            usersError = null,
        )
    }

    fun clearUserEnroll() {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        pendingUsersAction = null
        runtime.state.value = current.copy(
            userEnrollOpen = false,
            userEnrollPending = null,
            userEnrollBusy = false,
            userReauthOpen = false,
            userReauthBusy = false,
            usersError = null,
        )
    }

    fun beginUserEnroll(loginName: String, displayName: String, roleWire: String) {
        val token = runtime.captureSession() ?: return
        val session = token.handle
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.UsersRoles ||
            !current.userEnrollOpen ||
            current.userEnrollBusy
        ) {
            return
        }
        val input = CreateUserInput(
            loginName = loginName,
            displayName = displayName,
            role = roleWire,
        )
        runtime.state.value = current.copy(userEnrollBusy = true, usersError = null)
        runtime.scope.launch {
            try {
                val pending = withContext(Dispatchers.IO) {
                    facade.beginEnroll(session, input)
                }
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                if (latest.destination != NativeOwnerMoreDestination.UsersRoles) return@launch
                pendingUsersAction = null
                runtime.state.value = latest.copy(
                    userEnrollPending = pending,
                    userEnrollBusy = false,
                    userReauthOpen = false,
                    usersError = null,
                )
            } catch (error: Throwable) {
                if (error is SecurityFailure.RecentAuthenticationRequired) {
                    pendingUsersAction = PendingUsersRolesAction.BeginEnroll(input)
                    val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                    if (!runtime.isCurrent(token)) return@launch
                    runtime.state.value = latest.copy(
                        userEnrollBusy = false,
                        userReauthOpen = true,
                        userReauthBusy = false,
                        usersError = null,
                    )
                } else {
                    handleFailure(token, error)
                }
            }
        }
    }

    fun completeUserEnroll(passphrase: CharArray, confirmation: CharArray) {
        val token = runtime.captureSession()
        val current = runtime.state.value as? NativeOwnerMoreState.Ready
        if (token == null || current == null) {
            passphrase.fill('\u0000')
            confirmation.fill('\u0000')
            return
        }
        val session = token.handle
        val pending = current.userEnrollPending
        if (current.destination != NativeOwnerMoreDestination.UsersRoles ||
            pending == null ||
            current.userEnrollBusy
        ) {
            passphrase.fill('\u0000')
            confirmation.fill('\u0000')
            return
        }
        if (!passphrase.contentEquals(confirmation)) {
            passphrase.fill('\u0000')
            confirmation.fill('\u0000')
            runtime.state.value = current.copy(usersError = "Passphrases do not match")
            return
        }
        confirmation.fill('\u0000')
        runtime.state.value = current.copy(userEnrollBusy = true, usersError = null)
        runtime.scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    facade.completeEnroll(pending.enrollmentToken, passphrase)
                    DataChangeBus.notifyDataChanged()
                }
                val snapshot = withContext(Dispatchers.IO) { facade.loadUsers(session) }
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                if (latest.destination != NativeOwnerMoreDestination.UsersRoles) return@launch
                pendingUsersAction = null
                runtime.state.value = latest.copy(
                    users = snapshot.users,
                    userEnrollOpen = false,
                    userEnrollPending = null,
                    userEnrollBusy = false,
                    usersLoading = false,
                    usersError = null,
                )
            } catch (error: Throwable) {
                handleFailure(token, error)
            } finally {
                passphrase.fill('\u0000')
            }
        }
    }

    fun openUserRoleChange() {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.UsersRoles ||
            current.userDetail == null
        ) {
            return
        }
        pendingUsersAction = null
        runtime.state.value = current.copy(
            userRoleChangeOpen = true,
            userRoleChanging = false,
            userEnableConfirmOpen = false,
            userReauthOpen = false,
            usersError = null,
        )
    }

    fun clearUserRoleChange() {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        pendingUsersAction = null
        runtime.state.value = current.copy(
            userRoleChangeOpen = false,
            userRoleChanging = false,
            userReauthOpen = false,
            userReauthBusy = false,
            usersError = null,
        )
    }

    fun changeUserRole(roleWire: String) {
        val token = runtime.captureSession() ?: return
        val session = token.handle
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        val detail = current.userDetail ?: return
        if (current.destination != NativeOwnerMoreDestination.UsersRoles ||
            !current.userRoleChangeOpen ||
            current.userRoleChanging
        ) {
            return
        }
        runtime.state.value = current.copy(userRoleChanging = true, usersError = null)
        runtime.scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    facade.changeRole(session, detail.row.userId, roleWire)
                    DataChangeBus.notifyDataChanged()
                }
                val refreshed = withContext(Dispatchers.IO) {
                    facade.loadDetail(session, detail.row.userId)
                }
                val snapshot = withContext(Dispatchers.IO) { facade.loadUsers(session) }
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                if (latest.destination != NativeOwnerMoreDestination.UsersRoles) return@launch
                pendingUsersAction = null
                runtime.state.value = latest.copy(
                    users = snapshot.users,
                    userDetail = refreshed,
                    userRoleChangeOpen = false,
                    userRoleChanging = false,
                    userReauthOpen = false,
                    usersError = null,
                )
            } catch (error: Throwable) {
                if (error is SecurityFailure.RecentAuthenticationRequired) {
                    pendingUsersAction = PendingUsersRolesAction.ChangeRole(
                        userId = detail.row.userId,
                        roleWire = roleWire,
                    )
                    val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                    if (!runtime.isCurrent(token)) return@launch
                    runtime.state.value = latest.copy(
                        userRoleChanging = false,
                        userReauthOpen = true,
                        userReauthBusy = false,
                        usersError = null,
                    )
                } else {
                    handleFailure(token, error)
                }
            }
        }
    }

    fun requestSetUserEnabled(enabled: Boolean) {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.UsersRoles ||
            current.userDetail == null
        ) {
            return
        }
        runtime.state.value = current.copy(
            userEnableConfirmOpen = true,
            userEnableTargetEnabled = enabled,
            usersError = null,
        )
    }

    fun cancelSetUserEnabled() {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        runtime.state.value = current.copy(
            userEnableConfirmOpen = false,
            userEnableBusy = false,
        )
    }

    fun confirmSetUserEnabled() {
        val token = runtime.captureSession() ?: return
        val session = token.handle
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        val detail = current.userDetail ?: return
        if (current.destination != NativeOwnerMoreDestination.UsersRoles ||
            !current.userEnableConfirmOpen ||
            current.userEnableBusy
        ) {
            return
        }
        val enabled = current.userEnableTargetEnabled
        runtime.state.value = current.copy(userEnableBusy = true, usersError = null)
        runtime.scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    facade.setEnabled(session, detail.row.userId, enabled)
                    DataChangeBus.notifyDataChanged()
                }
                val refreshed = withContext(Dispatchers.IO) {
                    facade.loadDetail(session, detail.row.userId)
                }
                val snapshot = withContext(Dispatchers.IO) { facade.loadUsers(session) }
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                if (latest.destination != NativeOwnerMoreDestination.UsersRoles) return@launch
                pendingUsersAction = null
                runtime.state.value = latest.copy(
                    users = snapshot.users,
                    userDetail = refreshed,
                    userEnableConfirmOpen = false,
                    userEnableBusy = false,
                    userReauthOpen = false,
                    usersError = null,
                )
            } catch (error: Throwable) {
                if (error is SecurityFailure.RecentAuthenticationRequired) {
                    pendingUsersAction = PendingUsersRolesAction.SetEnabled(
                        userId = detail.row.userId,
                        enabled = enabled,
                    )
                    val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                    if (!runtime.isCurrent(token)) return@launch
                    runtime.state.value = latest.copy(
                        userEnableBusy = false,
                        userEnableConfirmOpen = false,
                        userReauthOpen = true,
                        userReauthBusy = false,
                        usersError = null,
                    )
                } else {
                    handleFailure(token, error)
                }
            }
        }
    }


    fun beginCredentialReset() {
        val token = runtime.captureSession() ?: return
        val session = token.handle
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        val detail = current.userDetail ?: return
        if (current.destination != NativeOwnerMoreDestination.UsersRoles || current.userEnrollBusy) return
        runtime.state.value = current.copy(userEnrollBusy = true, usersError = null)
        runtime.scope.launch {
            try {
                val pending = withContext(Dispatchers.IO) {
                    facade.beginCredentialReset(session, detail.row.userId)
                }
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                if (latest.destination != NativeOwnerMoreDestination.UsersRoles) return@launch
                pendingUsersAction = null
                runtime.state.value = latest.copy(
                    userEnrollOpen = true,
                    userEnrollPending = pending,
                    userEnrollBusy = false,
                    userReauthOpen = false,
                    usersError = null,
                )
            } catch (error: Throwable) {
                if (error is SecurityFailure.RecentAuthenticationRequired) {
                    pendingUsersAction = PendingUsersRolesAction.BeginCredentialReset(
                        userId = detail.row.userId,
                    )
                    val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                    if (!runtime.isCurrent(token)) return@launch
                    runtime.state.value = latest.copy(
                        userEnrollBusy = false,
                        userReauthOpen = true,
                        userReauthBusy = false,
                        usersError = null,
                    )
                } else {
                    handleFailure(token, error)
                }
            }
        }
    }

    fun resumeCredentialSetup() {
        val token = runtime.captureSession() ?: return
        val session = token.handle
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        val detail = current.userDetail ?: return
        if (current.destination != NativeOwnerMoreDestination.UsersRoles || current.userEnrollBusy) return
        runtime.state.value = current.copy(userEnrollBusy = true, usersError = null)
        runtime.scope.launch {
            try {
                val pending = withContext(Dispatchers.IO) {
                    facade.resumeCredentialSetup(session, detail.row.userId)
                }
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (!runtime.isCurrent(token)) return@launch
                if (latest.destination != NativeOwnerMoreDestination.UsersRoles) return@launch
                pendingUsersAction = null
                runtime.state.value = latest.copy(
                    userEnrollOpen = true,
                    userEnrollPending = pending,
                    userEnrollBusy = false,
                    userReauthOpen = false,
                    usersError = null,
                )
            } catch (error: Throwable) {
                if (error is SecurityFailure.RecentAuthenticationRequired) {
                    pendingUsersAction = PendingUsersRolesAction.ResumeCredentialSetup(
                        userId = detail.row.userId,
                    )
                    val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                    if (!runtime.isCurrent(token)) return@launch
                    runtime.state.value = latest.copy(
                        userEnrollBusy = false,
                        userReauthOpen = true,
                        userReauthBusy = false,
                        usersError = null,
                    )
                } else {
                    handleFailure(token, error)
                }
            }
        }
    }

    fun submitUsersRolesReauth(passphrase: CharArray) {
        val token = runtime.captureSession()
        val current = runtime.state.value as? NativeOwnerMoreState.Ready
        if (token == null || current == null) {
            passphrase.fill('\u0000')
            return
        }
        val session = token.handle
        if (current.destination != NativeOwnerMoreDestination.UsersRoles ||
            !current.userReauthOpen ||
            current.userReauthBusy
        ) {
            passphrase.fill('\u0000')
            return
        }
        val pending = pendingUsersAction
        if (pending == null) {
            passphrase.fill('\u0000')
            runtime.state.value = current.copy(userReauthOpen = false, usersError = null)
            return
        }
        runtime.state.value = current.copy(userReauthBusy = true, usersError = null)
        runtime.scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    facade.reauthenticate(session, passphrase)
                }
                when (pending) {
                    is PendingUsersRolesAction.BeginEnroll -> {
                        val enrollPending = withContext(Dispatchers.IO) {
                            facade.beginEnroll(session, pending.input)
                        }
                        val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                        if (!runtime.isCurrent(token)) return@launch
                        pendingUsersAction = null
                        runtime.state.value = latest.copy(
                            userEnrollPending = enrollPending,
                            userEnrollBusy = false,
                            userReauthOpen = false,
                            userReauthBusy = false,
                            usersError = null,
                        )
                    }
                    is PendingUsersRolesAction.SetEnabled -> {
                        withContext(Dispatchers.IO) {
                            facade.setEnabled(session, pending.userId, pending.enabled)
                        }
                        val refreshed = withContext(Dispatchers.IO) {
                            facade.loadDetail(session, pending.userId)
                        }
                        val snapshot = withContext(Dispatchers.IO) {
                            facade.loadUsers(session)
                        }
                        val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                        if (!runtime.isCurrent(token)) return@launch
                        pendingUsersAction = null
                        runtime.state.value = latest.copy(
                            users = snapshot.users,
                            userDetail = refreshed,
                            userEnableConfirmOpen = false,
                            userEnableBusy = false,
                            userReauthOpen = false,
                            userReauthBusy = false,
                            usersError = null,
                        )
                    }
                    is PendingUsersRolesAction.BeginCredentialReset -> {
                        val resetPending = withContext(Dispatchers.IO) {
                            facade.beginCredentialReset(session, pending.userId)
                        }
                        val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                        if (!runtime.isCurrent(token)) return@launch
                        pendingUsersAction = null
                        runtime.state.value = latest.copy(
                            userEnrollOpen = true,
                            userEnrollPending = resetPending,
                            userEnrollBusy = false,
                            userReauthOpen = false,
                            userReauthBusy = false,
                            usersError = null,
                        )
                    }
                    is PendingUsersRolesAction.ResumeCredentialSetup -> {
                        val resumedPending = withContext(Dispatchers.IO) {
                            facade.resumeCredentialSetup(session, pending.userId)
                        }
                        val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                        if (!runtime.isCurrent(token)) return@launch
                        pendingUsersAction = null
                        runtime.state.value = latest.copy(
                            userEnrollOpen = true,
                            userEnrollPending = resumedPending,
                            userEnrollBusy = false,
                            userReauthOpen = false,
                            userReauthBusy = false,
                            usersError = null,
                        )
                    }
                    is PendingUsersRolesAction.ChangeRole -> {
                        withContext(Dispatchers.IO) {
                            facade.changeRole(session, pending.userId, pending.roleWire)
                        }
                        val refreshed = withContext(Dispatchers.IO) {
                            facade.loadDetail(session, pending.userId)
                        }
                        val snapshot = withContext(Dispatchers.IO) {
                            facade.loadUsers(session)
                        }
                        val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                        if (!runtime.isCurrent(token)) return@launch
                        pendingUsersAction = null
                        runtime.state.value = latest.copy(
                            users = snapshot.users,
                            userDetail = refreshed,
                            userRoleChangeOpen = false,
                            userRoleChanging = false,
                            userReauthOpen = false,
                            userReauthBusy = false,
                            usersError = null,
                        )
                    }
                }
            } catch (error: Throwable) {
                handleFailure(token, error)
            } finally {
                passphrase.fill('\u0000')
            }
        }
    }

    fun cancelUsersRolesReauth() {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        pendingUsersAction = null
        runtime.state.value = current.copy(
            userReauthOpen = false,
            userReauthBusy = false,
            userEnrollBusy = false,
            userRoleChanging = false,
            userEnableBusy = false,
            usersError = null,
        )
    }


    fun resetPending() {
        pendingUsersAction = null
    }

    private fun handleFailure(token: OwnerSessionToken, error: Throwable) {
        runtime.handleFailure(token, NativeOwnerMoreDestination.UsersRoles, error) { current, message ->
            current.copy(
                usersLoading = false,
                userEnrollBusy = false,
                userRoleChanging = false,
                userEnableBusy = false,
                userEnableConfirmOpen = false,
                userReauthBusy = false,
                usersError = message,
            )
        }
    }
}
