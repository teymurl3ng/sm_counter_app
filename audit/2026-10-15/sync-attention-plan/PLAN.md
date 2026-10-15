# Phase 2: persistent sync review attention

Date: 15 October 2026. Baseline source: `3af011775b5529a3228cfbf1557ebdab8d6355af` (no tracked main-source changes observed). Status: implemented and accepted at the named local Owner held-entry scope; see [result](RESULT.md). Parent: [six-phase plan](../../../NEXT_WORK_PLAN_2026-10-15.md).

## Goal and explicit scope decision

An Owner must be able to discover entries already awaiting an Owner decision without opening Sync by chance. Keep the existing header and navigation dimensions stable.

**First implementation scope: persisted held entries in `sync_rebaseline_holds`.** These have an existing review/action model. That includes differing entries held during enrollment/rebaseline as well as restore. It does not mean every audited sync conflict is unresolved or manually resolvable.

The parent plan's earlier broad phrase "held/conflicting work" is narrowed explicitly here. Historical `sync_apply_conflict` audit records, automatic dependency parks, ordinary offline uploads and halted push reasons must not be added to the held-entry count. Broader conflict-notification commitment B8 remains partial until its distinct audit-only review/acknowledgment policy is separately designed. Completing this patch must not be reported as complete coverage of every conflict type.

## Verified source baseline

| Finding | Evidence | Implication |
|---|---|---|
| Held entries are persisted, with count/list queries. Count has no observable query today. | `data/SyncEntities.kt`, `SyncRebaselineHoldDao` | Observe actual database count; no new alert flag/copy of the count. |
| `NativeSyncStatus.holdCount` exists, but `SyncRuntime.status()` returns null if config/binding is missing. | `sync/SyncStatus.kt`, `sync/SyncRuntime.kt` | Badge observation cannot depend on having a current online enrollment/session. |
| Sync's controller loads status/holds when that destination opens or refreshes; leaving resets `OwnerSyncState`. | `nativeui/owner/OwnerSyncController.kt` | A count stored only in this destination would disappear elsewhere or go stale. |
| Owner More's Sync row has static wording; shell's More navigation icon has no review indicator. | `OwnerMoreScreen.kt`, `OwnerShell.kt`, actual host `OwnerPreviewHost.kt` | Add attention to existing navigation and row; do not redesign the shell. |
| The active Owner host owns one shared header above section transitions. | `OwnerPreviewHost.kt` | Do not add a dynamic header banner or change bar height. Badge belongs inside the existing icon allocation. |
| Sync review already lists held entries and offers Keep mine / Take server. Its explanation assumes restore and says nothing uploads until a decision. | `OwnerSyncScreen.kt` | Broaden explanation to held differences; only held entries are described as waiting, without claiming all sync is stopped. |
| Existing resolution uses transaction + operation gate; failure retains the hold. Keep mine releases the outbox entry; Take server applies the parked version before deleting the hold. | `application/owner/NativeOwnerSyncFacade.kt`; `SyncRepairBoundaryTest.failedTakeServerKeepsReviewAndLocalEditThenSuccessfulApplyResolvesBoth` | Reuse the resolver unchanged. Disappearance of a hold is not proof its released upload has reached the server. |
| Stop syncing clears credentials/config/binding and scheduling, not the hold table. Holds currently render only inside enrolled Sync body. | `sync/SyncEnrollment.kt`, `OwnerSyncScreen.kt` | A retained hold must not vanish from attention because the connection is removed. Show retained entries/read-only reconnect guidance when disconnected. |
| Existing Android notification covers halted uploads, depends on notification permission, auto-cancels, and opens native launcher. | `sync/SyncNotifier.kt`, `SyncRuntime.engineDrainLocked` | It does not supply durable in-app review status. Preserve this notification in this patch; no new permission request. |
| Sibling correction/planning conflicts can be skipped and audited as `sync_apply_conflict`; they are not equivalent to a review hold. | `sync/SyncApplyEngine.kt` | Counting every conflict audit as an unresolved hold would be false and could create a warning with no valid resolution. |

Paths above are relative to `phase18/app/src/main/java/com/saaf/milkshop/`, except the named Android test. These are source findings; no current business record or runtime defect was manufactured on a phone for this planning pass.

## Research and adaptation

Primary sources checked on 15 October:

1. [Android Compose badges](https://developer.android.com/develop/ui/compose/components/badges): status/count indicators can overlay an existing icon. **Adaptation:** show count on Owner More icon while preserving the navigation frame. This is a design choice, not a rule requiring our exact placement.
2. [Android offline-first architecture](https://developer.android.com/topic/architecture/data-layer/offline-first): UI should read observable local data and remain useful without the network. **Adaptation:** Room-backed count/list, no network request merely to show attention.
3. [Android notification permission](https://developer.android.com/develop/ui/compose/notifications/notification-permission): notifications may be denied. **Adaptation:** the in-app indicator must work independently of notification access.
4. [Android Compose semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics): semantics communicate UI meaning and state. **Adaptation:** announce "More, 3 sync entries need review"; retain written explanation in More/Sync so color alone carries no meaning.
5. [Microsoft OneDrive status conventions](https://support.microsoft.com/en-us/onedrive/what-do-the-onedrive-icons-mean): OneDrive distinguishes syncing, blocked/error and attention states and links status to explanation. **Adaptation:** use persistent, actionable attention and distinguish it from routine pending uploads. This is evidence of a broadly used convention, not measured proof that our proposed design is usable.

Material's old banner page did not expose useful readable guidance in this run and is not used as evidence. No broad competitor survey or design-library migration is required for this change.

## Judgment and rejected alternatives

The independent-judgment hook emits a reminder to verify claims, not an external approval or code-quality verdict. Its captured output is [hook-output.json](hook-output.json). The following decision follows from the source and references above.

- **Notification only:** insufficient. Permission denial, dismissal and navigation leave the Owner without persistent in-app attention.
- **Global banner above every screen:** unnecessarily intrusive and risks the changing header/content geometry the owner previously rejected. Not selected.
- **Red warning for any queued upload, parked record or historic conflict:** misleading. Those states differ, and some need no human action. Not selected.
- **Persist a new dismissed/count flag:** duplicates already persistent truth and risks clearing a warning while holds remain. Not selected.
- **Existing More badge + honest Sync row + current review:** smallest useful change with a real action and no financial/transport redesign. Selected.

**Limitation accepted for this patch:** attention is visible only when an Owner is signed in on the installation holding those rows. No cross-phone alert delivery, cashier disclosure or guarantee of unattended conflict discovery is claimed. A phone that only runs Cashier is not made an Owner monitoring device by this change.

## Planned behavior

| Local state | More/navigation | Sync destination |
|---|---|---|
| Count known zero | No badge; ordinary Sync row | Existing status/enrollment and actions. Zero holds does not mean all sync is healthy. |
| Count 1 or more | Count badge (`99+` visually above 99; exact count in semantics); Sync row says "1 entry needs review" / "N entries need review" | Existing held-entry section with correct explanation and actions, when connected. |
| More/Sync opened | Attention stays until actual hold removal | Merely viewing/reloading does not clear or acknowledge holds. |
| Some holds resolved | Count updates from committed data | Failed resolution keeps entry/count. Last successful decision removes held-entry attention; remaining pending upload state is not labelled synced. |
| Offline or notification denied | Same local attention | Existing online operations may fail normally; no permission prompt is needed for attention. |
| Connection removed, holds retained | Attention stays | Show retained count/list and "Reconnect sync to review these entries" guidance; no functioning resolution buttons until connection/config is restored. Do not alter enrollment or resolver semantics. |
| Count not yet known / read failed | No fabricated zero/success; retain last confirmed count in same Owner session on transient failure | Sync status/menu gives retryable unavailable state. On fresh load, use "Checking sync review" / "Sync review unavailable" where appropriate; do not block work. |
| Lock/switch/sign-out | Cancel old collector and clear old UI state | No held row names or counts leaked to unauthorized roles. New Owner session rereads current installation data. |

The More badge uses existing guarded navigation to More, then the Sync row opens the existing destination. Do not silently bypass unsaved-form guards or add deep links to a resolver. No full-width flashing alert, recurring vibration, repeated snackbar, forced modal, animation loop or expanding header.

Use existing theme contrast/typography tokens and existing list treatment. Badge is status inside the icon box, not another capsule around menu text. Add no fixed-height clipping for large text. The short subtitle replaces the existing subtitle; allow normal wrapping in body content. Header and bottom-bar geometry must remain identical as count changes.

## Implementation boundary and order

1. Add an observable count query to `SyncRebaselineHoldDao` and expose a small local attention stream via the existing Owner sync boundary. Use Room invalidation rather than polling or relying on the transient DataChangeBus. This query adds no table/schema version change.
2. Collect attention for the signed-in Owner session across Overview/Work/Records/More, lifecycle-aware. A small separate attention state must not require refreshing the entire More feature or invoking enrollment/network status. Cancel/reset on session change. Do not rebuild the 20-family feature state graph.
3. Pass count/state through the active host to More navigation and Sync menu row. Check fallback `OwnerScaffold` callers before changing signatures; default inactive parameters preserve existing hosts. No Manager/Admin/Cashier/Operations behavior changes.
4. Reuse/extract the current held-entry content so retained holds are visible in disconnected Sync; guard resolution controls and correct the two narrowly related explanations. Observe current holds while Sync is open so count/list do not diverge when a background transaction changes them.
5. Verify the affected query/state/UI and existing resolution preservation methods, then one focused isolated phone walkthrough. Record results and stop.

Expected production touch points: `data/SyncEntities.kt`, `application/owner/NativeOwnerSyncFacade.kt`, small Owner attention state/collector, `nativeui/owner/OwnerPreviewHost.kt`, `OwnerShell.kt`, `OwnerMoreScreen.kt`, `OwnerSyncScreen.kt` and test tags/strings used by those surfaces. `OwnerSyncController.kt`/Owner dependency wiring may need the observation hookup. Inspect only those dependency seams when implementation begins. No sync push/pull algorithm, server SQL, auth refresh, release version or cloud change belongs in this patch.

## Exact acceptance checklist

1. Query stream emits current count and updates after committed insert/removal; closing/reopening a file-backed database rediscovers the same unresolved hold. No copied preference/count is required.
2. A nonzero hold count is visible from Owner Overview/Work/Records/More via More badge, then identifiable in Sync. Zero does not show a review warning.
3. Changes update count/list without a full-screen cold reload, navigation bounce or manual refresh. Opening/closing Sync does not dismiss attention.
4. A failed Take server retains attention; successful existing Keep mine/Take server decisions update it from actual data. Keep mine is described as awaiting upload, not already synced. Reuse the existing repair-boundary method rather than retest unrelated recovery.
5. Disconnected/offline/notification-denied states do not hide persisted holds. Disconnected resolution controls are unavailable with clear reconnect guidance.
6. Lock/role/session switching cancels/reset collectors and does not leak row details or badge state to a non-Owner session. Old asynchronous results cannot overwrite a new session.
7. Count read failure is not reported as healthy/zero. Last known state is preserved within the same session with retryable unavailable copy; new-session state does not reuse a previous session's private count.
8. At normal and 200% text in light/dark themes, test 0/1/many/99+ counts: no overlap, no badge-induced header/bottom geometry change, and usable semantics/full exact count. Existing More/Sync actions and unsaved-form guards still work.
9. Ordinary outbox, dependency-park and audit-only conflict fixtures do not create a held-entry count. No business transaction, hold decision or sync is triggered merely by viewing a badge.

Tests are restricted to new attention query/state coverage, affected Owner More/Sync presentation/navigation methods, existing `failedTakeServerKeepsReviewAndLocalEditThenSuccessfulApplyResolvesBoth` and existing Keep mine/Take server callbacks. Do not run whole classes/full suite merely because files are shared. One connected phone is sufficient for the isolated walkthrough; two-phone convergence and hosted recovery are outside this presentation/local-observation change. Never seed holds in the real shop app for QA.

If a required acceptance case cannot be verified, report that exact gap rather than widening the run or marking the whole phase complete. No tests/builds/phone operations ran in this planning pass.

## Stop and report

On acceptance, cross out **phase 2's named held-entry attention patch**, state the Owner/local-device scope and retain B8 audit-only conflict attention as partial. Do not claim every conflict is now handled. Record unrelated issues without investigating them. Phase 3 Credit basics is the next feature scope; it must receive its own baseline/research/judgment before coding.

## Execution checklist

- [x] ✅ ~~Baseline, research and independent judgment.~~
- [x] ✅ ~~Observable held-entry data and session/lifecycle state.~~
- [x] ✅ ~~Owner navigation/menu and retained disconnected review.~~
- [x] ✅ ~~Affected verification and synthetic visual inspection.~~
- [x] ✅ ~~Result and plan reconciliation.~~

The original planned wording above is retained as the baseline. [RESULT.md](RESULT.md) records executed scope, badge-only visual refinement, failures/retries and limits.
