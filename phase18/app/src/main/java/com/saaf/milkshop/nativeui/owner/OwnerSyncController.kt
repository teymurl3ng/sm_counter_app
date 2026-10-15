package com.saaf.milkshop.nativeui.owner

import com.saaf.milkshop.application.owner.NativeOwnerSyncFacade
import com.saaf.milkshop.sync.NativeSyncShopChoice
import com.saaf.milkshop.sync.NativeSyncOwnerChoice
import com.saaf.milkshop.sync.SyncRuntime
import com.saaf.milkshop.sync.SyncEnrollment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Owns the owner-facing sync screen: enrollment, status, shop choice, and
 * unenrollment. All state lives in [OwnerSyncState] on the shared runtime;
 * credentials are held only in locally-scoped CharArrays and never in state.
 */
internal class OwnerSyncController(
    private val facade: NativeOwnerSyncFacade,
    private val runtime: OwnerMoreRuntime,
) {

    fun openSync() {
        val token = runtime.captureSession() ?: return
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        // Keep any previously loaded status so the entering pane never flashes
        // the enrollment form (or a wiped one) over real data while the section
        // fade settles; only a genuine first load shows the busy pane.
        val statusKnown = current.syncState.status != null
        runtime.state.value = current.copy(
            destination = NativeOwnerMoreDestination.Sync,
            syncState = current.syncState.copy(
                busy = !statusKnown,
                error = null,
                notice = null,
            ),
        )
        loadStatus(token)
    }

    fun refreshSync() {
        val token = runtime.captureSession() ?: return
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Sync) return
        runtime.updateReady { it.copy(syncState = it.syncState.copy(busy = true, error = null, notice = null)) }
        loadStatus(token)
    }

    fun enroll(username: String, credential: CharArray, environment: String, localUserId: Long? = null) {
        val token = runtime.captureSession() ?: return
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Sync) return
        runtime.updateReady { it.copy(syncState = it.syncState.copy(busy = true, error = null, notice = null)) }
        runtime.scope.launch {
            try {
                val outcome = withContext(Dispatchers.IO) {
                    facade.enroll(username, credential, shopId = null, environment = environment, localUserId = localUserId)
                }
                if (!runtime.isCurrent(token)) return@launch
                when (outcome) {
                    is SyncEnrollment.Outcome.Enrolled -> {
                        runtime.updateReady {
                            it.copy(
                                syncState = it.syncState.copy(
                                    busy = false,
                                    notice = "Enrolled as ${outcome.binding.role} for " +
                                        "'${outcome.binding.shopName ?: outcome.binding.shopId}'.",
                                    shopChoices = emptyList(),
                                ),
                            )
                        }
                        loadStatus(token)
                    }
                    is SyncEnrollment.Outcome.ShopsAvailable ->
                        runtime.updateReady {
                            it.copy(
                                syncState = it.syncState.copy(
                                    busy = false,
                                    shopChoices = outcome.shops.map { shop ->
                                        NativeSyncShopChoice(shop.shopId, shop.shopName)
                                    },
                                ),
                            )
                        }
                    is SyncEnrollment.Outcome.OwnersAvailable ->
                        runtime.updateReady {
                            it.copy(
                                syncState = it.syncState.copy(
                                    busy = false,
                                    ownerChoices = outcome.owners.map { owner ->
                                        NativeSyncOwnerChoice(owner.userId, owner.displayName)
                                    },
                                ),
                            )
                        }
                    is SyncEnrollment.Outcome.Failed ->
                        runtime.updateReady {
                            it.copy(syncState = it.syncState.copy(busy = false, error = describeFailure(outcome)))
                        }
                }
            } catch (error: Throwable) {
                if (runtime.isCurrent(token)) {
                    runtime.updateReady {
                        it.copy(syncState = it.syncState.copy(busy = false, error = error.message ?: "Enrollment failed"))
                    }
                }
            }
        }
    }

    fun enrollInShop(shopId: String, username: String, credential: CharArray, environment: String, localUserId: Long? = null) {
        val token = runtime.captureSession() ?: return
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Sync) return
        runtime.updateReady { it.copy(syncState = it.syncState.copy(busy = true, error = null, notice = null)) }
        runtime.scope.launch {
            try {
                val outcome = withContext(Dispatchers.IO) {
                    facade.enroll(username, credential, shopId = shopId, environment = environment, localUserId = localUserId)
                }
                if (!runtime.isCurrent(token)) return@launch
                when (outcome) {
                    is SyncEnrollment.Outcome.Enrolled -> {
                        runtime.updateReady {
                            it.copy(
                                syncState = it.syncState.copy(
                                    busy = false,
                                    notice = "Enrolled as ${outcome.binding.role} for " +
                                        "'${outcome.binding.shopName ?: outcome.binding.shopId}'.",
                                    shopChoices = emptyList(),
                                ),
                            )
                        }
                        loadStatus(token)
                    }
                    is SyncEnrollment.Outcome.OwnersAvailable ->
                        runtime.updateReady {
                            it.copy(
                                syncState = it.syncState.copy(
                                    busy = false,
                                    ownerChoices = outcome.owners.map { owner ->
                                        NativeSyncOwnerChoice(owner.userId, owner.displayName)
                                    },
                                ),
                            )
                        }
                    // Single-shop path already handled elsewhere; a second
                    // ambiguous answer here keeps the choices on screen.
                    is SyncEnrollment.Outcome.ShopsAvailable ->
                        runtime.updateReady { it.copy(syncState = it.syncState.copy(busy = false)) }
                    is SyncEnrollment.Outcome.Failed ->
                        runtime.updateReady {
                            it.copy(syncState = it.syncState.copy(busy = false, error = describeFailure(outcome)))
                        }
                }
            } catch (error: Throwable) {
                if (runtime.isCurrent(token)) {
                    runtime.updateReady {
                        it.copy(syncState = it.syncState.copy(busy = false, error = error.message ?: "Enrollment failed"))
                    }
                }
            }
        }
    }

    fun syncNow() {
        val token = runtime.captureSession() ?: return
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Sync) return
        runtime.updateReady { it.copy(syncState = it.syncState.copy(busy = true, error = null, notice = null)) }
        runtime.scope.launch {
            try {
                // Push before pull (C1): local edits reach the server before
                // any pull can overwrite them.
                val push = withContext(Dispatchers.IO) { facade.push() }
                val pull = withContext(Dispatchers.IO) { facade.pull() }
                val pullSummary = when (pull) {
                    is SyncRuntime.SyncPullSummary.Done ->
                        "applied=${pull.applied} updated=${pull.updated} skipped=${pull.skipped} parked=${pull.parked} conflicts=${pull.conflicts}"
                    is SyncRuntime.SyncPullSummary.RetryLater -> "will retry (${pull.reason})"
                    SyncRuntime.SyncPullSummary.NotEnrolled -> "not enrolled"
                }
                if (!runtime.isCurrent(token)) return@launch
                runtime.updateReady {
                    it.copy(
                        syncState = it.syncState.copy(
                            busy = false,
                            notice = "Push: ${push.javaClass.simpleName} · Pull: $pullSummary",
                        ),
                    )
                }
                loadStatus(token)
            } catch (error: Throwable) {
                if (runtime.isCurrent(token)) {
                    runtime.updateReady {
                        it.copy(syncState = it.syncState.copy(busy = false, error = error.message ?: "Sync failed"))
                    }
                }
            }
        }
    }

    fun unenroll() {
        val token = runtime.captureSession() ?: return
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Sync) return
        runtime.updateReady { it.copy(syncState = it.syncState.copy(busy = true, error = null, notice = null)) }
        runtime.scope.launch {
            try {
                withContext(Dispatchers.IO) { facade.unenroll() }
                if (!runtime.isCurrent(token)) return@launch
                runtime.updateReady {
                    it.copy(syncState = it.syncState.copy(busy = false, notice = "Stopped syncing this phone."))
                }
                loadStatus(token)
            } catch (error: Throwable) {
                if (runtime.isCurrent(token)) {
                    runtime.updateReady {
                        it.copy(syncState = it.syncState.copy(busy = false, error = error.message ?: "Failed"))
                    }
                }
            }
        }
    }

    fun leave() {
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        runtime.state.value = current.copy(
            destination = NativeOwnerMoreDestination.Menu,
            syncState = OwnerSyncState(),
        )
    }

    private fun loadStatus(token: OwnerSessionToken) {
        runtime.scope.launch {
            try {
                val status = withContext(Dispatchers.IO) { facade.status() }
                val holds = withContext(Dispatchers.IO) { facade.holds() }
                if (!runtime.isCurrent(token)) return@launch
                val latest = runtime.state.value as? NativeOwnerMoreState.Ready ?: return@launch
                if (latest.destination != NativeOwnerMoreDestination.Sync) return@launch
                runtime.state.value = latest.copy(
                    syncState = latest.syncState.copy(
                        status = status,
                        holds = holds,
                        busy = false,
                        error = null,
                    ),
                    loadedDestinations = latest.loadedDestinations + NativeOwnerMoreDestination.Sync,
                )
            } catch (error: Throwable) {
                if (!runtime.isCurrent(token)) return@launch
                runtime.updateReady {
                    it.copy(syncState = it.syncState.copy(busy = false, error = error.message ?: "Failed to load"))
                }
            }
        }
    }

    /**
     * go 8 sync-review: rules on a held restored entry. keep-mine releases it
     * into the push (audited); take-server drops it and lets the server
     * version apply. Reloads the hold list afterwards.
     */
    fun resolveHold(tableName: String, rowUid: String, keepMine: Boolean) {
        val token = runtime.captureSession() ?: return
        val current = runtime.state.value as? NativeOwnerMoreState.Ready ?: return
        if (current.destination != NativeOwnerMoreDestination.Sync) return
        runtime.updateReady { it.copy(syncState = it.syncState.copy(busy = true, error = null)) }
        runtime.scope.launch {
            try {
                withContext(Dispatchers.IO) { facade.resolveHold(tableName, rowUid, keepMine) }
                if (!runtime.isCurrent(token)) return@launch
                runtime.updateReady {
                    it.copy(
                        syncState = it.syncState.copy(
                            busy = false,
                            notice = if (keepMine) {
                                "Your version will upload with the next sync."
                            } else {
                                "The server version has been applied on this phone."
                            },
                        ),
                    )
                }
                loadStatus(token)
            } catch (error: Throwable) {
                if (runtime.isCurrent(token)) {
                    runtime.updateReady {
                        it.copy(syncState = it.syncState.copy(busy = false, error = error.message ?: "Failed"))
                    }
                }
            }
        }
    }

    private fun describeFailure(outcome: SyncEnrollment.Outcome.Failed): String = when (outcome.reason) {
        "invalid_credentials" -> "The server login or credential is not correct."
        "disabled" -> "This account is disabled on the server."
        "not_authorized" -> "This account has no staff profile on the server."
        "no_shops" -> "This account is not assigned to any shop."
        "owner_enrollment_required" -> "Only an owner account can connect this phone to the server."
        else -> "Could not reach the server (${outcome.reason}). Try again."
    }
}
