from pathlib import Path
import subprocess, json, sys

folder=Path(__file__).resolve().parent
root=folder.parents[2]
adb='C:/Users/Teymur/AppData/Local/Android/Sdk/platform-tools/adb.exe'
serial,label=sys.argv[1:3]
methods=(folder/(sys.argv[3] if len(sys.argv)>3 else 'baseline-selection.txt')).read_text(encoding='utf-8').splitlines()
for path in [root/'phase18/app/build/outputs/apk/debug/app-debug.apk',root/'phase18/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk']:
    result=subprocess.run([adb,'-s',serial,'install','-r','--user','0',str(path)],capture_output=True,text=True)
    (folder/f'{label}-install-{path.stem}.txt').write_text(result.stdout+result.stderr,encoding='utf-8')
    if result.returncode or 'Success' not in result.stdout: raise RuntimeError(result.stdout+result.stderr)
runner='com.saaf.milkshop.qa.tests.test/com.saaf.milkshop.SaafMilkTestRunner'
registered=subprocess.check_output([adb,'-s',serial,'shell','pm','list','instrumentation'],text=True)
assert runner in registered and '(target=com.saaf.milkshop.qa.tests)' in registered
result=subprocess.run([adb,'-s',serial,'shell','am','instrument','--user','0','-w','-r','-e','class',','.join(methods),'-e','saafSkipBootstrap','true',runner],capture_output=True,text=True,encoding='utf-8',errors='replace')
(folder/f'{label}-console.txt').write_text(result.stdout+result.stderr,encoding='utf-8')
state={}; tests=[]
for line in result.stdout.splitlines():
    if line.startswith('INSTRUMENTATION_STATUS: '):
        key,_,value=line[len('INSTRUMENTATION_STATUS: '):].partition('=');state[key]=value
    if line.startswith('INSTRUMENTATION_STATUS_CODE: '):
        code=int(line.split(':',1)[1])
        if code!=1 and 'test' in state: tests.append({'test':state['test'],'code':code,'stack':state.get('stack')})
        state={}
summary={'serial':serial,'methods':methods,'tests':tests,'returncode':result.returncode,'passed':sum(t['code']==0 for t in tests)}
(folder/f'{label}-summary.json').write_text(json.dumps(summary,indent=2)+'\n',encoding='utf-8')
print(json.dumps(summary,indent=2))
if any('#pinOpeningTrace' in m for m in methods):
    dest=folder/f'{label}-evidence';dest.mkdir(exist_ok=True)
    for source,name in [('/sdcard/Android/data/com.saaf.milkshop.qa.tests/files/entry-trace/frames.json','frames.json'),('/sdcard/Download/phase14-native-evidence/native-entry-trace-a06','screens')]:
        pull=subprocess.run([adb,'-s',serial,'pull',source,str(dest/name)],capture_output=True,text=True)
        if pull.returncode: print('Evidence pull failed:',pull.stderr)
if any('#pinSetupKeyboardTrace' in m for m in methods):
    dest=folder/f'{label}-evidence';dest.mkdir(exist_ok=True)
    for source,name in [('/sdcard/Android/data/com.saaf.milkshop.qa.tests/files/security-ime-trace/frames.json','frames.json'),('/sdcard/Download/phase14-native-evidence/native-security-ime-a06','screens')]:
        pull=subprocess.run([adb,'-s',serial,'pull',source,str(dest/name)],capture_output=True,text=True)
        if pull.returncode: print('Evidence pull failed:',pull.stderr)
assert len(tests)==len(methods) and summary['passed']==len(methods) and result.returncode==0, 'Affected checks failed; inspect retained console.'
