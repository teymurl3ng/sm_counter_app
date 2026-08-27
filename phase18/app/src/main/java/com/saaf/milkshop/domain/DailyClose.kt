package com.saaf.milkshop.domain

import androidx.room3.withWriteTransaction
import com.saaf.milkshop.data.DailyCloseActionEntity
import com.saaf.milkshop.data.MilkShopDatabase
import java.time.Instant
import java.time.LocalDate
import kotlin.math.abs

data class DailyCloseInput(
    val businessDate: String,
    val physicalCashCountId: Long,
    val actorName: String,
    val actorRole: String,
    val varianceReason: String? = null,
)

data class DailyReopenInput(
    val businessDate: String,
    val actorName: String,
    val actorRole: String,
    val reason: String,
)

data class VerifiedDailyCloseInput(
    val businessDate: String,
    val physicalCashCountId: Long,
    val varianceReason: String? = null,
)

data class VerifiedDailyReopenInput(
    val businessDate: String,
    val reason: String,
)

data class DailyCloseRecord(
    val action: DailyCloseActionEntity,
    val status: String,
    val countedCash: Double?,
    val expectedCash: Double?,
    val variance: Double?,
)

class DailyCloseService(private val database: MilkShopDatabase) {
    suspend fun close(input: DailyCloseInput): DailyCloseRecord = closeInternal(input, actorUserId = null)

    suspend fun closeAuthenticated(
        sessionId: String,
        security: UserSecurityService,
        input: VerifiedDailyCloseInput,
    ): DailyCloseRecord {
        val actor = security.requirePermission(sessionId, AppPermission.CASH_COUNT_AND_CLOSE)
        return closeInternal(
            DailyCloseInput(
                businessDate = input.businessDate,
                physicalCashCountId = input.physicalCashCountId,
                actorName = actor.displayName,
                actorRole = actor.role,
                varianceReason = input.varianceReason,
            ),
            actorUserId = actor.userId,
        )
    }

    private suspend fun closeInternal(input: DailyCloseInput, actorUserId: Long?): DailyCloseRecord = database.withWriteTransaction {
        LocalDate.parse(input.businessDate)
        val actor = validateActor(input.actorName, input.actorRole, reopen = false)
        val current = database.dailyCloseDao().currentAction(input.businessDate)
        require(current == null || current.action == ACTION_REOPEN) { "Business date is already closed" }

        val count = database.physicalCashCountDao().findById(input.physicalCashCountId)
            ?: error("Physical cash count ${input.physicalCashCountId} does not exist")
        require(count.businessDate == input.businessDate) { "Cash count date does not match the close date" }
        val latest = database.physicalCashCountDao().latest(count.accountId, input.businessDate)
            ?: error("No physical cash count exists for the close date")
        require(latest.countId == count.countId) { "Daily close must use the latest physical cash count" }
        if (current?.action == ACTION_REOPEN) {
            require(count.countedAt > current.createdAt) { "Reclose requires a new cash count after reopening" }
        }

        val expected = database.physicalCashCountDao().canonicalBalanceAt(count.accountId, count.countedAt)
        val variance = count.countedAmount - expected
        val reason = input.varianceReason.cleanOptional()
        if (abs(variance) >= MONEY_TOLERANCE) {
            require(reason != null) { "Explain the non-zero cash variance before closing" }
        }

        val action = DailyCloseActionEntity(
            businessDate = input.businessDate,
            action = ACTION_CLOSE,
            physicalCashCountId = count.countId,
            actorName = actor.first,
            actorRole = actor.second,
            actorUserId = actorUserId,
            reason = reason,
            createdAt = Instant.now().toString(),
            supersedesActionId = current?.actionId,
        )
        val actionId = database.dailyCloseDao().insert(action)
        DailyCloseRecord(action.copy(actionId = actionId), STATUS_CLOSED, count.countedAmount, expected, variance)
    }

    suspend fun reopen(input: DailyReopenInput): DailyCloseRecord = reopenInternal(input, actorUserId = null)

    suspend fun reopenAuthenticated(
        sessionId: String,
        security: UserSecurityService,
        input: VerifiedDailyReopenInput,
    ): DailyCloseRecord {
        val actor = security.requirePermission(sessionId, AppPermission.REOPEN_DAY)
        security.sessionManager().requireRecentAuthentication(sessionId)
        return reopenInternal(
            DailyReopenInput(input.businessDate, actor.displayName, actor.role, input.reason),
            actorUserId = actor.userId,
        )
    }

    private suspend fun reopenInternal(input: DailyReopenInput, actorUserId: Long?): DailyCloseRecord = database.withWriteTransaction {
        LocalDate.parse(input.businessDate)
        val actor = validateActor(input.actorName, input.actorRole, reopen = true)
        val reason = input.reason.cleanOptional()
        require(reason != null) { "Enter a reason for reopening the business date" }
        val current = database.dailyCloseDao().currentAction(input.businessDate)
            ?: error("Business date has not been closed")
        require(current.action == ACTION_CLOSE) { "Business date is already open" }

        val action = DailyCloseActionEntity(
            businessDate = input.businessDate,
            action = ACTION_REOPEN,
            actorName = actor.first,
            actorRole = actor.second,
            actorUserId = actorUserId,
            reason = reason,
            createdAt = Instant.now().toString(),
            supersedesActionId = current.actionId,
        )
        val actionId = database.dailyCloseDao().insert(action)
        DailyCloseRecord(action.copy(actionId = actionId), STATUS_REOPENED, null, null, null)
    }

    suspend fun current(businessDate: String): DailyCloseRecord? {
        val action = database.dailyCloseDao().currentAction(businessDate) ?: return null
        if (action.action == ACTION_REOPEN) {
            return DailyCloseRecord(action, STATUS_REOPENED, null, null, null)
        }
        val count = database.physicalCashCountDao().findById(action.physicalCashCountId!!)
            ?: error("Close action ${action.actionId} has no cash count")
        val expected = database.physicalCashCountDao().canonicalBalanceAt(count.accountId, count.countedAt)
        return DailyCloseRecord(
            action,
            STATUS_CLOSED,
            count.countedAmount,
            expected,
            count.countedAmount - expected,
        )
    }

    suspend fun history(businessDate: String): List<DailyCloseActionEntity> =
        database.dailyCloseDao().history(businessDate)

    private fun validateActor(name: String, role: String, reopen: Boolean): Pair<String, String> {
        val cleanName = name.trim()
        val cleanRole = role.trim().lowercase()
        require(cleanName.isNotEmpty()) { "Enter the acting person's name" }
        require(cleanRole in ACTOR_ROLES) { "Invalid daily-close actor role" }
        if (reopen) require(cleanRole in REOPEN_ROLES) { "Only an owner or admin can reopen a date" }
        return cleanName to cleanRole
    }

    companion object {
        const val ACTION_CLOSE = "close"
        const val ACTION_REOPEN = "reopen"
        const val STATUS_CLOSED = "closed"
        const val STATUS_REOPENED = "reopened"
        private const val MONEY_TOLERANCE = 0.005
        private val ACTOR_ROLES = setOf("owner", "admin", "manager", "cashier", "operator")
        private val REOPEN_ROLES = setOf("owner", "admin")
    }
}
