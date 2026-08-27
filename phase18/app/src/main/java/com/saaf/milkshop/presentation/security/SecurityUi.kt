package com.saaf.milkshop.presentation.security

import android.app.Activity
import android.app.AlertDialog
import android.text.InputType
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import com.saaf.milkshop.R
import com.saaf.milkshop.data.AppUserEntity
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.CreateUserInput
import com.saaf.milkshop.domain.PendingUserEnrollment
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.domain.SecurityFailure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Native-only credential and user-administration presentation boundary. */
class SecurityUi(
    private val activity: Activity,
    private val security: UserSecurityService,
    private val uiScope: CoroutineScope,
    private val onAuthenticated: (String, AppUserEntity) -> Unit,
    private val onSignedOut: () -> Unit,
    private val onRestoreBeforeSetup: () -> Unit,
) {
    fun start(existingSessionId: String?) {
        uiScope.launch {
            val existing = existingSessionId?.let { sessionId ->
                runCatching {
                    withContext(Dispatchers.IO) {
                        security.requirePermission(sessionId, AppPermission.VIEW_OPERATIONAL)
                    }
                }.getOrNull()?.let { sessionId to it }
            }
            if (existing != null) {
                onAuthenticated(existing.first, existing.second)
            } else if (withContext(Dispatchers.IO) { security.needsOwnerBootstrap() }) {
                showOwnerBootstrap()
            } else {
                showLogin()
            }
        }
    }

    fun logout(sessionId: String) {
        uiScope.launch {
            withContext(Dispatchers.IO) { security.logout(sessionId) }
            onSignedOut()
            showLogin()
        }
    }

    fun showChangeOwnPassphrase(sessionId: String) {
        val current = passwordField(activity.getString(R.string.current_passphrase_hint))
        val replacement = passwordField(activity.getString(R.string.new_passphrase_hint))
        val confirmation = passwordField(activity.getString(R.string.confirm_passphrase_hint))
        val error = errorView()
        val dialog = AlertDialog.Builder(activity)
            .setTitle(R.string.change_passphrase_button)
            .setView(form(current, replacement, confirmation, error))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (replacement.text.toString() != confirmation.text.toString()) {
                    showError(error, "Passphrases do not match")
                    return@setOnClickListener
                }
                uiScope.launch {
                    runCatching {
                        withContext(Dispatchers.Default) {
                            security.changeOwnPassphrase(
                                sessionId,
                                current.text.toString().toCharArray(),
                                replacement.text.toString().toCharArray(),
                            )
                        }
                    }.onSuccess {
                        dialog.dismiss()
                        onSignedOut()
                        showLogin()
                    }.onFailure { showError(error, it.message) }
                }
            }
        }
        dialog.show()
    }

    fun showReauthentication(sessionId: String, onSuccess: () -> Unit) {
        val passphrase = passwordField(activity.getString(R.string.current_passphrase_hint))
        val error = errorView()
        val dialog = AlertDialog.Builder(activity)
            .setTitle("Confirm your passphrase")
            .setMessage("This sensitive action requires recent authentication.")
            .setView(form(passphrase, error))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                uiScope.launch {
                    runCatching {
                        withContext(Dispatchers.Default) {
                            security.reauthenticate(sessionId, passphrase.text.toString().toCharArray())
                        }
                    }.onSuccess {
                        dialog.dismiss()
                        onSuccess()
                    }.onFailure { showError(error, it.message) }
                }
            }
        }
        dialog.show()
    }

    fun showUserManagement(sessionId: String) {
        uiScope.launch {
            runCatching { withContext(Dispatchers.IO) { security.listUsers(sessionId) } }
                .onSuccess { users ->
                    val labels = users.map { user ->
                        buildString {
                            append(user.displayName).append(" — ").append(user.role)
                            if (user.status != "active") append(" — disabled")
                            if (user.credentialSetupRequired) append(" — setup required")
                        }
                    }.toTypedArray()
                    AlertDialog.Builder(activity)
                        .setTitle(R.string.user_management_title)
                        .setItems(labels) { _, which -> showUserActions(sessionId, users[which]) }
                        .setPositiveButton(R.string.new_user_title) { _, _ -> showCreateUser(sessionId) }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
                }
                .onFailure { showMessage(it.message) }
        }
    }

    private fun showOwnerBootstrap() {
        val login = textField(activity.getString(R.string.login_name_hint))
        val display = textField(activity.getString(R.string.display_name_hint))
        val passphrase = passwordField(activity.getString(R.string.passphrase_hint))
        val confirmation = passwordField(activity.getString(R.string.confirm_passphrase_hint))
        val error = errorView()
        val dialog = AlertDialog.Builder(activity)
            .setTitle(R.string.first_owner_title)
            .setView(form(login, display, passphrase, confirmation, error))
            .setCancelable(false)
            .setNeutralButton("Restore existing backup") { _, _ -> onRestoreBeforeSetup() }
            .setPositiveButton(R.string.create_owner_button, null)
            .create()
        dialog.setCanceledOnTouchOutside(false)
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (passphrase.text.toString() != confirmation.text.toString()) {
                    showError(error, "Passphrases do not match")
                    return@setOnClickListener
                }
                uiScope.launch {
                    runCatching {
                        withContext(Dispatchers.Default) {
                            security.bootstrapOwner(
                                login.text.toString(),
                                display.text.toString(),
                                passphrase.text.toString().toCharArray(),
                            )
                        }
                    }.onSuccess { result ->
                        dialog.dismiss()
                        showRecoveryCode(result.recoveryCode) {
                            onAuthenticated(result.session.sessionId, result.user)
                        }
                    }.onFailure { showError(error, it.message) }
                }
            }
        }
        dialog.show()
    }

    private fun showLogin() {
        val login = textField(activity.getString(R.string.login_name_hint))
        val passphrase = passwordField(activity.getString(R.string.passphrase_hint))
        val error = errorView()
        val dialog = AlertDialog.Builder(activity)
            .setTitle(R.string.login_title)
            .setView(form(login, passphrase, error))
            .setCancelable(false)
            .setNeutralButton(R.string.recover_owner_button) { _, _ -> showOwnerRecovery() }
            .setPositiveButton(R.string.sign_in_button, null)
            .create()
        dialog.setCanceledOnTouchOutside(false)
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                uiScope.launch {
                    runCatching {
                        withContext(Dispatchers.Default) {
                            security.login(login.text.toString(), passphrase.text.toString().toCharArray())
                        }
                    }.onSuccess { session ->
                        val actor = withContext(Dispatchers.IO) {
                            security.requirePermission(session.sessionId, AppPermission.VIEW_OPERATIONAL)
                        }
                        dialog.dismiss()
                        onAuthenticated(session.sessionId, actor)
                    }.onFailure { showError(error, it.message) }
                }
            }
        }
        dialog.show()
    }

    private fun showOwnerRecovery() {
        val login = textField(activity.getString(R.string.login_name_hint))
        val code = textField(activity.getString(R.string.recovery_code_hint))
        val passphrase = passwordField(activity.getString(R.string.new_passphrase_hint))
        val confirmation = passwordField(activity.getString(R.string.confirm_passphrase_hint))
        val error = errorView()
        val dialog = AlertDialog.Builder(activity)
            .setTitle(R.string.recover_owner_button)
            .setView(form(login, code, passphrase, confirmation, error))
            .setNegativeButton(android.R.string.cancel) { _, _ -> showLogin() }
            .setPositiveButton(android.R.string.ok, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (passphrase.text.toString() != confirmation.text.toString()) {
                    showError(error, "Passphrases do not match")
                    return@setOnClickListener
                }
                uiScope.launch {
                    runCatching {
                        withContext(Dispatchers.Default) {
                            security.recoverOwner(
                                login.text.toString(),
                                code.text.toString(),
                                passphrase.text.toString().toCharArray(),
                            )
                        }
                    }.onSuccess { recovered ->
                        val actor = withContext(Dispatchers.IO) {
                            security.requirePermission(recovered.first.sessionId, AppPermission.VIEW_OPERATIONAL)
                        }
                        dialog.dismiss()
                        showRecoveryCode(recovered.second) {
                            onAuthenticated(recovered.first.sessionId, actor)
                        }
                    }.onFailure { showError(error, it.message) }
                }
            }
        }
        dialog.show()
    }

    private fun showCreateUser(sessionId: String) {
        val login = textField(activity.getString(R.string.login_name_hint))
        val display = textField(activity.getString(R.string.display_name_hint))
        val role = Spinner(activity).apply {
            adapter = ArrayAdapter(
                activity,
                android.R.layout.simple_spinner_dropdown_item,
                AppRole.entries.map { it.wireValue },
            )
        }
        val error = errorView()
        val dialog = AlertDialog.Builder(activity)
            .setTitle(R.string.new_user_title)
            .setMessage("Create the identity, then hand the device to this person so they can privately choose their passphrase.")
            .setView(form(login, display, role, error))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                uiScope.launch {
                    runCatching {
                        withContext(Dispatchers.IO) {
                            security.beginUserEnrollment(
                                sessionId,
                                CreateUserInput(
                                    login.text.toString(),
                                    display.text.toString(),
                                    role.selectedItem.toString(),
                                ),
                            )
                        }
                    }.onSuccess { pending ->
                        dialog.dismiss()
                        showEnrollmentPassphrase(sessionId, pending)
                    }.onFailure {
                        if (it is SecurityFailure.RecentAuthenticationRequired) {
                            dialog.dismiss()
                            showReauthentication(sessionId) { showCreateUser(sessionId) }
                        } else showError(error, it.message)
                    }
                }
            }
        }
        dialog.show()
    }

    private fun showEnrollmentPassphrase(sessionId: String, pending: PendingUserEnrollment) {
        val passphrase = passwordField(activity.getString(R.string.passphrase_hint))
        val confirmation = passwordField(activity.getString(R.string.confirm_passphrase_hint))
        val error = errorView()
        val dialog = AlertDialog.Builder(activity)
            .setTitle("Passphrase for ${pending.user.displayName}")
            .setMessage("The new user should enter this privately. The administrator must look away and must not record it.")
            .setView(form(passphrase, confirmation, error))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (passphrase.text.toString() != confirmation.text.toString()) {
                    showError(error, "Passphrases do not match")
                    return@setOnClickListener
                }
                uiScope.launch {
                    runCatching {
                        withContext(Dispatchers.Default) {
                            security.completeUserEnrollment(
                                pending.enrollmentToken,
                                passphrase.text.toString().toCharArray(),
                            )
                        }
                    }.onSuccess {
                        dialog.dismiss()
                        showUserManagement(sessionId)
                    }.onFailure { showError(error, it.message) }
                }
            }
        }
        dialog.show()
    }

    private fun showUserActions(sessionId: String, user: AppUserEntity) {
        val actions = mutableListOf<String>()
        if (user.credentialSetupRequired) actions += "Complete credential setup"
        else actions += activity.getString(R.string.reset_passphrase_button)
        actions += activity.getString(R.string.change_role_button)
        actions += if (user.status == "active") activity.getString(R.string.disable_user_button)
        else activity.getString(R.string.enable_user_button)
        AlertDialog.Builder(activity)
            .setTitle(user.displayName)
            .setItems(actions.toTypedArray()) { _, which ->
                when (actions[which]) {
                    "Complete credential setup" -> resumeEnrollment(sessionId, user.userId)
                    activity.getString(R.string.reset_passphrase_button) -> beginReset(sessionId, user.userId)
                    activity.getString(R.string.change_role_button) -> showRoleChange(sessionId, user)
                    else -> setEnabled(sessionId, user, user.status != "active")
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun resumeEnrollment(sessionId: String, userId: Long) {
        uiScope.launch {
            runCatching { withContext(Dispatchers.IO) { security.resumeUserEnrollment(sessionId, userId) } }
                .onSuccess { showEnrollmentPassphrase(sessionId, it) }
                .onFailure { handleSensitiveFailure(it, sessionId) { resumeEnrollment(sessionId, userId) } }
        }
    }

    private fun beginReset(sessionId: String, userId: Long) {
        uiScope.launch {
            runCatching { withContext(Dispatchers.IO) { security.beginCredentialReset(sessionId, userId) } }
                .onSuccess { showEnrollmentPassphrase(sessionId, it) }
                .onFailure { handleSensitiveFailure(it, sessionId) { beginReset(sessionId, userId) } }
        }
    }

    private fun showRoleChange(sessionId: String, user: AppUserEntity) {
        val role = Spinner(activity).apply {
            adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_dropdown_item, AppRole.entries.map { it.wireValue })
            setSelection(AppRole.entries.indexOfFirst { it.wireValue == user.role })
        }
        AlertDialog.Builder(activity)
            .setTitle(R.string.change_role_button)
            .setView(form(role))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                uiScope.launch {
                    runCatching {
                        withContext(Dispatchers.IO) {
                            security.changeRole(sessionId, user.userId, role.selectedItem.toString())
                        }
                    }.onSuccess { showUserManagement(sessionId) }
                        .onFailure { handleSensitiveFailure(it, sessionId) { showRoleChange(sessionId, user) } }
                }
            }
            .show()
    }

    private fun setEnabled(sessionId: String, user: AppUserEntity, enabled: Boolean) {
        uiScope.launch {
            runCatching { withContext(Dispatchers.IO) { security.setUserEnabled(sessionId, user.userId, enabled) } }
                .onSuccess { showUserManagement(sessionId) }
                .onFailure { handleSensitiveFailure(it, sessionId) { setEnabled(sessionId, user, enabled) } }
        }
    }

    private fun handleSensitiveFailure(error: Throwable, sessionId: String, retry: () -> Unit) {
        if (error is SecurityFailure.RecentAuthenticationRequired) {
            showReauthentication(sessionId, retry)
        } else {
            showMessage(error.message)
        }
    }

    private fun showRecoveryCode(code: String, onAcknowledged: () -> Unit) {
        val value = TextView(activity).apply {
            text = code
            textSize = 18f
            setTextIsSelectable(true)
            setPadding(dp(20), dp(16), dp(20), dp(16))
        }
        AlertDialog.Builder(activity)
            .setTitle(R.string.recovery_code_title)
            .setMessage(R.string.recovery_code_message)
            .setView(value)
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok) { _, _ -> onAcknowledged() }
            .show()
    }

    private fun form(vararg views: View): LinearLayout = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20), dp(8), dp(20), 0)
        views.forEach { view ->
            addView(view, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(8)
            })
        }
    }

    private fun textField(hintText: String) = EditText(activity).apply {
        hint = hintText
        inputType = InputType.TYPE_CLASS_TEXT
        setSingleLine(true)
    }

    private fun passwordField(hintText: String) = EditText(activity).apply {
        hint = hintText
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        setSingleLine(true)
    }

    private fun errorView() = TextView(activity).apply {
        visibility = View.GONE
        setTextColor(activity.getColor(android.R.color.holo_red_dark))
    }

    private fun showError(view: TextView, message: String?) {
        view.text = message ?: "The operation could not be completed"
        view.visibility = View.VISIBLE
    }

    private fun showMessage(message: String?) {
        AlertDialog.Builder(activity)
            .setMessage(message ?: "The operation could not be completed")
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun dp(value: Int): Int = (value * activity.resources.displayMetrics.density).toInt()
}
