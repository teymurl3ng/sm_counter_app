package com.saaf.milkshop.application.bridge

import android.util.Log
import com.saaf.milkshop.BuildConfig
import com.saaf.milkshop.application.readmodels.HybridReadModelService
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.SecurityFailure
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

class HybridBridgeDispatcher(
    database: MilkShopDatabase,
    security: com.saaf.milkshop.domain.UserSecurityService,
    private val onSessionChanged: (String?) -> Unit = {},
    backupManager: com.saaf.milkshop.BackupManager? = null,
    backupDocuments: BackupDocumentPicker? = null,
    application: com.saaf.milkshop.MilkShopApplication? = null,
    cloudAuthorization: com.saaf.milkshop.cloud.InteractiveCloudAuthorization? = null,
    private val quickUnlockService: com.saaf.milkshop.quickunlock.QuickUnlockService? = null,
) {
    private val reads = HybridReadModelService(database, security)
    private val authentication = AuthenticationBridgeHandler(reads, security, onSessionChanged, quickUnlockService)
    private val quickUnlock = quickUnlockService?.let { QuickUnlockBridgeHandler(reads, it, onSessionChanged) }
    private val securityAdministration = SecurityAdministrationBridgeHandler(reads, SecurityAdministrationBridgeCommandService(database, security))
    private val masterData = MasterDataBridgeHandler(reads, HybridCommandService(database, security))
    private val employees = EmployeeBridgeHandler(reads, EmployeeBridgeCommandService(database, security))
    private val settings = SettingsBridgeHandler(reads, SettingsBridgeCommandService(database, security))
    private val operations = OperationsBridgeHandler(reads, OperationsBridgeCommandService(database, security))
    private val deliveries = DeliveryBridgeHandler(reads, DeliveryBridgeCommandService(database, security))
    private val ledgers = LedgerBridgeHandler(reads)
    private val history = HistoryBridgeHandler(reads, HistoryBridgeCommandService(database, security))
    private val reconciliation = ReconciliationBridgeHandler(reads, ReconciliationBridgeCommandService(database, security))
    private val costingPlanning = CostingPlanningBridgeHandler(reads, CostingPlanningBridgeCommandService(database, security))
    private val backup = if (backupManager != null && backupDocuments != null) BackupBridgeHandler(
        database,
        security,
        backupManager,
        backupDocuments,
        onSessionChanged,
        onRestoreCompleted = { quickUnlockService?.clearAll() },
    ) else null
    private val cloudBackup = if (application != null && cloudAuthorization != null) {
        CloudBackupBridgeHandler(
            application,
            cloudAuthorization,
            onSessionChanged,
            onRestoreCompleted = { quickUnlockService?.clearAll() },
        )
    } else null
    private val inFlightRequestIds = ConcurrentHashMap.newKeySet<String>()

    suspend fun dispatch(rawMessage: String): String {
        var requestId = "unknown"
        var action = "unknown"
        var acceptedRequestId: String? = null
        return try {
            if (rawMessage.toByteArray(Charsets.UTF_8).size > MAX_MESSAGE_BYTES) {
                throw BridgeProtocolFailure("invalid_request", "The request is too large")
            }
            val envelope = JSONObject(rawMessage)
            requestId = envelope.optString("requestId", "unknown")
            action = envelope.optString("action", "unknown")
            val request = parseBridgeRequest(envelope)
            requestId = request.requestId
            action = request.action
            if (!inFlightRequestIds.add(requestId)) throw BridgeCommandConflict("A request with this ID is already running")
            acceptedRequestId = requestId
            successEnvelope(request, execute(request)).toString()
        } catch (error: Throwable) {
            if (error is SecurityFailure.SessionExpired) onSessionChanged(null)
            val failure = mapBridgeFailure(error)
            if (BuildConfig.DEBUG && failure.code == "internal_error") {
                Log.e(LOG_TAG, "Bridge action '$action' failed", error)
            }
            failureEnvelope(requestId, action, failure).toString()
        } finally {
            acceptedRequestId?.let(inFlightRequestIds::remove)
        }
    }

    fun close() {
        backup?.close()
        cloudBackup?.close()
        quickUnlockService?.close()
    }

    private suspend fun execute(request: BridgeRequest): Any = when (request.action) {
        HybridBridgeActions.BRIDGE_INFO -> JSONObject()
            .put("contractVersion", BridgeContractSnapshot.VERSION)
            .put("contractSha256", BridgeContractSnapshot.CONTRACT_SHA256)
            .put("supportedActions", jsonArray(HybridBridgeActions.supported))
        in HybridBridgeActions.authentication -> authentication.execute(request)
        in HybridBridgeActions.quickUnlock -> quickUnlock?.execute(request) ?: unavailableBridgeAction()
        in HybridBridgeActions.securityAdministration -> securityAdministration.execute(request)
        in HybridBridgeActions.masterData -> masterData.execute(request)
        in HybridBridgeActions.employees -> employees.execute(request)
        in HybridBridgeActions.settings -> settings.execute(request)
        in HybridBridgeActions.operations -> operations.execute(request)
        in HybridBridgeActions.deliveries -> deliveries.execute(request)
        in HybridBridgeActions.ledgers -> ledgers.execute(request)
        in HybridBridgeActions.history -> history.execute(request)
        in HybridBridgeActions.reconciliation -> reconciliation.execute(request)
        in HybridBridgeActions.costingPlanning -> costingPlanning.execute(request)
        in HybridBridgeActions.backup -> backup?.execute(request) ?: unavailableBridgeAction()
        in HybridBridgeActions.cloudBackup -> cloudBackup?.execute(request) ?: unavailableBridgeAction()
        else -> throw BridgeProtocolFailure("invalid_request", "This action is not available in the current app slice")
    }

    companion object {
        private const val LOG_TAG = "SaafBridge"
        const val MAX_MESSAGE_BYTES = 1024 * 1024
        val SUPPORTED_ACTIONS: List<String> get() = HybridBridgeActions.supported
    }
}
