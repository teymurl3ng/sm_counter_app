package com.saaf.milkshop.nativeui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.saaf.milkshop.application.authentication.NativeAccountSummary
import com.saaf.milkshop.application.authentication.NativeAuthenticationFacade
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.authentication.NativeQuickUnlockFacade
import com.saaf.milkshop.application.authentication.NativeQuickUnlockMethod
import com.saaf.milkshop.domain.SecurityFailure
import com.saaf.milkshop.quickunlock.QuickUnlockFailure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface NativeAuthenticationState {
    data object Loading : NativeAuthenticationState
    data class StartupFailure(val message: String) : NativeAuthenticationState
    data class OwnerSetup(val busy: Boolean = false, val error: String? = null) : NativeAuthenticationState
    data class AccountPicker(
        val accounts: List<NativeAccountSummary>,
        val error: String? = null,
        /** True when opened from Cashier More → Switch account; Back restores the session. */
        val cancelReturnsToSession: Boolean = false,
    ) : NativeAuthenticationState
    data class Passphrase(
        val account: NativeAccountSummary,
        val busy: Boolean = false,
        val error: String? = null,
    ) : NativeAuthenticationState
    data class MethodPicker(
        val account: NativeAccountSummary,
        val busy: Boolean = false,
        val error: String? = null,
    ) : NativeAuthenticationState
    data class QuickPin(
        val account: NativeAccountSummary,
        val busy: Boolean = false,
        val error: String? = null,
    ) : NativeAuthenticationState
    data class RecoveryCode(
        val identity: NativeSignedInIdentity,
        val code: String,
    ) : NativeAuthenticationState
    data class Authenticated(
        val identity: NativeSignedInIdentity,
        val sessionHandle: String,
    ) : NativeAuthenticationState
}

class NativeAuthenticationViewModel(
    private val facade: NativeAuthenticationFacade,
    private val onSessionChanged: (String?) -> Unit,
) : ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow<NativeAuthenticationState>(NativeAuthenticationState.Loading)
    private var activeSessionHandle: String? = null
    private var quickUnlock: NativeQuickUnlockFacade? = null
    private var switchReturn: NativeAuthenticationState.Authenticated? = null
    val state: StateFlow<NativeAuthenticationState> = _state.asStateFlow()

    fun attachQuickUnlock(facade: NativeQuickUnlockFacade) {
        quickUnlock = facade
        if (_state.value is NativeAuthenticationState.Loading) refreshStartup()
    }

    fun detachQuickUnlock(facade: NativeQuickUnlockFacade) {
        if (quickUnlock === facade) quickUnlock = null
    }

    fun refreshStartup() {
        _state.value = NativeAuthenticationState.Loading
        scope.launch {
            _state.value = try {
                loadStartupState()
            } catch (error: Throwable) {
                NativeAuthenticationState.StartupFailure(publicMessage(error))
            }
        }
    }

    fun selectAccount(account: NativeAccountSummary) {
        val current = _state.value as? NativeAuthenticationState.AccountPicker ?: return
        if (account !in current.accounts) return
        val pending = switchReturn
        if (pending != null && account.userId == pending.identity.userId) {
            cancelAccountSwitch()
            return
        }
        // Keep the current session until a different account actually signs in (or switch is cancelled).
        _state.value = if (account.hasQuickUnlock) {
            NativeAuthenticationState.MethodPicker(account)
        } else {
            NativeAuthenticationState.Passphrase(account)
        }
    }

    fun beginAccountSwitch() {
        val current = _state.value as? NativeAuthenticationState.Authenticated ?: return
        switchReturn = current
        _state.value = NativeAuthenticationState.Loading
        scope.launch {
            try {
                when (val startup = loadStartupState()) {
                    is NativeAuthenticationState.AccountPicker -> {
                        _state.value = startup.copy(cancelReturnsToSession = true)
                    }
                    else -> {
                        switchReturn = null
                        _state.value = startup
                    }
                }
            } catch (error: Throwable) {
                val restored = switchReturn
                switchReturn = null
                _state.value = restored
                    ?: NativeAuthenticationState.StartupFailure(publicMessage(error))
            }
        }
    }

    fun cancelAccountSwitch() {
        val restored = switchReturn ?: return
        switchReturn = null
        activeSessionHandle = restored.sessionHandle
        onSessionChanged(restored.sessionHandle)
        _state.value = restored
    }

    fun isAccountSwitchPending(): Boolean = switchReturn != null

    fun chooseAnotherAccount() {
        val keepSwitch = switchReturn != null
        scope.launch {
            _state.value = try {
                val startup = loadStartupState()
                if (keepSwitch && startup is NativeAuthenticationState.AccountPicker) {
                    startup.copy(cancelReturnsToSession = true)
                } else {
                    startup
                }
            } catch (error: Throwable) {
                NativeAuthenticationState.StartupFailure(publicMessage(error))
            }
        }
    }

    fun useQuickPin() {
        val current = _state.value as? NativeAuthenticationState.MethodPicker ?: return
        if (!current.account.quickPinEnabled || current.busy) return
        _state.value = NativeAuthenticationState.QuickPin(current.account)
    }

    fun useFullPassphrase() {
        val account = when (val current = _state.value) {
            is NativeAuthenticationState.MethodPicker -> current.account.takeUnless { current.busy }
            is NativeAuthenticationState.QuickPin -> current.account.takeUnless { current.busy }
            else -> null
        } ?: return
        _state.value = NativeAuthenticationState.Passphrase(account)
    }

    fun backFromQuickPin() {
        val current = _state.value as? NativeAuthenticationState.QuickPin ?: return
        if (!current.busy) _state.value = NativeAuthenticationState.MethodPicker(current.account)
    }

    fun backFromPassphrase() {
        val current = _state.value as? NativeAuthenticationState.Passphrase ?: return
        if (current.busy) return
        if (current.account.hasQuickUnlock) {
            _state.value = NativeAuthenticationState.MethodPicker(current.account)
        } else {
            chooseAnotherAccount()
        }
    }

    fun signInWithPhoneSecurity() {
        val current = _state.value as? NativeAuthenticationState.MethodPicker ?: return
        if (!current.account.phoneSecurityEnabled || current.busy) return
        val quick = quickUnlock ?: run {
            _state.value = current.copy(error = "Phone security is unavailable. Use your full passphrase.")
            return
        }
        _state.value = current.copy(busy = true, error = null)
        scope.launch {
            try {
                val session = withContext(Dispatchers.IO) {
                    quick.signIn(current.account, NativeQuickUnlockMethod.PHONE_SECURITY, null)
                }
                acceptSession(session)
            } catch (error: Throwable) {
                _state.value = current.copy(error = publicMessage(error, current.account))
            }
        }
    }

    fun signInWithQuickPin(pin: CharArray) {
        val current = _state.value as? NativeAuthenticationState.QuickPin ?: run {
            pin.fill('\u0000')
            return
        }
        if (current.busy) {
            pin.fill('\u0000')
            return
        }
        val quick = quickUnlock ?: run {
            pin.fill('\u0000')
            _state.value = current.copy(error = "Quick PIN is unavailable. Use your full passphrase.")
            return
        }
        _state.value = current.copy(busy = true, error = null)
        scope.launch {
            try {
                val session = withContext(Dispatchers.IO) {
                    quick.signIn(current.account, NativeQuickUnlockMethod.PIN, pin)
                }
                acceptSession(session)
            } catch (error: Throwable) {
                _state.value = current.copy(error = publicMessage(error, current.account))
            } finally {
                pin.fill('\u0000')
            }
        }
    }

    fun signIn(passphrase: CharArray) {
        val current = _state.value as? NativeAuthenticationState.Passphrase ?: run {
            passphrase.fill('\u0000')
            return
        }
        if (current.busy) {
            passphrase.fill('\u0000')
            return
        }
        _state.value = current.copy(busy = true, error = null)
        scope.launch {
            try {
                val session = withContext(Dispatchers.IO) { facade.signIn(current.account, passphrase) }
                try {
                    withContext(Dispatchers.IO) { quickUnlock?.fullPassphraseAccepted(session.identity.userId) }
                    acceptSession(session)
                } catch (error: Throwable) {
                    withContext(Dispatchers.IO) { facade.signOut(session.sessionHandle) }
                    throw error
                }
            } catch (error: Throwable) {
                _state.value = current.copy(error = publicMessage(error))
            } finally {
                passphrase.fill('\u0000')
            }
        }
    }

    fun bootstrapOwner(
        loginName: String,
        displayName: String,
        passphrase: CharArray,
        confirmation: CharArray,
    ) {
        val current = _state.value as? NativeAuthenticationState.OwnerSetup ?: run {
            passphrase.fill('\u0000')
            confirmation.fill('\u0000')
            return
        }
        if (current.busy) {
            passphrase.fill('\u0000')
            confirmation.fill('\u0000')
            return
        }
        if (!passphrase.contentEquals(confirmation)) {
            passphrase.fill('\u0000')
            confirmation.fill('\u0000')
            _state.value = current.copy(error = "The two passphrases do not match.")
            return
        }
        _state.value = current.copy(busy = true, error = null)
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    quickUnlock?.clearAll()
                    facade.bootstrapOwner(loginName, displayName, passphrase)
                }
                activeSessionHandle = result.session.sessionHandle
                onSessionChanged(result.session.sessionHandle)
                _state.value = NativeAuthenticationState.RecoveryCode(
                    identity = result.session.identity,
                    code = result.oneTimeRecoveryCode,
                )
            } catch (error: Throwable) {
                _state.value = current.copy(error = publicMessage(error))
            } finally {
                passphrase.fill('\u0000')
                confirmation.fill('\u0000')
            }
        }
    }

    fun acknowledgeRecoveryCode() {
        val current = _state.value as? NativeAuthenticationState.RecoveryCode ?: return
        val sessionHandle = activeSessionHandle ?: return
        _state.value = NativeAuthenticationState.Authenticated(current.identity, sessionHandle)
    }

    /**
     * Debug/preview helper: ensure Preview Operator exists (using the current privileged
     * session when needed), then sign in as that Operator and open Operations Work.
     */
    fun openPreviewOperatorWork() {
        val privileged = activeSessionHandle
            ?: (_state.value as? NativeAuthenticationState.Authenticated)?.sessionHandle
            ?: switchReturn?.sessionHandle
        scope.launch {
            _state.value = NativeAuthenticationState.Loading
            try {
                val session = withContext(Dispatchers.IO) {
                    ensurePreviewOperatorReady(privileged)
                    facade.signInPreviewOperator()
                }
                acceptSession(session)
            } catch (error: Throwable) {
                _state.value = try {
                    when (val startup = loadStartupState()) {
                        is NativeAuthenticationState.AccountPicker -> startup.copy(
                            error = publicMessage(error),
                        )
                        is NativeAuthenticationState.OwnerSetup -> startup.copy(
                            error = publicMessage(error),
                        )
                        else -> NativeAuthenticationState.StartupFailure(publicMessage(error))
                    }
                } catch (loadError: Throwable) {
                    NativeAuthenticationState.StartupFailure(publicMessage(loadError))
                }
            }
        }
    }

    private suspend fun ensurePreviewOperatorReady(privilegedSessionHandle: String?) {
        val sample = com.saaf.milkshop.application.preview.NativePreviewSampleData
        val startup = facade.startup()
        var privilegedHandle = privilegedSessionHandle
        var temporaryOwnerHandle: String? = null
        try {
            if (startup.needsOwnerBootstrap) {
                val boot = facade.bootstrapOwner(
                    loginName = sample.PREVIEW_OWNER_LOGIN,
                    displayName = sample.PREVIEW_OWNER_DISPLAY_NAME,
                    passphrase = sample.PREVIEW_OWNER_PASSPHRASE.toCharArray(),
                )
                privilegedHandle = boot.session.sessionHandle
                temporaryOwnerHandle = privilegedHandle
            } else if (privilegedHandle == null) {
                val ownerAccount = startup.accounts.firstOrNull {
                    it.loginName == sample.PREVIEW_OWNER_LOGIN
                }
                if (ownerAccount != null) {
                    val ownerSession = runCatching {
                        facade.signIn(
                            ownerAccount,
                            sample.PREVIEW_OWNER_PASSPHRASE.toCharArray(),
                        )
                    }.getOrNull()
                    if (ownerSession != null) {
                        privilegedHandle = ownerSession.sessionHandle
                        temporaryOwnerHandle = privilegedHandle
                    }
                }
            }
            if (privilegedHandle != null) {
                facade.ensurePreviewOperator(privilegedHandle)
            }
            facade.ensurePreviewVendor()
            facade.ensurePreviewOperatorEmployeeLink()
        } finally {
            temporaryOwnerHandle?.let { handle ->
                runCatching { facade.signOut(handle) }
            }
        }
    }

    fun leaveAuthenticatedArea() {
        val sessionHandle = activeSessionHandle ?: return
        switchReturn = null
        activeSessionHandle = null
        onSessionChanged(null)
        _state.value = NativeAuthenticationState.Loading
        scope.launch {
            _state.value = try {
                withContext(Dispatchers.IO) { facade.signOut(sessionHandle) }
                loadStartupState()
            } catch (error: Throwable) {
                NativeAuthenticationState.StartupFailure(publicMessage(error))
            }
        }
    }

    fun validateAuthenticatedSession() {
        val current = _state.value as? NativeAuthenticationState.Authenticated ?: return
        val sessionHandle = activeSessionHandle ?: return
        scope.launch {
            try {
                val restored = withContext(Dispatchers.IO) { facade.restoreSession(sessionHandle) }
                if (activeSessionHandle == sessionHandle && _state.value == current) {
                    _state.value = NativeAuthenticationState.Authenticated(
                        identity = restored.identity,
                        sessionHandle = restored.sessionHandle,
                    )
                }
            } catch (_: SecurityFailure) {
                if (activeSessionHandle != sessionHandle) return@launch
                activeSessionHandle = null
                onSessionChanged(null)
                _state.value = NativeAuthenticationState.Loading
                _state.value = try {
                    when (val startup = loadStartupState()) {
                        is NativeAuthenticationState.AccountPicker -> startup.copy(
                            error = "Your session expired. Sign in again.",
                        )
                        else -> startup
                    }
                } catch (error: Throwable) {
                    NativeAuthenticationState.StartupFailure(publicMessage(error))
                }
            }
        }
    }

    override fun onCleared() {
        scope.cancel()
        super.onCleared()
    }

    private suspend fun loadStartupState(): NativeAuthenticationState {
        val startup = withContext(Dispatchers.IO) { facade.startup() }
        if (startup.needsOwnerBootstrap) return NativeAuthenticationState.OwnerSetup()
        val quick = quickUnlock
        val accounts = if (quick == null) {
            startup.accounts
        } else {
            withContext(Dispatchers.IO) { quick.decorate(startup.accounts) }
        }
        return NativeAuthenticationState.AccountPicker(accounts)
    }

    private fun acceptSession(session: com.saaf.milkshop.application.authentication.NativeAuthenticatedSession) {
        val previousSwitch = switchReturn
        switchReturn = null
        val previousHandle = activeSessionHandle
        activeSessionHandle = session.sessionHandle
        onSessionChanged(session.sessionHandle)
        _state.value = NativeAuthenticationState.Authenticated(session.identity, session.sessionHandle)
        val staleHandles = buildList {
            if (previousSwitch != null && previousSwitch.sessionHandle != session.sessionHandle) {
                add(previousSwitch.sessionHandle)
            }
            if (previousHandle != null &&
                previousHandle != session.sessionHandle &&
                previousHandle != previousSwitch?.sessionHandle
            ) {
                add(previousHandle)
            }
        }
        if (staleHandles.isNotEmpty()) {
            scope.launch {
                withContext(Dispatchers.IO) {
                    staleHandles.forEach { handle ->
                        runCatching { facade.signOut(handle) }
                    }
                }
            }
        }
    }

    private fun publicMessage(error: Throwable, account: NativeAccountSummary? = null): String = when (error) {
        is QuickUnlockFailure.Authentication -> account?.let {
            "That Quick PIN did not unlock ${it.displayName}. Check the selected account or use the full passphrase."
        } ?: error.message.orEmpty()
        is SecurityFailure,
        is QuickUnlockFailure -> error.message ?: "Sign-in could not be completed."
        is IllegalArgumentException -> error.message ?: "Check the information and try again."
        else -> "Saaf could not load account access. Try again."
    }

    class Factory(
        private val facade: NativeAuthenticationFacade,
        private val onSessionChanged: (String?) -> Unit,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(NativeAuthenticationViewModel::class.java))
            return NativeAuthenticationViewModel(facade, onSessionChanged) as T
        }
    }
}
