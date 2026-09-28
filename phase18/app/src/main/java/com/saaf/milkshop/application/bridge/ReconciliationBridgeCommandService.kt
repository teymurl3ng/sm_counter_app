package com.saaf.milkshop.application.bridge

import androidx.room3.withWriteTransaction
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.data.SecurityAuditEntryEntity
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.CashReconciliation
import com.saaf.milkshop.domain.DailyCloseRecord
import com.saaf.milkshop.domain.DailyCloseService
import com.saaf.milkshop.domain.PhysicalCashCountInput
import com.saaf.milkshop.domain.PhysicalCashCountService
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.domain.VerifiedDailyCloseInput
import com.saaf.milkshop.domain.VerifiedDailyReopenInput
import com.saaf.milkshop.sync.BusinessDateGuard
import com.saaf.milkshop.sync.SyncScheduler
import java.time.Instant

internal class ReconciliationBridgeCommandService(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
) {
    suspend fun recordCount(sessionHandle: String, input: PhysicalCashCountInput): CashReconciliation =
        database.withWriteTransaction {
            val actor = security.requirePermission(sessionHandle, AppPermission.CASH_COUNT_AND_CLOSE)
            val account = database.accountDao().findById(input.accountId)
            if (account?.category != "Cash/Bank" || account.name != "Cash") {
                throw BridgeCommandNotFound("The physical Cash drawer account is unavailable")
            }
            if (database.dailyCloseDao().isPostingClosed(input.businessDate)) {
                throw BridgeCommandDayClosed("This business day is closed. Reopen it before counting cash.")
            }
            BusinessDateGuard.requireHappenedWithinHorizon(input.businessDate)
            val result = PhysicalCashCountService(database).record(input)
            audit(
                ACTION_RECORD_CASH_COUNT,
                "count_id=${result.count.countId};business_date=${input.businessDate}",
                actor.userId,
                actor.displayName,
                actor.role,
            )
            result
        }.also { SyncScheduler.requestPush() }

    suspend fun closeDate(sessionHandle: String, input: VerifiedDailyCloseInput): DailyCloseRecord =
        database.withWriteTransaction {
            val actor = security.requirePermission(sessionHandle, AppPermission.CASH_COUNT_AND_CLOSE)
            BusinessDateGuard.requireHappenedWithinHorizon(input.businessDate)
            val current = database.dailyCloseDao().currentAction(input.businessDate)
            if (current?.action == DailyCloseService.ACTION_CLOSE) {
                throw BridgeCommandConflict("This business date is already closed")
            }
            val count = database.physicalCashCountDao().findById(input.physicalCashCountId)
                ?: throw BridgeCommandNotFound("This physical cash count no longer exists")
            if (count.businessDate != input.businessDate) {
                throw BridgeProtocolFailure("validation_failed", "Cash count date does not match the close date", "physicalCashCountId")
            }
            val latest = database.physicalCashCountDao().latest(count.accountId, input.businessDate)
            if (latest?.countId != count.countId) {
                throw BridgeCommandConflict("Use the latest physical cash count for this date")
            }
            if (current?.action == DailyCloseService.ACTION_REOPEN && count.countedAt <= current.createdAt) {
                throw BridgeCommandConflict("Count the drawer again after reopening before closing this date")
            }
            val result = DailyCloseService(database).closeAuthenticated(sessionHandle, security, input)
            audit(
                ACTION_CLOSE_DATE,
                "action_id=${result.action.actionId};count_id=${input.physicalCashCountId};business_date=${input.businessDate}",
                actor.userId,
                actor.displayName,
                actor.role,
            )
            result
        }.also { SyncScheduler.requestPush() }

    suspend fun reopenDate(sessionHandle: String, input: VerifiedDailyReopenInput): DailyCloseRecord =
        database.withWriteTransaction {
            val actor = security.requirePermission(sessionHandle, AppPermission.REOPEN_DAY)
            BusinessDateGuard.requireHappenedWithinHorizon(input.businessDate)
            val current = database.dailyCloseDao().currentAction(input.businessDate)
                ?: throw BridgeCommandNotFound("This business date has not been closed")
            if (current.action == DailyCloseService.ACTION_REOPEN) {
                throw BridgeCommandConflict("This business date is already open")
            }
            val result = DailyCloseService(database).reopenAuthenticated(sessionHandle, security, input)
            audit(
                ACTION_REOPEN_DATE,
                "action_id=${result.action.actionId};business_date=${input.businessDate}",
                actor.userId,
                actor.displayName,
                actor.role,
            )
            result
        }.also { SyncScheduler.requestPush() }

    private suspend fun audit(action: String, details: String, actorUserId: Long, actorName: String, actorRole: String) {
        database.securityAuditDao().insert(
            SecurityAuditEntryEntity(
                occurredAt = Instant.now().toString(),
                action = action,
                result = RESULT_SUCCESS,
                actorUserId = actorUserId,
                actorNameSnapshot = actorName,
                actorRoleSnapshot = actorRole,
                details = details,
            )
        )
    }

    companion object {
        const val ACTION_RECORD_CASH_COUNT = "bridge_record_physical_cash_count"
        const val ACTION_CLOSE_DATE = "bridge_close_business_date"
        const val ACTION_REOPEN_DATE = "bridge_reopen_business_date"
        const val RESULT_SUCCESS = "success"
    }
}
