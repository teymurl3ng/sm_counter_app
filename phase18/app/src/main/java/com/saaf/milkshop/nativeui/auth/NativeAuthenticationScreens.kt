package com.saaf.milkshop.nativeui.auth

import com.saaf.milkshop.nativeui.common.NativeListStyle
import com.saaf.milkshop.nativeui.common.nativeListItemContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.saaf.milkshop.nativeui.common.nativeListItem
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.saaf.milkshop.nativeui.common.hapticTap
import com.saaf.milkshop.nativeui.common.hapticSelect
import com.saaf.milkshop.nativeui.common.hapticCommit
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saaf.milkshop.application.authentication.NativeAccountSummary
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.pressScale
import com.saaf.milkshop.nativeui.common.rememberPressSource
import com.saaf.milkshop.nativeui.theme.SaafGreen
import com.saaf.milkshop.nativeui.theme.SaafInk
import com.saaf.milkshop.nativeui.theme.SaafIvory
import com.saaf.milkshop.nativeui.theme.SaafYellow
import com.saaf.milkshop.nativeui.common.NativeSectionTransition
import com.saaf.milkshop.nativeui.theme.SaafTheme

private val LocalAccessContentOnly = compositionLocalOf { false }
private data class AccessHeader(
    val title: String, val subtitle: String, val back: (() -> Unit)? = null,
    val backTag: String = NativeUiTags.AuthBackToWelcome,
)

private fun accessHeader(
    state: NativeAuthenticationState,
    welcome: () -> Unit, picker: () -> Unit, passphraseBack: () -> Unit, pinBack: () -> Unit,
): AccessHeader = when (state) {
    NativeAuthenticationState.Loading -> AccessHeader("Loading accounts", "Checking this phone’s accounts…")
    is NativeAuthenticationState.StartupFailure -> AccessHeader("Accounts could not open", state.message)
    is NativeAuthenticationState.OwnerSetup -> AccessHeader("Create the Owner account", "This is the first private account for this shop’s data vault.", welcome)
    is NativeAuthenticationState.AccountPicker -> AccessHeader(
        if (state.cancelReturnsToSession) "Switch account" else "Choose account",
        if (state.cancelReturnsToSession) "Choose another account. Back keeps you signed in."
        else "Tap your name to sign in.", welcome)
    is NativeAuthenticationState.Passphrase -> AccessHeader("Welcome back, ${state.account.displayName}",
        "Sign in with the full passphrase for this account.", passphraseBack, NativeUiTags.AuthPassphraseBack)
    is NativeAuthenticationState.MethodPicker -> AccessHeader("Unlock ${state.account.displayName}",
        "Choose a sign-in method already set up for this account on this phone.", picker, NativeUiTags.AuthMethodBack)
    is NativeAuthenticationState.QuickPin -> AccessHeader("Enter Quick PIN",
        "Enter your account PIN.", pinBack, NativeUiTags.AuthQuickPinBack)
    is NativeAuthenticationState.RecoveryCode -> AccessHeader("Save your recovery code",
        "${state.identity.displayName}, store this code somewhere private. It is shown only once.")
    is NativeAuthenticationState.Authenticated -> AccessHeader("", "")
}

@Composable
fun NativeAuthenticationHost(
    state: NativeAuthenticationState,
    onRetry: () -> Unit,
    onBackToWelcome: () -> Unit,
    onSelectAccount: (NativeAccountSummary) -> Unit,
    onChooseAnotherAccount: () -> Unit,
    onBackFromPassphrase: () -> Unit,
    onUseQuickPin: () -> Unit,
    onUsePhoneSecurity: () -> Unit,
    onUseFullPassphrase: () -> Unit,
    onBackFromQuickPin: () -> Unit,
    onQuickPinSignIn: (CharArray) -> Unit,
    onSignIn: (CharArray) -> Unit,
    onBootstrapOwner: (String, String, CharArray, CharArray) -> Unit,
    onAcknowledgeRecoveryCode: () -> Unit,
    modifier: Modifier = Modifier,
    onQuickPinEdit: () -> Unit = {},
) {
    // The neutral toolbar stays mounted; only the step's body transitions.
    if (state is NativeAuthenticationState.Authenticated) return
    val frame = accessHeader(state, onBackToWelcome, onChooseAnotherAccount,
        onBackFromPassphrase, onBackFromQuickPin)
    val focus = LocalFocusManager.current
    LaunchedEffect(state::class) { focus.clearFocus() }
    AccessFrame(frame.title, frame.subtitle, frame.back, frame.backTag, modifier) {
    CompositionLocalProvider(LocalAccessContentOnly provides true) {
    NativeSectionTransition(
        targetState = state,
        contentKey = { it::class },
        modifier = Modifier.fillMaxWidth(),
    ) { state -> when (state) {
            NativeAuthenticationState.Loading -> AccessMessageScreen(
                title = "Loading accounts",
                message = "Checking this phone’s accounts…",
                loading = true,
                modifier = modifier,
            )
            is NativeAuthenticationState.StartupFailure -> AccessMessageScreen(
                title = "Accounts could not open",
                message = state.message,
                actionLabel = "Try again",
                actionTag = NativeUiTags.AuthStartupRetry,
                onAction = onRetry,
                modifier = modifier,
            )
            is NativeAuthenticationState.OwnerSetup -> OwnerSetupScreen(
                busy = state.busy,
                error = state.error,
                onBack = onBackToWelcome,
                onCreateOwner = onBootstrapOwner,
                modifier = modifier,
            )
            is NativeAuthenticationState.AccountPicker -> AccountPickerScreen(
                accounts = state.accounts,
                error = state.error,
                cancelReturnsToSession = state.cancelReturnsToSession,
                onBack = onBackToWelcome,
                onSelectAccount = onSelectAccount,
                modifier = modifier,
            )
            is NativeAuthenticationState.Passphrase -> PassphraseSignInScreen(
                account = state.account,
                busy = state.busy,
                error = state.error,
                onBack = onBackFromPassphrase,
                onChooseAnotherAccount = onChooseAnotherAccount,
                onSignIn = onSignIn,
                modifier = modifier,
            )
            is NativeAuthenticationState.MethodPicker -> MethodPickerScreen(
                account = state.account,
                busy = state.busy,
                error = state.error,
                onBack = onChooseAnotherAccount,
                onUseQuickPin = onUseQuickPin,
                onUsePhoneSecurity = onUsePhoneSecurity,
                onUseFullPassphrase = onUseFullPassphrase,
                onChooseAnotherAccount = onChooseAnotherAccount,
                modifier = modifier,
            )
            is NativeAuthenticationState.QuickPin -> QuickPinSignInScreen(
                account = state.account,
                busy = state.busy,
                error = state.error,
                onBack = onBackFromQuickPin,
                onUseFullPassphrase = onUseFullPassphrase,
                onUsePhoneSecurity = onUsePhoneSecurity,
                onEdit = onQuickPinEdit,
                checkingPhoneSecurity = state.checkingPhoneSecurity,
                onSignIn = onQuickPinSignIn,
                modifier = modifier,
            )
            is NativeAuthenticationState.RecoveryCode -> RecoveryCodeScreen(
                displayName = state.identity.displayName,
                code = state.code,
                onAcknowledge = onAcknowledgeRecoveryCode,
                modifier = modifier,
            )
            is NativeAuthenticationState.Authenticated -> Unit
        }
    }
    }
    }
}

@Composable
private fun AccountPickerScreen(
    accounts: List<NativeAccountSummary>,
    error: String?,
    cancelReturnsToSession: Boolean,
    onBack: () -> Unit,
    onSelectAccount: (NativeAccountSummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    AccessFrame(
        title = if (cancelReturnsToSession) "Switch account" else "Choose account",
        subtitle = if (cancelReturnsToSession) {
            "Choose another account. Back keeps you signed in."
        } else {
            "Tap your name to sign in."
        },
        onBack = onBack,
        modifier = modifier,
    ) {
        if (error != null) AccessError(error)
        if (accounts.isEmpty()) {
            AccessError("No active account is ready to sign in.")
        } else {
            accounts.forEachIndexed { index, account ->
                AccountRow(account = account, onClick = com.saaf.milkshop.nativeui.common.hapticSelectionAction { onSelectAccount(account) })
                if (index != accounts.lastIndex) {
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
        if (accounts.any {
                com.saaf.milkshop.application.preview.NativePreviewSampleData.isPreviewLogin(it.loginName)
            }
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = com.saaf.milkshop.application.preview.NativePreviewSampleData.previewAccountsHint(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Staff accounts are created by an Owner or Administrator.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AccountRow(account: NativeAccountSummary, onClick: () -> Unit) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth().nativeListItem(NativeListStyle.Entity)
            .testTag(NativeUiTags.authAccount(account.loginName))
            .clickable(onClick = { context.hapticSelect(); onClick() })
            .nativeListItemContent(NativeListStyle.Entity),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.sizeIn(minWidth = 40.dp, minHeight = 40.dp),
        ) {
            Box(Modifier.padding(4.dp), contentAlignment = Alignment.Center) {
                Text(account.displayName.initials(), style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 14.sp, lineHeight = 20.sp), maxLines = 1)
            }
        }
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(account.displayName, style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold))
            Text(
                text = "${account.role.label()} · ${account.availableMethodSummary()}",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, modifier = Modifier.size(24.dp))
    }
}

private fun NativeAccountSummary.availableMethodSummary(): String = buildList {
    if (quickPinEnabled) add("Quick PIN")
    if (phoneSecurityEnabled) add(phoneSecurityLabel)
    add("full passphrase")
}.joinToString(", ")

@Composable
private fun PassphraseSignInScreen(
    account: NativeAccountSummary,
    busy: Boolean,
    error: String?,
    onBack: () -> Unit,
    onChooseAnotherAccount: () -> Unit,
    onSignIn: (CharArray) -> Unit,
    modifier: Modifier = Modifier,
) {
    var passphrase by remember(account.loginName) { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    fun submit() {
        if (busy || passphrase.isBlank()) return
        val secret = passphrase.toCharArray()
        passphrase = ""
        focusManager.clearFocus()
        onSignIn(secret)
    }

    AccessFrame(
        title = "Welcome back, ${account.displayName}",
        subtitle = "Sign in with the full passphrase for this account.",
        onBack = onBack,
        backTag = NativeUiTags.AuthPassphraseBack,
        modifier = modifier,
    ) {
        IdentityCard(account)
        Spacer(Modifier.height(18.dp))
        OutlinedTextField(
            value = passphrase,
            onValueChange = { passphrase = it },
            label = { Text("Full passphrase") },
            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            singleLine = true,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().testTag(NativeUiTags.AuthPassphraseInput),
        )
        if (error != null && passphrase.isBlank()) {
            Spacer(Modifier.height(10.dp))
            AccessError(error)
        }
        Spacer(Modifier.height(18.dp))
        AccessPrimaryButton(
            label = if (busy) "Signing in…" else "Sign in",
            tag = NativeUiTags.AuthSignIn,
            enabled = !busy && passphrase.isNotBlank(),
            onClick = ::submit,
        )
        TextButton(
            onClick = com.saaf.milkshop.nativeui.common.hapticAction(onChooseAnotherAccount),
            enabled = !busy,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag(NativeUiTags.AuthChooseAnotherAccount),
        ) {
            Text("Choose another account")
        }
    }
}

@Composable
private fun MethodPickerScreen(
    account: NativeAccountSummary,
    busy: Boolean,
    error: String?,
    onBack: () -> Unit,
    onUseQuickPin: () -> Unit,
    onUsePhoneSecurity: () -> Unit,
    onUseFullPassphrase: () -> Unit,
    onChooseAnotherAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AccessFrame(
        title = "Unlock ${account.displayName}",
        subtitle = "Choose a sign-in method already set up for this account on this phone.",
        onBack = onBack,
        backTag = NativeUiTags.AuthMethodBack,
        modifier = modifier,
    ) {
        IdentityCard(account)
        Spacer(Modifier.height(18.dp))
        Text(
            text = "AVAILABLE FOR THIS ACCOUNT",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        if (account.quickPinEnabled) {
            AccessMethodButton(
                label = "Quick PIN",
                supporting = "Use this account's PIN on this phone",
                tag = NativeUiTags.AuthQuickPinMethod,
                enabled = !busy,
                icon = { Icon(Icons.Outlined.Dialpad, contentDescription = null) },
                onClick = onUseQuickPin,
            )
            Spacer(Modifier.height(10.dp))
        }
        if (account.phoneSecurityEnabled) {
            AccessMethodButton(
                label = if (busy) "Waiting for phone security…" else account.phoneSecurityLabel,
                supporting = "Use Android's enrolled phone security",
                tag = NativeUiTags.AuthPhoneSecurityMethod,
                enabled = !busy,
                icon = { Icon(Icons.Outlined.PhoneAndroid, contentDescription = null) },
                onClick = onUsePhoneSecurity,
            )
            Spacer(Modifier.height(10.dp))
        }
        AccessMethodButton(
            label = "Full passphrase",
            supporting = "Always available for this account",
            tag = NativeUiTags.AuthFullPassphraseMethod,
            enabled = !busy,
            icon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
            onClick = onUseFullPassphrase,
        )
        if (error != null) {
            Spacer(Modifier.height(12.dp))
            AccessError(error)
        }
        Spacer(Modifier.height(8.dp))
        TextButton(
            onClick = com.saaf.milkshop.nativeui.common.hapticAction(onChooseAnotherAccount),
            enabled = !busy,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag(NativeUiTags.AuthChooseAnotherAccount),
        ) {
            Text("Choose another account")
        }
    }
}

@Composable
private fun QuickPinSignInScreen(
    account: NativeAccountSummary,
    busy: Boolean,
    error: String?,
    onBack: () -> Unit,
    onUseFullPassphrase: () -> Unit,
    onUsePhoneSecurity: () -> Unit,
    onEdit: () -> Unit,
    checkingPhoneSecurity: Boolean,
    onSignIn: (CharArray) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pin by remember(account.userId) { mutableStateOf("") }
    val submit = {
        if (!busy && pin.length in 4..12) {
            val secret = pin.toCharArray()
            pin = ""
            onSignIn(secret)
        }
    }
    val edit: (String) -> Unit = { entered ->
        if (!busy) { pin = entered; onEdit() }
    }
    // State replacements after an attempt keep the same content pane and keypad anchors.
    AccessFrame("Enter Quick PIN", "Enter your account PIN.", onBack,
        NativeUiTags.AuthQuickPinBack, modifier) {
        IdentityCard(account)
        Spacer(Modifier.height(12.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Column(Modifier.widthIn(max = 320.dp).fillMaxWidth()) {
                QuickPinKeypad(pin.length, !busy,
                    onDigit = { if (pin.length < 12) edit(pin + it) },
                    onClear = { edit("") }, onDelete = { edit(pin.dropLast(1)) }, onSubmit = submit)
                Spacer(Modifier.height(12.dp))
                AccessPrimaryButton(
                    label = if (busy && !checkingPhoneSecurity) "Checking PIN…" else "Unlock",
                    tag = NativeUiTags.AuthQuickPinSubmit,
                    enabled = !busy && pin.length in 4..12, onClick = submit,
                )
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().heightIn(min = 24.dp)) {
                    if (error != null) AccessError(error)
                    else if (checkingPhoneSecurity) Text("Waiting for phone security…",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.semantics { liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite })
                }
                if (account.phoneSecurityEnabled) {
                    TextButton(onClick = com.saaf.milkshop.nativeui.common.hapticAction {
                        pin = ""; onUsePhoneSecurity()
                    }, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .testTag(NativeUiTags.AuthPhoneSecurityMethod)) { Text(account.phoneSecurityLabel) }
                }
                TextButton(onClick = com.saaf.milkshop.nativeui.common.hapticAction {
                    pin = ""; onUseFullPassphrase()
                }, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .testTag(NativeUiTags.AuthQuickPinUsePassphrase)) { Text("Use full passphrase") }
            }
        }
    }
}

@Composable
private fun OwnerSetupScreen(
    busy: Boolean,
    error: String?,
    onBack: () -> Unit,
    onCreateOwner: (String, String, CharArray, CharArray) -> Unit,
    modifier: Modifier = Modifier,
) {
    var displayName by remember { mutableStateOf("") }
    var loginName by remember { mutableStateOf("") }
    var passphrase by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    fun submit() {
        if (busy || displayName.isBlank() || loginName.isBlank() || passphrase.isBlank() || confirmation.isBlank()) return
        val secret = passphrase.toCharArray()
        val repeated = confirmation.toCharArray()
        passphrase = ""
        confirmation = ""
        focusManager.clearFocus()
        onCreateOwner(loginName, displayName, secret, repeated)
    }

    AccessFrame(
        title = "Create the Owner account",
        subtitle = "This is the first private account for this shop’s data vault.",
        onBack = onBack,
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text("Your name") },
            leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().testTag(NativeUiTags.AuthOwnerDisplayInput),
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = loginName,
            onValueChange = { loginName = it },
            label = { Text("Login name") },
            supportingText = { Text("Use a name you will recognize on the account picker.") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().testTag(NativeUiTags.AuthOwnerLoginInput),
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = passphrase,
            onValueChange = { passphrase = it },
            label = { Text("Full passphrase") },
            supportingText = { Text("At least 15 characters. Keep it private.") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            singleLine = true,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().testTag(NativeUiTags.AuthOwnerPassphraseInput),
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = confirmation,
            onValueChange = { confirmation = it },
            label = { Text("Confirm full passphrase") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            singleLine = true,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().testTag(NativeUiTags.AuthOwnerConfirmationInput),
        )
        if (error != null) {
            Spacer(Modifier.height(10.dp))
            AccessError(error)
        }
        Spacer(Modifier.height(18.dp))
        AccessPrimaryButton(
            label = if (busy) "Creating Owner…" else "Create Owner",
            tag = NativeUiTags.AuthCreateOwner,
            enabled = !busy && displayName.isNotBlank() && loginName.isNotBlank() &&
                passphrase.isNotBlank() && confirmation.isNotBlank(),
            onClick = ::submit,
        )
    }
}

@Composable
private fun RecoveryCodeScreen(
    displayName: String,
    code: String,
    onAcknowledge: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AccessFrame(
        title = "Save your recovery code",
        subtitle = "$displayName, store this code somewhere private. It is shown only once.",
        onBack = null,
        modifier = modifier,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Outlined.Key, contentDescription = null, modifier = Modifier.size(28.dp))
                Spacer(Modifier.height(10.dp))
                Text(
                    text = code,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = "This code can recover the Owner account if the full passphrase is lost. Anyone with the code can attempt recovery, so do not leave it beside this phone.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        AccessPrimaryButton(
            label = "I saved this code",
            tag = NativeUiTags.AuthRecoveryAcknowledge,
            onClick = onAcknowledge,
        )
    }
}

@Composable
private fun AccessMessageScreen(
    title: String,
    message: String,
    loading: Boolean = false,
    actionLabel: String? = null,
    actionTag: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    AccessFrame(title = title, subtitle = message, onBack = null, modifier = modifier) {
        if (loading) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        if (actionLabel != null && actionTag != null && onAction != null) {
            AccessPrimaryButton(label = actionLabel, tag = actionTag, onClick = onAction)
        }
    }
}

@Composable
private fun AccessFrame(
    title: String,
    subtitle: String,
    onBack: (() -> Unit)?,
    backTag: String = NativeUiTags.AuthBackToWelcome,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (LocalAccessContentOnly.current) {
        Column(Modifier.fillMaxWidth(), content = content)
        return
    }
    val context = LocalContext.current
    val scroll = rememberScrollState()
    LaunchedEffect(title) { scroll.scrollTo(0) }
    val density = LocalDensity.current
    val toolbarStyle = MaterialTheme.typography.bodyMedium.copy(
        fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
    val metrics = rememberTextMeasurer().measure("Ag", style = toolbarStyle, maxLines = 1, softWrap = false)
    val toolbarHeight = with(density) { metrics.size.height.toDp() + 16.dp }.coerceAtLeast(56.dp)
    Surface(color = MaterialTheme.colorScheme.background, modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp)
                .height(toolbarHeight), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(48.dp), contentAlignment = Alignment.CenterStart) {
                    if (onBack != null) {
                        androidx.compose.material3.IconButton(
                            onClick = { context.hapticTap(); onBack() },
                            modifier = Modifier.size(48.dp).testTag(backTag),
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                // Text semantics retains the complete shop name even when the toolbar ellipsizes.
                Text(com.saaf.milkshop.nativeui.common.LocalShopName.current,
                    modifier = Modifier.weight(1f), style = toolbarStyle,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scroll)
                    .padding(horizontal = 16.dp, vertical = 24.dp),
            ) {
                Text(compactAccessTitle(title), modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold))
                Spacer(Modifier.height(8.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(20.dp))
                content()
            }
        }
    }
}

private fun compactAccessTitle(title: String): String = when {
    title.startsWith("Welcome back,") -> "Sign in"
    title.startsWith("Unlock ") -> "Sign-in method"
    title == "Who is signing in?" -> "Choose account"
    title == "Create the Owner account" -> "Create Owner"
    title == "Save your recovery code" -> "Recovery code"
    title == "Enter Quick PIN" -> "Quick PIN"
    title == "Accounts could not open" -> "Accounts unavailable"
    else -> title
}

@Composable
private fun IdentityCard(account: NativeAccountSummary) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(account.displayName, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${account.role.label()} · ${account.loginName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AccessError(message: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(12.dp).testTag(NativeUiTags.AuthError),
        )
    }
}

@Composable
private fun AccessPrimaryButton(
    label: String,
    tag: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val press = rememberPressSource()
    Button(
        interactionSource = press,
        onClick = { context.hapticCommit(); onClick() },
        enabled = enabled,
        shape = RoundedCornerShape(13.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
        ),
        modifier = Modifier.pressScale(press).fillMaxWidth().heightIn(min = 54.dp).testTag(tag),
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun AccessMethodButton(
    label: String,
    supporting: String,
    tag: String,
    enabled: Boolean,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val press = rememberPressSource()
    OutlinedButton(
        onClick = { context.hapticSelect(); onClick() },
        enabled = enabled,
        shape = RoundedCornerShape(13.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.pressScale(press).fillMaxWidth().heightIn(min = 68.dp).testTag(tag),
        interactionSource = press,
    ) {
        icon()
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(
                supporting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null)
    }
}

private fun String.initials(): String = trim()
    .split(Regex("\\s+"))
    .filter(String::isNotBlank)
    .take(2)
    .joinToString("") { it.take(1).uppercase() }
    .ifBlank { "?" }

private fun AppRole.label(): String = when (this) {
    AppRole.OWNER -> "Owner"
    AppRole.ADMIN -> "Administrator"
    AppRole.MANAGER -> "Manager"
    AppRole.CASHIER -> "Cashier"
    AppRole.OPERATOR -> "Operations"
}
