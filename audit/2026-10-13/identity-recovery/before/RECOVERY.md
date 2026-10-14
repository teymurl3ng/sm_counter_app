# Identity export & disaster recovery

Governing brief: `saaf-milk-account-server-brief-v3.1.md` (Step 7).

This is a **simple identity-state** export/recovery mechanism — not a full
Supabase project backup product.

---

## Export contents

Command:

```text
python admin.py --env dev|prod export
```

Writes a dated JSON file under `exports/` (git-ignored).

### Included (non-secret)

| Area | Fields |
|---|---|
| Business | `id` (UUID), `name`, `created_at` |
| Shops | `id`, `business_id`, `name`, `active`, `created_at` |
| Staff | `id`, `business_id`, `username`, `display_name`, `role`, `active`, `terminated_at`, `created_at`, `updated_at` |
| Assignments | `staff_id`, `shop_id` |
| Audit history | `at`, `action`, `target_username`, `detail` (semantic history) |
| Meta | `exported_at`, `project_name`, `fake_domain`, recovery notes |

### Excluded (never)

* server credentials / passwords
* password hashes
* `auth_user_id`
* access / refresh tokens
* publishable or secret API keys
* `.env` contents

**Stable IDs are mandatory.** Future Android linking may depend on preserved
`staff_id` / `business_id` / shop IDs.

---

## Credential consequence

> Old server credentials cannot and should not be recovered.
> Recovery generates **new** 15-digit credentials for accounts that should still
> have Auth identities (non-terminated: active or disabled).
> Each new credential is shown **once** on screen at recovery time.

---

## Terminated accounts

On recovery, terminated staff rows are restored with:

* `active = false`
* `terminated_at` set
* `auth_user_id = null`
* **no** Auth identity created
* username remains reserved

---

## Audit history

Export includes audit events. Recovery restores **semantic** history
(`at`, `action`, `target_username`, `detail`).

The PostgreSQL `audit_log.id` bigint values are **not** required to match the
pre-disaster IDs (identity column). That is an accepted v1 limitation; the
events themselves are preserved.

---

## Emergency recovery tool

**Not** an everyday CLI command. Use only after catastrophic identity loss.

```text
python recover_identity.py --env dev --from exports/<file>.json ^
  --confirm-project saafmilk-dev ^
  --i-understand-this-wipes-current-identity-state
```

For a real PROD disaster, substitute `--env prod` and
`--confirm-project saafmilk-prod`. Prefer rehearsing on DEV first.

### What it does

1. Requires matching `--confirm-project` and an explicit wipe acknowledgement
2. Deletes current identity rows and linked Auth users (keeps `roles` catalog)
3. Re-inserts businesses / shops / staff / assignments using **export UUIDs**
4. Restores audit events (new bigint ids)
5. Recreates Auth + **new** credentials for every non-terminated staff row
6. Leaves terminated accounts credential-less

### Plain-English owner steps after total loss

1. Create/repair the Supabase project; apply `migration.sql` if schema is empty
2. Put keys in `.env.dev` or `.env.prod` (never paste secrets into chat)
3. Place the latest off-PC export JSON on this machine
4. Run `recover_identity.py` with the confirmations above
5. Save every printed server credential into the password manager
6. Spot-check: sign in as owner → `my_profile` → `ALLOWED`
7. Tell staff their **new** server credentials (old ones are dead)

---

## Stable IDs

> Business, shop, and staff UUIDs **must** be preserved across recovery.
> A recovery that mints new staff UUIDs is a failure for this product.

Verified on DEV in Step 7 (rehearsal PASS).

---

## Free-tier backups (context)

Official docs (verified Step 7):

* Free plan has **no** automatic daily backups
  (https://supabase.com/docs/guides/platform/backups)
* Keep identity exports **off the PC** after every meaningful change
* Optional: also use `supabase db dump` for full SQL dumps

Platform pause/resume is separate from this identity export (see keep-alive docs).
