"""Record the bounded B data result and exact checkpoint files; no device/cloud action."""
from pathlib import Path
import hashlib,json

OUT=Path(__file__).resolve().parent
ROOT=OUT.parents[2]
latest={}
for label in ['b-data-first','b-data-corrected']:
    run=json.loads((OUT/(label+'-results.json')).read_text())
    for item in run['tests']:
        latest[(item['class'],item['test'])]={**item,'run':label}
assert len(latest)==13 and all(t['code']==0 for t in latest.values())
verification={'baseline':'36da8f0','device':'A06','target':'com.saaf.milkshop.qa.tests',
    'uniqueAffectedMethods':13,'latestResults':list(latest.values()),'ordinaryAppChanged':False,
    'cloudChanged':False,'physicalConcurrencyEstablished':False,
    'scope':'B Android data only; no ordinary route, scheduler or external side effect'}
(OUT/'B_ANDROID_DATA_VERIFICATION.json').write_text(json.dumps(verification,indent=2)+'\n')

def replace(path,old,new):
    p=ROOT/path;s=p.read_text(encoding='utf-8')
    assert old in s,path
    p.write_text(s.replace(old,new,1),encoding='utf-8')

replace('audit/2026-10-16/reminder-automation-plan/PLAN.md',
    "B Android/cache/scheduling/handoff wiring and actual two-phone acceptance remain pending, as does C's final pilot/provider decision.",
    "✅ ~~B Android cache/authenticated data/journal/backup checkpoint~~ passes13 unique affected methods; [result](B_ANDROID_DATA_RESULT.md). B current-session UI/saves, quiet scheduling, shared handoff gating, actual two-phone acceptance and exact PROD/delivery remain pending, as does C's final pilot/provider decision.")
replace('NEXT_WORK_PLAN_2026-10-15.md',
    "B app/cache/scheduling/handoff wiring and actual two-phone acceptance remain, then C's final sending-route decision.",
    "✅ ~~B Android data/cache/journal/backup checkpoint~~ (13 unique passing affected methods); B UI/saves, quiet scheduling/shared handoff wiring, actual two-phone acceptance and exact PROD/delivery remain, then C's final sending-route decision. [Data result](audit/2026-10-16/reminder-automation-plan/B_ANDROID_DATA_RESULT.md).")
replace('LATER_FEATURE_PLAN_2026-10-15.md',
    'L4 reminder automation and identity S2 remain unimplemented.',
    'L4 reminder automation remains partially implemented: A and B DEV/data checkpoints are complete; B UI/scheduling/handoff/acceptance and C decision remain. Identity S2 remains deferred/unimplemented.')
replace('LATER_FEATURE_PLAN_2026-10-15.md',
    'B app/cache/scheduling/handoff wiring, actual two-phone acceptance and the explicit C SMS route/activation decision remain.',
    '✅ ~~B Android data/cache/journal/backup checkpoint~~ passes13 unique affected methods; [result](audit/2026-10-16/reminder-automation-plan/B_ANDROID_DATA_RESULT.md). B current-session UI/saves, quiet scheduling/shared handoff wiring, actual two-phone acceptance, exact PROD/delivery and the explicit C SMS route/activation decision remain.')
replace('audit/2026-10-16/reminder-automation-plan/B_RESULT.md',
    'Connect the accepted snapshot to a Room cache/migration and authenticated transport; map schedule revisions into A and retain pending command IDs without replaying external side effects.',
    '✅ ~~Android Room cache/migration, strict authenticated data path, schedule revision mapping, durable request identity and guarded backup recovery~~ are complete at [the data checkpoint](B_ANDROID_DATA_RESULT.md), with13 unique affected methods passing. The remaining paragraphs describe still-open B wiring.')
p=ROOT/'CURRENT_BASELINE.md';s=p.read_text(encoding='utf-8');title,rest=s.split('\n',1)
p.write_text(title+'\n\n**16 October — phase6 B Android data checkpoint:** ✅ ~~Room19 cache/journal, strict authenticated loading and backup schema14 recovery~~ accepted with13 unique passing affected methods across scoped isolated A06 runs. [Result](audit/2026-10-16/reminder-automation-plan/B_ANDROID_DATA_RESULT.md). Existing B UI/saves, quiet scheduling/shared handoff gates, actual two-phone acceptance and exact PROD/delivery remain; C SMS decision and phase1 human delivery/custody remain. No ordinary app install, background activation, cloud mutation or customer contact. Earlier checkpoints below are historical.\n'+rest,encoding='utf-8')

files=[
 'CURRENT_BASELINE.md','NEXT_WORK_PLAN_2026-10-15.md','LATER_FEATURE_PLAN_2026-10-15.md',
 'phase18/app/src/main/java/com/saaf/milkshop/BackupManager.kt',
 'phase18/app/src/main/java/com/saaf/milkshop/BackupRestoreEngine.kt',
 'phase18/app/src/main/java/com/saaf/milkshop/FollowUpBackup.kt',
 'phase18/app/src/main/java/com/saaf/milkshop/data/MilkShopDatabase.kt',
 'phase18/app/src/main/java/com/saaf/milkshop/data/FollowUpData.kt',
 'phase18/app/src/main/java/com/saaf/milkshop/sync/SyncRuntime.kt',
 'phase18/app/src/main/java/com/saaf/milkshop/sync/FollowUpWire.kt',
 'phase18/app/src/main/java/com/saaf/milkshop/sync/FollowUpDataStore.kt',
 'phase18/app/src/androidTest/java/com/saaf/milkshop/FollowUpDataTest.kt',
 'phase18/app/src/androidTest/java/com/saaf/milkshop/CreditMetadataStorageTest.kt',
 'phase18/app/schemas/com.saaf.milkshop.data.MilkShopDatabase/19.json']
prefix=OUT.relative_to(ROOT).as_posix()+'/'
files += [prefix+name for name in ['B_RESULT.md','PLAN.md','B_ANDROID_DATA_SCOPE.md',
 'B_ANDROID_DATA_RESULT.md','B_ANDROID_DATA_VERIFICATION.json','b-android-data-hook.json',
 'b-android-data-finish-hook.json','run-b-data.py','finish-b-data.py','b-data-selection.json',
 'b-data-first-results.json','b-data-corrected-results.json']]
manifest={'files':[{'path':f,'sha256':hashlib.sha256((ROOT/f).read_bytes()).hexdigest()} for f in files],
 'excludes':'Raw console/device logs, captures, APKs and historical untracked artifacts'}
(OUT/'B_ANDROID_DATA_FILES.json').write_text(json.dumps(manifest,indent=2)+'\n')
print(json.dumps({'uniquePassingMethods':len(latest),'checkpointFiles':len(files)+1}))
