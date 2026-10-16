"""Only the pinned ordinary Customers integration checks on the disposable A06 target."""
from pathlib import Path
import json,subprocess
OUT=Path(__file__).resolve().parent
source=(OUT.parents[1]/'2026-10-15/credit-status-plan/run-finish.py').read_text(encoding='utf-8')
handoff=json.loads((OUT/'b-handoff-selection.json').read_text())['FollowUpHandoffTest']
selection={
 'FollowUpNavigationTest':['standaloneRetainsManualWithoutRpc','connectedModeNeverFallsBackOnUnknown','modeIsRecheckedBeforeEveryOpen','boundWorkspaceRejectsChangedSession','restoredRequestCannotBecomeStandalone'],
 'FollowUpHandoffTest':handoff,
 'FollowUpNavigationUiTest':['ownerReturnPathsRetainReviewAndChrome','managerAndAdminUseTheirOwnChromeAndGuards','darkLargeReviewAndStatementReturn','directCustomerReminderUsesSharedHook'],
 'CustomerStatementUiTest':['statementReminderAndHistoryKeepOneHostAndReturnToCustomer','statementLightPreviewAndReturnKeepChromeAndDisclosure','statementDarkLargeTextPreviewAndReturnKeepChromeAndDisclosure'],
 'CreditFinishUiTest':['customerFiltersIncludeSettledOwnerFlagsAndSearchWithoutChangingRows']}
line=next(s for s in source.splitlines() if s.startswith('SELECTION = '))
source=source.replace(line,'SELECTION = '+repr(selection)).replace("label+'-console.txt'","label+'-instrumentation.txt'")
adb_path='C:/Users/Teymur/AppData/Local/Android/Sdk/platform-tools/adb.exe'
available=[s.split()[0] for s in subprocess.check_output([adb_path,'devices'],text=True).splitlines()[1:] if s.strip().endswith('device')]
serial=next((s for s in available if s=='R8VXA02SNKH'),None) or next((s for s in available if 'RFCX116CYYX' in s),None)
assert serial,'No authorized QA phone is connected'
model=subprocess.check_output([adb_path,'-s',serial,'shell','getprop','ro.product.model'],text=True).strip()
assert model in ('SM-S921W','SM-A065F','SM-A065M')
source=source.replace("SERIAL = 'R8VXA02SNKH'",'SERIAL = '+repr(serial))
source=source.replace("'device': 'A06'","'device': "+repr('S24' if model=='SM-S921W' else 'A06'))
source=source.replace('raw = result.stdout + result.stderr', '''raw = result.stdout + result.stderr
subprocess.run([ADB,'-s',SERIAL,'pull','/sdcard/Android/data/'+TARGET+'/files/followup-navigation',str(OUT/(label+'-captures'))],capture_output=True,text=True)''')
(OUT/'b-navigation-selection.json').write_text(json.dumps(selection,indent=2)+'\n')
exec(compile(source,str(OUT/'run-b-navigation.py'),'exec'))
