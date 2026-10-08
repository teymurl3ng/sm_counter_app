package com.saaf.milkshop.sync

import android.content.Context
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.ServerAccountPolicy

/**
 * Enrollment (Account Bridge spec §6, minimal sync form): prove the server
 * credential online once, pick the shop this installation pushes as, then
 * persist config + session + binding and switch the push scheduler on.
 * Nothing is saved on any non-definitive outcome (UNKNOWN never creates
 * state). The interactive owner screen comes later; the debug build drives
 * this through an adb receiver for now.
 */
class SyncEnrollment(
    private val context: Context,
    private val http: SyncHttp = UrlConnectionSyncHttp(),
    private val database: MilkShopDatabase? = null,
) {

    sealed interface Outcome {
        data class Enrolled(
            val binding: SyncShopBinding,
            val shopTimezone: String?,
        ) : Outcome

        /** shop_id was omitted and the account sees several shops: pick one. */
        data class ShopsAvailable(val shops: List<AuthShop>) : Outcome

        data class OwnersAvailable(val owners: List<OwnerChoice>) : Outcome
        data class Failed(val reason: String) : Outcome
    }

    /** One linkable local owner, for the enrollment chooser. */
    data class OwnerChoice(val userId: Long, val displayName: String)

    suspend fun enroll(
        config: SyncServerConfig,
        username: String,
        credential: CharArray,
        shopId: String? = null,
        localUserId: Long? = null,
    ): Outcome {
        val auth = when (val result = AccountAuthClient(http, config).signIn(username, credential)) {
            is AccountAuthResult.Allowed -> result
            AccountAuthResult.InvalidCredentials -> return Outcome.Failed("invalid_credentials")
            AccountAuthResult.Disabled -> return Outcome.Failed("disabled")
            AccountAuthResult.NotAuthorized -> return Outcome.Failed("not_authorized")
            // Refresh-only verdict: sign-in can never produce it (the
            // sign-in 400 branch never maps to it); typed exhaustiveness.
            AccountAuthResult.RefreshTokenInvalid -> return Outcome.Failed("refresh_token_invalid")
            is AccountAuthResult.Unknown -> return Outcome.Failed("unknown:${result.reason}")
        }
        // C1/M9: the enrollment's account is this installation's push
        // identity for every local user, so it must hold the role the sync
        // contract requires — owner (ACCOUNT_BRIDGE_PHASE1_SPEC: "Enrollment
        // requires ALLOWED, role owner"; master-data write gating server-side
        // assumes it).
        if (auth.role != "owner") {
            return Outcome.Failed("owner_enrollment_required")
        }

        // Track 1 A2' (Claude review): link resolution is a REQUIRED,
        // fail-closed step. The grace clock belongs to the enrollment, so it
        // must never depend on an ambiguous link. Zero owners cannot happen
        // post-bootstrap; two or more require an explicit choice.
        val activeOwners = database?.appUserDao()?.all()
            ?.filter { it.role == "owner" && it.status == "active" }
            .orEmpty()
        val resolvedLocalUserId = localUserId ?: when {
            activeOwners.size == 1 -> activeOwners.single().userId
            activeOwners.isEmpty() -> return Outcome.Failed("owner_link_unresolved")
            else -> return Outcome.OwnersAvailable(
                activeOwners.map { OwnerChoice(it.userId, it.displayName) },
            )
        }

        val target = shopId?.let { wanted -> auth.shops.firstOrNull { it.shopId == wanted } }
            ?: auth.shops.singleOrNull()
            ?: return if (auth.shops.isEmpty()) {
                Outcome.Failed("no_shops")
            } else {
                Outcome.ShopsAvailable(auth.shops)
            }

        // go 8: a restore/reset marks the installation for a fresh identity —
        // consume it here, on the definitive enrollment path, so the restored
        // device never pushes as the id that wrote its backup (same-origin
        // pushes escape the server's conflict audit). The replaced id was
        // already recorded as prev_device_id by the restore for outbox
        // classification. Ordinary enrollments (environment switch) keep
        // their identity.
        val rebaseline = SyncRebaselineStore(context)
        if (rebaseline.needsFreshIdentity) {
            SyncIdentity.regenerate(context)
            rebaseline.needsFreshIdentity = false
        }

        SyncServerConfigStore.save(context, config)
        SyncTokenStore(context).save(
            SyncTokenStore.Session(auth.accessToken, auth.refreshToken, auth.accessTokenExpiresAtEpochMs),
        )
        val binding = SyncShopBinding(
            environment = config.environment,
            businessId = auth.businessId,
            shopId = target.shopId,
            shopName = target.shopName,
            staffId = auth.staffId,
            role = auth.role,
        )
        SyncShopBindingStore.save(context, binding)
        (target.timezone ?: auth.shopTimezone)?.let(BusinessClock::setZone)
        // Re-baseline for the newly enrolled server: the pull cursor restarts
        // at zero so the device converges with the server from the beginning.
        // go 8 Z-leg: a PLAIN enrollment enqueues the complete local history
        // PRE-HELD (flag first, then the enqueue — both crash gaps self-heal)
        // and the first drain classifies it against server row bodies: blind
        // push-first would clobber a server that may be the newer party
        // (after a rollback this device legitimately holds newer rows; after
        // an offline gap the server does). A format-5 restore instead runs
        // the origin-based classified flow on its imported outbox — enqueueAll
        // here would defeat it.
        database?.let { db ->
            if (!rebaseline.restoredRebaseline) {
                rebaseline.liveRebaselinePending = true
                SyncBaseline.enqueueAllHeld(db)
            }
            db.syncPullStateDao().upsert(
                com.saaf.milkshop.data.SyncPullStateEntity(
                    lastServerSeq = 0,
                    updatedAt = java.time.Instant.now().toString(),
                ),
            )
            // Track 1 A2'/A6: the ENROLLMENT carries the grace clock — the
            // Room binding row is its authoritative storage (the binding
            // file stays the sync-runtime source). ACTIVE + fresh clock
            // here also re-arms enforcement after a restore's
            // RESTORED_UNVERIFIED, independent of the owner link.
            val nowMs = System.currentTimeMillis()
            val nowIso = java.time.Instant.now().toString()
            db.serverAccountDao().deleteBindings()
            db.serverAccountDao().insertBinding(
                com.saaf.milkshop.data.AccountInstallationBindingEntity(
                    environment = config.environment,
                    businessId = binding.businessId,
                    shopId = binding.shopId,
                    shopNameSnapshot = binding.shopName ?: target.shopName ?: "Shop ${binding.shopId}",
                    status = ServerAccountPolicy.BindingStatus.ACTIVE,
                    verifiedAtEpochMs = nowMs,
                    createdAt = nowIso,
                    updatedAt = nowIso,
                ),
            )
        }
        SyncScheduler.setSchedulingEnabled(true)
        SyncScheduler.requestPush()
        linkVerifiedOwner(auth, username, resolvedLocalUserId)
        return Outcome.Enrolled(binding, target.timezone ?: auth.shopTimezone)
    }

    /**
     * H13 (spec §6.5 step 5): enrollment IS the online verification — link
     * the verified owner to the local owner and start the grace clock
     * fresh. [localUserId] comes from the signed-in session when the caller
     * has one; otherwise the bootstrap invariant resolves it (exactly one
     * active owner per installation). With zero or several owners and no
     * session there is nothing unambiguous to link: skip (audited) and
     * leave linking to the bridge verification flow.
     */
    private suspend fun linkVerifiedOwner(
        auth: AccountAuthResult.Allowed,
        username: String,
        localUserId: Long?,
    ) {
        val db = database ?: return
        val target = localUserId ?: run {
            db.appUserDao().all()
                .filter { it.role == "owner" && it.status == "active" }
                .singleOrNull()?.userId
        }
        if (target == null) {
            runCatching {
                db.securityAuditDao().insert(
                    com.saaf.milkshop.data.SecurityAuditEntryEntity(
                        auditId = 0,
                        occurredAt = java.time.Instant.now().toString(),
                        action = "sync_identity_link",
                        result = "warning",
                        reasonCode = "identity_link_skipped",
                        details = "no unambiguous local owner for the verified server account",
                    ),
                )
            }
            return
        }
        val now = System.currentTimeMillis()
        val existing = db.serverAccountDao().findIdentityLink(target)
        val fresh = (existing ?: com.saaf.milkshop.data.ServerIdentityLinkEntity(
            localUserId = target,
            serverStaffId = auth.staffId,
            businessId = auth.businessId,
            status = ServerAccountPolicy.LinkStatus.VERIFIED,
        )).copy(
            serverStaffId = auth.staffId,
            businessId = auth.businessId,
            usernameSnapshot = username,
            roleSnapshot = auth.role,
            status = ServerAccountPolicy.LinkStatus.VERIFIED,
            verifiedAtEpochMs = now,
        )
        if (existing == null) {
            db.serverAccountDao().insertIdentityLink(fresh)
        } else {
            // Server is the authority (spec §5 "Verify/update link"): any
            // prior state — suspended, revoked, restored-unverified — yields
            // to a fresh ALLOWED verification; the old error code survives
            // the copy as history.
            db.serverAccountDao().updateIdentityLink(fresh)
        }
    }

    suspend fun unenroll() {
        // A4': stop-syncing is the honest "server logout" moment — revoke
        // this device's session (scope=local) before the files go. The
        // access token may outlive the call (platform behavior), so this is
        // hygiene, not a security control.
        runCatching {
            val config = SyncServerConfigStore.load(context)
            val session = SyncTokenStore(context).load()
            if (config != null && session != null) {
                AccountAuthClient(http, config).logout(session.accessToken)
            }
        }
        SyncServerConfigStore.clear(context)
        SyncShopBindingStore.clear(context)
        SyncTokenStore(context).clear()
        SyncScheduler.setSchedulingEnabled(false)
    }
}
