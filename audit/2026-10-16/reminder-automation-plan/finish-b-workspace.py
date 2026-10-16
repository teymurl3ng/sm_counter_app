"""Close only the accepted B workspace/save checkpoint; no device or cloud action."""
from pathlib import Path
import json,hashlib
OUT=Path(__file__).resolve().parent
ROOT=OUT.parents[2]
latest={}
for label in ['b-workspace-first','b-workspace-visual','b-workspace-draft']:
    data=json.loads((OUT/(label+'-results.json')).read_text())
    for t in data['tests']: latest[(t['class'],t['test'])]={**t,'run':label}
assert len(latest)==18 and all(t['code']==0 for t in latest.values())
captures=[{'file':p.name,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()} for p in sorted((OUT/'b-workspace-captures').glob('*.png'))]
evidence={'baseline':'eac65d4','uniqueAffectedMethods':18,'latestResults':list(latest.values()),
 'device':'A06','target':'com.saaf.milkshop.qa.tests','ordinaryAppModified':False,'cloudModified':False,
 'transport':'Injected around actual client, Room reader, session wrapper and request journal',
 'ordinaryRouteEnabled':False,'physicalConcurrencyEstablished':False,'localSyntheticCaptures':captures,
 'visualReview':['primary-confirmation-dark-2x.png','list-1.0-false.png','settings-2.0-true.png'],
 'initialVisualBlocker':'Confirmed sleeping keyguard; owner privately unlocked before eight passing UI reruns'}
(OUT/'B_WORKSPACE_VERIFICATION.json').write_text(json.dumps(evidence,indent=2)+'\n')

def replace(path,old,new):
    p=ROOT/path;s=p.read_text(encoding='utf-8');assert old in s,path
    p.write_text(s.replace(old,new,1),encoding='utf-8')

replace('NEXT_WORK_PLAN_2026-10-15.md',
 'B UI/saves, quiet scheduling/shared handoff wiring, actual two-phone acceptance and exact PROD/delivery remain,',
 '✅ ~~B connected workspace/save/recovery checkpoint~~ (18 unique passing affected methods); quiet alerts/persistent entry attention, shared handoff and ordinary navigation/return wiring, remaining activity/restore presentation, actual two-phone acceptance and exact PROD/delivery remain,')
replace('audit/2026-10-16/reminder-automation-plan/PLAN.md',
 'B current-session UI/saves, quiet scheduling, shared handoff gating, actual two-phone acceptance and exact PROD/delivery remain pending,',
 '✅ ~~B connected workspace/save/recovery checkpoint~~ passes18 unique affected methods; [result](B_WORKSPACE_RESULT.md). Quiet alerts/persistent entry attention, shared handoff gating and ordinary navigation/return wiring, remaining activity/restore presentation, actual two-phone acceptance and exact PROD/delivery remain pending,')
replace('LATER_FEATURE_PLAN_2026-10-15.md',
 'B current-session UI/saves, quiet scheduling/shared handoff wiring, actual two-phone acceptance, exact PROD/delivery and the explicit C SMS route/activation decision remain.',
 '✅ ~~B connected workspace/save/recovery checkpoint~~ passes18 unique affected methods; [result](audit/2026-10-16/reminder-automation-plan/B_WORKSPACE_RESULT.md). Quiet alerts/persistent entry attention, shared handoff and ordinary navigation/return wiring, remaining activity/restore presentation, actual two-phone acceptance, exact PROD/delivery and the explicit C SMS route/activation decision remain.')
replace('LATER_FEATURE_PLAN_2026-10-15.md',
 'A and B DEV/data checkpoints are complete; B UI/scheduling/handoff/acceptance and C decision remain.',
 'A and B DEV/data/workspace-save checkpoints are complete; B scheduling/handoff/navigation/acceptance and C decision remain.')
replace('audit/2026-10-16/reminder-automation-plan/B_RESULT.md',
 'Wire current-session screens, explicit Owner enable/primary controls, persistent in-app attention, quiet WorkManager detection and the shared gate on every supported coordinated reminder entry.',
 '✅ ~~Connected current-session workspace, Owner policy/primary confirmation and metadata-save recovery~~ are accepted at [the workspace checkpoint](B_WORKSPACE_RESULT.md), with18 unique affected methods passing. Ordinary navigation stays closed. Persistent entry attention, quiet WorkManager detection, shared activity/restore presentation, ordinary customer/statement return wiring and the shared gate on every supported coordinated reminder entry remain.')
p=ROOT/'CURRENT_BASELINE.md';s=p.read_text(encoding='utf-8');title,rest=s.split('\n',1)
p.write_text(title+'\n\n**16 October — phase6 B connected workspace/save checkpoint:** ✅ ~~Current-session adapter, Owner policy/primary confirmation, shared customer saves and explicit unfinished-save recovery~~ accepted with18 unique passing affected methods across isolated A06 runs. [Result](audit/2026-10-16/reminder-automation-plan/B_WORKSPACE_RESULT.md). The first visual run was blocked by a sleeping keyguard; the owner privately unlocked it and all eight visual reruns pass, including dark200% dialogs and fixed chrome. No ordinary route/install, cloud mutation, worker/notification or customer contact. Existing B quiet alerts/persistent entry attention, shared handoff/ordinary navigation and remaining activity/restore presentation, DEV/two-phone acceptance and exact PROD/delivery remain. C SMS decision, phase1 human custody and deferred wording keep their boundaries. Earlier entries below are historical.\n'+rest,encoding='utf-8')
replace('audit/2026-10-16/reminder-automation-plan/B_WORKSPACE_SCOPE.md',
 'use direct read fallback. No Supabase feature/schema change is planned.',
 'direct read fallback succeeded; reviewed the upcoming CBC TLS cipher removal, with no TLS override added. No Supabase feature/schema change is planned.')

files=['CURRENT_BASELINE.md','NEXT_WORK_PLAN_2026-10-15.md','LATER_FEATURE_PLAN_2026-10-15.md',
 'phase18/app/src/main/java/com/saaf/milkshop/sync/SyncRuntime.kt',
 'phase18/app/src/main/java/com/saaf/milkshop/sync/FollowUpSessionAccess.kt',
 'phase18/app/src/main/java/com/saaf/milkshop/nativeui/owner/FollowUpWorkspace.kt',
 'phase18/app/src/main/java/com/saaf/milkshop/nativeui/owner/FollowUpWorkspaceBinding.kt',
 'phase18/app/src/androidTest/java/com/saaf/milkshop/FollowUpWorkspaceUiTest.kt',
 'phase18/app/src/androidTest/java/com/saaf/milkshop/FollowUpWorkspaceBindingTest.kt']
prefix=OUT.relative_to(ROOT).as_posix()+'/'
files += [prefix+n for n in ['PLAN.md','B_RESULT.md','B_WORKSPACE_SCOPE.md','B_WORKSPACE_RESULT.md',
 'B_WORKSPACE_VERIFICATION.json','b-workspace-hook.json','b-workspace-finish-hook.json',
 'b-workspace-first-results.json','b-workspace-visual-results.json','b-workspace-draft-results.json','b-workspace-selection.json',
 'run-b-workspace.py','finish-b-workspace.py']]
manifest={'files':[{'path':f,'sha256':hashlib.sha256((ROOT/f).read_bytes()).hexdigest()} for f in files],
 'excludes':'Raw device/console logs, screenshots, APKs and historical untracked artifacts'}
(OUT/'B_WORKSPACE_FILES.json').write_text(json.dumps(manifest,indent=2)+'\n')
print(json.dumps({'latestPassingMethods':len(latest),'exactCheckpointFiles':len(files)+1,'captureHashes':len(captures)}))
