import { ArrowLeft, PencilLine } from 'lucide-react';
import type { ReactNode } from 'react';
import type { EventDetailDto } from '../../bridge/contract.generated';
import { formatPaisa } from '../../bridge/nativeBridge';
import { correctionType, historyEventLabel, signedQuantity } from './historyPresentation';

export function HistoryDetail({ detail, canCorrect, onBack, onCorrect }: { detail: EventDetailDto; canCorrect: boolean; onBack(): void; onCorrect(): void }) {
  const event = detail.event;
  const supported = correctionType(event) !== null;
  return <section id="history-detail" className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
    <div className="border-b border-slate-100 p-5"><button id="history-back-button" onClick={onBack} className="mb-4 flex items-center gap-1 text-sm font-semibold text-emerald-800"><ArrowLeft className="h-4 w-4" />All history</button><div className="flex flex-wrap items-start justify-between gap-4"><div><h2 className="text-xl font-bold">{historyEventLabel(event.eventTypeCode)}</h2><p className="mt-1 text-sm text-slate-500">{event.occurredAt.slice(0, 10)} · Record #{event.eventId}</p></div><span className={`rounded-full px-3 py-1 text-xs font-bold ${event.isSuperseded ? 'bg-slate-200 text-slate-600' : event.isCorrection ? 'bg-sky-100 text-sky-800' : 'bg-emerald-100 text-emerald-800'}`}>{event.isSuperseded ? 'Replaced by correction' : event.isCorrection ? `Correction of #${event.correctsEventId}` : 'Current record'}</span></div>{event.notes && <p className="mt-4 rounded-xl bg-slate-50 p-3 text-sm">{event.notes}</p>}<p className="mt-3 text-xs text-slate-500">Recorded by {event.actor?.displayName ?? 'legacy/unverified user'}{event.actor?.roleCode ? ` (${event.actor.roleCode})` : ''}.</p>
      {canCorrect && supported && !event.isSuperseded && <button id={`correct-event-${event.eventId}`} onClick={onCorrect} className="mt-4 flex items-center gap-2 rounded-xl bg-amber-600 px-4 py-2.5 text-sm font-semibold text-white"><PencilLine className="h-4 w-4" />Correct this record</button>}
      {event.isSuperseded && <p className="mt-3 text-xs text-slate-500">Open the latest correction if another correction is needed.</p>}
    </div>
    <div className="grid gap-4 p-5 sm:grid-cols-2"><MovementGroup title="Stock movements" empty="No stock changed in this record.">{detail.inventoryMovements.map((item, index) => <div key={`${item.productName}-${index}`} className="rounded-xl bg-slate-50 p-3 text-sm"><strong>{item.productName}</strong><p className="mt-1 text-slate-600">{signedQuantity(item.quantity, item.unit)}</p></div>)}</MovementGroup><MovementGroup title="Money movements" empty={detail.financialMovements === null ? 'Money details are hidden for this user account.' : 'No money changed in this record.'}>{detail.financialMovements?.map((item, index) => <div key={`${item.accountName}-${index}`} className="rounded-xl bg-slate-50 p-3 text-sm"><strong>{item.accountName}</strong><p className="mt-1 text-slate-600">{formatPaisa(item.amountPaisa)}</p></div>)}</MovementGroup></div>
  </section>;
}

function MovementGroup({ title, empty, children }: { title: string; empty: string; children: ReactNode }) {
  const hasChildren = Array.isArray(children) ? children.length > 0 : Boolean(children);
  return <section><h3 className="mb-2 font-bold">{title}</h3>{hasChildren ? <div className="space-y-2">{children}</div> : <p className="text-sm text-slate-500">{empty}</p>}</section>;
}
