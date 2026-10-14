# Phase 4 batch 8a — protected credential-reset baseline

## Current evidence

Read the current Viewer route, mutation context, account-server CLI, reset-audit RPC and reconciliation route. The Android repository contains references to these separate checkouts, rather than their implementation. Viewer source is at `C:/Users/Teymur/Documents/Projects/saaf-milk-admin-viewer`; account-server source is at `C:/Users/Teymur/Desktop/saaf-anchor-tmp`. Git metadata access to both external checkouts was denied in this sandbox; current publication/deployment parity is not established.

The Viewer refreshes the caller profile in requireMutationContext and checks active/non-terminated Owner/Admin status. Its target lookup selects business, terminated state and Auth identity, but omits role and active state. The privileged Auth password update happens before the caller-authorized reset-audit RPC. The contract currently explicitly permits Admin resets without a protected-target exception. This is a policy change against the chosen completion plan, not an undocumented claim that the old implementation violated its old contract.

The CLI is a different trust tier: it holds a privileged server key and has no staff-session caller. It also updates Auth before audit. The reset-audit RPC does not change passwords; adding a policy check there alone would be too late. Reconciliation's reset-audit branch records missing audit work and does not reset passwords.

## Reproduction

`current-route.test.cjs` transpiles the exact inspected route and replaces only imported gateways with synthetic offline stubs. Nine passing characterization cases establish the route behavior: ordinary Admin/Cashier success; Admin/Owner and Admin/Admin success; cross-business, terminated and unlinked denial without mutation; simulated target promotion after lookup; simulated caller demotion after context authorization with Auth mutation preceding audit denial; disabled target currently resettable. Passing characterization tests mean the existing behavior is reproduced, not that the new policy is implemented or accepted. The concurrency cases inject changes at request boundaries; they are not live concurrent database tests.

Inspected route SHA-256: `c4874947e495406b596f00fdb40f77a97988853fa975bb751adc67d201cff7b3`. No network gateway, real credential, database or Auth identity is used by the harness.

## Chosen policy

| Current caller | Target in same business | Decision |
|---|---|---|
| Active, non-terminated Owner | Non-terminated Owner/Admin/Manager/Cashier/Operator with linked Auth identity | Allow, including disabled target; reset does not enable access |
| Active, non-terminated Admin | Active, non-terminated Manager/Cashier/Operator with linked Auth identity | Allow |
| Admin | Owner/Admin, including self; disabled ordinary target | Deny |
| Any other, demoted, disabled, terminated or unlinked caller | Any | Deny |
| Any caller | Other business, missing/terminated/unlinked target or unknown role | Deny without Auth mutation |
| Privileged CLI maintenance operator | Same supported targets | Separate Owner-controlled maintenance trust; use the same target reservation/guard and explicit project confirmation, never a caller ID supplied as proof |

Disabled Owner-managed targets remain resettable so recovery can repair their credential without enabling the account. Admin must have an Owner enable a disabled target first. Unknown roles fail closed. This table is the implementation target, not deployed behavior.

## Why a dependency is required

A fresh role lookup followed by a privileged Auth API request still leaves a request-boundary race. The old reset-audit check is post-mutation. Making every reset Owner-only would not eliminate a concurrent caller demotion. Hiding a button offers no protection to direct requests. Supabase's supported Auth admin update applies changes directly on the server; it does not carry this application's staff caller/target policy transaction. See the [Auth admin update documentation](https://supabase.com/docs/reference/javascript/auth-admin-updateuserbyid).

Chosen design for the next unit: a database-enforced pending reset reservation that locks/rechecks authoritative caller and target rows in a consistent order, then prevents role, active, terminated, Auth-link and business changes to either participant while the privileged request is unresolved. A guard must cover supported SQL/RPC/CLI state changes, not only Viewer buttons. Successful Auth mutation plus durable audit/reconciliation finalizes the reservation. An ambiguous request failure retains an explicit unresolved reservation; it must not silently expire while an Auth request might still complete. Resolution is an explicit trusted operation, not automatic credential replay or a timeout that reopens the race. No plaintext password is persisted. A compromised privileged service key remains outside ordinary staff authorization; supported CLI use must share the reservation path.

## Two bounded implementation units

**8a1 — server policy and concurrency guard.** Add only reset reservation/finalization policy, the participant-change guard and a synthetic database fixture. Verify the table above, simultaneous role/active/link/termination changes, concurrent reset, ambiguous outcome handling and no mutation on denial. Establish deterministic lock order and audit attribution. Create the actual migration via the configured Supabase CLI when implementing; do not invent a migration timestamp here. Deliver exact SQL and selected tests before remote application.

**8a2 — Viewer and CLI integration.** Replace the unguarded reset orchestration with reservation → privileged Auth update → durable completion/reconciliation. Route failure keeps unresolved state when the mutation outcome is unknown. Apply the same supported path to CLI maintenance; update contract text and UI eligibility/copy from the policy. Test actual route orchestration with synthetic gateways and caller/target changes, then a disposable environment only. Prepare an exact deployment diff and ordering/rollback notes. Publication and actual server acceptance remain separate.

Stop conditions: no unrelated credential system redesign, central device/person revocation, general account creation cleanup, recovery overhaul, dependency update or ordinary staff password reset. Do not report B4 complete until the guard and both supported reset paths meet the selected tests and deployment status is explicitly recorded.

## This run's result

Baseline and scope are complete; B4 remains in progress, with implementation not started. No external source, local app source, server configuration or real identity changed. The independent-judgment hook was read and executed; it supplies a reminder, not independent validation. The Supabase skill was used; its Markdown changelog fetch failed on content type, so the HTML index and Auth admin API reference were consulted. No platform change was needed for this baseline.
