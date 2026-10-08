# IDENTITY DECISION MEMO — Track 2 evidence + the decision you owe

Prepared 2026-10-08. Evidence collected from the live PROD database (`pg_get_functiondef`),
the account-server repo, and the Admin Viewer source. This memo exists so the owner makes
ONE informed decision about the identity layer instead of three scattered ones.

## Verified findings (evidence, not opinion)

1. **`staff.active` IS checked on every sync call — disable works.**
   `sync_push` filters `staff.active and terminated_at is null` on every invocation;
   `sync_pull` scopes shops through `sync_staff_shop_ids()`, which applies the same filter.
   A disabled staff row loses sync access on its very next call. No gap here.

2. **Admin-RPC exposure from sync tokens: CONFIRMED.** The admin write RPCs
   (create/disable/set-role/assign-shop/terminate/add-shop) authorize via
   `admin_require_write_caller()` — active OWNER/ADMIN staff row matching `auth.uid()`.
   The counter tablet holds the OWNER's sync session, so from the server's perspective it
   IS the owner: it can create/disable/terminate staff accounts. Mitigations, in order of
   cost:
   - **Accept for the pilot** (2 devices, counter physically inside the shop) — documented risk.
   - **Enroll the counter with a non-owner server account** once non-owner enrollment exists
     (needs the enrollment UI extended — bundles with staff linking).
   - **`devices` table + device-scoped sync credentials**: server issues each device its own
     credential; `sync_push`/`sync_pull` check a device allowlist; admin RPCs stay
     owner-credential-only. The correct long-term fix; medium build.

3. **The sync RPC layer is now anchored** — `sync-rpc-anchor/` in the account-server repo
   holds the deployed definitions of `sync_push`, `sync_pull`, `sync_staff_shop_ids`,
   `sync_normalize_payload`, extracted from PROD 2026-10-08. The layer was previously
   undocked (existed in no repository — a real infrastructure risk). Any future change:
   edit the anchor SQL, apply to BOTH projects, re-extract to verify.

## The decision you owe (choose one)

**Option A — Accept + document (recommended for the pilot).**
Keep owner-credential-on-both-devices through the pilot week. Risks: a stolen counter
tablet can invoke admin writes until you terminate/disable the account from the Viewer
(which works). Cost: zero. Revisit: 3+ devices, or when the shop tablet replaces the A06.

**Option B — Devices table + device-scoped credentials (pre-pilot or right after).**
Server issues per-device sync credentials; revocation becomes per-device ("lost the counter
tablet" → disable THAT device, owner phone unaffected); admin writes stay exclusive to the
owner credential. Cost: ~1 session (server SQL + enrollment change + the app already sends
device_id). This bundles naturally with staff linking (below) — one identity migration.

**Option C — Staff linking now (per-clerk server identities).**
Each clerk proves themselves once online at activation; actions attributable by name
server-side; by-person revocation. Cost: enrollment UI extension + C1 admin gate +
SUSPENDED/REVOKED enforcement shipping first (Track 1 A1' currently records these
display-only). Sequence AFTER the pilot unless a cashier problem forces it earlier.

**Recommended:** Option A for the pilot, then Option B + C together as one identity
migration (they share the devices/staff schema work).

**TABLET UPDATE (2026-10-08, after this memo was drafted):** the shop tablet is planned as
CASHIER-ONLY and SHARED — everyone records daily entries on it. That changes the calculus:
the tablet must NOT hold an owner-level sync credential (a shared, publicly-accessible
device carrying owner sync powers is the exact risk §2 describes). The tablet therefore
needs the Option B/C mechanism (cashier-scoped sync credential or device-scoped key) before
it ships — or it stays off-sync until that tier exists. The owner's S24 can remain
owner-scoped (it stays with the owner).

## Standing items from this pass

- **Identity DR**: staff/roles/shop bindings/Auth users exist only in Supabase (free tier =
  no automatic backups). Options: paid plan daily backups, or a scheduled dump (needs a DB
  password secret in CI). The LEDGER data is safe by construction (every device holds full
  history) — it's the server identity layer that needs the dump.
- **Admin Viewer auth**: mutations require a session cookie + fresh `my_profile` re-check
  (owner/admin, active, non-terminated); the service key lives only in Vercel server-side
  env. Remaining verification: cookie flags (httpOnly/secure) and session lifetime in the
  Viewer repo.
- **Keep-alive**: the GitHub scheduled workflow auto-disables after 60 days of repo
  inactivity. The account-server repo received commits 2026-10-08 (window reset). Long-term:
  external cron or a calendar reminder every ~45 days.
