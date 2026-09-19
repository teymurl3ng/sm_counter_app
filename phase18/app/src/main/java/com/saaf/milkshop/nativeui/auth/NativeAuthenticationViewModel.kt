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
    data class Authenticated(val identity: NativeSignedInIdentity) : NativeAuthenticationState
}

class NativeAuthenticationViewModel(
    private val facade: NativeAuthenticationFacade,
    private val onSessionChanged: (String?) -> Unit,
) : ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow<NativeAuthenticationState>(NativeAuthenticationState.Loading)
    private var activeSessionHandle: String? = null
    private var quickUnlock: NativeQuickUnlockFacade? = null
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
        _state.value = if (account.hasQuickUnlock) {
            NativeAuthenticationState.MethodPicker(account)
        } else {
            NativeAuthenticationState.Passphrase(account)
        }
    }

    fun chooseAnotherAccount() {
        scope.launch {
            _state.value = try {
                loadStartupState()
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
        _state.value = NativeAuthenticationState.Authenticated(current.identity)
    }

    fun leaveAuthenticatedArea() {
        val sessionHandle = activeSessionHandle ?: return
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
        activeSessionHandle = session.sessionHandle
        onSessionChanged(session.sessionHandle)
        _state.value = NativeAuthenticationState.Authenticated(session.identity)
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
