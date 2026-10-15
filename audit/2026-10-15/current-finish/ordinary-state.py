from pathlib import Path
import json, subprocess, xml.etree.ElementTree as ET
OUT=Path(__file__).resolve().parent
ADB='C:/Users/Teymur/AppData/Local/Android/Sdk/platform-tools/adb.exe'
SERIAL='R8VXA02SNKH'
PACKAGE='com.saaf.milkshop'
def adb(*args):
    result=subprocess.run([ADB,'-s',SERIAL,*args],capture_output=True,text=True,encoding='utf-8',errors='replace',timeout=40)
    assert result.returncode==0,'Ordinary app observation unavailable'
    return result.stdout
activity=adb('shell','cmd','package','resolve-activity','--brief','--user','0','-a','android.intent.action.MAIN','-c','android.intent.category.LAUNCHER',PACKAGE).strip().splitlines()[-1]
assert activity.startswith(PACKAGE+'/')
adb('shell','am','start','--user','0','-n',activity)
temporary='/data/local/tmp/saaf-finish-ui.xml'
try:
    adb('shell','uiautomator','dump',temporary)
    raw=adb('shell','cat',temporary)
finally:
    adb('shell','rm','-f',temporary)
start=raw.find('<?xml'); end=raw.rfind('</hierarchy>')
assert start>=0 and end>=0,'No readable ordinary-app hierarchy'
tree=ET.fromstring(raw[start:end+len('</hierarchy>')])
texts=[n.get('text','') for n in tree.iter('node') if n.get('password')!='true']
all_text='\n'.join(texts).lower()
known=['welcome','choose account','sign in','overview','new sale','personal security','get started','create owner']
record={'serial':SERIAL,'package':PACKAGE,'normalProfile':0,'observedStaticLabels':[s for s in known if s in all_text],
    'credentialEntryPerformed':False,'businessWritePerformed':False,'rawHierarchySaved':False,
    'limitation':'Private ordinary-app authentication/setup and retained-record comparison are not inferred from installation or QA fixtures.'}
(OUT/'ordinary-app-observation.json').write_text(json.dumps(record,indent=2)+'\n')
print(json.dumps(record,indent=2))
