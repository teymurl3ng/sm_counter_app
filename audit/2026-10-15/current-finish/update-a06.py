from pathlib import Path
import hashlib, json, subprocess, sys

OUT = Path(__file__).resolve().parent
ROOT = OUT.parents[2]
ADB = 'C:/Users/Teymur/AppData/Local/Android/Sdk/platform-tools/adb.exe'
SERIAL = 'R8VXA02SNKH'
PACKAGE = 'com.saaf.milkshop'
manifest = json.loads((ROOT/'audit/2026-10-14/credit-label-fix/PACKAGE_MANIFEST.json').read_text())
apk = ROOT/'audit/2026-10-14/credit-label-fix'/manifest['apk']
assert hashlib.sha256(apk.read_bytes()).hexdigest() == manifest['apkSha256']
for name, digest in manifest['sourceHashes'].items():
    assert hashlib.sha256((ROOT/name).read_bytes()).hexdigest() == digest, name

def adb(*args):
    result = subprocess.run([ADB, '-s', SERIAL, *args], capture_output=True, text=True, encoding='utf-8', errors='replace', timeout=120)
    if result.returncode:
        raise RuntimeError('ADB failed: '+result.stderr[:300])
    return result.stdout

def package_meta():
    lines = adb('shell', 'dumpsys', 'package', PACKAGE).splitlines()
    keys = ('versionCode=', 'versionName=', 'firstInstallTime=', 'lastUpdateTime=', 'userId=')
    return [x.strip() for x in lines if x.strip().startswith(keys)]

before = package_meta()
assert any('firstInstallTime=' in x for x in before), 'Ordinary app absent: stop rather than call fresh install an upgrade'
verify_existing = '--verify-existing' in sys.argv
if not verify_existing:
    (OUT/'a06-before-update.json').write_text(json.dumps(before, indent=2)+'\n')
    installed = adb('install', '-r', '--user', '0', str(apk))
    assert 'Success' in installed
after = package_meta()
assert [x for x in before if x.startswith(('firstInstallTime=', 'userId='))] == [x for x in after if x.startswith(('firstInstallTime=', 'userId='))]
assert any('versionName=0.4.2' in x for x in after)
remote = adb('shell', 'pm', 'path', '--user', '0', PACKAGE).strip().removeprefix('package:')
binary = subprocess.run([ADB, '-s', SERIAL, 'exec-out', 'cat', remote], capture_output=True, timeout=90)
assert binary.returncode == 0
assert hashlib.sha256(binary.stdout).hexdigest() == manifest['apkSha256']
activity = adb('shell', 'cmd', 'package', 'resolve-activity', '--brief', '--user', '0', '-a', 'android.intent.action.MAIN', '-c', 'android.intent.category.LAUNCHER', PACKAGE).strip().splitlines()[-1]
assert activity.startswith(PACKAGE+'/')
launcher = adb('shell', 'am', 'start', '--user', '0', '-n', activity)
record = {'serial': SERIAL, 'package': PACKAGE, 'user': 0, 'operation': 'install -r; no uninstall or data clear',
    'sourceCommit': manifest['sourceCommit'], 'apkSha256': manifest['apkSha256'], 'before': before, 'after': after,
    'installedApkHashMatches': True, 'sameInstallationAndUid': True if not verify_existing else None,
    'verificationOnlyAfterCompletedInstall': verify_existing,
    'launcherOpened': 'Starting:' in launcher or 'brought to the front' in launcher,
    'limitation': 'Installation continuity is verified; record/login preservation requires app-level acceptance.'}
(OUT/'a06-update.json').write_text(json.dumps(record, indent=2)+'\n', encoding='utf-8')
print(json.dumps(record, indent=2))
