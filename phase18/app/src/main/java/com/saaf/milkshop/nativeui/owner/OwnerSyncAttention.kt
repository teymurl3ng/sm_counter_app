package com.saaf.milkshop.nativeui.owner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.saaf.milkshop.sync.NativeSyncHold
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

data class OwnerSyncAttention(
    val count: Int? = null,
    val unavailable: Boolean = false,
    val holds: List<NativeSyncHold>? = null,
    val holdsUnavailable: Boolean = false,
) {
    val description: String get() = when {
        unavailable && count != null && count > 0 -> "$count held ${if (count == 1) "entry" else "entries"}; review status unavailable. Open Sync to retry."
        unavailable -> "Sync review unavailable. Open Sync to retry."
        count == null -> "Checking sync review…"
        count == 1 -> "1 entry needs review"
        count > 1 -> "$count entries need review"
        else -> "Upload entries to your shop's online record"
    }
}

/** Keep the last confirmed count on read failure; cancellation must not become an error. */
internal fun reviewAttentionFlow(
    counts: Flow<Int>,
    previous: OwnerSyncAttention = OwnerSyncAttention(),
): Flow<OwnerSyncAttention> = flow {
    var latest = previous
    try {
        counts.collect { count ->
            latest = latest.copy(count = count, unavailable = false)
            emit(latest)
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        emit(latest.copy(unavailable = true))
    }
}

/** Session-keyed and lifecycle-bound; no destination load, event bus or network required. */
@Composable
internal fun rememberOwnerSyncAttention(
    sessionKey: String,
    counts: Flow<Int>,
    holds: Flow<List<NativeSyncHold>>,
    reviewOpen: Boolean,
    retryKey: Int,
): OwnerSyncAttention {
    var state by remember(sessionKey, counts) { mutableStateOf(OwnerSyncAttention()) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(sessionKey, counts, lifecycle, retryKey) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            reviewAttentionFlow(counts, state).flowOn(Dispatchers.IO).collect {
                state = state.copy(count = it.count, unavailable = it.unavailable)
            }
        }
    }
    LaunchedEffect(sessionKey, holds, reviewOpen, lifecycle, retryKey) {
        state = state.copy(holds = null, holdsUnavailable = false)
        if (reviewOpen) lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            try {
                holds.flowOn(Dispatchers.IO).collect { rows ->
                    // Both values from this emission agree, even if count invalidation arrives first.
                    state = state.copy(count = rows.size, holds = rows, holdsUnavailable = false)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                state = state.copy(holdsUnavailable = true)
            }
        }
    }
    return state
}
