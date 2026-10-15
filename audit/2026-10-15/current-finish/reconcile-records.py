from pathlib import Path
import csv, hashlib, io, json, re, subprocess
OUT=Path(__file__).resolve().parent
ROOT=OUT.parents[2]
plan=ROOT/'audit/2026-10-11/remaining-work-plan'
hosts=json.loads((OUT/'hosts-results.json').read_text())
contracts=json.loads((OUT/'contracts-results.json').read_text())
retry=json.loads((OUT/'costing-retry-results.json').read_text())
latest={r['test']:r for batch in (hosts,contracts,retry) for r in batch['tests']}
assert len(latest)==17 and all(r['code']==0 for r in latest.values())
changed=subprocess.check_output(['git','diff','--name-only','305a91d','--','phase18',':!phase18/app/src/androidTest'],cwd=ROOT,text=True).splitlines()
assert not changed,'Production source differs from frozen installed candidate'
manifest=json.loads((ROOT/'audit/2026-10-14/credit-label-fix/PACKAGE_MANIFEST.json').read_text())
freeze={'productionSourceCommit':manifest['sourceCommit'],'productionDiffFromCandidate':changed,
    'package':manifest['package'],'apkSha256':manifest['apkSha256'],'certificateSha256':manifest['signingCertificateSha256'],
    'affectedUniqueChecks':17,'affectedLatestPasses':sorted(latest),
    'testSourceHashes':{str(p.relative_to(ROOT)).replace('\\','/'):hashlib.sha256(p.read_bytes()).hexdigest() for p in [
        ROOT/'phase18/app/src/androidTest/java/com/saaf/milkshop/NativeFoundationUiTest.kt',
        ROOT/'phase18/app/src/androidTest/java/com/saaf/milkshop/NativeOwnerCostingPlansScreenContractTest.kt']},
    'ordinaryPrivateSetupAndRecordsLoginAcceptance':'pending user participation','independentBackupCustody':'pending user confirmation'}
(OUT/'freeze.json').write_text(json.dumps(freeze,indent=2)+'\n')

flat=plan/'FLAT_CHECKLIST.md'
text=flat.read_text(encoding='utf-8')
for n in [22,23,24,25,26,27,28,30,31,32,33,34,35]:
    pattern=rf'^- \[ \] \*\*R{n:02d}\*\* (.*?)(\s*<!--.*?-->)?$'
    text=re.sub(pattern,lambda m:f'- [x] **R{n:02d}** ~~{m[1]}~~'+(m[2] or ''),text,flags=re.M)
banner='**15 October reconciliation:** R22–R28 and R30–R35 are complete at their named technical scope. Hosted recovery, source publication, production browser/reset, both signed phone installations and seventeen affected checks are credited. R29 still needs independent custody/recovered-backup acceptance; R36/R37 need private ordinary-app records/login and real setup; R38 has completed instructions but final private handover remains open. Batch 4 human outcomes remain open. [Current result](../../2026-10-15/current-finish/RESULT.md). Older dated notes below are historical.\n\n'
first=text.index('\n')+1
if '**15 October reconciliation:**' not in text: text=text[:first]+'\n'+banner+text[first:]
old='R22 does not authorize pausing DEV or creating/deleting a project. R25 retains the actual approval-review blocker and must not be retried by bypassing it. Production reset publication is separate from current DEV acceptance. Ordinary app installation has not occurred.'
text=text.replace(old,'Historical pre-acceptance note superseded: the user subsequently authorized the exact hosted-recovery route and the current technical finish. Those tests, publication and installation are now accepted at the scopes above. No permission to broaden platform changes is implied.')
flat.write_text(text,encoding='utf-8')

path=plan/'ITEM_MAP.csv'
rows=list(csv.DictReader(io.StringIO(path.read_text(encoding='utf-8'))))
updates={
    'B4':('PROD_BROWSER_POLICY_SUCCESS_PENDING_ACCEPTED','PROD d2c94e1 actual browser Owner/Admin policy success pending completion accepted; five QA Auth identities retired; original rows/Auth IDs unchanged; current-finish RESULT 2026-10-15','DONE'),
    'O1':('PUBLISHED_HOSTED_SYNTHETIC_27_ACCEPTED','Published c21b983; 27 hosted identity/Auth checks accepted; both QA projects deleted DEV resumed unchanged and Free; hosted-recovery RESULT 2026-10-14','DONE'),
    'O5':('PRODUCTION_D2C94E1_PUBLISHED_RUNTIME_ACCEPTED','Published d2c94e1 production target aliases verified 14 October; actual authenticated PROD runtime/browser reset accepted 15 October','DONE'),
    'A5':('FROZEN_AFFECTED_17_ACCEPTED_PRIVATE_HANDOVER_PENDING','Installed production source305a91d; 17 unique affected checks pass; production tree unchanged; ordinary private records/login and setup remain distinct','COMPLETE_BOUNDED_QA'),
    'A6':('COMMITTED_SIGNED_INSTALLED_BOTH','Published source305a91d and evidence through9b7f24a; exact signed candidate S24 verified14October A06 updated/hash verified15October; no data clear','DONE'),
    'A7':('PROD_RESET_BOUNDED_ACCEPTED_OTHER_ADMIN_CONDITIONAL','Named guarded-reset policy/browser accepted with disposable identities; other administration only if separately used/needed','COMPLETE_BOUNDED_QA'),
    'F1':('FUTURE_CORE_PLANNED_WORDING_DONE','Original T1-T13 blueprint retained; T4 Credit wording completed305a91d; customer search and core credit/reminders remain planned','NOT_STARTED'),
}
for row in rows:
    if row['id'] in updates:
        row['evidence_status'],row['scope_note'],row['execution_status']=updates[row['id']]
with path.open('w',encoding='utf-8',newline='') as f:
    writer=csv.DictWriter(f,fieldnames=list(rows[0]));writer.writeheader();writer.writerows(rows)
print('Reconciled17 accepted affected checks, frozen production source and completed checklist subparts.')
