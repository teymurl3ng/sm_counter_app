package com.saaf.milkshop.application.bridge

import com.saaf.milkshop.application.readmodels.HybridReadModelService

internal class LedgerBridgeHandler(
    private val reads: HybridReadModelService,
) {
    suspend fun execute(request: BridgeRequest): Any = when (request.action) {
        HybridBridgeActions.ACCOUNT_LEDGER -> {
            strictPayload(request.payload, setOf("accountId"))
            try {
                accountLedgerJson(reads.accountLedger(request.requireSession(), request.payload.requiredId("accountId")))
            } catch (failure: IllegalStateException) {
                if (failure.message?.contains("does not exist", ignoreCase = true) == true) {
                    throw BridgeCommandNotFound("This customer or supplier no longer exists")
                }
                throw failure
            } catch (failure: IllegalArgumentException) {
                if (failure.message?.contains("available only for Customer or Vendor", ignoreCase = true) == true) {
                    throw BridgeCommandNotFound("Choose a customer or supplier account")
                }
                throw failure
            }
        }
        else -> unavailableBridgeAction()
    }
}
