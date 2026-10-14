from pathlib import Path
import subprocess, json, hashlib, re

folder = Path(__file__).resolve().parent
adb = 'C:/Users/Teymur/AppData/Local/Android/Sdk/platform-tools/adb.exe'
manifest = json.loads((folder/'PACKAGE_MANIFEST.json').read_text(encoding='utf-8'))
apk = folder/manifest['apk']
assert hashlib.sha256(apk.read_bytes()).hexdigest() == manifest['apkSha256']
connected = subprocess.check_output([adb, 'devices'], text=True).splitlines()
available = {line.split()[0] for line in connected if len(line.split()) == 2 and line.split()[1] == 'device'}
record = {'sourceCommit': manifest['sourceCommit'], 'package': manifest['package'], 'devices': [], 'unavailable': []}
for name, serial in [('A06', 'R8VXA02SNKH'), ('S24', 'RFCX116CYYX')]:
    if serial not in available:
        record['unavailable'].append(name)
        continue
    result = subprocess.run([adb, '-s', serial, 'install', '-r', '--user', '0', str(apk)], capture_output=True, text=True)
    (folder/f'installed-{name}-console.txt').write_text(result.stdout+result.stderr, encoding='utf-8')
    assert result.returncode == 0 and 'Success' in result.stdout, name+': update failed; no data reset attempted'
    path = subprocess.check_output([adb, '-s', serial, 'shell', 'pm', 'path', '--user', '0', manifest['package']], text=True).strip()
    assert path.startswith('package:/data/app/') and path.endswith('/base.apk') and '\n' not in path
    sha = subprocess.check_output([adb, '-s', serial, 'shell', 'sha256sum', path[len('package:'):]], text=True).split()[0]
    assert sha == manifest['apkSha256'], name+': APK mismatch'
    package = subprocess.check_output([adb, '-s', serial, 'shell', 'dumpsys', 'package', manifest['package']], text=True)
    assert re.search(r'versionCode=6\b', package) and re.search(r'versionName=0\.4\.2\b', package)
    launch = subprocess.run([adb, '-s', serial, 'shell', 'am', 'start', '--user', '0', '-n', 'com.saaf.milkshop/.Main'], capture_output=True, text=True)
    assert launch.returncode == 0 and 'Error:' not in launch.stdout+launch.stderr, name+': launcher failed'
    record['devices'].append(dict(name=name, serial=serial, normalProfile=0, install='install -r --user 0', apkSha256=sha, launcherStarted=True))
    (folder/'INSTALLED.json').write_text(json.dumps(record, indent=2)+'\n', encoding='utf-8')
    print(name+': normal app updated, installed hash matched, launcher opened.', flush=True)
assert record['devices'], 'No requested phone connected; nothing installed'
(folder/'INSTALLED.json').write_text(json.dumps(record, indent=2)+'\n', encoding='utf-8')
print('Unavailable: '+', '.join(record['unavailable']), flush=True)
