from pathlib import Path
import hashlib, json, subprocess, sys
OUT=Path(__file__).resolve().parent
ADB='C:/Users/Teymur/AppData/Local/Android/Sdk/platform-tools/adb.exe'
serial=sys.argv[1] if len(sys.argv)>1 else 'R8VXA02SNKH'
assert serial in ('R8VXA02SNKH','RFCX116CYYX') or serial.startswith('adb-RFCX116CYYX-')
package='com.saaf.milkshop'
manifest=json.loads((OUT/'PACKAGE_MANIFEST.json').read_text())
apk=OUT/manifest['apk']
assert hashlib.sha256(apk.read_bytes()).hexdigest()==manifest['apkSha256']
def adb(*args):
    result=subprocess.run([ADB,'-s',serial,*args],capture_output=True,text=True,encoding='utf-8',errors='replace',timeout=120)
    assert result.returncode==0,'Android operation failed'
    return result.stdout
def metadata():
    lines=adb('shell','dumpsys','package',package).splitlines()
    return [s.strip() for s in lines if s.strip().startswith(('versionCode=','versionName=','firstInstallTime=','lastUpdateTime='))]
before=metadata()
assert any('firstInstallTime=' in s for s in before),'Stop rather than substitute fresh installation for an upgrade'
uid_before=adb('shell','cmd','package','list','packages','-U','--user','0',package).strip()
assert package in uid_before
label='a06' if serial=='R8VXA02SNKH' else 's24'
(OUT/(label+'-before.json')).write_text(json.dumps({'metadata':before,'normalProfile':0},indent=2)+'\n')
assert 'Success' in adb('install','-r','--user','0',str(apk))
after=metadata()
assert [s for s in before if s.startswith('firstInstallTime=')]==[s for s in after if s.startswith('firstInstallTime=')]
assert uid_before==adb('shell','cmd','package','list','packages','-U','--user','0',package).strip()
assert any('versionCode=7' in s for s in after) and any('versionName=0.4.3' in s for s in after)
remote=adb('shell','pm','path','--user','0',package).strip().removeprefix('package:')
binary=subprocess.run([ADB,'-s',serial,'exec-out','cat',remote],capture_output=True,timeout=90)
assert binary.returncode==0 and hashlib.sha256(binary.stdout).hexdigest()==manifest['apkSha256']
activity=adb('shell','cmd','package','resolve-activity','--brief','--user','0','-a','android.intent.action.MAIN','-c','android.intent.category.LAUNCHER',package).strip().splitlines()[-1]
assert activity.startswith(package+'/')
launch=adb('shell','am','start','--user','0','-n',activity)
record={'serial':serial,'normalProfile':0,'package':package,'sourceCommit':manifest['sourceCommit'],
    'apkSha256':manifest['apkSha256'],'versionName':'0.4.3','versionCode':7,'sameUidAndFirstInstallTime':True,
    'dataCleared':False,'before':before,'after':after,'installedBytesMatch':True,'launcherOpened':'Starting:' in launch}
(OUT/(label+'-installed.json')).write_text(json.dumps(record,indent=2)+'\n')
print(json.dumps(record,indent=2))
