import type { CommandCorrectEventRequest, EventDetailDto, EventHistoryDto, EventMutationResultDto, Int64String, IsoDateString, SessionHandle } from '../../bridge/contract.generated';
import { callNative, rupeesToPaisa, validateDecimalInput } from '../../bridge/nativeBridge';
import type { CorrectionDraft } from './hybridModels';

type Refresh = <T>(operation: () => Promise<T>, success: string) => Promise<T>;

export function historyBridgeActions(session: SessionHandle | null, refresh: Refresh) {
  const requireSession = (): SessionHandle => {
    if (!session) throw new Error('Sign in to view transaction history.');
    return session;
  };
  const loadEventHistory = (): Promise<ReadonlyArray<EventHistoryDto>> => callNative('query.eventHistory', {}, requireSession());
  const loadEventDetail = (eventId: Int64String): Promise<EventDetailDto> => callNative('query.eventDetail', { eventId }, requireSession());
  const correctEvent = (draft: CorrectionDraft): Promise<EventMutationResultDto> => refresh(
    () => callNative('command.correctEvent', correctionRequest(draft), requireSession()),
    `Correction saved for record #${draft.originalEventId}.`,
  );
  return { loadEventHistory, loadEventDetail, correctEvent };
}

function correctionRequest(draft: CorrectionDraft): CommandCorrectEventRequest {
  const common = { correctionTypeCode: draft.correctionTypeCode, originalEventId: draft.originalEventId, occurredAt: draft.occurredAt as IsoDateString, ...(draft.notes.trim() ? { notes: draft.notes.trim() } : {}) };
  if (draft.correctionTypeCode === 'purchase') return { ...common, rawLiters: validateDecimalInput(draft.rawLiters, 'corrected milk liters'), fatPercent: validateDecimalInput(draft.fatPercent, 'corrected fat percentage'), lrReading: validateDecimalInput(draft.lrReading, 'corrected LR reading') };
  if (draft.correctionTypeCode === 'production') return { ...common, milkLitersUsed: validateDecimalInput(draft.milkLitersUsed, 'corrected milk used'), yogurtKgProduced: validateDecimalInput(draft.yogurtKgProduced, 'corrected yogurt produced') };
  if (draft.correctionTypeCode === 'monetary') return { ...common, amountPaisa: rupeesToPaisa(draft.amountRupees) };
  if (draft.correctionTypeCode === 'spoilage') return { ...common, quantity: validateDecimalInput(draft.quantity, 'corrected spoilage quantity'), reasonCode: draft.reasonCode };
  return {
    ...common,
    unitRatePaisa: rupeesToPaisa(draft.unitRateRupees),
    ...(draft.entryMode === 'quantity' ? { quantity: validateDecimalInput(draft.quantity, 'corrected sale quantity') } : { valuePaisa: rupeesToPaisa(draft.valueRupees) }),
    ...(draft.surchargeRupees.trim() ? { surchargePaisa: rupeesToPaisa(draft.surchargeRupees) } : {}),
    ...(draft.channelCode ? { channelCode: draft.channelCode } : {}),
  };
}
