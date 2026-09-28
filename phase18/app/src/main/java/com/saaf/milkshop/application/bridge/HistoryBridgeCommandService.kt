package com.saaf.milkshop.application.bridge

import androidx.room3.withWriteTransaction
import com.saaf.milkshop.data.EventActorLinkEntity
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.data.SecurityAuditEntryEntity
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.CorrectionInput
import com.saaf.milkshop.domain.CorrectionResult
import com.saaf.milkshop.domain.CorrectionService
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.sync.BusinessDateGuard
import java.time.Instant

internal class HistoryBridgeCommandService(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
) {
    suspend fun correct(sessionHandle: String, input: CorrectionInput): CorrectionResult = database.withWriteTransaction {
        val actor = security.requirePermission(sessionHandle, AppPermission.CORRECTION)
        val original = database.eventDao().findById(input.originalEventId)
            ?: throw BridgeCommandNotFound("This transaction no longer exists")
        if (database.eventDao().findAllDescending().any { it.correctsEventId == original.eventId }) {
            throw BridgeCommandConflict("This transaction was already replaced. Correct its latest correction instead.")
        }
        if (database.dailyCloseDao().isPostingClosed(input.occurredAt)) {
            throw BridgeCommandDayClosed("This business day is closed. Reopen it before recording a correction.")
        }
        BusinessDateGuard.requireHappenedWithinHorizon(input.occurredAt)
        val result = try {
            CorrectionService(database).correct(input)
        } catch (failure: Throwable) {
            if (failure.messages().any { it.contains("stock", true) && it.contains("negative", true) || it.contains("insufficient stock", true) }) {
                throw BridgeCommandInsufficientStock("This correction would make saved stock negative.")
            }
            throw failure
        }
        database.eventActorLinkDao().insert(EventActorLinkEntity(result.eventId, actor.userId, actor.displayName, actor.role, Instant.now().toString()))
        database.securityAuditDao().insert(SecurityAuditEntryEntity(
            occurredAt = Instant.now().toString(), action = ACTION_CORRECT_EVENT, result = RESULT_SUCCESS,
            actorUserId = actor.userId, actorNameSnapshot = actor.displayName, actorRoleSnapshot = actor.role,
            details = "event_id=${result.eventId};original_event_id=${input.originalEventId};event_type=${result.eventType}",
        ))
        result
    }

    companion object {
        const val ACTION_CORRECT_EVENT = "bridge_correct_event"
        const val RESULT_SUCCESS = "success"
    }
}

private fun Throwable.messages(): Sequence<String> = generateSequence(this) { it.cause }.mapNotNull { it.message }
