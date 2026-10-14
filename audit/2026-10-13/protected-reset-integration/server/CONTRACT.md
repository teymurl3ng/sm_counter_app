# Saaf Milk Account Server — CONTRACT (Phase A v1)

Frozen from the **tested** implementation (Steps 1–7).  
Governing brief: `saaf-milk-account-server-brief-v3.1.md`.

This document describes the **server contract only**. It does not define Android behavior.

---

## Service responsibility

The Account Server owns only:

* business identity
* shop identity
* staff identity
* usernames
* server credentials (Supabase Auth passwords)
* role **strings**
* account active / disabled / terminated state
* staff ↔ shop assignments
* server-side audit history
* the public Auth + `my_profile` / `ping` contract

It does **not** own:

* ERP / business data (sales, inventory, etc.)
* Android local authentication (passphrase, Quick PIN, biometrics)
* Android permission implementation
* Google Drive
* cloud backup scheduling / restore policy
* device management or wipe
* Android offline login policy
* an admin website (future)

---

## Environment model

| Env | Project name | Purpose |
|---|---|---|
| DEV | `saafmilk-dev` | Testing, contract suite, recovery rehearsal |
| PROD | `saafmilk-prod` | Real staff identities |

* Separate Supabase projects; **same** migration, schema, roles, RPCs, and public contract.
* Select with `admin.py --env dev|prod` (loads `.env.dev` / `.env.prod`).
* **Privileged secret key** (`sb_secret_…`) — owner PC / admin CLI only. Never in Android, never in GitHub Actions keep-alive, never committed.
* **Publishable/client key** (`sb_publishable_…`) — public Auth + RPC calls that obey RLS.

Do not put real keys in this document.

---

## Frozen roles (v1)

Exactly:

* `owner`
* `admin`
* `manager`
* `cashier`
* `operator`

* Role strings are **server metadata**. Android later decides permission meaning.
* Unknown roles are **rejected**.
* There is **no** `add-role` (and no dynamic role catalog edits) in v1.

---

## Username contract

Normalization (admin CLI and any future client must match):

1. trim whitespace
2. lowercase

Must match:

```text
^[a-z0-9._-]{3,32}$
```

* Duplicate normalized usernames are rejected (including **terminated** usernames — reserved forever in v1).

---

## Server credential contract

* Exactly **15 digits**
* Generated with a cryptographically secure random source (`secrets`)
* Shown **once** on create / reset / recovery of credentialed accounts
* This project never stores plaintext credentials
* Input normalization: strip every non-digit character (so `12345-67890-12345` and spaced forms are accepted)
* Display grouping is optional cosmetics (`12345-67890-12345`); Auth always uses 15 plain digits

**Not** the Android local passphrase, Quick PIN, or biometrics.

---

## FAKE_DOMAIN

**Verified value:** `saafmilk.local`

Tested through real Supabase Auth create + client sign-in for:

* username length 3
* username length 32
* `.` `_` `-` in usernames

Mapping:

```text
{username}@saafmilk.local
```

No email is sent. Users are created with email confirmed via the Admin Auth API.

---

## Public HTTP / Auth contract

Verified against current Supabase Auth + PostgREST on this project.

### Sign-in

```http
POST {SUPABASE_URL}/auth/v1/token?grant_type=password
apikey: {PUBLISHABLE_KEY}
Authorization: Bearer {PUBLISHABLE_KEY}
Content-Type: application/json

{"email":"{username}@saafmilk.local","password":"{15 digits}"}
```

Success: HTTP `200` with `access_token` (and related Auth fields).

### Profile

```http
POST {SUPABASE_URL}/rest/v1/rpc/my_profile
apikey: {PUBLISHABLE_KEY}
Authorization: Bearer {access_token}
Content-Type: application/json

{}
```

### Sign-out (best effort)

```http
POST {SUPABASE_URL}/auth/v1/logout
apikey: {PUBLISHABLE_KEY}
Authorization: Bearer {access_token}
Content-Type: application/json

{}
```

Observed success: HTTP `204`. Treat failures as non-fatal for identity classification.

### Keep-alive / health

```http
POST {SUPABASE_URL}/rest/v1/rpc/ping
apikey: {PUBLISHABLE_KEY}
Authorization: Bearer {PUBLISHABLE_KEY}
Content-Type: application/json

{}
```

Executable by anon and authenticated. Returns a timestamptz.

---

## `my_profile` response

JSON object fields (frozen):

| Field | Meaning |
|---|---|
| `staff_id` | Staff UUID (stable) |
| `username` | Normalized username |
| `display_name` | Display name |
| `role` | One of the frozen role strings |
| `active` | boolean |
| `terminated_at` | timestamptz or `null` |
| `business_id` | Business UUID (stable) |
| `shops` | array of `{id, name}` for **active** shops |

Shop visibility:

* **owner** — all active shops of the business (no `staff_shops` rows required)
* **other roles** — only assigned active shops

The live DEV/PROD profile also supplies an additive nullable `shop_timezone` on each shop. The fresh bootstrap preserves that field; null means the client keeps its existing timezone fallback. The frozen required fields above are unchanged.

`updated_at` is **not** part of `my_profile`.

Inactive/terminated profiles are still returned when Auth maps to a staff row (so callers can see DISABLED rather than guessing).

---

## Result classification

| Result | Condition |
|---|---|
| **ALLOWED** | Auth succeeds **and** profile exists **and** `active = true` |
| **DISABLED** | Auth succeeds **and** profile exists **and** `active = false` |
| **INVALID_CREDENTIALS** | Auth explicitly rejects credentials (see below) |
| **NOT_AUTHORIZED** | Auth succeeds **and** `my_profile` returns `null` (Auth user with no staff row) |
| **UNKNOWN** | Network/timeout/DNS/TLS/429/5xx/malformed/unexpected — anything that is not a definitive identity decision |

### INVALID_CREDENTIALS (verified)

* HTTP status: `400`
* Body includes stable `error_code`: `"invalid_credentials"`
* Observed message: `"Invalid login credentials"`

Classify primarily by **`error_code`**, not message text alone.

Phase A does **not** define what Android does with **UNKNOWN**.

---

## Account lifecycle (admin CLI)

Commands: `create`, `list`, `disable`, `enable`, `set-role`, `assign-shop`, `unassign-shop`, `terminate`, `reset-server-credential`, `add-shop`, `export`.

### create

1. Create Auth identity (`{username}@FAKE_DOMAIN` + new 15-digit credential, email confirmed)
2. Insert staff (+ shop assignments)
3. Audit `create`
4. Print credential once

If staff/assignment work fails after Auth create: **compensating cleanup** deletes the Auth user (no orphan Auth). DEV may use `--dev-fail-after-auth` to test this.

Non-owners require at least one shop at create time.

### disable / enable

* `disable` → `active = false` (Auth password remains valid; profile reads as DISABLED)
* `enable` → `active = true`
* `enable` **rejected** if `terminated_at` is set

### reset-server-credential

Uses the Owner-controlled service-key maintenance tier: reserve target → claim once → replace Auth password → atomically audit/finalize. Prints the new 15-digit credential once after known Auth success, even when finalization acknowledgement is pending. Old credential → INVALID_CREDENTIALS after success. `--status` reads pending metadata without an Auth call. No automatic Auth retry; unknown outcomes keep the participant guard until trusted outcome review. Requires the protected-reset migration before this CLI version is used.

### set-role

Changes role within the frozen set. Demoting the last active owner is rejected.

### assign-shop / unassign-shop

Maintain `staff_shops` links; audit accordingly.

### add-shop

Adds an active shop to the business; unique per `(business_id, name)`.

### terminate (permanent in v1)

* `active = false`
* `terminated_at` set
* Auth identity deleted
* `auth_user_id = null`
* staff UUID / row kept
* username remains reserved
* cannot `enable` afterward
* requires `--confirm-username` (retype)
* on PROD also requires `--confirm-project {project_name}`

### Last-active-owner invariant

The business must always retain **at least one** active, non-terminated staff with role `owner`.

Rejected when the target is the last such owner:

* `disable`
* `terminate`
* `set-role` demotion away from `owner`

Inactive or terminated owners do **not** count toward the invariant.

---

## Stable identity guarantees

These UUIDs are stable server identities and must survive export / disaster recovery:

* business UUID
* shop UUID
* staff UUID (`staff_id`)

Future Android linking may depend on `staff_id`. Casual regeneration is a **failure**.

---

## Security model

* Public sign-up **closed** (`disable_signup = true`)
* Anonymous/client direct table access denied (RLS on, **zero** client policies; privileges revoked from `anon` / `authenticated`)
* `ping` allowed for anon + authenticated
* `my_profile` executable only by authenticated; returns only the caller’s staff profile (or null)
* Identity **writes** only via privileged admin context (secret key / CLI)
* Auth user existence alone grants **no** staff authorization (→ NOT_AUTHORIZED)
* Secret/admin key never client-facing

---

## Read-only admin RPCs (C0.0 / Admin Viewer)

**Intended client:** Saaf Milk Admin Viewer (server-side or browser with staff JWT + publishable key).  
**Not** a substitute for the privileged CLI. **No write operations.**

Additive migration: `migration_c0_admin_read.sql` (does not rewrite Phase A `migration.sql`).

### Authorization (every RPC)

1. Caller must be authenticated (`auth.uid()` present)
2. Must map to a `staff` row with:
   * `active = true`
   * `terminated_at IS NULL`
   * `role IN ('owner','admin')`
3. All returned rows are scoped to that staff row’s `business_id`
4. Callers **must not** supply `business_id`; it is derived only from the caller’s staff row
5. UUID lookups for another business return not-found / forbidden (no cross-business leak)

Denied (HTTP error, typically privilege / not authorized): `manager`, `cashier`, `operator`, disabled owner/admin, terminated accounts (no usable Auth), anonymous, Auth-without-staff.

### Schema note: `audit_log.business_id`

Activity isolation required a non-null `business_id` on `audit_log` (FK → `businesses`). Existing rows were backfilled. New CLI audit writes always set `business_id` (from target staff when possible, else the env’s business).

### RPCs

| RPC | Parameters | Returns |
|---|---|---|
| `admin_overview()` | (none) | Counts: `active_staff`, `disabled_staff`, `terminated_staff`, `active_owners`, `active_admins`, `shops`, `active_shops`; plus `recent_activity` (up to 10 events) |
| `admin_list_staff(...)` | `p_search`, `p_role`, `p_status` (`active`/`disabled`/`terminated`/`all`), `p_shop_id`, `p_limit` (default 50, max 100), `p_offset` | Array of staff objects |
| `admin_get_staff(p_staff_id)` | staff UUID | One staff object (same-business only) |
| `admin_list_shops(...)` | `p_limit`, `p_offset` (same caps) | Array of shops + `assigned_staff_count` |
| `admin_get_shop(p_shop_id)` | shop UUID | Shop + `assigned_staff` + non-terminated `owners` |
| `admin_list_activity(...)` | `p_action`, `p_target_username`, `p_from`, `p_to`, `p_limit`, `p_offset` | Array of `{id,at,action,target_username,detail}` |

Staff list/detail fields: `staff_id`, `username`, `display_name`, `role`, `active`, `terminated_at`, `created_at`, `updated_at`, `shops[{id,name}]` (and `business_id` on get). Owners’ `shops` = all **active** shops of the business (same rule as `my_profile`).

### Intentionally excluded from RPC payloads

* `auth_user_id`
* server credentials / password material
* Auth tokens / refresh tokens
* privileged or publishable API keys
* fake Auth email (`{username}@saafmilk.local`)

Presentation/translation of audit `action` + `detail` is the Viewer client's responsibility (raw fields only from these RPCs).

### Pagination

List RPCs clamp `p_limit` to **1..100** and `p_offset` to a non-negative bound. There is no unbounded list endpoint.

---

## Write-administration RPCs and routes (C1 / Admin Viewer)

**Intended client:** Saaf Milk Admin Viewer (Vercel server-side).
**Architecture:** Hybrid — DB-only operations via SECURITY DEFINER write RPCs; Auth-touching operations via Vercel API routes with a scoped admin key.

Additive migration: `migration_c1_admin_write.sql` (does not rewrite Phase A `migration.sql` or C0.0 `migration_c0_admin_read.sql`).

### Schema addition: audit actor attribution

`audit_log` gains two nullable columns:

| Column | Type | Purpose |
|---|---|---|
| `performed_by` | `uuid REFERENCES staff(id)` | Staff ID of the actor (NULL for legacy/CLI rows) |
| `performed_by_username` | `text` | Denormalized username of the actor |

All C1 write operations populate both columns. Existing CLI writes remain `performed_by = NULL`.

### Authorization (write operations)

Same base requirement as read RPCs (active, non-terminated, owner or admin in same business), plus **per-operation role restrictions**:

| Operation | Owner | Admin | Rationale |
|---|---|---|---|
| Disable staff | ✅ | ✅ | Routine |
| Enable staff | ✅ | ✅ | Routine |
| Set role (to manager/cashier/operator) | ✅ | ✅ | Routine |
| Set role (to owner or admin) | ✅ | ❌ | Privilege escalation |
| Set role (demoting an owner) | ✅ | ❌ | Ownership authority |
| Assign shop | ✅ | ✅ | Routine |
| Unassign shop | ✅ | ✅ | Routine |
| Add shop | ✅ | ✅ | Routine |
| Create staff | ✅ | ✅ | Routine (role restrictions apply to target role) |
| Reset server credential | ✅ linked non-terminated targets, including disabled | Active Manager/Cashier/Operator only | Admin cannot reset Owner/Admin, including self |
| Terminate staff | ✅ | ❌ | Permanent + destructive |

### Write RPCs (SECURITY DEFINER, no privileged key needed)

| RPC | Parameters | Audit Action |
|---|---|---|
| `admin_disable_staff(p_staff_id)` | staff UUID | `disable` |
| `admin_enable_staff(p_staff_id)` | staff UUID | `enable` |
| `admin_set_role(p_staff_id, p_new_role)` | staff UUID, role string | `set_role` |
| `admin_assign_shop(p_staff_id, p_shop_id)` | staff UUID, shop UUID | `assign_shop` |
| `admin_unassign_shop(p_staff_id, p_shop_id)` | staff UUID, shop UUID | `unassign_shop` |
| `admin_add_shop(p_name)` | shop name string | `add_shop` |

### Vercel admin routes (require scoped admin key for Auth Admin API)

| Route | Body | Audit Action |
|---|---|---|
| `POST /api/admin/create-staff` | `{username, display_name, role, shop_ids[]}` | `create` |
| `POST /api/admin/reset-credential` | `{staff_id}` | `reset_server_credential` |
| `POST /api/admin/terminate-staff` | `{staff_id, confirm_username}` | `terminate` |

### Invariants enforced by write RPCs

* **Last-active-owner**: disable, terminate, and demote-from-owner check that at least one active non-terminated owner remains after the operation. Uses `SELECT ... FOR UPDATE` to serialize concurrent checks.
* **Terminated rejection**: disable, enable, set-role, assign-shop, and reset-credential reject terminated accounts.
* **Business isolation**: all operations scoped to caller's `business_id`.
* **Audit no-secrets**: the `admin_write_audit` helper strips banned keys (`password`, `credential`, `secret`, `token`, etc.) from detail JSONB.
* **Compensating cleanup**: create-staff Vercel route deletes the Auth user if the staff DB insert fails.

---

## Export / recovery (identity state)

See `RECOVERY.md` and `recover_identity.py`.

* Export includes non-secret identity + semantic audit history; no credentials, tokens, keys, or `auth_user_id`
* Recovery preserves business/shop/staff UUIDs
* Non-terminated accounts get **new** Auth credentials
* Terminated accounts stay credential-less
* Audit **semantic** history restored; PostgreSQL `audit_log.id` bigint values are not required to match
* `performed_by` / `performed_by_username` are included in export/recovery when present (NULL for legacy rows)

---

## Known v1 limitations

* ~~No admin website~~ → Admin Viewer (C0 read-only, C1 write administration)
* No Android bridge in this project
* No device management
* No dynamic roles / no `add-role`
* Terminated usernames cannot be reused
* Old server credentials are not recoverable after disaster recovery (new ones are issued)
* Free-plan projects pause after ~7 days of inactivity without keep-alive (see `KEEP-ALIVE.md`)
* Free plan has no automatic daily platform backups — rely on identity exports (+ optional `db dump`)
* `audit_log.id` bigint values are not preserved across recovery (semantic events are)
* `performed_by` is NULL for all pre-C1 audit rows (CLI and legacy)
* Admin Viewer write operations require `ACCOUNT_SERVER_ADMIN_KEY` for Auth-touching operations only

---

## Verification anchors (implementation)

* DEV automated: `test_contract.py` (includes Group 19 admin read RPCs, Group 20+ write RPCs)
* PROD: smoke only (never full destructive suite)
* Keep-alive: `.github/workflows/keepalive.yml` (publishable key only)
* C0.0 additive SQL: `migration_c0_admin_read.sql`
* C1.0 additive SQL: `migration_c1_admin_write.sql`

## Protected reset integration

Viewer reset uses the authenticated caller reservation, then a server-only single-use claim. The database rechecks policy and blocks access/identity changes to caller and target while unresolved. `POST /api/admin/reset-credential` with `{staff_id, action: "status"}` reads current eligibility and pending reference/state without changing credentials. Cross-business requests remain hidden; no Auth IDs or credentials are returned by status. HTTP 202 can mean unresolved Auth outcome (no credential) or known success with pending completion (one-time credential included); the UI must preserve that distinction. No pending reset expires automatically. Deployment order and trusted recovery guidance are in `PROTECTED_RESET_OPERATIONS.md`.
