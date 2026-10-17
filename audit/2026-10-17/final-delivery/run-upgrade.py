"""Only the three existing migration/recovery checks needed for this upgrade."""
from pathlib import Path
import json
OUT=Path(__file__).resolve().parent
ROOT=OUT.parents[2]
selection={'FollowUpDataTest':['migrationPreservesLedgerAndStartsEmpty','restoredJournalIsHeldAndCacheIsNotRestored','malformedRestoreRollsBackAndLegacyBackupStillWorks']}
source=(ROOT/'audit/2026-10-15/credit-status-plan/run-finish.py').read_text(encoding='utf-8')
line=next(s for s in source.splitlines() if s.startswith('SELECTION = '))
source=source.replace(line,'SELECTION = '+repr(selection)).replace("label+'-console.txt'","label+'-instrumentation.txt'")
(OUT/'UPGRADE_SELECTION.json').write_text(json.dumps(selection,indent=2)+'\n')
exec(compile(source,str(OUT/'run-upgrade.py'),'exec'))
