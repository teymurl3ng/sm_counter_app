from pathlib import Path
import subprocess, os

folder=Path(__file__).resolve().parent
root=folder.parents[2]
env=dict(os.environ,JAVA_HOME='C:/Program Files/Android/Android Studio/jbr')
env['PATH']=env['JAVA_HOME']+'/bin;'+env['PATH']
label=__import__('sys').argv[1]
with (folder/f'{label}-build.txt').open('w',encoding='utf-8') as log:
    result=subprocess.run([str(root/'phase18/gradlew.bat'),':app:assembleDebug',':app:assembleDebugAndroidTest',
        '-Psaaf.instrumentationSandbox=true','--console=plain'],cwd=root/'phase18',env=env,stdout=log,stderr=subprocess.STDOUT)
print(label+' scoped QA build exit '+str(result.returncode))
raise SystemExit(result.returncode)
