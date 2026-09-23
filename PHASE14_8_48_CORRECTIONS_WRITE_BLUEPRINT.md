# Phase 14.8.48 — Native Corrections write UI (Step 1)

Status: **AUTO-ACCEPTED FOR NATIVE IMPLEMENTATION**

Date: 2026-09-22

## CORE METHOD (mandatory)

Authority: `PHASE14_8_15A_SHELL_VS_CONTENT.md` and `.cursor/rules/native-shell-vs-content.mdc`.

Corrections write depth lives inside the existing **Records** surface already hosted by Owner / Manager / Admin via `OwnerRecordsScreen` + `HostedScaffold`:

1. **Role-neutral content** — detail Correct CTA, correction editor form, loading/saving/failure, draft leave-guard presentation. No `OwnerSection` / `ManagerSection` / `AdminSection` / foreign bottom nav inside reusable content.
2. **Role-owned shell** — keep each host’s scaffold / nav / snackbar / Back path (`OwnerScaffold`, `ManagerScaffold`, Admin Records host). Hardware Back and bottom-nav exits while the correction draft is dirty use `EntryLeaveGuard`.
3. **Foundation proof** — Manager is the primary host ask: Manager chrome ✓, foreign chrome ✗, Back from editor → detail → Records list → Work home, session intact. Owner and Admin Records hosts must stay green.

**Forbidden:** mounting hybrid History/Corrections XML UI; calling bridge JSON (`command.correctEvent`); inventing an approval-queue domain API; voids; changing `CorrectionService` semantics or Room schema.

## Connected (this slice)

Replace Records detail deferred notice (“Corrections write UI comes next”) with a typed correction form.

1. From event detail, when the event is correctable (supported type, not already superseded, actor has `CORRECTION`), show **Correct** and open the editor.
2. Editor matches `CorrectionInput` sealed variants for the event type: Sale, Purchase, Production, Monetary, Spoilage. Prefill original values from the event / detail.
3. Save via `NativeOwnerRecordsFacade.correct(session, CorrectionInput)` — same auth and day-closed rules as `HistoryBridgeCommandService.correct` (permission `CORRECTION`, already-replaced conflict, posting-closed day, actor link + audit). Never bridge JSON.
4. Success refreshes history + opens the new correction event detail.
5. Dirty draft: leave-guard on hardware Back and bottom-nav exits.
6. Records group chip **Corrections**: connected filter over correction/superseded lineage (no deferred snackbar). Balances / Closing remain deferred notices.
7. Manager / Owner / Admin hosts that previously snackbar’d “Corrections stay deferred” on the Corrections group now use the connected filter path (write still starts from a correctable detail).

## Explicit deferred

- Approval queue write UI (no domain API — do not invent)
- Voids / supervisor approval workflows beyond existing `CorrectionService`
- Balances & ledgers / Closing history Records groups
- Live inventing of new correction types beyond `CorrectionInput`

## Native data boundary

`NativeOwnerRecordsFacade`:

- keep `loadHistory` / `loadDetail` / `filterRecords`
- add `correct(sessionHandle, CorrectionInput): CorrectionResult` (and any presentation helpers for correctability / draft prefill)
- roles with Records host + `CORRECTION`: Owner, Manager, Admin
- never call bridge JSON; do not change Room schema, RolePermissions, or `CorrectionService` algorithms

## Control contract

| Control | Required workflow | Visible result | Native test obligation |
|---|---|---|---|
| Detail Correct | Open editor for correctable event | Prefill matching type | Contract + facade |
| Save correction | Typed facade write | New correction detail; list refreshed | Facade + contract |
| Leave dirty editor | Confirm discard | Back to detail without save | Contract leave-guard |
| Corrections group | Filter lineage | Correction/superseded rows (or honest empty) | Contract |
| Balances / Closing | Deferred notice | Explicit not-connected copy | Existing tags |
| Manager host | Open editor under Manager | Manager nav ✓; Owner/Cashier/Ops nav ✗ | Foundation |
| Owner / Admin hosts | Existing Records chrome | Stay green | Existing + regression |

## Accept gate

**AUTO-ACCEPTED.** Connected = typed Corrections write on Records detail under CORE METHOD for Owner/Manager/Admin hosts; Corrections group filter connected; approval queue remains deferred. Gates: `qaScreen -Pqa.screen=managershell` and `qaScreen -Pqa.screen=ownermore` (and `ownerrecords` when exercised) stay PASS; debug APK with `open_manager=true`.
