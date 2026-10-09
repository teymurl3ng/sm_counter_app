package com.saaf.milkshop.cloud

import android.accounts.Account
import android.app.Activity
import android.Manifest
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.text.InputType
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.RevokeAccessRequest
import com.google.android.gms.common.api.Scope
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.saaf.milkshop.BuildConfig
import com.saaf.milkshop.MilkShopApplication
import com.saaf.milkshop.domain.AppPasswordPolicy
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class CloudBackupScheduler(context: Context) {
    private val workManager = WorkManager.getInstance(context.applicationContext)

    fun apply(enabled: Boolean, allowMetered: Boolean) {
        if (!enabled) {
            workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<CloudBackupWorker>(
            CloudBackupSchedulingPolicy.PERIODIC_INTERVAL_HOURS,
            TimeUnit.HOURS,
        )
            .setConstraints(CloudBackupSchedulingPolicy.periodicConstraints(allowMetered))
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                CloudBackupSchedulingPolicy.BACKOFF_MINUTES,
                TimeUnit.MINUTES,
            )
            .addTag(WORK_TAG)
            .build()
        workManager.enqueueUniquePeriodicWork(PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun runNow() {
        val request = OneTimeWorkRequestBuilder<CloudBackupWorker>()
            .setConstraints(CloudBackupSchedulingPolicy.immediateConstraints())
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                CloudBackupSchedulingPolicy.BACKOFF_MINUTES,
                TimeUnit.MINUTES,
            )
            .addTag(WORK_TAG)
            .build()
        workManager.enqueueUniqueWork(IMMEDIATE_WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    fun cancelAll() {
        workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
        workManager.cancelUniqueWork(IMMEDIATE_WORK_NAME)
    }

    companion object {
        const val PERIODIC_WORK_NAME = "saaf-milk-cloud-backup-periodic-v1"
        const val IMMEDIATE_WORK_NAME = "saaf-milk-cloud-backup-immediate-v1"
        const val WORK_TAG = "saaf-milk-cloud-backup"
    }
}

internal object CloudBackupSchedulingPolicy {
    const val PERIODIC_INTERVAL_HOURS = 24L
    const val BACKOFF_MINUTES = 30L

    fun periodicConstraints(allowMetered: Boolean): Constraints = Constraints.Builder()
        .setRequiredNetworkType(if (allowMetered) NetworkType.CONNECTED else NetworkType.UNMETERED)
        .setRequiresBatteryNotLow(true)
        .setRequiresStorageNotLow(true)
        .build()

    fun immediateConstraints(): Constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
}

class CloudBackupWorker(
    appContext: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(appContext, parameters) {
    override suspend fun doWork(): Result {
        val app = applicationContext as MilkShopApplication
        val state = app.cloudBackupSettings.read() ?: return Result.success()
        if (!state.enabled) return Result.success()
        val notifier = CloudBackupNotifier(applicationContext)
        return try {
            app.cloudBackupCoordinator(GoogleAuthorizationTokenSource(applicationContext)).runBackup()
            Result.success()
        } catch (_: CloudBackupFailure.Network) {
            val latest = runCatching { app.cloudBackupSettings.read() }.getOrNull() ?: state
            if (CloudBackupWorkerPolicy.shouldNotifyNetwork(latest)) {
                notifier.actionRequired("Cloud backup is delayed", "The encrypted backup is still waiting for a working network connection.")
            }
            Result.retry()
        } catch (_: CloudBackupFailure.Quota) {
            notifier.actionRequired("Cloud backup needs attention", "Google Drive storage is full. Existing recovery points were kept.")
            Result.failure()
        } catch (_: CloudBackupFailure.AuthorizationRequired) {
            notifier.actionRequired("Reconnect Google Drive", "Open Backup & restore as the owner to resume automatic protection.")
            Result.failure()
        } catch (_: CloudBackupFailure.AccountMismatch) {
            notifier.actionRequired("Cloud-backup account changed", "Reconnect the Google account selected for this backup vault.")
            Result.failure()
        } catch (_: CloudBackupFailure.KeyUnavailable) {
            notifier.actionRequired("Cloud-backup key needs attention", "Open Backup & restore as the owner to create a new backup vault.")
            Result.failure()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            if (runAttemptCount < MAX_UNCLASSIFIED_RETRIES) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val MAX_UNCLASSIFIED_RETRIES = 3
    }
}

internal object CloudBackupWorkerPolicy {
    private val STALE_AFTER = Duration.ofHours(48)
    private val REPEATED_NETWORK_FAILURE_AFTER = Duration.ofHours(24)

    fun shouldNotifyNetwork(
        state: CloudBackupState,
        now: Instant = Instant.now(),
    ): Boolean {
        val protectedAt = state.lastSuccessAt ?: state.configuredAt
        if (Duration.between(Instant.parse(protectedAt), now) > STALE_AFTER) return true
        val failureStartedAt = state.lastFailureStartedAt ?: return false
        return state.lastErrorCode == "network" &&
            Duration.between(Instant.parse(failureStartedAt), now) >= REPEATED_NETWORK_FAILURE_AFTER
    }
}

class GoogleAuthorizationTokenSource(context: Context) : CloudAccessTokenSource {
    private val authorizationClient = Identity.getAuthorizationClient(context.applicationContext)
    @Volatile private var cachedToken: String? = null

    override suspend fun accessToken(): String {
        cachedToken?.let { return it }
        val result = authorizationClient.authorize(authorizationRequest()).await()
        if (result.hasResolution()) throw CloudBackupFailure.AuthorizationRequired()
        return (result.accessToken ?: throw CloudBackupFailure.AuthorizationRequired("Google Drive did not issue an access token"))
            .also { cachedToken = it }
    }
}

data class InteractiveCloudAuthorizationResult(
    val selectedEmail: String,
    val accessToken: String,
)

interface InteractiveCloudAuthorization {
    suspend fun requestNewRecoveryPassphrase(): CharArray
    suspend fun requestRecoveryPassphrase(): CharArray
    suspend fun connect(): InteractiveCloudAuthorizationResult
    suspend fun revokeSelectedAccount(expectedPermissionId: String)
    suspend fun requestNotificationPermission(): Boolean
}

class HybridGoogleDriveAuthorization(
    private val activity: ComponentActivity,
) : InteractiveCloudAuthorization, AutoCloseable {
    private val credentialManager = CredentialManager.create(activity)
    private val authorizationClient = Identity.getAuthorizationClient(activity)
    private val mutex = Mutex()
    private var pendingResolution: CompletableDeferred<Intent?>? = null
    private var pendingNotificationPermission: CompletableDeferred<Boolean>? = null
    private var pendingRecoverySecret: CompletableDeferred<CharArray>? = null
    private var recoveryDialog: AlertDialog? = null
    private val resolutionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val pending = pendingResolution ?: return@registerForActivityResult
        pendingResolution = null
        if (result.resultCode == Activity.RESULT_OK) pending.complete(result.data)
        else pending.completeExceptionally(CloudBackupFailure.AuthorizationRequired("Google Drive authorization was cancelled"))
    }
    private val notificationPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        pendingNotificationPermission?.complete(granted)
        pendingNotificationPermission = null
    }

    override suspend fun connect(): InteractiveCloudAuthorizationResult = mutex.withLock {
        val email = selectGoogleAccount()
        val initial = authorizationClient.authorize(authorizationRequest()).await()
        val completed = if (initial.hasResolution()) {
            val pendingIntent = initial.pendingIntent ?: throw CloudBackupFailure.AuthorizationRequired()
            val deferred = CompletableDeferred<Intent?>()
            pendingResolution = deferred
            resolutionLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
            val intent = deferred.await()
            authorizationClient.getAuthorizationResultFromIntent(intent)
        } else initial
        val token = completed.accessToken
            ?: throw CloudBackupFailure.AuthorizationRequired("Google Drive did not issue an access token")
        InteractiveCloudAuthorizationResult(email, token)
    }

    override suspend fun requestNewRecoveryPassphrase(): CharArray {
        googleWebClientId()
        return requestRecoverySecret(confirm = true)
    }

    override suspend fun requestRecoveryPassphrase(): CharArray {
        googleWebClientId()
        return requestRecoverySecret(confirm = false)
    }

    override suspend fun revokeSelectedAccount(expectedPermissionId: String) = mutex.withLock {
        val email = selectGoogleAccount()
        val token = GoogleAuthorizationTokenSource(activity).accessToken()
        val identity = GoogleDriveCloudBackupProvider(FixedCloudAccessTokenSource(token)).accountIdentity()
        if (identity.permissionId != expectedPermissionId) throw CloudBackupFailure.AccountMismatch()
        val request = RevokeAccessRequest.builder()
            .setAccount(Account(email, GOOGLE_ACCOUNT_TYPE))
            .setScopes(requestedScopes())
            .build()
        authorizationClient.revokeAccess(request).await()
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
    }

    override suspend fun requestNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return true
        val deferred = CompletableDeferred<Boolean>()
        pendingNotificationPermission = deferred
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        return deferred.await()
    }

    private suspend fun selectGoogleAccount(): String {
        val clientId = googleWebClientId()
        val nonce = ByteArray(32).also(SecureRandom()::nextBytes)
        val option = GetSignInWithGoogleOption.Builder(clientId)
            .setNonce(Base64.getUrlEncoder().withoutPadding().encodeToString(nonce))
            .build()
        val result = credentialManager.getCredential(
            context = activity,
            request = GetCredentialRequest.Builder().addCredentialOption(option).build(),
        )
        val credential = result.credential
        if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            throw CloudBackupFailure.AuthorizationRequired("Choose a Google account to continue")
        }
        val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
        return googleCredential.email ?: googleCredential.id
    }

    private fun googleWebClientId(): String = BuildConfig.GOOGLE_WEB_CLIENT_ID.trim().also { clientId ->
        if (clientId.isEmpty()) {
            throw CloudBackupFailure.NotConfigured(
                "This build has no Google OAuth web client ID. Set -Psaaf.googleWebClientId before packaging.",
            )
        }
    }

    private suspend fun requestRecoverySecret(confirm: Boolean): CharArray = withContext(Dispatchers.Main.immediate) {
        check(pendingRecoverySecret == null) { "A cloud recovery prompt is already open" }
        val deferred = CompletableDeferred<CharArray>()
        pendingRecoverySecret = deferred
        val padding = (20 * activity.resources.displayMetrics.density).toInt()
        val layout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding / 2, padding, 0)
        }
        val first = EditText(activity).apply {
            hint = if (confirm) "New recovery passphrase" else "Cloud recovery passphrase"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val repeated = EditText(activity).apply {
            hint = "Repeat recovery passphrase"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            visibility = if (confirm) android.view.View.VISIBLE else android.view.View.GONE
        }
        val error = TextView(activity).apply { setTextColor(0xffb91c1c.toInt()) }
        layout.addView(first)
        layout.addView(repeated)
        layout.addView(error)
        val dialog = AlertDialog.Builder(activity)
            .setTitle(if (confirm) "Protect cloud recovery" else "Open cloud recovery point")
            .setMessage(if (confirm) "Use a separate passphrase of at least 15 characters and keep it outside this phone." else "Enter the separate passphrase used when cloud backup was connected.")
            .setView(layout)
            .setPositiveButton(if (confirm) "Continue" else "Inspect", null)
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                if (!deferred.isCompleted) deferred.completeExceptionally(CloudBackupFailure.UserCancelled())
            }
            .setOnCancelListener {
                if (!deferred.isCompleted) deferred.completeExceptionally(CloudBackupFailure.UserCancelled())
            }
            .create()
        recoveryDialog = dialog
        dialog.setOnShowListener {
            dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val secret = first.text.toString().toCharArray()
                val confirmation = repeated.text.toString().toCharArray()
                try {
                    if (confirm && !secret.contentEquals(confirmation)) {
                        throw IllegalArgumentException("The two recovery passphrases do not match")
                    }
                    AppPasswordPolicy.validateBackupPassphrase(secret)
                    if (deferred.complete(secret)) dialog.dismiss() else secret.fill('\u0000')
                } catch (validation: IllegalArgumentException) {
                    secret.fill('\u0000')
                    error.text = validation.message ?: "Choose a valid recovery passphrase"
                } finally {
                    confirmation.fill('\u0000')
                    first.text?.clear()
                    repeated.text?.clear()
                }
            }
        }
        dialog.setOnDismissListener {
            recoveryDialog = null
            pendingRecoverySecret = null
            if (!deferred.isCompleted) deferred.completeExceptionally(CloudBackupFailure.UserCancelled())
        }
        dialog.show()
        deferred.await()
    }

    override fun close() {
        pendingResolution?.cancel()
        pendingResolution = null
        pendingNotificationPermission?.cancel()
        pendingNotificationPermission = null
        pendingRecoverySecret?.cancel()
        pendingRecoverySecret = null
        recoveryDialog?.dismiss()
        recoveryDialog = null
    }

    companion object {
        private const val GOOGLE_ACCOUNT_TYPE = "com.google"
    }
}

private const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"

private fun requestedScopes(): List<Scope> = listOf(Scope(DRIVE_APPDATA_SCOPE))

private fun authorizationRequest(): AuthorizationRequest = AuthorizationRequest.builder()
    .setRequestedScopes(requestedScopes())
    .build()

class CloudBackupNotifier(private val context: Context) {
    fun actionRequired(title: String, message: String) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Backup attention", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Alerts only when automatic cloud backup needs owner action"
                },
            )
        }
        val openApp = PendingIntent.getActivity(
            context,
            0,
            // M27: the retired WebView shell is no longer a notification
            // target — taps land on the native launcher activity.
            Intent(context, com.saaf.milkshop.nativeui.NativePreviewActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val CHANNEL_ID = "cloud_backup_attention"
        private const val NOTIFICATION_ID = 1040
    }
}
