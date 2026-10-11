"""Build a reviewable patch; never contacts or writes the server repository."""
from pathlib import Path
root = Path('C:/Users/Teymur/Desktop/saaf-anchor-tmp')
patch = ['*** Begin Patch']
def add(name, value):
    patch.append('*** Add File: ' + (root / name).as_posix())
    patch.extend('+' + line for line in value.splitlines())
def replace(name, old, new):
    text = (root / name).read_text(encoding='utf-8')
    assert old in text, name
    patch.extend(['*** Update File: ' + (root / name).as_posix(), '@@'])
    patch.extend('-' + line for line in old.splitlines())
    patch.extend('+' + line for line in new.splitlines())

source = (root / 'migration_c1_4_reconciliation.sql').read_text()
record = source[source.index('CREATE OR REPLACE FUNCTION public.admin_record_auth_reconciliation('):source.index('CREATE OR REPLACE FUNCTION public.admin_list_auth_reconciliation(')]
record = record.replace("('create_cleanup', 'terminate_cleanup', 'reset_audit')", "('create_cleanup', 'create_verify', 'terminate_cleanup', 'reset_audit')")
migration = '''-- C1.6: preserve ambiguous account creation; protect the final owner in every write path.
-- Apply after C1.5. No Auth identity deletion is authorized by this migration.
BEGIN;
''' + record + '''
CREATE OR REPLACE FUNCTION public.admin_verify_created_identity(p_reconciliation_id uuid)
RETURNS boolean LANGUAGE plpgsql SECURITY DEFINER SET search_path = '' AS $$
DECLARE pending record;
BEGIN
  IF auth.role() IS DISTINCT FROM 'service_role' THEN
    RAISE EXCEPTION 'service role required' USING errcode = '42501';
  END IF;
  SELECT * INTO pending FROM public.auth_reconciliation
  WHERE id = p_reconciliation_id AND operation IN ('create_verify', 'create_cleanup')
    AND state = 'pending' FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'reconciliation record not found' USING errcode = 'P0002'; END IF;
  IF EXISTS (SELECT 1 FROM public.staff
    WHERE auth_user_id = pending.auth_user_id AND business_id = pending.business_id
      AND username = pending.target_username) THEN
    UPDATE public.auth_reconciliation SET state = 'resolved', resolved = true,
      error_code = NULL, last_attempt_at = now(), resolved_at = now(), updated_at = now(),
      attempt_count = attempt_count + 1 WHERE id = pending.id;
    RETURN true;
  END IF;
  UPDATE public.auth_reconciliation SET last_attempt_at = now(), updated_at = now(),
    attempt_count = attempt_count + 1 WHERE id = pending.id;
  RETURN false;
END; $$;
REVOKE ALL ON FUNCTION public.admin_verify_created_identity(uuid) FROM PUBLIC, anon, authenticated;
GRANT EXECUTE ON FUNCTION public.admin_verify_created_identity(uuid) TO service_role;

CREATE OR REPLACE FUNCTION public.protect_last_active_owner()
RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER SET search_path = '' AS $$
BEGIN
  IF OLD.role <> 'owner' OR NOT OLD.active OR OLD.terminated_at IS NOT NULL THEN
    IF TG_OP = 'DELETE' THEN RETURN OLD; ELSE RETURN NEW; END IF;
  END IF;
  IF TG_OP = 'UPDATE' THEN
    IF NEW.business_id = OLD.business_id AND NEW.role = 'owner'
      AND NEW.active AND NEW.terminated_at IS NULL THEN RETURN NEW; END IF;
  END IF;
  -- Version the business row as well as locking it. A competing removal
  -- under REPEATABLE READ must serialize/retry, not count its old snapshot.
  UPDATE public.businesses SET name = name WHERE id = OLD.business_id;
  IF NOT EXISTS (SELECT 1 FROM public.staff WHERE business_id = OLD.business_id
      AND id <> OLD.id AND role = 'owner' AND active AND terminated_at IS NULL) THEN
    RAISE EXCEPTION 'cannot remove the last active owner' USING errcode = 'P0001';
  END IF;
  IF TG_OP = 'DELETE' THEN RETURN OLD; ELSE RETURN NEW; END IF;
END; $$;
REVOKE ALL ON FUNCTION public.protect_last_active_owner() FROM PUBLIC, anon, authenticated;
DROP TRIGGER IF EXISTS staff_last_owner_guard ON public.staff;
CREATE TRIGGER staff_last_owner_guard BEFORE UPDATE OR DELETE ON public.staff
FOR EACH ROW EXECUTE FUNCTION public.protect_last_active_owner();
COMMIT;
'''
add('migration_c1_6_integrity.sql', migration)
add('supabase/migrations/20261010120000_c1_6_integrity.sql', migration)

old = '''    def count_active_owners(self) -> int:
        rows = self.rest_select('''
new = '''    def rest_select_all(self, table: str, query: dict[str, str]) -> list[dict[str, Any]]:
        # A server row cap may be smaller than our requested page. Only an empty
        # page proves exhaustion. Stable primary-key ordering prevents tied pages.
        order = query.get("order", "")
        tie = "staff_id.asc,shop_id.asc" if table == "staff_shops" else "id.asc"
        page_query = {**query, "order": f"{order},{tie}" if order else tie, "limit": "500"}
        result: list[dict[str, Any]] = []
        while True:
            rows = self.rest_select(table, {**page_query, "offset": str(len(result))})
            if not rows:
                return result
            result.extend(rows)

    def count_active_owners(self, business_id: str) -> int:
        rows = self.rest_select_all('''
replace('admin.py', old, new)
replace('admin.py', '''                "role": "eq.owner",
                "active": "eq.true",''', '''                "role": "eq.owner",
                "business_id": f"eq.{business_id}",
                "active": "eq.true",''')
replace('admin.py', '        if self.count_active_owners() <= 1:', '        if self.count_active_owners(str(staff["business_id"])) <= 1:')
text = (root / 'admin.py').read_text()
start, end = text.index('    businesses = client.rest_select(', text.index('def cmd_export')), text.index('    payload = {', text.index('def cmd_export'))
replace('admin.py', text[start:end], text[start:end].replace('client.rest_select(', 'client.rest_select_all('))

add('scripts/identity_snapshot.py', '''"""Paginated identity backup. No implicit credential file loading.
This is a sequential export, not a transactionally consistent database snapshot.
"""
import json
import os
from datetime import datetime, timezone
from pathlib import Path
from urllib.parse import urlencode
from urllib.request import Request, urlopen

TABLES = ("businesses", "shops", "staff", "staff_shops", "audit_log", "auth_reconciliation")

def collect_rest(fetch, table):
    rows, seen = [], set()
    order = "staff_id.asc,shop_id.asc" if table == "staff_shops" else "id.asc"
    while True:
        page = fetch(f"/rest/v1/{table}?" + urlencode({"select": "*", "order": order,
            "limit": 500, "offset": len(rows)}))
        if not isinstance(page, list): raise ValueError("Invalid table response: " + table)
        if not page: return rows
        for row in page:
            identity = (row["staff_id"], row["shop_id"]) if table == "staff_shops" else row["id"]
            if identity in seen: raise ValueError("Duplicate row during export: " + table)
            seen.add(identity)
            rows.append(row)

def collect_auth(fetch):
    rows, seen, page = [], set(), 1
    while True:
        body = fetch(f"/auth/v1/admin/users?per_page=100&page={page}")
        users = body.get("users") if isinstance(body, dict) else None
        if not isinstance(users, list): raise ValueError("Invalid Auth response")
        if not users: return {"users": rows}
        for user in users:
            if user["id"] in seen: raise ValueError("Duplicate Auth user during export")
            seen.add(user["id"])
            rows.append(user)
        page += 1

def main():
    url, key = os.environ["URL"].rstrip("/"), os.environ["KEY"]
    def fetch(path):
        request = Request(url + path, headers={"apikey": key, "Authorization": "Bearer " + key})
        with urlopen(request, timeout=60) as response: return json.load(response)
    started = datetime.now(timezone.utc)
    destination = Path(os.environ["SNAPSHOT_DIR"])
    destination.mkdir(parents=True, exist_ok=False)
    counts = {}
    for table in TABLES:
        data = collect_rest(fetch, table)
        (destination / (table + ".json")).write_text(json.dumps(data, indent=2), encoding="utf-8")
        counts[table] = len(data)
    auth = collect_auth(fetch)
    (destination / "auth_users.json").write_text(json.dumps(auth, indent=2), encoding="utf-8")
    counts["auth_users"] = len(auth["users"])
    (destination / "manifest.json").write_text(json.dumps({"started_at": started.isoformat(),
        "finished_at": datetime.now(timezone.utc).isoformat(), "counts": counts,
        "consistency": "sequential_nontransactional"}, indent=2), encoding="utf-8")
    print("Identity snapshot complete; counts:", counts)

if __name__ == "__main__": main()
''')
old_workflow = (root / '.github/workflows/identity-dr.yml').read_text(encoding='utf-8')
new_workflow = '''name: identity-dr
on:
  schedule:
    - cron: "0 6 * * 1"
  workflow_dispatch: {}
permissions:
  contents: write
concurrency:
  group: identity-dr-backup-branch
  cancel-in-progress: false
jobs:
  dump:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Prepare backup branch
        run: |
          set -euo pipefail
          cp scripts/identity_snapshot.py "$RUNNER_TEMP/identity_snapshot.py"
          git config user.name "identity-dr bot"
          git config user.email "identity-dr@users.noreply.github.com"
          if git ls-remote --exit-code --heads origin identity-backups >/dev/null; then
            git fetch origin identity-backups
            git checkout --detach FETCH_HEAD
          else
            git checkout --orphan identity-backups
            git rm -rf .
          fi
          echo "SNAPSHOT_DIR=$(date -u +%Y%m%dT%H%M%S%N)" >> "$GITHUB_ENV"
      - name: Snapshot identity tables and Auth users
        env:
          URL: ${{ secrets.ACCOUNT_SERVER_URL }}
          KEY: ${{ secrets.ACCOUNT_SERVER_ADMIN_KEY }}
        run: python3 "$RUNNER_TEMP/identity_snapshot.py"
      - name: Commit complete snapshot
        run: |
          set -euo pipefail
          git add -- "$SNAPSHOT_DIR"
          git commit -m "identity snapshot $SNAPSHOT_DIR"
          git push origin HEAD:refs/heads/identity-backups
'''
replace('.github/workflows/identity-dr.yml', old_workflow, new_workflow)
patch.append('*** End Patch')
Path(__file__).with_name('server-integrity.patch.txt').write_text('\n'.join(patch) + '\n', encoding='utf-8')
