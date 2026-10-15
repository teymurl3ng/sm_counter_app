from pathlib import Path
import csv, io, json, re
OUT=Path(__file__).resolve().parent
ROOT=OUT.parents[2]
live=json.loads((OUT/'live-acceptance.json').read_text())
assert live['authenticatedOwnerObserved'] and live['liveSync']['push']=='Drained'
assert all(value==0 for value in live['liveSync']['pull'].values())
assert all(value is True for key,value in live['sampledPreservation'].items() if key!='entireDatabaseAuditClaimed')
path=OUT/'ordinary-followup.json'
record=json.loads(path.read_text())
record['postPatch'].update({'privateSignIn':'authenticated Owner observed after user private sign-in',
    'liveSyncAcceptance':'accepted: Push Drained; all pull counters zero; queue empty',
    'historicalRecordComparison':'accepted at sampled balances, three sale summaries and one sale detail scope'})
path.write_text(json.dumps(record,indent=2)+'\n')
plan=ROOT/'audit/2026-10-11/remaining-work-plan'
path=plan/'ITEM_MAP.csv'
rows=list(csv.DictReader(io.StringIO(path.read_text(encoding='utf-8'))))
for row in rows:
    if row['id']=='A5':
        row['evidence_status']='PATCH_AUTH_6_AND_ORDINARY_A06_LIVE_ACCEPTED'
        row['scope_note']='Previous17 presentation checks credited;6 affected auth passes; signed0.4.3 A06 privateOwner livePROD Drained allpull0 and retainedrecord/balance samples accepted; S24/custody/human handover separate'
    elif row['id']=='O3':
        row['evidence_status']='ORDINARY_OWNER_PROD_ACCEPTED_STAFF_CUSTODY_PENDING'
        row['scope_note']='RealA06 shop/Owner confirmed;0.4.3 actualOwner access livePROD sync and retainedrecord samples accepted; actualstaff/human handover and independentcustody remain'
with path.open('w',encoding='utf-8',newline='') as f:
    writer=csv.DictWriter(f,fieldnames=list(rows[0]));writer.writeheader();writer.writerows(rows)
path=plan/'FLAT_CHECKLIST.md'
text=path.read_text(encoding='utf-8')
text=re.sub(r'^- \[ \] \*\*R36\*\* (.*?)(\s*<!--.*?-->)?$',lambda m:'- [x] **R36** ~~'+m[1]+'~~'+(m[2] or ''),text,flags=re.M)
first=text.index('\n')+1
banner='\n**15October post-patch live acceptance:** R36 is complete at the sampled ordinary-app upgrade/login/records scope: privateOwner access on0.4.3, successful livePROD Drained/zero-pull sync, and matching historicalrecords/detail/Overview balance samples. S24 matching installation, humanstaff outcomes and independent custody/private handover remain open. [Live result](../../2026-10-15/prod-timezone-fix/RESULT.md). Earlier R36-pending notes are historical.\n'
if '**15October post-patch live acceptance:**' not in text:text=text[:first]+banner+text[first:]
path.write_text(text,encoding='utf-8')
print('Accepted current A06 ordinary-app sample; remaining human/S24 boundaries retained.')
