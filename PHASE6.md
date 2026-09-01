# Phase 6 — Canonical Bridge Contract

**Status: COMPLETE — 2026-08-28**

## 1. Purpose and boundary

Phase 6 freezes the message contract that the packaged React UI and Kotlin application layer must use. It converts the Phase 5 read-model catalog and the implemented native command/security capabilities into one reviewable, machine-checked vocabulary before any WebView bridge is built.

This phase defines messages only. It does not expose a JavaScript interface, implement a WebView message listener, adapt commands to domain services, or replace React mock/local state. Those begin with the deliberately small Phase 7 authenticated vertical slice.

## 2. Canonical artifacts

- `bridge/CONTRACT.md` is the normative human-readable transport, authentication, validation, error, refresh, concurrency, and version-negotiation contract.
- `bridge/contract.json` is its machine-readable schema appendix.
- `scripts/verify-bridge-contract.py` validates the manifest against the frozen Phase 5 Kotlin DTOs, native read methods, and native permission enum, then checks generated projections byte-for-byte.
- `phase18/app/src/main/java/com/saaf/milkshop/application/bridge/BridgeContractSnapshot.kt` is the generated Kotlin version/hash/action snapshot.
- `ui/src/bridge/contract.generated.ts` is the generated TypeScript scalar, DTO, request, response, error, envelope, and endpoint projection.

Contract version 2 contains the same 78 unique operations: 20 queries, 54 commands, and 4 system operations. Version 2 is an additive least-privilege correction that exposes active expense categories inside the already role-filtered `TransactionReferencesDto`; no endpoint or command request changed. Its canonical manifest SHA-256 is:

`AB374869603B9D773AD86B56EF801DA8C4493816C7301B05214AE596E33D0920`

### Phase 10 additive amendment — contract version 3

Checkpoint 10.2 advances the canonical contract to version 3 without adding or removing an operation and without changing any request. The existing `PlanningDto` response now carries native-calculated budget actuals/variances, derived sales targets and actual sales/variance, plus collection-target actuals/variance. Kotlin remains the sole calculator. The version-3 manifest SHA-256 is:

`2B4FA647E2BFE4F3A12EA9CC12516E93CD52EEEA6BFA3508BE48E6991A88B08B`

## 3. Representation and security decisions

1. Native 64-bit IDs/counts, integer PKR paisa, and precise decimal quantities cross JSON as branded base-10 strings. They never depend on JavaScript floating-point precision.
2. Dates, months, and instants are explicitly typed. The manifest records the exact encoding of numeric/date string fields inherited from Phase 5 DTOs.
3. Protected requests carry only an opaque memory-only session handle in the request envelope. Payloads may not contain a session, actor ID, role, or asserted authority.
4. Kotlin remains responsible for session validity, recent authentication, permissions, validation, accounting, stock, actor attribution, persistence, and safe error mapping.
5. Authentication, enrollment, user administration, and encrypted backup/restore have explicit messages. Backup bytes and arbitrary paths never cross JavaScript; Android owns file selection and restore tickets.
6. Unknown fields are rejected. Commands are never automatically retried. Query refresh lists are explicit and may contain only known query actions.
7. Failures use one fixed 19-code catalog and never expose exceptions, SQL, paths, credential state, secret input, or lockout timing.
8. `system.bridgeInfo` must prove the version, manifest hash, and supported actions before the UI proceeds. A mismatch blocks interaction without changing native data.

## 4. Mandatory drift gate

The checker rejects:

- duplicate or malformed actions;
- unknown permissions, enums, schema types, responses, errors, or refresh targets;
- missing native Phase 5 query methods;
- forbidden caller-supplied session/actor/role fields;
- implicit persistence or refresh behavior;
- stale Kotlin or TypeScript generated files;
- a stale manifest hash in `bridge/CONTRACT.md`.

Android `preBuild` depends on the checker. The UI `prebuild` script also runs it. Intentional changes require editing both canonical contract files, running `python scripts/verify-bridge-contract.py --write`, reviewing both generated projections, and rerunning the verification commands.

## 5. Verification

- Canonical contract validation — PASS: current version 3, 78 endpoints, generated SHA-256; the historical version-2 amendment above remains recorded.
- TypeScript type check (`npm run lint`) — PASS.
- Production React/Vite build, including `prebuild` drift gate — PASS. The existing large-chunk advisory remains a non-failing later optimization item.
- Kotlin and Android-test Kotlin compilation — PASS; Gradle ran `verifyBridgeContract` before `app:preBuild`.
- Frozen Phase 18 logic specification hash — unchanged.
- Frozen Phase 18 SQL blueprint hash — unchanged.

Phase 6 changes no Room schema, accounting formula, stored business record, user permission bundle, or existing native screen behavior. Therefore its acceptance evidence is contract validation plus both consumer compilations; the established 124/124 connected-device native regression remains the latest full device baseline.

## 6. Next gate

Phase 7 must implement only a narrow packaged-device vertical slice first:

1. origin-restricted WebView asset loading and asynchronous message transport;
2. `system.bridgeInfo` version/hash negotiation;
3. native-backed authentication/session handling;
4. one permission-checked Phase 5 query;
5. one protected command with verified native actor attribution and Room persistence;
6. response/error/refresh handling proven end-to-end on a physical device.

Do not wire all 78 operations or convert screens broadly until that slice proves React → bridge → Kotlin → Room → React without local business truth.

## Plain English

The app now has one locked dictionary for everything the future screens may ask Android to read or save. It says exactly what information is allowed, who must be signed in, what permission is needed, what can go wrong, and what the screen should refresh afterward. No shop records or calculations were changed. The next phase builds one small real connection using this dictionary and proves it on the phone before we connect the rest of the screens.
