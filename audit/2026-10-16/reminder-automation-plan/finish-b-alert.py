"""Close the bounded alert/count checkpoint, with exact staging paths and no live actions."""
from pathlib import Path
import json,hashlib
OUT=Path(__file__).resolve().parent
ROOT=OUT.parents[2]
labels=['b-alert-first','b-alert-remaining','b-alert-notification','b-alert-ui','b-alert-ui-final']
latest={};hashes=set()
for label in labels:
    run=json.loads((OUT/(label+'-results.json')).read_text());hashes.add(run['appApkSha256'])
    for test in run['tests']: latest[(test['class'],test['test'])]={**test,'run':label}
assert len(latest)==17 and all(t['code']==0 for t in latest.values()) and len(hashes)==1
evidence={'baseline':'1b94d2c','uniqueAffectedMethods':17,'latestResults':list(latest.values()),
 'device':'A06','target':'com.saaf.milkshop.qa.tests','appApkSha256':next(iter(hashes)),
 'ordinaryAppModified':False,'cloudModified':False,'ordinaryActivation':False,
 'physicalPeriodicTimingOrConcurrencyEstablished':False,
 'permissionBranches':'Denied in remaining run; allowed platform post and blocked channel in separate notification run; QA permission revoked afterward',
 'initialProcessExit':{'reason':8,'label':'PERMISSION CHANGE','description':'permissions revoked','source':'device dumpsys activity exit-info for isolated target'},
 'captures':[{'file':p.name,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()} for p in sorted((OUT/'b-alert-captures').glob('*.png'))]}
(OUT/'B_ALERT_VERIFICATION.json').write_text(json.dumps(evidence,indent=2)+'\n')
def replace(path,old,new):
    p=ROOT/path;s=p.read_text(encoding='utf-8');assert old in s,path
    p.write_text(s.replace(old,new,1),encoding='utf-8')

replace('NEXT_WORK_PLAN_2026-10-15.md',
 'quiet alerts/persistent entry attention, shared handoff and ordinary navigation/return wiring,',
 '✅ ~~B quiet-alert engine/scheduler/notifier and persistent-attention checkpoint~~ (17 unique passing affected methods); ordinary lifecycle/permission/destination activation stays closed with shared handoff and ordinary navigation/return wiring,')
replace('audit/2026-10-16/reminder-automation-plan/PLAN.md',
 'Quiet alerts/persistent entry attention, shared handoff gating and ordinary navigation/return wiring,',
 '✅ ~~B quiet-alert engine/scheduler/notifier and persistent-attention checkpoint~~ passes17 unique affected methods; [result](B_ALERT_RESULT.md). Ordinary lifecycle/permission/destination activation stays closed with shared handoff gating and ordinary navigation/return wiring,')
replace('LATER_FEATURE_PLAN_2026-10-15.md',
 'Quiet alerts/persistent entry attention, shared handoff and ordinary navigation/return wiring,',
 '✅ ~~B quiet-alert engine/scheduler/notifier and persistent-attention checkpoint~~ passes17 unique affected methods; [result](audit/2026-10-16/reminder-automation-plan/B_ALERT_RESULT.md). Ordinary lifecycle/permission/destination activation stays closed with shared handoff and ordinary navigation/return wiring,')
replace('LATER_FEATURE_PLAN_2026-10-15.md',
 'A and B DEV/data/workspace-save checkpoints are complete; B scheduling/handoff/navigation/acceptance and C decision remain.',
 'A and B DEV/data/workspace-save/quiet-alert checkpoints are complete; B ordinary lifecycle/handoff/navigation/acceptance and C decision remain.')
replace('audit/2026-10-16/reminder-automation-plan/B_RESULT.md',
 'Persistent entry attention, quiet WorkManager detection, shared activity/restore presentation, ordinary customer/statement return wiring and the shared gate on every supported coordinated reminder entry remain.',
 '✅ ~~Quiet-alert detector, private persistent count, protected attention flow/entry, six-hour scheduler/worker and generic permission-aware notifier~~ are accepted at [the alert checkpoint](B_ALERT_RESULT.md), with17 unique affected methods passing. Ordinary lifecycle/count-invalidation/permission/deep-link activation, shared activity/restore presentation, ordinary customer/statement return wiring and the shared gate on every supported coordinated reminder entry remain.')
p=ROOT/'CURRENT_BASELINE.md';s=p.read_text(encoding='utf-8');title,rest=s.split('\n',1)
p.write_text(title+'\n\n**16 October — phase6 B quiet-alert / persistent-attention checkpoint:** ✅ ~~Fresh server-authorized background detection, daily reservation/recovery suppression, private persistent count, protected attention entry/flow, six-hour network worker and generic permission/channel-aware notifier~~ accepted with17 unique passing affected methods on isolated A06. [Result](audit/2026-10-16/reminder-automation-plan/B_ALERT_RESULT.md). A permission-fixture process exit and UI selector/host setup were corrected with affected reruns only. Production APK hash is identical across all runs. Ordinary activation/lifecycle/permission/deep-link wiring remains closed with the shared message/navigation gate; shared activity/restore presentation, DEV/two-phone acceptance and exact PROD/delivery remain. No ordinary install, cloud change, live-shop enablement or customer contact. C SMS decision, phase1 human custody and deferred wording retain their scope. Earlier entries below are historical.\n'+rest,encoding='utf-8')

files=['CURRENT_BASELINE.md','NEXT_WORK_PLAN_2026-10-15.md','LATER_FEATURE_PLAN_2026-10-15.md',
 'phase18/app/src/main/java/com/saaf/milkshop/application/owner/CustomerFollowUpReader.kt',
 'phase18/app/src/main/java/com/saaf/milkshop/sync/SyncRuntime.kt',
 'phase18/app/src/main/java/com/saaf/milkshop/sync/FollowUpDetector.kt',
 'phase18/app/src/main/java/com/saaf/milkshop/sync/FollowUpAttentionStore.kt',
 'phase18/app/src/main/java/com/saaf/milkshop/sync/FollowUpNotifier.kt',
 'phase18/app/src/main/java/com/saaf/milkshop/sync/FollowUpWorker.kt',
 'phase18/app/src/main/java/com/saaf/milkshop/nativeui/owner/FollowUpAttentionEntry.kt',
 'phase18/app/src/androidTest/java/com/saaf/milkshop/FollowUpAlertTest.kt',
 'phase18/app/src/androidTest/java/com/saaf/milkshop/FollowUpAttentionUiTest.kt']
prefix=OUT.relative_to(ROOT).as_posix()+'/'
files += [prefix+n for n in ['PLAN.md','B_RESULT.md','B_ALERT_SCOPE.md','B_ALERT_RESULT.md','B_ALERT_VERIFICATION.json',
 'b-alert-hook.json','b-alert-finish-hook.json','b-alert-selection.json','run-b-alert.py','finish-b-alert.py']+[label+'-results.json' for label in labels]]
manifest={'files':[{'path':f,'sha256':hashlib.sha256((ROOT/f).read_bytes()).hexdigest()} for f in files],
 'excludes':'Raw logs, device captures, APKs and historical untracked artifacts'}
(OUT/'B_ALERT_FILES.json').write_text(json.dumps(manifest,indent=2)+'\n')
print(json.dumps({'uniquePassingMethods':17,'exactCheckpointFiles':len(files)+1,'productionApkIdenticalAcrossRuns':True}))
