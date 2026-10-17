"""Replace the ordinary app on the two authorized phones, preserving their installations."""
from pathlib import Path
import hashlib, json, re, subprocess, sys, time

OUT=Path(__file__).resolve().parent
ROOT=OUT.parents[2]
ADB='C:/Users/Teymur/AppData/Local/Android/Sdk/platform-tools/adb.exe'
PACKAGE='com.saaf.milkshop'
manifest=json.loads((OUT/'RELEASE_MANIFEST.json').read_text())
qa=json.loads((OUT/'upgrade-results.json').read_text())
assert qa['expected']==3 and qa['passed']==3 and len(qa['tests'])==3
apk=OUT/manifest['apk']
assert manifest['package']==PACKAGE and manifest['versionCode']==9 and manifest['versionName']=='0.6.0'
assert manifest['certificateSha256']=='9c9e03d7969d79018e2880b79b8d9ec46c43ab50eaf30512ee3fb989bc6abfa1'
assert hashlib.sha256(apk.read_bytes()).hexdigest()==manifest['apkSha256']
assert all(hashlib.sha256((ROOT/p).read_bytes()).hexdigest()==digest for p,digest in manifest['sourceSha256'].items())

def adb(serial,*args):
    r=subprocess.run([ADB,'-s',serial,*args],capture_output=True,text=True,encoding='utf-8',errors='replace',timeout=180)
    assert r.returncode==0, 'ADB failed: '+serial+'/'+args[0]
    return r.stdout

def info(serial):
    raw=adb(serial,'shell','dumpsys','package',PACKAGE)
    return {key:re.findall(pattern,raw) for key,pattern in {
        'versionName':r'versionName=(.*)','versionCode':r'versionCode=(\d+)',
        'uid':r'(?:userId|appId)=(\d+)','firstInstall':r'firstInstallTime=(.*)','lastUpdate':r'lastUpdateTime=(.*)'}.items()}

available=[s.split()[0] for s in subprocess.check_output([ADB,'devices'],text=True).splitlines()[1:] if s.strip().endswith('device')]
targets={'A06':next((s for s in available if s=='R8VXA02SNKH'),None),
         'S24':next((s for s in available if s=='RFCX116CYYX' or s.startswith('adb-RFCX116CYYX-')),None)}
requested=sys.argv[1:] or ['A06','S24']
assert all(name in targets for name in requested)
records=json.loads((OUT/'RELEASE_INSTALLS.json').read_text()) if (OUT/'RELEASE_INSTALLS.json').is_file() else []
for name in requested:
    serial=targets[name]
    assert serial, name+' is not connected'
    assert adb(serial,'shell','getprop','ro.product.model').strip() in ('SM-A065F','SM-A065M','SM-S921W')
    before=info(serial)
    assert before['uid'] and before['firstInstall'], 'Existing ordinary installation required'
    assert before['versionCode'] in (['8'],['9']), 'Unexpected installed version; inspect first'
    existing=next((r for r in records if r['phone']==name),None)
    if existing and existing.get('verified'):
        path=adb(serial,'shell','pm','path','--user','0',PACKAGE).strip().splitlines()[0].removeprefix('package:')
        assert adb(serial,'shell','sha256sum',path).split()[0]==manifest['apkSha256']
        print(name+': already verified; no repeated install',flush=True)
        continue
    record={'phone':name,'serial':serial,'before':before,'operation':'install -r --user 0; no uninstall or data clear','verified':False}
    records=[r for r in records if r['phone']!=name]+[record]
    (OUT/'RELEASE_INSTALLS.json').write_text(json.dumps(records,indent=2)+'\n')
    assert 'Success' in adb(serial,'install','-r','--user','0',str(apk))
    after=info(serial)
    assert before['uid']==after['uid'] and before['firstInstall']==after['firstInstall']
    assert after['versionCode']==['9'] and any(v.strip()=='0.6.0' for v in after['versionName'])
    path=adb(serial,'shell','pm','path','--user','0',PACKAGE).strip().splitlines()[0].removeprefix('package:')
    assert adb(serial,'shell','sha256sum',path).split()[0]==manifest['apkSha256']
    activity=adb(serial,'shell','cmd','package','resolve-activity','--brief','--user','0','-a','android.intent.action.MAIN','-c','android.intent.category.LAUNCHER',PACKAGE).strip().splitlines()[-1]
    assert activity.startswith(PACKAGE+'/')
    adb(serial,'shell','am','start','--user','0','-n',activity)
    time.sleep(3)
    assert adb(serial,'shell','pidof',PACKAGE).strip()
    record.update(after=after,apkHashMatches=True,sameInstallationAndUid=True,processAlive=True,verified=True,
                  privateLoginAndBusinessRecords='Awaiting private sign-in and ordinary-app observation; installation identity alone is not ledger proof')
    (OUT/'RELEASE_INSTALLS.json').write_text(json.dumps(records,indent=2)+'\n')
    print(name+': signed 0.6.0/code9 verified; exact APK hash and existing installation/UID retained; process alive',flush=True)
