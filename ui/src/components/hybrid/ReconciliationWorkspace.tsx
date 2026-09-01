import { useState } from 'react';
import type { CashCountResultDto, DailyCloseResultDto, Int64String, ReconciliationArchiveDto } from '../../bridge/contract.generated';
import { CashClosePanel } from './CashClosePanel';
import { CashCountForm } from './CashCountForm';
import { Notice } from './HybridUi';
import { ReconciliationArchiveList } from './ReconciliationArchiveList';
import { errorMessage, todayInput, type CashCountDraft, type ReopenDayDraft } from './hybridModels';
import { currentActionForDate, latestCountForDate } from './reconciliationPresentation';

export function ReconciliationWorkspace({ busy, archive, cashAccountId, canReopen, error, notice, onRecordCount, onCloseDate, onReopenDate }: { busy: boolean; archive: ReconciliationArchiveDto | null; cashAccountId: Int64String | null; canReopen: boolean; error: string | null; notice: string | null; onRecordCount(draft: CashCountDraft, accountId: Int64String): Promise<CashCountResultDto>; onCloseDate(date: string, countId: Int64String, reason: string): Promise<DailyCloseResultDto>; onReopenDate(draft: ReopenDayDraft): Promise<DailyCloseResultDto> }) {
  const [businessDate, setBusinessDate] = useState(todayInput());
  const [localError, setLocalError] = useState<string | null>(null);
  if (!archive) return <p className="rounded-2xl border border-slate-200 bg-white p-5 text-sm text-slate-500">Loading cash-close records…</p>;
  const action = currentActionForDate(archive, businessDate);
  const closed = action?.actionCode === 'close';
  const guard = async <T,>(operation: Promise<T>): Promise<T> => { setLocalError(null); try { return await operation; } catch (failure) { setLocalError(errorMessage(failure)); throw failure; } };
  return <div className="space-y-5">{(notice || error || localError) && <Notice message={localError ?? error ?? notice ?? ''} success={Boolean(notice && !error && !localError)} />}<div className="grid gap-5 lg:grid-cols-2"><CashCountForm busy={busy} businessDate={businessDate} cashAccountId={cashAccountId} disabled={closed} onDateChange={setBusinessDate} onSave={(draft, accountId) => guard(onRecordCount(draft, accountId))} /><CashClosePanel busy={busy} businessDate={businessDate} latestCount={latestCountForDate(archive, businessDate)} currentAction={action} canReopen={canReopen} onClose={(date, countId, reason) => guard(onCloseDate(date, countId, reason))} onReopen={(draft) => guard(onReopenDate(draft))} /></div><ReconciliationArchiveList archive={archive} selectedDate={businessDate} onSelect={setBusinessDate} /></div>;
}
