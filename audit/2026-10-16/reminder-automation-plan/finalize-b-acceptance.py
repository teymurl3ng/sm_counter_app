"""Verify the fixed two-phone story and separate real native-Activity route check."""
from pathlib import Path
import hashlib,json
OUT=Path(__file__).resolve().parent
ROOT=OUT.parents[2]
def read(name): return json.loads((OUT/name).read_text(encoding='utf-8'))
def sha(path): return hashlib.sha256(path.read_bytes()).hexdigest()
def write(name,value): (OUT/name).write_text(json.dumps(value,indent=2)+'\n',encoding='utf-8')
first=read('b-acceptance-first-results.json');live=read('b-acceptance-results.json')
assert first['failure'] and live['failure'] is None
for result in [first,live]:
    assert result['project']=='tghzmofkkdsaasvnixps'
    assert len(result['fixtureStaff'])==2
    assert {s['phone'] for s in result['steps']}=={'owner','counter'}
for name in ['b-acceptance-first-cleanup.json','b-acceptance-cleanup.json']:
    c=read(name);assert c['existingRowsUnchanged'] and c['before']==c['after']
    assert not any('needs attention' in s for s in c['cleanup'])
    assert len(c['cleanup'])==5
assert {s['stage'] for s in first['steps']} >= {'verify_schedule','verify_paused','resume'}
assert {s['stage'] for s in live['steps']} >= {'race','release','start_lost','recover','blocked','resolve','open','digest','transfer','cleanup'}
racers=[s for s in live['steps'] if s['stage']=='race']
assert len(racers)==2 and sum(s['raceAccepted'] for s in racers)==1
overlap=next(c for c in live['comparisons'] if c['label']=='overlapping-acquire')
assert overlap['overlap'] and max(v['started'] for v in overlap['httpIntervals'])<min(v['finished'] for v in overlap['httpIntervals'])
lost=next(s for s in live['steps'] if s['stage']=='start_lost')
recovered=next(s for s in live['steps'] if s['stage']=='recover')
assert lost['pid']!=recovered['pid'] and recovered['pending']==0
assert sum(s['launches'] for s in live['steps'])==1
digests=[s['notifyGrant'] for s in live['steps'] if s['stage']=='digest']
assert digests==['NOTIFY','NONE']
for result in [first,live]:
    projections=[c for c in result['comparisons'] if 'tablesEqual' in c]
    assert all(c['tablesEqual']==20 and c['sha256']==projections[0]['sha256'] for c in projections)
ui=read('b-acceptance-route-results.json')
assert ui['expected']==ui['passed']==1 and ui['tests'][0]['code']==0
assert ui['device']=='S24' and not ui['realShopPackageModified']
apk=ROOT/'phase18/app/build/outputs/apk/debug/app-debug.apk'
assert ui['appApkSha256']==sha(apk)
capture=OUT/'b-acceptance-route-captures/native-notification-route.png'
write('B_ACCEPTANCE_VERIFICATION.json',{'date':'2026-10-17','baseline':'ad0743a',
    'initialPassedStages':len(first['steps']),'remainingPassedStages':len(live['steps']),
    'nativeActivityMethodsPassed':1,'productionApkSha256':sha(apk),
    'concurrentRequestEvidence':overlap,'lostStartRecoveredInFreshProcess':True,
    'externalCallbacks':1,'actualExternalAppsOpened':0,'dailyNotifyGrants':digests,
    'captureReviewed':{str(capture.relative_to(OUT)):sha(capture)},
    'initialBoundary':read('b-acceptance-first-boundary.json'),
    'scope':{'twoPhysicalPhones':True,'realDevHttpsAuthRpc':True,'ordinaryAppChanged':False,
        'prodChanged':False,'schemaChanged':False,'realCustomerContact':False,'exactTimeDeliveryProven':False,
        'deviceIdentityAttestationProven':False,'nativeRouteUsesSyntheticLocalIdentity':True}})
paths=['phase18/app/src/androidTest/java/com/saaf/milkshop/PhysicalFollowUpProbe.kt',
    'phase18/app/src/androidTest/java/com/saaf/milkshop/FollowUpDestinationLiveUiTest.kt']
write('B_ACCEPTANCE_FILES.json',{'sha256':{p:sha(ROOT/p) for p in paths},
    'productSource':'No production source changed from ad0743a; only scoped acceptance harnesses added.'})
print('Two physical DEV story, original recovery, daily replay, cleanup and native Activity route evidence verified.')
