from pathlib import Path
import csv, io
OUT=Path(__file__).resolve().parent
ROOT=OUT.parents[2]
plan=ROOT/'audit/2026-10-11/remaining-work-plan'
path=plan/'ITEM_MAP.csv'
rows=list(csv.DictReader(io.StringIO(path.read_text(encoding='utf-8'))))
updates={
    'A5':('AFFECTED_AUTH_6_PATCH_SIGNED_A06_LIVE_PENDING','Previous17 presentation checks credited; reproduced null-timezone refresh fix passes6 affected auth checks; signed0.4.3 source62c9d31 onA06; post-patch live acceptance awaits private signin','COMPLETE_BOUNDED_QA'),
    'A6':('PATCH_SIGNED_A06_INSTALLED_S24_PENDING','Signed0.4.3/code7 source62c9d31 exact hash verified onA06 with sameUID/firstInstallTime; S24 absent fromADB and oneMDNS check; matching patch pending','IN_PROGRESS'),
    'O3':('OWNER_REAL_SHOP_PROD_CONFIRMED_PATCH_ACCEPTANCE_PENDING','User confirms currentA06 is configured realshop; ordinaryOwner access historicalrecords and verified activePROD observed; post-patch private signin and actualstaff/custody handover remain','IN_PROGRESS'),
}
for row in rows:
    if row['id'] in updates: row['evidence_status'],row['scope_note'],row['execution_status']=updates[row['id']]
with path.open('w',encoding='utf-8',newline='') as f:
    writer=csv.DictWriter(f,fieldnames=list(rows[0]));writer.writeheader();writer.writerows(rows)
path=plan/'FLAT_CHECKLIST.md'
text=path.read_text(encoding='utf-8')
first=text.index('\n')+1
banner='\n**Later15October current patch:** Real ordinaryA06 Owner/PROD setup and historicalrecords are observed. A live null-timezone sync failure is reproduced/fixed with6 affected passes. Signed0.4.3/code7 is installed/hash-verified onA06, dataretained; S24 matching patch is pending connection, so R35 is open for the current patch. R36 remains partial until post-update private signin/live sync/record comparison. [Patch](../../2026-10-15/prod-timezone-fix/RESULT.md). Earlier installation/freeze completion notes refer to0.4.2.\n'
if '**Later15October current patch:**' not in text: text=text[:first]+banner+text[first:]
text=text.replace('- [x] **R35** ~~Install matching accepted builds on the intended shop phones.~~','- [ ] **R35** Install the current0.4.3 patch on intended phones: A06 done; S24 awaits connection.')
path.write_text(text,encoding='utf-8')
print('Current patch status reconciled; no private/live acceptance inferred.')
