import type { IsoDateString, PurchaseRecordedResultDto, SessionHandle } from '../../bridge/contract.generated';
import { callNative, validateDecimalInput } from '../../bridge/nativeBridge';
import type { PurchaseDraft } from './hybridModels';

type RunAndRefreshResult = <T>(operation: () => Promise<T>, success: string) => Promise<T>;

export function dailyOperationsBridgeActions(
  session: SessionHandle | null,
  runAndRefresh: RunAndRefreshResult,
) {
  const recordPurchase = (draft: PurchaseDraft): Promise<PurchaseRecordedResultDto> => runAndRefresh(async () => {
    if (!session) throw new Error('Sign in to record a purchase.');
    return callNative('command.recordPurchase', {
      vendorAccountId: draft.vendorAccountId,
      occurredAt: draft.occurredAt as IsoDateString,
      rawLiters: validateDecimalInput(draft.rawLiters, 'the milk received'),
      fatPercent: validateDecimalInput(draft.fatPercent, 'the fat percentage'),
      lrReading: validateDecimalInput(draft.lrReading, 'the LR reading'),
      ...(draft.notes.trim() ? { notes: draft.notes.trim() } : {}),
    }, session);
  }, 'Supplier milk purchase recorded.');

  return { recordPurchase };
}
