from pathlib import Path
import hashlib, json, subprocess, sys
OUT=Path(__file__).resolve().parent
ROOT=OUT.parents[2]
ADB='C:/Users/Teymur/AppData/Local/Android/Sdk/platform-tools/adb.exe'
SERIAL='R8VXA02SNKH'
RUNNER='com.saaf.milkshop.qa.tests.test/com.saaf.milkshop.SaafMilkTestRunner'
groups={
    'costing-retry': ['NativeOwnerCostingPlansScreenContractTest#switchingSectionsKeepsTabAndActionBoundsStableAtDoubleText'],
    'hosts': ['NativeFoundationUiTest#'+m for m in [
        'ownerHostedSecurityImeKeepsHeaderStable','cashierHostedSecurityImeKeepsHeaderStable',
        'managerHostedSecurityImeKeepsHeaderStable','adminHostedSecurityImeKeepsHeaderStable',
        'operationsHostedSecurityImeKeepsHeaderStable','saleEntryDoneActionClearsKeyboardFocus']],
    'contracts': [
        'NativeOwnerCostingPlansScreenContractTest#switchingSectionsKeepsTabAndActionBoundsStable',
        'NativeOwnerCostingPlansScreenContractTest#switchingSectionsKeepsTabAndActionBoundsStableAtDoubleText',
        'NativeOwnerCostingPlansScreenContractTest#emptyStateIsHonest',
        'NativeOwnerCostingPlansScreenContractTest#loadFailureOffersRetry',
        'NativeOwnerCostingPlansScreenContractTest#reauthDialogIsShownWhenRequired',
        'NativeChromeGeometryTest#workHeaderDefault','NativeChromeGeometryTest#workHeaderDarkExtraLarge',
        'NativeSecurityImeTraceTest#pinSetupKeyboardTrace',
        'NativeSaleScreenContractTest#catalogPickerHandlesZeroOneTwoThreeTwelveAndFiftyProducts',
        'NativeSaleScreenContractTest#salePaymentPickerCancelsAndChangesDestination',
        'NativeSaleScreenContractTest#saleWholesaleRateAndNoteStayEditable',
    ],
}
group=sys.argv[1]
methods=['com.saaf.milkshop.'+m for m in groups[group]]
def adb(*args, timeout=120):
    result=subprocess.run([ADB,'-s',SERIAL,*args],capture_output=True,text=True,encoding='utf-8',errors='replace',timeout=timeout)
    assert result.returncode==0,'ADB failed; inspect isolated log'
    return result.stdout
if group in ('hosts','costing-retry'):
    for rel in ['app/build/outputs/apk/debug/app-debug.apk','app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk']:
        assert 'Success' in adb('install','-r','--user','0',str(ROOT/'phase18'/rel))
registered=adb('shell','pm','list','instrumentation')
assert RUNNER in registered and '(target=com.saaf.milkshop.qa.tests)' in registered
raw=adb('shell','am','instrument','--user','0','-w','-r','-e','class',','.join(methods),'-e','saafSkipBootstrap',str(group!='hosts').lower(),RUNNER,timeout=900)
(OUT/(group+'-console.txt')).write_text(raw,encoding='utf-8')
state={}; tests=[]
for line in raw.splitlines():
    if line.startswith('INSTRUMENTATION_STATUS: '):
        key,_,value=line[len('INSTRUMENTATION_STATUS: '):].partition('=');state[key]=value
    if line.startswith('INSTRUMENTATION_STATUS_CODE: '):
        code=int(line.split(':',1)[1])
        if code!=1 and 'test' in state: tests.append({'test':state['test'],'code':code,'stack':state.get('stack')})
        state={}
summary={'group':group,'serial':SERIAL,'target':'com.saaf.milkshop.qa.tests','methods':methods,'tests':tests,'passed':sum(t['code']==0 for t in tests)}
(OUT/(group+'-results.json')).write_text(json.dumps(summary,indent=2)+'\n',encoding='utf-8')
print(json.dumps(summary,indent=2))
assert len(tests)==len(methods) and summary['passed']==len(methods),'Affected checks failed; inspect retained console'
