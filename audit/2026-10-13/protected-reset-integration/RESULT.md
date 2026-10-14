# Phase 4, 8a2 — local integration complete

The supported Viewer reset route and CLI reset command now use the prepared reservation → single-use claim → Auth update → completion protocol. These changes are installed in the two local source checkouts. **Nothing was deployed, no real credential was reset, and B4 remains open until deployment acceptance.**

## Delivered behavior

- Viewer preflight includes target role, active state, business, termination and Auth linkage. The authenticated database reservation is the authoritative live decision; preflight is not claimed as race protection.
- Admin can reset active Manager/Cashier/Operator only. Owner can reset linked non-terminated known-role targets, including disabled accounts. Reset never enables an account. CLI remains separate Owner-controlled privileged maintenance with project confirmation.
- A matching reservation acknowledgement and one valid claim are required before one Auth request. Denial, stale-snapshot rejection, lost/malformed begin or claim acknowledgement sends no Auth request. Neither route nor CLI automatically retries Auth.
- Unknown Auth responses, including malformed/mismatched success bodies, retain a pending guard. Known success displays the one-time credential even if completion/audit acknowledgement fails. Passwords are not persisted or included in status/audit/diff evidence.
- Viewer staff detail shows eligibility, pending reference and Owner guidance, disables repeat submission, and offers read-only status checking. The credential stays visible without a fallible staff refresh; changing staff routes remounts the panel to isolate its state. CLI prints its request reference before dispatch and supports `--status` without an Auth update.
- Status returns only authorized pending UUID/state; other-business or malformed metadata fails closed. Service-only claim/completion RPCs are absent from the caller write allowlist. Existing internal transport already accepts JSON tables and SQL-void acknowledgements; no parser change was necessary.
- The 8a1 generated migration was copied byte-identically into the local account-server migration directory. Contract and operation guidance describe deployment order, held participants, completion-only retries, safe cancellation prerequisites and the absence of an automatic unknown-outcome release.

## Focused verification

Final installed source passed 35 synthetic route/eligibility tests, 4 actual transport/allowlist tests, 4 controlled staff-page state/render checks, 8 CLI tests and 11 existing security/response-parser tests. The eligibility test exercises the role/active/termination matrix. Scoped TypeScript checks cover the four changed Viewer roots and their imports; targeted ESLint and Git whitespace checks passed.

The route harness transpiles the exact installed route and replaces external gateways with offline stand-ins. Transport tests load the installed transport and actual header/response helpers with synthetic fetch responses. UI tests execute the actual page through controlled hooks and JSX objects; they are not browser appearance or screen-reader acceptance. CLI tests import the installed module without invoking main/configuration or a network request. SQL concurrency proof remains the separately saved 20-group 8a1 result; SQL was unchanged and was not rerun. No Android suite, build, real Auth acceptance or remote catalog parity check ran in this unit.

The first CLI fixture expected the wrong display separator; it was corrected to use the existing formatter. The first local installer hit a Windows default-encoding error before writing anything. Dependent checks mistakenly ran against old files and failed; those are not accepted results. UTF-8 decoding was made explicit, the hash-checked install succeeded, and affected checks were run against the installed source. Original before-images and final source hashes are retained, including pre-existing account-server changes.

## Reviewable changes and next boundary

`manifest.json` identifies the eight installed source/document/migration files and their original/final hashes. `viewer.patch` and `server.patch` describe this unit relative to the inspected working files, preserving earlier server work. Prepared copies and original before-images are retained alongside focused tests. No Git commit or push occurred.

Next bounded run: **protected-reset deployment preflight and concrete deployment proposal only**. Check exact destination/version/migration parity, review these patches and prepare disposable-identity acceptance. Publication and a real disposable reset need their own authorization under the existing plan. Recovery (8b) and shop setup (8c) remain separate. Do not reopen phases 1–3 or begin Android/backup work in that run.

Whole plan: phases 1 money/withdrawals, 2 receiving sync and 3 mapped menus/lists are complete at bounded QA scope. Phase 4 reset integration is locally complete; deployment, recovery rehearsal and setup remain. Phase 5 shop-day/phone acceptance and phase 6 accepted release remain.
