"""Only B reminder data and touched encrypted backup checks; isolated A06 target."""
from pathlib import Path
import json
OUT=Path(__file__).resolve().parent
source=(OUT.parents[1]/'2026-10-15/credit-status-plan/run-finish.py').read_text(encoding='utf-8')
selection={'FollowUpDataTest': ['migrationPreservesLedgerAndStartsEmpty', 'snapshotCodecRejectsCoercionAndConflictingFacts', 'cacheMapsScheduleVersionsWithoutFinancialWrites', 'refreshFailureAndRevocationNeverLeaveCurrentCache', 'unknownAndRecoveredCommandsNeverAuthorizeSideEffects', 'mismatchedAcknowledgmentRetainsOriginalRequest', 'bindingAndIdentityConflictsCannotRetargetCommands', 'restoredJournalIsHeldAndCacheIsNotRestored', 'malformedRestoreRollsBackAndLegacyBackupStillWorks', 'runtimeRefreshesOnceAndChecksLocalRole', 'runtimeUnknownRefreshPreservesTokens'], 'CreditMetadataStorageTest': ['encryptedRoundTripKeepsCreditHistoryAndHoldsPendingWithoutReenabling', 'malformedNewBackupFailsWithoutReplacingExistingBusinessAndOldBackupGetsExplicitLegacy']}
line=next(s for s in source.splitlines() if s.startswith('SELECTION = '))
source=source.replace(line,'SELECTION = '+repr(selection))
(OUT/'b-data-selection.json').write_text(json.dumps(selection,indent=2)+'\n')
exec(compile(source,str(OUT/'run-b-data.py'),'exec'))
