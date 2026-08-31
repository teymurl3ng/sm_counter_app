import { useState } from 'react';
import { Archive, Check, Link2, X } from 'lucide-react';
import type { DeliveryDto, DeliveryStatusCode, Int64String } from '../../bridge/contract.generated';
import { Notice } from './HybridUi';
import { errorMessage } from './hybridModels';

export function DeliveryList({ busy, deliveries, onTransition, onLinkSale, onArchive }: {
  busy: boolean;
  deliveries: ReadonlyArray<DeliveryDto>;
  onTransition(delivery: DeliveryDto, status: DeliveryStatusCode, notes: string): Promise<void>;
  onLinkSale(deliveryId: DeliveryDto['deliveryId'], saleEventId: Int64String): Promise<void>;
  onArchive(delivery: DeliveryDto): Promise<void>;
}) {
  const [notes, setNotes] = useState<Record<string, string>>({});
  const [saleNumbers, setSaleNumbers] = useState<Record<string, string>>({});
  const [localError, setLocalError] = useState<string | null>(null);
  const pendingCount = deliveries.filter((item) => item.statusCode === 'pending').length;

  const attempt = (operation: () => Promise<void>) => {
    setLocalError(null);
    void operation().catch((failure) => setLocalError(errorMessage(failure)));
  };

  return <section className="rounded-2xl border border-slate-200 bg-white shadow-sm">
    <div className="flex items-center justify-between border-b border-slate-100 p-5"><div><h2 className="font-bold">Active deliveries</h2><p className="mt-0.5 text-sm text-slate-500">{pendingCount} pending · {deliveries.length} shown</p></div></div>
    {localError && <div className="px-5 pt-4"><Notice message={localError} /></div>}
    <div id="delivery-list" className="divide-y divide-slate-100">
      {deliveries.length === 0 && <p className="p-5 text-sm text-slate-500">No active deliveries yet.</p>}
      {deliveries.map((delivery) => {
        const pending = delivery.statusCode === 'pending';
        const note = notes[delivery.deliveryId] ?? '';
        const saleNumber = saleNumbers[delivery.deliveryId] ?? '';
        return <article key={delivery.deliveryId} data-delivery-id={delivery.deliveryId} className="p-5">
          <div className="flex flex-wrap items-start justify-between gap-3"><div><h3 className="font-bold">{delivery.customerName}</h3><p className="mt-1 text-sm text-slate-700">{delivery.quantity} {delivery.unit} {delivery.productName}</p><p className="mt-1 text-xs text-slate-500">{delivery.businessDate}{delivery.route ? ` · ${delivery.route}` : ''} · Delivery #{delivery.deliveryId}</p></div><Status status={delivery.statusCode} /></div>
          {delivery.notes && <p className="mt-3 rounded-lg bg-slate-50 p-2 text-xs text-slate-600">{delivery.notes}</p>}
          <p className="mt-3 text-xs font-medium text-slate-500">{delivery.saleEventId ? `Linked to sale #${delivery.saleEventId}` : 'No sale linked yet'}</p>
          {!delivery.saleEventId && <div className="mt-3 flex gap-2"><input id={`delivery-link-sale-${delivery.deliveryId}`} inputMode="numeric" placeholder="Sale record number" value={saleNumber} onChange={(event) => setSaleNumbers((current) => ({ ...current, [delivery.deliveryId]: event.target.value }))} className="min-w-0 flex-1 rounded-xl border border-slate-300 px-3 py-2 text-sm" /><button id={`link-delivery-sale-${delivery.deliveryId}`} disabled={busy} onClick={() => {
            const clean = saleNumber.trim();
            if (!/^[1-9][0-9]*$/.test(clean)) { setLocalError('Sale record number must be a positive whole number.'); return; }
            attempt(() => onLinkSale(delivery.deliveryId, clean as Int64String));
          }} className="flex items-center gap-1 rounded-xl border border-emerald-700 px-3 py-2 text-sm font-semibold text-emerald-800 disabled:opacity-50"><Link2 className="h-4 w-4" />Link</button></div>}
          {pending && <><input id={`delivery-status-notes-${delivery.deliveryId}`} value={note} onChange={(event) => setNotes((current) => ({ ...current, [delivery.deliveryId]: event.target.value }))} placeholder="Completion or cancellation note (optional)" className="mt-3 w-full rounded-xl border border-slate-300 px-3 py-2 text-sm" /><div className="mt-2 grid grid-cols-2 gap-2"><button id={`mark-delivered-${delivery.deliveryId}`} disabled={busy} onClick={() => attempt(() => onTransition(delivery, 'delivered', note))} className="flex items-center justify-center gap-1 rounded-xl bg-emerald-700 px-3 py-2 text-sm font-semibold text-white disabled:opacity-50"><Check className="h-4 w-4" />Delivered</button><button id={`cancel-delivery-${delivery.deliveryId}`} disabled={busy} onClick={() => attempt(() => onTransition(delivery, 'cancelled', note))} className="flex items-center justify-center gap-1 rounded-xl border border-rose-300 px-3 py-2 text-sm font-semibold text-rose-700 disabled:opacity-50"><X className="h-4 w-4" />Cancel</button></div></>}
          {!pending && <button id={`archive-delivery-${delivery.deliveryId}`} disabled={busy} onClick={() => attempt(() => onArchive(delivery))} className="mt-3 flex items-center gap-1 rounded-xl border border-slate-300 px-3 py-2 text-sm font-semibold text-slate-700 disabled:opacity-50"><Archive className="h-4 w-4" />Remove from active list</button>}
        </article>;
      })}
    </div>
  </section>;
}

function Status({ status }: { status: string | null }) {
  const label = status === 'delivered' ? 'Delivered' : status === 'cancelled' ? 'Cancelled' : 'Pending';
  const style = status === 'delivered' ? 'bg-emerald-100 text-emerald-800' : status === 'cancelled' ? 'bg-rose-100 text-rose-800' : 'bg-amber-100 text-amber-800';
  return <span className={`rounded-full px-2.5 py-1 text-xs font-bold ${style}`}>{label}</span>;
}
