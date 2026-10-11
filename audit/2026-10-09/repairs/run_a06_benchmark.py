"""Install/run only verified QA benchmark APK identities; preserve native artifacts."""
from pathlib import Path
import hashlib
import json
import subprocess
import time
import sys

ROOT=Path(__file__).resolve().parents[3]
BASE=Path(__file__).resolve().parent
OUT=BASE/'evidence'
ADB='C:/Users/Teymur/AppData/Local/Android/Sdk/platform-tools/adb.exe'
AAPT='C:/Users/Teymur/AppData/Local/Android/Sdk/build-tools/36.0.0/aapt.exe'
SERIAL='R8VXA02SNKH'
TARGET='com.saaf.milkshop.qa.benchmark'
RUNNER=TARGET+'.test/com.saaf.milkshop.SaafMilkTestRunner'
app=ROOT/'phase18/app/build/outputs/apk/benchmark/app-benchmark.apk'
tests=ROOT/'phase18/app/build/outputs/apk/androidTest/benchmark/app-benchmark-androidTest.apk'

def run(args,timeout=60):
    return subprocess.run(args,capture_output=True,timeout=timeout,creationflags=subprocess.CREATE_NO_WINDOW)
def adb(*args,timeout=60):
    r=run([ADB,'-s',SERIAL,*args],timeout)
    assert r.returncode==0,r.stderr.decode(errors='replace')+r.stdout.decode(errors='replace')
    return r.stdout.decode(errors='replace')

manifest={}
for label,path,package in [('app',app,TARGET),('tests',tests,TARGET+'.test')]:
    assert path.is_file(),path
    badging=run([AAPT,'dump','badging',str(path)])
    assert badging.returncode==0
    text=badging.stdout.decode(errors='replace')
    assert f"package: name='{package}'" in text,text[:1000]
    xml=run([AAPT,'dump','xmltree',str(path),'AndroidManifest.xml'])
    assert xml.returncode==0
    (OUT/f'a06-benchmark-{label}-manifest.txt').write_bytes(xml.stdout)
    manifest[label]={'path':str(path),'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'verified_package':package}
    if label=='app':
        assert 'E: profileable' in xml.stdout.decode() and 'android:shell' in xml.stdout.decode()
    else:
        assert TARGET in xml.stdout.decode() and 'SaafMilkTestRunner' in xml.stdout.decode()
manifest['requested_compilation']='speed for both QA packages' if '--full-compilation' in sys.argv else 'normal installation/JIT'
(OUT/'a06-benchmark-apk-provenance.json').write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf-8')
print('Verified isolated profileable APK identities; installing A06 QA benchmark only.',flush=True)
for label,path in [('app',app),('tests',tests)]:
    (OUT/f'a06-benchmark-install-{label}.txt').write_text(adb('install','-r','-t',str(path)),encoding='utf-8')
if '--full-compilation' in sys.argv:
    for package in [TARGET,TARGET+'.test']:
        result=adb('shell','cmd','package','compile','-m','speed','-f',package,timeout=120)
        assert 'Success' in result,result
        (OUT/('a06-benchmark-full-compile-'+('app' if package==TARGET else 'tests')+'.txt')).write_text(result,encoding='utf-8')

def instrument(method,label):
    print('Running benchmark '+method,flush=True)
    r=run([ADB,'-s',SERIAL,'shell','am','instrument','-w','-r','-e','saafSkipBootstrap','true','-e','class',
           'com.saaf.milkshop.NativeReleaseProfileProbe#'+method,RUNNER],timeout=480)
    text=(r.stdout+r.stderr).decode(errors='replace')
    (OUT/f'a06-benchmark-{label}-run.txt').write_text(text,encoding='utf-8')
    assert r.returncode==0 and 'OK (1 test)' in text and 'FAILURES' not in text,text[-4500:]

def collect(name):
    remote=f'/sdcard/Android/data/{TARGET}/files/audit_release_profile/{name}'
    destination=OUT/('a06-benchmark-'+name)
    adb('pull',remote,str(destination))
    if name.endswith('.json'):
        value=json.loads(destination.read_text(encoding='utf-8'))
        assert value['build']=='benchmark' and value['device']=='SM-A065F'
        summary={key:({k:v for k,v in val.items() if k!='values_ms'} if isinstance(val,dict) else val)
                 for key,val in value.items() if key!='deadline_ms'}
        print(json.dumps(summary),flush=True)
    else:
        assert destination.read_bytes().startswith(b'\x89PNG\r\n\x1a\n')

if '--motion-only' not in sys.argv:
    # This package contains only the synthetic benchmark fixtures; preserve host artifacts first.
    adb('shell','pm','clear',TARGET)
    instrument('profilePersistentMixedCostingDataset','data')
    collect('native-data-profile.json')
    if '--data-only' in sys.argv:
        print('A06_BENCHMARK_DATA_COLLECTION: PASS. No ordinary app or live DB changed.',flush=True)
        raise SystemExit(0)
remote_config='/data/misc/perfetto-configs/saaf-audit-benchmark.pbtxt'
remote_trace='/data/misc/perfetto-traces/saaf-audit-benchmark.perfetto-trace'
adb('push',str(BASE/'a06-benchmark-trace.pbtxt'),remote_config)
started=time.monotonic()
start=adb('shell','perfetto','--background-wait','--txt','-c',remote_config,'-o',remote_trace)
(OUT/'a06-benchmark-trace-start.txt').write_text(start,encoding='utf-8')
instrument('profileRealTimeNativeTransitionFramesAndGuardOwnership','motion')
for name in ['native-motion-profile.json','motion-before.png','motion-mid.png','motion-after.png']: collect(name)
remaining=47-(time.monotonic()-started)
if remaining>0:
    print('Waiting for bounded 45-second trace to flush.',flush=True)
    time.sleep(min(remaining,47))
adb('pull',remote_trace,str(OUT/'a06-benchmark-motion.perfetto-trace'))
assert (OUT/'a06-benchmark-motion.perfetto-trace').stat().st_size>10000
print('A06_BENCHMARK_COLLECTION: PASS. No ordinary app or live DB changed.',flush=True)
