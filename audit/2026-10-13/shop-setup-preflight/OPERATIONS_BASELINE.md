# 8c1 verified operations baseline — 13 October 2026

This preflight closes current visibility checks and scopes setup; it does not configure an ordinary phone or certify the whole launch. No source behavior, remote account/schema, deployment, provider policy or billing change ran.

## Current external state

| Surface | Observed evidence | Conclusion / limit |
|---|---|---|
| DEV / PROD | Both ACTIVE_HEALTHY, PostgreSQL 17.6, Tokyo, selected organization Free | No pause/resume needed; ordinary deployment unchanged |
| Active-project capacity | Two active Free projects already present | Hosted isolated identity rehearsal still needs the exact optional route in the existing proposal; no paid upgrade implied |
| Database footprint | DEV 12,864,659 bytes / 66 sync rows; PROD 12,201,107 bytes / 36 sync rows | Small current raw DB footprint; these are database SQL measures, not complete provider-billing/egress/storage/MAU quota totals |
| Reconciliation | Zero unresolved reconciliation rows in each project at the selected read | Not a claim that every private reset/session operation is idle |
| Sync RPC source | Same selected `sync_push(jsonb,text,uuid,bigint)` definition MD5 `cc681db1a8a41e61971876750b908da8` in DEV/PROD | Current definition parity only; no new real Auth/transport mutation test |
| Keepalive | Workflow active; latest recorded run `38307571485`, 12 October 20:57 UTC, both Ping DEV/PROD jobs success | Current keepalive works; scheduled GitHub activity is not a contractual uptime guarantee |
| Backup | Two accepted identity backups on 13 October remain credited | Recovery proof is separate from keepalive; see existing backup result |
| Server repo | Private main `802ec50`; broader security CI remains red as recorded in backup result | No new publication or blanket scanner suppression |
| Viewer | Production `dpl_C13bg6KbNN1mCsCCbKqbVc3MLHts`, READY, alias `sm-server-view.vercel.app`; main `b5122e8` | The earlier Viewer presentation is already live; do not republish it merely because an old plan says pending |

O5 current publication visibility is satisfied for the named earlier presentation, not the newer protected-reset production changes. B4 browser/production acceptance remains open. Real PROD administration fixtures were not used here (A7).

## Provider/security policy observations

Current security advisor categories: DEV has 9 no-policy RLS INFO rows, 19 ordinary-role SECURITY DEFINER WARN findings, and leaked-password protection WARN; PROD has 7, 18 and 1 respectively. Names/counts/remediation links are saved in `remote-metadata.json`. Warnings are observed facts, not proof of exploit or automatic permission to change production grants. Explicit deny-by-default tables and intentionally authorized RPCs require their specific policies/caller checks; no full re-audit or blanket change to SECURITY INVOKER ran.

Review references: [RLS without policy](https://supabase.com/docs/guides/database/database-linter?lint=0008_rls_enabled_no_policy), [authenticated SECURITY DEFINER RPC](https://supabase.com/docs/guides/database/database-linter?lint=0029_authenticated_security_definer_function_executable), [password protection](https://supabase.com/docs/guides/auth/password-security#password-strength-and-leaked-password-protection). Leaked-password protection is disabled by current advisors and available on Pro and above per official docs. An upgrade or password-policy change is a separate concrete decision; it is not applied to this Free organization as a hidden setup action. Current exact minimum/character Auth configuration was not read anew or claimed from old notes.

Supabase manages platform updates. The latest [TLS notice](https://supabase.com/changelog/cbc-tls-ciphers-deprecated) removes specified old CBC ciphers starting 10 November at the earliest; no current device failure was reproduced. Do not upgrade app libraries or database engines on that notice alone. Existing native restore proof is PostgreSQL 18.6; these hosted projects remain 17.6. Provider upgrade/migration verification belongs to a separately scoped change when applicable.

Keepalive uses the public ping RPC with client/publishable credentials; no privileged key is needed for monitoring. Current successful jobs prove existing configured requests work, not every future key format. Read GitHub Actions failed-run activity and Owner provider notifications; do not silently change notification/account settings or create recurring automation. [Free-project pausing](https://supabase.com/docs/guides/platform/free-project-pausing) documents low activity and the resume window; retain usable independent backups even with keepalive.

Project deletion is permanent and removes data/backups/configuration. Only the already named synthetic QA project may be deleted under exact teardown authorization; never shared DEV/PROD. Pausing is a distinct reversible provider action with downtime, not deletion or automatic clearance of full quota. [Deletion policy](https://supabase.com/docs/guides/platform/delete-project).

## Setup/access baseline and selected verification

See [SETUP_AND_ACCESS_NOTE.md](SETUP_AND_ACCESS_NOTE.md) for actual local sign-out, account switch, stop-sync, server verdict and degraded offline behavior. Cashier/Admin/Manager/Operations menus call their action `Sign out` with subtitle `Return to Welcome`; Owner already says `Lock / switch user` and explains separate sync enrollment. Older notes claiming a universal rename therefore describe only part of current behavior. Owner Sync retains `Sync` header and explains only local data preservation in its stop-sync dialog. The specific gap is inconsistent explanation of local app-session lock and background business transport, not broken sign-out logic.

Six existing A06 instrumented methods pass on the exact already-built isolated APK pair: Owner enrollment/timezone; failed enrollment saves nothing; stop-sync clears connection files/tokens; unauthorized/cross-shop selection preserves existing binding; unknown sign-in never becomes identity denial; offline/disabled/malformed/wrong-shop response never renews grace. HTTP/Auth are synthetic in these methods. Ordinary/preview/QA package provenance is unchanged. No actual counter credential, account, token, app data or signing key was retrieved.

The test name `unenroll_clearsEverything` does not assert Room deletion; source clearly retains Room/local history. No central per-person offline revoke guarantee exists while `ENFORCE_LINK_STATES=false`. Grace expiry after 14 days restricts privileged actions but retains daily entry and reads. These are verified implementation boundaries, not pilot trust-model approval.

## Next 8c2 — small explanation and boundary checks only

Keep existing actions/design/portrait. Add consistent short explanation beside local Sign out across the five role menus: it locks the local app session while business sync continues. Explain business-sync independence in the Sync connection card/stop confirmation, including retained local data and both upload/pull stopping after connection removal. Explain that a server suspended/revoked verdict affects remote access without centrally disabling offline local users. Phrase degraded mode as daily work continuing while privileged actions require reconnection; do not change the 14-day policy.

Touched boundaries: the five More menu definitions (including Owner's shared menu definition if applicable), `OwnerSyncScreen`, and only the existing affected screen contracts plus two meaningful synthetic boundary cases: local sign-out preserves transport/unsent data; stop-sync clears connection files/tokens while preserving Room users/binding history/unsent records. Use existing fake HTTP and isolated package. No actual phone setup, credential change, account-server transport redesign, new revoke button, enforcement flag, notification feature, dependency upgrade or general UI redesign. Select the existing theme/larger-text Sync fixture only if the copy layout is touched. Stop when the copy and those actions are verified.

Ordinary shop enrollment is a later concrete handover within O3/phase 5/6, using the intended release/device/shop and privately held Owner-approved credentials. No need to ask for or inspect a secret to implement 8c2. Central identity B7 remains conditional; persistent conflict attention B8 stays its existing separate conditional requirement. O4 policy changes and A7 real PROD-admin acceptance stay separate.
