from pathlib import Path
import subprocess, os, shutil, json, hashlib, re
OUT=Path(__file__).resolve().parent
ROOT=OUT.parents[2]
env=dict(os.environ,JAVA_HOME='C:/Program Files/Android/Android Studio/jbr')
env['PATH']=env['JAVA_HOME']+'/bin;'+env['PATH']
paths=['phase18/app/src/main/java/com/saaf/milkshop/sync/AccountAuthClient.kt','phase18/app/build.gradle.kts']
assert not subprocess.check_output(['git','status','--porcelain','--',*paths],cwd=ROOT,text=True).strip()
commit=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip()
hashes={p:hashlib.sha256((ROOT/p).read_bytes()).hexdigest() for p in paths}
with (OUT/'release-build-console.txt').open('w',encoding='utf-8') as log:
    result=subprocess.run([str(ROOT/'phase18/gradlew.bat'),':app:assembleRelease','--console=plain'],cwd=ROOT/'phase18',env=env,stdout=log,stderr=subprocess.STDOUT)
assert result.returncode==0,'Signed build failed; inspect local console'
apk=OUT/'Saaf-Milk-0.4.3-prod-timezone-fix.apk'
shutil.copy2(ROOT/'phase18/app/build/outputs/apk/release/app-release.apk',apk)
sdk=Path('C:/Users/Teymur/AppData/Local/Android/Sdk/build-tools/36.0.0')
signature=subprocess.check_output([str(sdk/'apksigner.bat'),'verify','--print-certs',str(apk)],env=env,text=True)
cert=re.search(r'Signer #1 certificate SHA-256 digest: (\w+)',signature).group(1)
assert cert=='9c9e03d7969d79018e2880b79b8d9ec46c43ab50eaf30512ee3fb989bc6abfa1'
badging=subprocess.check_output([str(sdk/'aapt.exe'),'dump','badging',str(apk)],env=env,text=True)
assert "package: name='com.saaf.milkshop' versionCode='7' versionName='0.4.3'" in badging
assert 'application-debuggable' not in badging
config=(ROOT/'phase18/app/build/generated/source/buildConfig/release/com/saaf/milkshop/BuildConfig.java').read_text()
assert re.search(r'ENABLE_NATIVE_PREVIEW_FIXTURES\s*=\s*false',config)
assert re.search(r'NATIVE_PREVIEW_SHARED_PASSPHRASE\s*=\s*""',config)
assert re.search(r'GIT_SHA\s*=\s*"'+re.escape(commit[:7])+r'"',config)
assert all(hashlib.sha256((ROOT/p).read_bytes()).hexdigest()==h for p,h in hashes.items())
record={'package':'com.saaf.milkshop','versionName':'0.4.3','versionCode':7,'sourceCommit':commit,'sourceHashes':hashes,
    'apk':apk.name,'apkSha256':hashlib.sha256(apk.read_bytes()).hexdigest(),'signingCertificateSha256':cert,'debuggable':False,'previewFixtures':False}
(OUT/'PACKAGE_MANIFEST.json').write_text(json.dumps(record,indent=2)+'\n')
print(json.dumps(record,indent=2))
