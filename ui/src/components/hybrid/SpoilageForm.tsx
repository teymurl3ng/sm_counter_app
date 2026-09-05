import { useState } from 'react';
import { AlertTriangle } from 'lucide-react';
import type { DashboardDto, EventMutationResultDto, Int64String, SpoilageDto, TransactionReferencesDto } from '../../bridge/contract.generated';
import { Notice, TextInput } from './HybridUi';
import { errorMessage, todayInput, type SpoilageDraft } from './hybridModels';

const REASONS = [
  ['spoiled_or_sour', 'Spoiled, sour, or curdled'],
  ['spill', 'Spilled'],
  ['contamination', 'Contaminated'],
  ['power_failure', 'Power failure'],
  ['equipment_failure', 'Equipment failure'],
  ['other', 'Other'],
] as const;

type RecordedSpoilage = SpoilageDraft & EventMutationResultDto & { productName: string; unit: string };

export function SpoilageForm({ busy, dashboard, history, references, onRecordSpoilage }: {
  busy: boolean;
  dashboard: DashboardDto | null;
  history: ReadonlyArray<SpoilageDto> | null;
  references: TransactionReferencesDto | null;
  onRecordSpoilage(draft: SpoilageDraft): Promise<EventMutationResultDto>;
}) {
  const [productId, setProductId] = useState('');
  const [quantity, setQuantity] = useState('');
  const [occurredAt, setOccurredAt] = useState(todayInput());
  const [reasonCode, setReasonCode] = useState('spoiled_or_sour');
  const [notes, setNotes] = useState('');
  const [localError, setLocalError] = useState<string | null>(null);
  const [recorded, setRecorded] = useState<RecordedSpoilage | null>(null);

  if (!references || !dashboard || !history) return <p className="rounded-2xl border border-slate-200 bg-white p-5 text-sm text-slate-500">Loading stock and spoilage history…</p>;
  if (references.products.length === 0) return <Notice message="No active product is available for spoilage recording." />;
  const selected = references.products.find((item) => item.productId === productId);
  const stock = dashboard.stock.find((item) => item.productId === productId);

  return <div className="space-y-5">
    <section className="rounded-2xl border border-slate-200 bg-white shadow-sm">
      <div className="border-b border-slate-100 p-5"><div className="flex items-center gap-3"><span className="rounded-xl bg-rose-50 p-2 text-rose-700"><AlertTriangle className="h-5 w-5" /></span><div><h2 className="font-bold">Record spoiled or wasted stock</h2><p className="mt-0.5 text-sm text-slate-500">Enter the physical quantity that can no longer be sold.</p></div></div></div>
      <form onSubmit={(event) => {
        event.preventDefault(); setLocalError(null); setRecorded(null);
        const product = references.products.find((item) => item.productId === productId);
        if (!product) { setLocalError('Choose a product.'); return; }
        if (!reasonCode) { setLocalError('Choose why the stock was lost.'); return; }
        const draft: SpoilageDraft = { productId: productId as Int64String, quantity, occurredAt, reasonCode, notes };
        void onRecordSpoilage(draft).then((saved) => {
          setRecorded({ ...draft, ...saved, productName: product.name, unit: product.unit });
          setQuantity(''); setNotes('');
        }).catch((failure) => setLocalError(errorMessage(failure)));
      }} className="p-5">
        <div className="grid gap-x-4 sm:grid-cols-2">
          <label className="mb-4 block text-sm font-semibold text-slate-700">Product lost<select id="spoilage-product" required value={productId} onChange={(event) => setProductId(event.target.value)} className="mt-1.5 w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5"><option value="">Choose product</option>{references.products.map((item) => <option key={item.productId} value={item.productId}>{item.name} ({item.unit})</option>)}</select></label>
          <TextInput id="spoilage-quantity" label={`Quantity lost${selected ? ` (${selected.unit})` : ''}`} value={quantity} onChange={setQuantity} inputMode="decimal" />
          <label className="mb-4 block text-sm font-semibold text-slate-700">Reason<select id="spoilage-reason" required value={reasonCode} onChange={(event) => setReasonCode(event.target.value)} className="mt-1.5 w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5">{REASONS.map(([code, label]) => <option key={code} value={code}>{label}</option>)}</select></label>
          <TextInput id="spoilage-date" label="Business date" type="date" value={occurredAt} onChange={setOccurredAt} />
        </div>
        {selected && <div className="mb-4 rounded-xl bg-slate-50 p-3 text-sm text-slate-700"><span className="font-semibold">Current saved stock:</span> {stock?.quantityOnHand ?? '0'} {selected.unit}</div>}
        <label className="mb-4 block text-sm font-semibold text-slate-700">Notes (optional)<textarea id="spoilage-notes" value={notes} onChange={(event) => setNotes(event.target.value)} rows={3} className="mt-1.5 w-full resize-none rounded-xl border border-slate-300 bg-white px-3 py-2.5" /></label>
        <p className="mb-4 text-sm leading-5 text-slate-600">The saved quantity is removed from stock. The app calculates any supported cost effect from existing inventory records; do not enter a loss rate.</p>
        {localError && <Notice message={localError} />}
        <button id="record-spoilage-button" disabled={busy} className="flex w-full items-center justify-center gap-2 rounded-xl bg-rose-700 px-4 py-3 font-semibold text-white disabled:cursor-wait disabled:opacity-60"><AlertTriangle className="h-4 w-4" />{busy ? 'Recording…' : 'Record spoiled stock'}</button>
        {recorded && <div id="spoilage-result" className="mt-5 rounded-2xl border border-emerald-200 bg-emerald-50 p-4 text-emerald-950"><p className="font-bold">Spoiled stock recorded</p><p className="mt-2 text-sm text-emerald-800"><strong>{recorded.quantity} {recorded.unit}</strong> of <strong>{recorded.productName}</strong> was removed from saved stock.</p><p className="mt-2 text-xs text-emerald-700">Spoilage record #{recorded.eventId}</p></div>}
      </form>
    </section>
    <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm"><h2 className="font-bold">Recent spoilage records</h2>{history.length === 0 ? <p className="mt-3 text-sm text-slate-500">No spoilage has been recorded yet.</p> : <div id="spoilage-history" className="mt-3 space-y-2">{history.slice(0, 8).map((item) => <div key={item.eventId} className={`rounded-xl border p-3 text-sm ${item.isSuperseded ? 'border-slate-200 bg-slate-50 text-slate-500' : 'border-rose-100 bg-rose-50/50 text-slate-700'}`}><div className="flex justify-between gap-3"><strong>{item.productName}</strong><span>{item.occurredAt}</span></div><p className="mt-1">{item.quantity} {item.unit} · {reasonLabel(item.reasonCode)}{item.isSuperseded ? ' · Replaced by a correction' : ''}</p>{item.notes && <p className="mt-1 text-xs">{item.notes}</p>}</div>)}</div>}</section>
  </div>;
}

function reasonLabel(code: string): string {
  return REASONS.find(([value]) => value === code)?.[1] ?? code.replace(/_/g, ' ');
}
