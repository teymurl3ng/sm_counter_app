import { useState } from 'react';
import type { EventDetailDto, EventHistoryDto, EventMutationResultDto, Int64String } from '../../bridge/contract.generated';
import { CorrectionForm } from './CorrectionForm';
import { HistoryDetail } from './HistoryDetail';
import { HistoryList } from './HistoryList';
import { Notice } from './HybridUi';
import { errorMessage, type CorrectionDraft } from './hybridModels';

export function HistoryWorkspace({ busy, events, canCorrect, error, notice, onLoadDetail, onCorrect }: { busy: boolean; events: ReadonlyArray<EventHistoryDto> | null; canCorrect: boolean; error: string | null; notice: string | null; onLoadDetail(eventId: Int64String): Promise<EventDetailDto>; onCorrect(draft: CorrectionDraft): Promise<EventMutationResultDto> }) {
  const [search, setSearch] = useState('');
  const [detail, setDetail] = useState<EventDetailDto | null>(null);
  const [correcting, setCorrecting] = useState(false);
  const [loading, setLoading] = useState(false);
  const [localError, setLocalError] = useState<string | null>(null);
  if (!events) return <p className="rounded-2xl border border-slate-200 bg-white p-5 text-sm text-slate-500">Loading transaction history…</p>;
  const open = async (eventId: Int64String) => { setLoading(true); setLocalError(null); try { setDetail(await onLoadDetail(eventId)); } catch (failure) { setLocalError(errorMessage(failure)); } finally { setLoading(false); } };
  const save = async (draft: CorrectionDraft) => { const result = await onCorrect(draft); setCorrecting(false); setDetail(await onLoadDetail(result.eventId)); return result; };
  return <div className="space-y-5">{(notice || error || localError) && <Notice message={localError ?? error ?? notice ?? ''} success={Boolean(notice && !error && !localError)} />}{loading ? <p className="rounded-2xl border border-slate-200 bg-white p-5 text-sm text-slate-500">Loading transaction details…</p> : detail ? <HistoryDetail detail={detail} canCorrect={canCorrect} onBack={() => setDetail(null)} onCorrect={() => setCorrecting(true)} /> : <HistoryList events={events} search={search} onSearch={setSearch} onSelect={(eventId) => void open(eventId)} />}{correcting && detail && <CorrectionForm detail={detail} busy={busy} onClose={() => setCorrecting(false)} onSave={save} />}</div>;
}
