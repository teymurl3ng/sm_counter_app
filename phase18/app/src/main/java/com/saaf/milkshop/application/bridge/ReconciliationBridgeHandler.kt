package com.saaf.milkshop.application.bridge

import com.saaf.milkshop.application.readmodels.HybridReadModelService
import com.saaf.milkshop.application.readmodels.money
import com.saaf.milkshop.domain.PhysicalCashCountInput
import com.saaf.milkshop.domain.VerifiedDailyCloseInput
import com.saaf.milkshop.domain.VerifiedDailyReopenInput
import org.json.JSONObject
import java.math.BigDecimal

internal class ReconciliationBridgeHandler(
    private val reads: HybridReadModelService,
    private val commands: ReconciliationBridgeCommandService,
) {
    suspend fun execute(request: BridgeRequest): Any = when (request.action) {
        HybridBridgeActions.RECONCILIATION_ARCHIVE -> archive(request)
        HybridBridgeActions.RECORD_PHYSICAL_CASH_COUNT -> recordCount(request)
        HybridBridgeActions.CLOSE_BUSINESS_DATE -> closeDate(request)
        HybridBridgeActions.REOPEN_BUSINESS_DATE -> reopenDate(request)
        else -> unavailableBridgeAction()
    }

    private suspend fun archive(request: BridgeRequest): JSONObject {
        strictPayload(request.payload, emptySet())
        return reconciliationArchiveJson(reads.reconciliationArchive(request.requireSession()))
    }

    private suspend fun recordCount(request: BridgeRequest): JSONObject {
        strictObject(
            request.payload,
            required = setOf("accountId", "businessDate", "countedCashPaisa"),
            allowed = setOf("accountId", "businessDate", "countedCashPaisa", "notes"),
        )
        val countedPaisa = request.payload.requiredPaisa("countedCashPaisa")
        if (countedPaisa < 0L) {
            throw BridgeProtocolFailure("validation_failed", "Counted drawer cash cannot be negative", "countedCashPaisa")
        }
        val result = commands.recordCount(
            request.requireSession(),
            PhysicalCashCountInput(
                accountId = request.payload.requiredId("accountId"),
                businessDate = request.payload.requiredDate("businessDate"),
                countedAmount = BigDecimal(countedPaisa).movePointLeft(2).toDouble(),
                notes = request.payload.optionalString("notes"),
            ),
        )
        return JSONObject()
            .put("countId", result.count.countId.toString())
            .put("countedCashPaisa", money(result.count.countedAmount).toString())
            .put("expectedCashPaisa", money(result.expectedBalance).toString())
            .put("variancePaisa", money(result.variance).toString())
    }

    private suspend fun closeDate(request: BridgeRequest): JSONObject {
        strictObject(
            request.payload,
            required = setOf("businessDate", "physicalCashCountId"),
            allowed = setOf("businessDate", "physicalCashCountId", "varianceReason"),
        )
        return dailyCloseResultJson(
            commands.closeDate(
                request.requireSession(),
                VerifiedDailyCloseInput(
                    businessDate = request.payload.requiredDate("businessDate"),
                    physicalCashCountId = request.payload.requiredId("physicalCashCountId"),
                    varianceReason = request.payload.optionalString("varianceReason"),
                ),
            ),
        )
    }

    private suspend fun reopenDate(request: BridgeRequest): JSONObject {
        strictPayload(request.payload, setOf("businessDate", "reason"))
        return dailyCloseResultJson(
            commands.reopenDate(
                request.requireSession(),
                VerifiedDailyReopenInput(
                    businessDate = request.payload.requiredDate("businessDate"),
                    reason = request.payload.requiredString("reason"),
                ),
            ),
        )
    }
}
