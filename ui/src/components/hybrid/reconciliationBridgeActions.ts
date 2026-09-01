import type { CashCountResultDto, DailyCloseResultDto, Int64String, IsoDateString, ReconciliationArchiveDto, SessionHandle } from '../../bridge/contract.generated';
import { callNative, rupeesToPaisa } from '../../bridge/nativeBridge';
import type { CashCountDraft, ReopenDayDraft } from './hybridModels';
import type { RefreshPlan, RunAndRefresh } from './authorizedDataRefresh';

const RECONCILIATION_REFRESH: RefreshPlan = ['cashierSummary', 'eventHistory', 'reconciliationArchive'];

export function reconciliationBridgeActions(session: SessionHandle | null, refresh: RunAndRefresh) {
  const requireSession = (): SessionHandle => {
    if (!session) throw new Error('Sign in to count or close the cash drawer.');
    return session;
  };
  const loadArchive = (): Promise<ReconciliationArchiveDto> => callNative('query.reconciliationArchive', {}, requireSession());
  const recordCount = (draft: CashCountDraft, accountId: Int64String): Promise<CashCountResultDto> => refresh(
    () => callNative('command.recordPhysicalCashCount', {
      accountId,
      businessDate: draft.businessDate as IsoDateString,
      countedCashPaisa: rupeesToPaisa(draft.countedCashRupees),
      ...(draft.notes.trim() ? { notes: draft.notes.trim() } : {}),
    }, requireSession()),
    'Cash count saved. The app has now compared it with the expected drawer cash.',
    RECONCILIATION_REFRESH,
  );
  const closeDate = (businessDate: string, countId: Int64String, varianceReason: string): Promise<DailyCloseResultDto> => refresh(
    () => callNative('command.closeBusinessDate', {
      businessDate: businessDate as IsoDateString,
      physicalCashCountId: countId,
      ...(varianceReason.trim() ? { varianceReason: varianceReason.trim() } : {}),
    }, requireSession()),
    `${businessDate} is closed. Earlier shop records are now protected.`,
    RECONCILIATION_REFRESH,
  );
  const reopenDate = (draft: ReopenDayDraft): Promise<DailyCloseResultDto> => refresh(async () => {
    const activeSession = requireSession();
    await callNative('command.auth.reauthenticate', { passphrase: draft.passphrase }, activeSession);
    return callNative('command.reopenBusinessDate', { businessDate: draft.businessDate as IsoDateString, reason: draft.reason.trim() }, activeSession);
  }, `${draft.businessDate} was reopened. Count the drawer again before closing it.`, RECONCILIATION_REFRESH);
  return { closeDate, loadArchive, recordCount, reopenDate };
}
