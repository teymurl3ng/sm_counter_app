"""Align only the prepared fresh bootstrap with verified live metadata."""
from pathlib import Path
import json
import difflib

TASK = Path(__file__).resolve().parents[3]
SERVER = Path('C:/Users/Teymur/Desktop/saaf-anchor-tmp').resolve()
CATALOG = json.loads((TASK / 'audit/2026-10-09/repairs/evidence/supabase-dev-catalog-2026-10-10.json').read_text())
profile = next(f for f in CATALOG['functions'] if f['name'] == 'my_profile')['definition']
assert 'shop_timezone' in profile and 'SECURITY DEFINER' in profile
patches = []
path = SERVER / 'migration.sql'
before = path.read_text(encoding='utf-8')
old = '  name text not null,\n  active boolean not null default true,'
assert before.count(old) == 1
after = before.replace(old, '  name text not null,\n  shop_timezone text, -- Nullable additive field already present in DEV/PROD.\n  active boolean not null default true,')
import re
pattern = r'(?is)create or replace function public\.my_profile\(\).*?as \$\$.*?\$\$;'
assert len(re.findall(pattern, after)) == 1
after = re.sub(pattern, lambda _: profile.rstrip() + ';', after)
path.write_text(after, encoding='utf-8')
patches.extend(difflib.unified_diff(before.splitlines(True), after.splitlines(True), fromfile='a/migration.sql', tofile='b/migration.sql'))
path = SERVER / 'migration_c1_3_hardening.sql'
before = path.read_text(encoding='utf-8')
old = 'RETURNS jsonb\nLANGUAGE plpgsql\nIMMUTABLE\nAS $$'
assert before.count(old) == 1
after = before.replace(old, "RETURNS jsonb\nLANGUAGE plpgsql\nIMMUTABLE\nSET search_path = ''\nAS $$")
path.write_text(after, encoding='utf-8')
patches.extend(difflib.unified_diff(before.splitlines(True), after.splitlines(True), fromfile='a/migration_c1_3_hardening.sql', tofile='b/migration_c1_3_hardening.sql'))
(TASK / 'audit/2026-10-09/repairs/evidence/supabase-bootstrap-parity-source.patch.txt').write_text(''.join(patches), encoding='utf-8')
print('Prepared source now preserves the live nullable shop timezone/profile field and pins the internal redactor search_path. No live database contacted.')
