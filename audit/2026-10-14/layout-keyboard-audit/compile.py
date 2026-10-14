from pathlib import Path
import os, subprocess

folder = Path(__file__).resolve().parent
root = folder.parents[2]
env = dict(os.environ, JAVA_HOME='C:/Program Files/Android/Android Studio/jbr')
env['PATH'] = env['JAVA_HOME'] + '/bin;' + env['PATH']
with (folder / 'compile-console.txt').open('w', encoding='utf-8') as log:
    result = subprocess.run([str(root/'phase18/gradlew.bat'), ':app:compileDebugKotlin', '--console=plain'],
                            cwd=root/'phase18', env=env, stdout=log, stderr=subprocess.STDOUT)
print('Affected native Kotlin compilation exit:', result.returncode)
raise SystemExit(result.returncode)
