import { Search } from 'lucide-react';
import type { EventHistoryDto, Int64String } from '../../bridge/contract.generated';
import { historyEventLabel } from './historyPresentation';

export function HistoryList({ events, search, onSearch, onSelect }: {
  events: ReadonlyArray<EventHistoryDto>;
  search: string;
  onSearch(value: string): void;
  onSelect(eventId: Int64String): void;
}) {
  const query = search.trim().toLowerCase();
  const shown = events.filter((item) => !query || `${item.eventId} ${historyEventLabel(item.eventTypeCode)} ${item.notes ?? ''} ${item.actor?.displayName ?? ''}`.toLowerCase().includes(query));
  return <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
    <div className="border-b border-slate-100 p-4"><label className="relative block"><Search className="absolute left-3 top-3 h-4 w-4 text-slate-400" /><span className="sr-only">Search transaction history</span><input id="history-search" value={search} onChange={(event) => onSearch(event.target.value)} placeholder="Search type, record number, note, or staff" className="w-full rounded-xl border border-slate-300 py-2.5 pl-9 pr-3 text-sm" /></label></div>
    <div id="history-list" className="divide-y divide-slate-100">
      {shown.length === 0 && <p className="p-5 text-sm text-slate-500">No matching transaction records.</p>}
      {shown.map((item) => <button key={item.eventId} id={`history-event-${item.eventId}`} onClick={() => onSelect(item.eventId)} className={`flex w-full items-start justify-between gap-4 p-4 text-left ${item.isSuperseded ? 'bg-slate-50 text-slate-500' : 'bg-white'}`}><span><strong className="block text-sm">{historyEventLabel(item.eventTypeCode)}</strong><span className="mt-1 block text-xs">{item.occurredAt.slice(0, 10)} · Record #{item.eventId}{item.channelCode ? ` · ${item.channelCode.replace(/-/g, ' ')}` : ''}</span>{item.notes && <span className="mt-1 block text-xs">{item.notes}</span>}</span><span className="shrink-0 text-right text-xs">{item.isSuperseded ? 'Replaced' : item.isCorrection ? `Corrects #${item.correctsEventId}` : 'Current'}<span className="mt-1 block">{item.actor?.displayName ?? 'Legacy/unverified'}</span></span></button>)}
    </div>
  </section>;
}
