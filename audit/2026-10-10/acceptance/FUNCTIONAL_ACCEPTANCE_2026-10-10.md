# Functional DEV acceptance — 2026-10-10

The owner confirmed both logins and enrolled the new A06 QA workspace into DEV, then reported **3 skipped / 24 updated**. This continuation completes the bounded real-session checks. Design reconsideration and further frame profiling are deferred at the owner's request. Earlier audit severities and failed evidence are preserved.

## Phone sync result

The three skipped records are **already-present immutable events**, not refused uploads: PURCHASE_RECEIPT, PRODUCTION and VENDOR_PAYMENT. Supabase MCP returned the three server payloads at sequences 220–222. Against the consistent phone database copy, every event header and inventory/financial/spoilage child multiset matches after UID resolution. The 24 updated records comprise two products, sixteen accounts and six settings. No planning/profile satellite record is part of this result.

`SyncApplyEngine.apply` combines PRESENT with SKIPPED in its counter. Its existing-event path returns PRESENT after checking the identical immutable header and subtracting matching ledger children. The phone has **zero semantic skip audit entries**. This explains the owner's observed count without relabeling unrelated refusal cases as safe.

- DEV acceptance shop: b0d97cf0-8304-4ef2-b8fc-3c3367230d94; 27 sync records, maximum sequence 222.
- A06 package: com.saaf.milkshop.qa, USB R8VXA02SNKH. Binding is DEV / owner / Acceptance Shop; server session is present and trusted time is anchored.
- Local pull cursor: 222. Outbox, parked rows and rebaseline holds: all zero. Three local events, three inventory legs and three financial legs remain; SQLite integrity check on the copy returns ok.
- Two repeat drains: pushed 0; pulled applied=0, updated=0, skipped=0, parked=0, conflicts=0. The second ran after the Viewer session tests. No duplicates or newly stranded work were observed.

The QA process was briefly paused to copy database and WAL consistently, then resumed in a finally block. No phone token was read, account reset, database cleared or ordinary production application modified. Host SQLite verifies this copy; the prior bundled-driver device tests remain separate evidence.

## A02 — MEDIUM: successful void staff mutations reported as unavailable

**Reproduced and repaired locally.** Creating the disposable DEV admin and signing it in succeeded. Disabling it through the actual Viewer returned HTTP 503 / SERVER_UNAVAILABLE, while MCP confirmed the account had become inactive. The write RPC succeeded, but `callWriteRpc` unconditionally called `res.json()` on its empty HTTP 204 acknowledgement and converted the parsing exception into an unreachable result. This is a product defect, not intentional behavior or a stale test.

The repair accepts HTTP 204 only for the six SQL void write RPCs: disable, enable, set role, assign shop, unassign shop and reset-credential audit. JSON-returning identity/shop operations still require parseable JSON, retaining ambiguous-outcome handling. SQL return contracts were checked through MCP. No grants, schema, authorization policy or credential format changed.

Changed Viewer files, in C:/Users/Teymur/Documents/Projects/saaf-milk-admin-viewer:

- src/lib/account-server.ts: use the response decoder for write RPCs.
- src/lib/write-rpc-response.ts: explicit void acknowledgement handling.
- src/lib/write-rpc-response.test.ts: five regression cases, including all six void names and rejection of absent/malformed identity results.

Validation: **114 Viewer tests pass**, no failures/skips; TypeScript, ESLint and a fresh production build pass. The existing middleware-convention deprecation remains a build warning. Rebuilt Viewer build ID: **3TqHSnfQhtAOsMF04k8Gl**. It is running locally on 127.0.0.1:3100 with explicit DEV configuration and the existing localhost COOKIE_SECURE=false setting. No published Viewer was changed.

## Actual DEV account/session checks

The final bounded HTTP workflow run has **17 passing checks**:

- A Viewer logout clears its own session; a separate Viewer session remains authenticated. A separately created real Supabase refresh token still refreshes successfully after logout, proving provider session isolation beyond an unexpired access JWT.
- The same disposable admin is enabled, signs in and reads a protected overview. Disabling it now returns HTTP 200.
- Its already-authenticated read session receives 401 / UNAUTHORIZED. A separate still-authenticated session receives 401 / UNAUTHORIZED for a mutation, independently exercising active-profile enforcement. A fresh login is denied with 403.
- Disabling the sole active owner is refused with 409 / LAST_OWNER. MCP confirms that owner remains active.
- The real reconciliation endpoint returns processed=0 / resolved=0. This proves an empty queue is reachable; it does **not** claim an injected ambiguous Auth failure was exercised live. Earlier rollback and identity-reconciliation fixtures retain their narrower evidence.

One disposable fixture, accept.staff.1010 (fc6eee75-2c1a-4f66-8958-8a5d00cddab4), was created through the actual Viewer route and remains disabled with its audit trail. Repeated runs reused it. Other accounts visible in the acceptance business were preserved. New fixture credentials stay in the owner's private temporary file; no credentials, cookies, tokens or privileged keys were saved in report evidence.

## Evidence, unsuccessful attempts and limits

- real-dev-workflows-first-failure.json preserves the committed-disable / false-503 reproduction. Staff creation/login passed in this run; later checks did not execute.
- real-dev-workflows-second-pass.json preserves the first repaired success. Its mutation check followed a read that cleared that session's cookies. The final run strengthens this with an independent authenticated mutation session; do not claim the older run proves that boundary.
- real-dev-workflows.json is the final 17-check run; check-real-dev-workflows.py is its bounded reproducer.
- sync-server-events.json and sync-first-state-verification.json preserve server payloads and local equality checks. real-dev-phone-sync.txt and real-dev-phone-final-state.json preserve final repeat-sync logs/state. real-dev-final-mcp-verification.json preserves the final DEV state and audit counts.
- viewer-void-rpc-tests.txt, viewer-void-rpc-types.txt, viewer-void-rpc-lint.txt, viewer-void-rpc-build.txt and viewer-void-rpc-server.txt record the repaired Viewer checks/runtime.
- Initial evidence queries used incorrect plural table/UID column names and a PowerShell argument combination; they stopped with errors and were corrected from the actual schema. A report-append helper also stopped on implicit Windows decoding before editing the original audit; it was corrected to UTF-8. Final phone collection verified and saved its JSON/log evidence before a Windows cp1252 console print failed on a Unicode arrow. The saved evidence was independently re-read and assertions passed with UTF-8 output. These are collection/infrastructure errors, not product failures or passing executions of the failed commands.

No new browser-GUI pass, second-phone convergence pass, exhaustive UI/accessibility matrix, production disaster-recovery restoration or physical power-loss guarantee is claimed. Prior Android/SQL regression evidence remains dated and unchanged. The source decoder tests cover the shared response contract; live enable/disable calls do not imply all six administrative workflows were individually executed live.

## What remains

No further phone action is needed for this DEV sync/session acceptance. Keep using Saaf Milk (preview), package com.saaf.milkshop.qa, and the local DEV Viewer. The earlier READY_FOR_OWNER.md and verification-new-owners.json are preparation snapshots; their old package/enrollment/pending-login descriptions are superseded by this report, not silently rewritten.

The next phase is **production rollout**, including publishing the repaired app/Viewer and applying the already-prepared server migration. **S01 remains HIGH and open on PROD**: automatic approval review rejected the proposed PROD migration because explicit production authorization was insufficient. No PROD mutation or production publication occurred in this continuation. The prepared migration remains audit/2026-10-09/repairs/SUPABASE_LIVE_REPAIR_2026-10-10.sql; deployment needs the owner's explicit approval.

Existing historical partial events need same-shop authority/recovery review only if that affected history will be migrated. Intentional planning/profile sync limitations and owner-reset/security policy decisions remain recorded in the original audit; this pass neither changes them nor certifies full multi-device feature parity. Owner visual redesign and additional motion profiling are deferred, not presented as prerequisites for these functional checks.
