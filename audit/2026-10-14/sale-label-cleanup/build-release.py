from pathlib import Path
import subprocess, os, shutil, json, hashlib, re

folder = Path(__file__).resolve().parent
root = folder.parents[2]
env = dict(os.environ, JAVA_HOME='C:/Program Files/Android/Android Studio/jbr')
env['PATH'] = env['JAVA_HOME'] + '/bin;' + env['PATH']
paths = [
    'phase18/app/src/main/java/com/saaf/milkshop/nativeui/auth/NativeAuthenticationScreens.kt',
    'phase18/app/src/main/java/com/saaf/milkshop/nativeui/common/NativeChrome.kt',
    'phase18/app/src/main/java/com/saaf/milkshop/nativeui/common/NativePersonalSecurityPanel.kt',
    'phase18/app/src/main/java/com/saaf/milkshop/nativeui/sale/CashierSaleScreen.kt',
    'phase18/app/src/main/res/values/strings.xml',
]
assert not subprocess.check_output(['git', 'status', '--porcelain', '--', *paths], cwd=root, text=True).strip()
commit = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=root, text=True).strip()
hashes = {p: hashlib.sha256((root/p).read_bytes()).hexdigest() for p in paths}
with (folder/'release-build.txt').open('w', encoding='utf-8') as log:
    result = subprocess.run([str(root/'phase18/gradlew.bat'), ':app:assembleRelease', '--console=plain'],
        cwd=root/'phase18', env=env, stdout=log, stderr=subprocess.STDOUT)
if result.returncode:
    raise SystemExit(result.returncode)
apk = folder/'Saaf-Milk-0.4.2-ui-cleanup.apk'
shutil.copy2(root/'phase18/app/build/outputs/apk/release/app-release.apk', apk)
sdk = Path('C:/Users/Teymur/AppData/Local/Android/Sdk/build-tools/36.0.0')
signature = subprocess.check_output([str(sdk/'apksigner.bat'), 'verify', '--print-certs', str(apk)], env=env, text=True)
cert = re.search(r'Signer #1 certificate SHA-256 digest: (\w+)', signature).group(1)
assert cert == '9c9e03d7969d79018e2880b79b8d9ec46c43ab50eaf30512ee3fb989bc6abfa1'
badging = subprocess.check_output([str(sdk/'aapt.exe'), 'dump', 'badging', str(apk)], env=env, text=True)
assert "package: name='com.saaf.milkshop' versionCode='6' versionName='0.4.2'" in badging
assert 'application-debuggable' not in badging
config = (root/'phase18/app/build/generated/source/buildConfig/release/com/saaf/milkshop/BuildConfig.java').read_text(encoding='utf-8')
assert re.search(r'ENABLE_NATIVE_PREVIEW_FIXTURES\s*=\s*false', config)
assert re.search(r'NATIVE_PREVIEW_SHARED_PASSPHRASE\s*=\s*""', config)
short_commit = subprocess.check_output(['git', 'rev-parse', '--short', 'HEAD'], cwd=root, text=True).strip()
assert re.search(r'GIT_SHA\s*=\s*"'+re.escape(short_commit)+r'"', config)
assert all(hashlib.sha256((root/p).read_bytes()).hexdigest() == h for p,h in hashes.items())
assert subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=root, text=True).strip() == commit
manifest = dict(package='com.saaf.milkshop', versionCode=6, versionName='0.4.2', sourceCommit=commit,
    sourceHashes=hashes, apk=apk.name, apkSha256=hashlib.sha256(apk.read_bytes()).hexdigest(),
    signingCertificateSha256=cert, debuggable=False, previewFixtures=False,
    scope='Committed recovery/header/Quick PIN setup fixes and sale presentation cleanup; functional tests deferred')
(folder/'PACKAGE_MANIFEST.json').write_text(json.dumps(manifest, indent=2)+'\n', encoding='utf-8')
print('Signed release built; certificate, package, source and APK verified.', flush=True)
