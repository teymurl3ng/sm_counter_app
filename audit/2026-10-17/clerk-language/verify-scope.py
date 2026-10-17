"""Compare final source to the recorded baseline and exact copy map."""
from pathlib import Path
import importlib.util, subprocess, difflib, json

OUT = Path(__file__).resolve().parent
ROOT = OUT.parents[2]
spec = importlib.util.spec_from_file_location('copy_map', OUT/'apply-copy.py')
copy = importlib.util.module_from_spec(spec)
spec.loader.exec_module(copy)
BASELINE = '6945500'
display = {'application/owner/CustomerCreditView.kt', 'nativeui/owner/FollowUpWorkspace.kt'}
inventory, exact, deltas = [], [], {}
for relative in copy.FILES:
    path = copy.BASE/relative
    rel = path.relative_to(ROOT).as_posix()
    before = subprocess.check_output(['git','show',f'{BASELINE}:{rel}'],cwd=ROOT).decode('utf-8').replace('\r\n','\n')
    expected = before
    for old,new in copy.COPY.items():
        count = expected.count('"'+old+'"')
        if count:
            inventory.append({'file': rel, 'old': old, 'new': new, 'count': count})
            expected = expected.replace('"'+old+'"', '"'+new+'"')
    actual = path.read_text(encoding='utf-8')
    if relative in display:
        deltas[rel] = ''.join(difflib.unified_diff(expected.splitlines(True), actual.splitlines(True),fromfile='mapped baseline',tofile='final display'))
    else:
        assert expected == actual, f'Unmapped change: {rel}'
        exact.append(rel)
changed = subprocess.check_output(['git','diff','--name-only',BASELINE,'--','phase18/app/src/main'],cwd=ROOT,text=True).splitlines()
assert set(changed) == {str((copy.BASE/f).relative_to(ROOT)).replace('\\','/') for f in copy.FILES}
(OUT/'COPY_MAP.json').write_text(json.dumps({'baseline': BASELINE, 'changes': inventory, 'exactCopyOnlyFiles': exact,
    'displayOnlyDeltasForManualReview': deltas},indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
print(json.dumps({'mappedReplacements':len(inventory),'exactCopyOnlyFiles':len(exact),'displayOnlyFiles':list(deltas)},indent=2))
for rel,delta in deltas.items():
    print(rel+'\n'+delta)
