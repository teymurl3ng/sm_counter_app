from pathlib import Path
import os, subprocess
OUT=Path(__file__).resolve().parent
ROOT=OUT.parents[2]
env=dict(os.environ,JAVA_HOME='C:/Program Files/Android/Android Studio/jbr')
env['PATH']=env['JAVA_HOME']+'/bin;'+env['PATH']
with (OUT/'qa-build-console.txt').open('w',encoding='utf-8') as log:
    result=subprocess.run([str(ROOT/'phase18/gradlew.bat'),'-Psaaf.instrumentationSandbox=true',':app:assembleDebug',':app:assembleDebugAndroidTest','--console=plain'],cwd=ROOT/'phase18',env=env,stdout=log,stderr=subprocess.STDOUT)
print('Isolated affected-QA build exit '+str(result.returncode))
raise SystemExit(result.returncode)
