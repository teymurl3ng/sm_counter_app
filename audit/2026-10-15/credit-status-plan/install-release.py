"""Upgrade the existing ordinary package on both named phones; never uninstall or clear data."""
from pathlib import Path
import hashlib,json,re,subprocess,time
OUT=Path(__file__).resolve().parent;ROOT=OUT.parents[2]
ADB='C:/Users/Teymur/AppData/Local/Android/Sdk/platform-tools/adb.exe'
PACKAGE='com.saaf.milkshop'
apk=ROOT/'phase18/app/build/outputs/apk/release/app-release.apk'
manifest=json.loads((OUT/'RELEASE_MANIFEST.json').read_text())
assert hashlib.sha256(apk.read_bytes()).hexdigest()==manifest['apkSha256']
for path,digest in manifest['sourceSha256'].items(): assert hashlib.sha256((ROOT/path).read_bytes()).hexdigest()==digest,path
records=[]
def adb(serial,*args):
    r=subprocess.run([ADB,'-s',serial,*args],capture_output=True,text=True,encoding='utf-8',errors='replace',timeout=180)
    assert r.returncode==0,'ADB operation failed: '+serial+'/'+args[0]
    return r.stdout
def info(serial):
    raw=adb(serial,'shell','dumpsys','package',PACKAGE)
    return {key:re.findall(pattern,raw) for key,pattern in {'versionName':r'versionName=(.*)','versionCode':r'versionCode=(\d+)',
        'uid':r'(?:userId|appId)=(\d+)','firstInstall':r'firstInstallTime=(.*)','lastUpdate':r'lastUpdateTime=(.*)'}.items()}
for name,serial in [('A06','R8VXA02SNKH'),('S24','RFCX116CYYX')]:
    before=info(serial)
    assert before['firstInstall'] and before['uid'],'Ordinary app must already exist; no fresh-install claim'
    record=dict(phone=name,serial=serial,before=before,operation='install -r --user 0; no uninstall/data clear')
    records.append(record)
    (OUT/'RELEASE_INSTALLS.json').write_text(json.dumps(records,indent=2)+'\n')
    assert 'Success' in adb(serial,'install','-r','--user','0',str(apk))
    after=info(serial)
    assert before['uid']==after['uid'] and before['firstInstall']==after['firstInstall']
    assert any(v.strip()=='0.5.0' for v in after['versionName']) and '8' in after['versionCode']
    path=adb(serial,'shell','pm','path','--user','0',PACKAGE).strip().splitlines()[0].removeprefix('package:')
    assert adb(serial,'shell','sha256sum',path).split()[0]==manifest['apkSha256']
    activity=adb(serial,'shell','cmd','package','resolve-activity','--brief','--user','0','-a','android.intent.action.MAIN','-c','android.intent.category.LAUNCHER',PACKAGE).strip().splitlines()[-1]
    assert activity.startswith(PACKAGE+'/')
    launch=adb(serial,'shell','am','start','--user','0','-n',activity)
    time.sleep(2)
    assert adb(serial,'shell','pidof',PACKAGE).strip()
    record.update(after=after,apkHashMatches=True,sameInstallationAndUid=True,launched=True,
        limitation='No private sign-in or live business transaction performed; human/private handover remains phase 1.')
    (OUT/'RELEASE_INSTALLS.json').write_text(json.dumps(records,indent=2)+'\n')
    print(name+': signed 0.5.0 installed, APK verified, installation/UID retained, launch alive',flush=True)
