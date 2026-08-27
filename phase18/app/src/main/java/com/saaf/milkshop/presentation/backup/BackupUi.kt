package com.saaf.milkshop.presentation.backup

import android.app.Activity
import android.app.AlertDialog
import android.net.Uri
import android.widget.EditText
import android.widget.TextView
import com.saaf.milkshop.BackupManager
import com.saaf.milkshop.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Presentation-only owner for Backup & Restore dialogs and status UI.
 *
 * Backup creation/restoration itself remains in MainActivity's existing
 * BackupManager calls. This class only owns dialog construction, selection
 * state, and the callbacks that trigger those existing operations.
 */
class BackupUi(
    private val activity: Activity,
    private val backupManager: BackupManager,
    private val uiScope: CoroutineScope,
    private val viewModel: BackupViewModel,
    private val onChooseExternalBackup: () -> Unit,
    private val onRestoreRequested: (Uri, BackupManager.RestoreSelection) -> Unit,
) {
    private val statusText: TextView = activity.findViewById(R.id.backupStatusText)

    private fun renderState() {
        statusText.text = viewModel.state.statusMessage
    }

    fun showRestoreSourceDialog() {
        viewModel.clearStatus()
        renderState()
        uiScope.launch {
            val locals = withContext(Dispatchers.IO) {
                backupManager.localBackups()
            }
            val labels = locals.map { (_, meta) -> meta.fileName }.toTypedArray()
            AlertDialog.Builder(activity)
                .setTitle(R.string.backup_restore_button)
                .setItems(labels) { _, which ->
                    showRestoreSelectionDialog(Uri.fromFile(locals[which].first))
                }
                .setNeutralButton("Choose external backup…") { _, _ ->
                    onChooseExternalBackup()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    fun showRestoreSelectionDialog(uri: Uri) {
        uiScope.launch {
            backupManager.readBackupMeta(uri).onSuccess { meta ->
                val labels = arrayOf(
                    activity.getString(R.string.backup_group_business),
                    activity.getString(R.string.backup_group_planning),
                    activity.getString(R.string.backup_group_preferences),
                )
                val checked = booleanArrayOf(true, true, true)
                AlertDialog.Builder(activity)
                    .setTitle(R.string.backup_select_groups_title)
                    .setMultiChoiceItems(labels, checked) { _, which, isChecked -> checked[which] = isChecked }
                    .setMessage(
                        activity.getString(
                            R.string.backup_meta_format,
                            meta.createdAt,
                            meta.appVersion,
                            meta.schemaVersion,
                        )
                    )
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        val selection = BackupManager.RestoreSelection(
                            checked[0],
                            checked[1],
                            checked[2],
                        )
                        if (!selection.business && !selection.planning && !selection.preferences) {
                            statusText.text = "Choose at least one restore group."
                        } else {
                            showRestoreWarning(uri, selection)
                        }
                    }
                    .show()
            }.onFailure { error ->
                val failure = error as? BackupManager.BackupFailure
                val message = activity.getString(
                    R.string.backup_restore_failed,
                    failure?.reason ?: (error.message ?: "Invalid backup"),
                    failure?.recovery ?: "No restore was attempted.",
                )
                viewModel.setStatus(message)
                renderState()
            }
        }
    }

    private fun showRestoreWarning(uri: Uri, selection: BackupManager.RestoreSelection) {
        val groups = buildString {
            if (selection.business) append("• ").append(activity.getString(R.string.backup_group_business)).append('\n')
            if (selection.planning) append("• ").append(activity.getString(R.string.backup_group_planning)).append('\n')
            if (selection.preferences) append("• ").append(activity.getString(R.string.backup_group_preferences)).append('\n')
        }.trim()
        AlertDialog.Builder(activity)
            .setTitle(R.string.backup_confirmation_title)
            .setMessage(activity.getString(R.string.backup_restore_warning, groups))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.backup_restore_button) { _, _ ->
                showTypedRestoreConfirmation(uri, selection)
            }
            .show()
    }

    private fun showTypedRestoreConfirmation(uri: Uri, selection: BackupManager.RestoreSelection) {
        val input = EditText(activity).apply { hint = BackupManager.CONFIRMATION_PHRASE }
        val dialog = AlertDialog.Builder(activity)
            .setTitle(R.string.backup_confirmation_title)
            .setMessage(
                activity.getString(
                    R.string.backup_confirmation_prompt,
                    BackupManager.CONFIRMATION_PHRASE,
                )
            )
            .setView(input)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.backup_restore_button, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (input.text.toString() != BackupManager.CONFIRMATION_PHRASE) {
                    input.error = activity.getString(R.string.backup_confirmation_mismatch)
                    return@setOnClickListener
                }
                dialog.dismiss()
                onRestoreRequested(uri, selection)
            }
        }
        dialog.show()
    }
}
