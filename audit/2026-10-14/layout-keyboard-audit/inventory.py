from pathlib import Path
import re, json, hashlib

root = Path(__file__).resolve().parents[3]
base = root / 'phase18/app/src/main/java/com/saaf/milkshop/nativeui'
out = Path(__file__).resolve().parent
patterns = {
    'fields': r'\b(?:OutlinedTextField|BasicTextField|TextField)\s*\(',
    'ime': r'\.(?:imePadding|imePaddingExcludingNavBar|imeNestedScroll)\s*\(',
    'scroll': r'\b(?:verticalScroll|LazyColumn|LazyRow)\s*\(',
    'consumption': r'\bconsumeWindowInsets\s*\(',
    'header': r'\b(?:CashierScreenHeader|NativeChromeBand)\s*\(',
    'buttons': r'\b(?:TextButton|Button|OutlinedButton|FilledTonalButton)\s*\(',
    'dialogs': r'\b(?:AlertDialog|Dialog|ModalBottomSheet)\s*\(',
}
rows = []
for path in sorted(base.rglob('*.kt')):
    text = path.read_text(encoding='utf-8-sig')
    code = re.sub(r'/\*.*?\*/|//[^\n]*', lambda m: '\n' * m.group().count('\n'), text, flags=re.S)
    found = {k: [code.count('\n', 0, m.start())+1 for m in re.finditer(v, code)] for k,v in patterns.items()}
    if not any(found.values()):
        continue
    funcs = [(m.start(), m.group(1)) for m in re.finditer(r'\bfun\s+(\w+)\s*\(', code)]
    fields = []
    for match in re.finditer(patterns['fields'], code):
        previous = [name for pos,name in funcs if pos < match.start()]
        fields.append({'line': code.count('\n',0,match.start())+1, 'function': previous[-1] if previous else None})
    rows.append({'path': path.relative_to(root).as_posix(), 'sha256': hashlib.sha256(path.read_bytes()).hexdigest(), **found, 'field_functions': fields})
(out/'INVENTORY.json').write_text(json.dumps({'scope': 'native launcher production Kotlin; call-site inventory, not runtime proof', 'files':rows}, indent=2)+'\n', encoding='utf-8')
print('FILES WITH FIELDS (line lists are call sites, not unique runtime screens):')
for row in rows:
    if row['fields']:
        print(f"{Path(row['path']).name}: fields={len(row['fields'])}; IME={row['ime']}; scroll={row['scroll']}; consume={row['consumption']}; funcs={','.join(dict.fromkeys(f['function'] or '?' for f in row['field_functions']))}")
print('TOTAL:',sum(len(r['fields']) for r in rows),'field call sites in',sum(bool(r['fields']) for r in rows),'files')
