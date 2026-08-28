import { useState } from 'react';
import { Droplets, ReceiptText } from 'lucide-react';
import type { DashboardDto, PurchaseRecordedResultDto, TransactionReferencesDto } from '../../bridge/contract.generated';
import { formatPaisa } from '../../bridge/nativeBridge';
import { Notice, SummaryCard, TextInput } from './HybridUi';
import { errorMessage, type PurchaseDraft } from './hybridModels';

export function DailyOperationsWorkspace({ busy, dashboard, references, error, notice, onRecordPurchase }: {
  busy: boolean;
  dashboard: DashboardDto | null;
  references: TransactionReferencesDto | null;
  error: string | null;
  notice: string | null;
  onRecordPurchase(draft: PurchaseDraft): Promise<PurchaseRecordedResultDto>;
}) {
  const milk = dashboard?.stock.find((item) => item.name.toLowerCase() === 'milk');
  const yogurt = dashboard?.stock.find((item) => item.name.toLowerCase() === 'yogurt');
  return <>
    <section className="mb-5 grid grid-cols-2 gap-3 sm:grid-cols-3">
      <SummaryCard label="Milk in stock" value={milk ? `${milk.quantityOnHand} ${milk.unit}` : '—'} />
      <SummaryCard label="Yogurt in stock" value={yogurt ? `${yogurt.quantityOnHand} ${yogurt.unit}` : '—'} />
      <SummaryCard label="Milk bought today" value={dashboard ? `${dashboard.totals.purchasedMilkLiters} L` : '—'} />
    </section>
    {(notice || error) && <Notice message={notice ?? error ?? ''} success={Boolean(notice)} />}
    <section className="rounded-2xl border border-slate-200 bg-white shadow-sm">
      <div className="border-b border-slate-100 p-5">
        <div className="flex items-center gap-3"><span className="rounded-xl bg-emerald-50 p-2 text-emerald-700"><Droplets className="h-5 w-5" /></span><div><h2 className="font-bold">Receive milk from a supplier</h2><p className="mt-0.5 text-sm text-slate-500">Enter the physical test readings. The app calculates the payable amount.</p></div></div>
      </div>
      <PurchaseForm busy={busy} references={references} onRecordPurchase={onRecordPurchase} />
    </section>
  </>;
}

function PurchaseForm({ busy, references, onRecordPurchase }: {
  busy: boolean;
  references: TransactionReferencesDto | null;
  onRecordPurchase(draft: PurchaseDraft): Promise<PurchaseRecordedResultDto>;
}) {
  const vendors = references?.vendors ?? [];
  const [vendorAccountId, setVendorAccountId] = useState('');
  const [occurredAt, setOccurredAt] = useState(todayInput());
  const [rawLiters, setRawLiters] = useState('');
  const [fatPercent, setFatPercent] = useState('');
  const [lrReading, setLrReading] = useState('');
  const [notes, setNotes] = useState('');
  const [localError, setLocalError] = useState<string | null>(null);
  const [result, setResult] = useState<PurchaseRecordedResultDto | null>(null);

  if (!references) return <p className="p-5 text-sm text-slate-500">Loading supplier and rate information…</p>;
  if (vendors.length === 0) return <div className="p-5"><Notice message="No active supplier is available. Add or restore a supplier first." /></div>;

  const submit = (event: React.FormEvent) => {
    event.preventDefault();
    setLocalError(null);
    setResult(null);
    if (!vendorAccountId) { setLocalError('Choose a supplier.'); return; }
    const draft: PurchaseDraft = {
      vendorAccountId: vendorAccountId as PurchaseDraft['vendorAccountId'],
      occurredAt,
      rawLiters,
      fatPercent,
      lrReading,
      notes,
    };
    void onRecordPurchase(draft).then((saved) => {
      setResult(saved);
      setRawLiters('');
      setFatPercent('');
      setLrReading('');
      setNotes('');
    }).catch((failure) => setLocalError(errorMessage(failure)));
  };

  return <form onSubmit={submit} className="p-5">
    <div className="grid gap-x-4 sm:grid-cols-2">
      <label className="mb-4 block text-sm font-semibold text-slate-700">Supplier<select id="purchase-supplier" required value={vendorAccountId} onChange={(event) => setVendorAccountId(event.target.value)} className="mt-1.5 w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5"><option value="">Choose supplier</option>{vendors.map((vendor) => <option key={vendor.accountId} value={vendor.accountId}>{vendor.name}</option>)}</select></label>
      <TextInput id="purchase-date" label="Business date" type="date" value={occurredAt} onChange={setOccurredAt} />
      <TextInput id="purchase-raw-liters" label="Milk received (liters)" value={rawLiters} onChange={setRawLiters} inputMode="decimal" />
      <TextInput id="purchase-fat-percent" label="Fat test (%)" value={fatPercent} onChange={setFatPercent} inputMode="decimal" />
      <TextInput id="purchase-lr-reading" label="Lactometer reading (LR)" value={lrReading} onChange={setLrReading} inputMode="decimal" />
    </div>
    <label className="mb-4 block text-sm font-semibold text-slate-700">Notes (optional)<textarea id="purchase-notes" value={notes} onChange={(event) => setNotes(event.target.value)} rows={3} className="mt-1.5 w-full resize-none rounded-xl border border-slate-300 bg-white px-3 py-2.5" /></label>
    <div className="mb-4 rounded-xl bg-slate-50 p-3 text-xs leading-5 text-slate-600"><p className="font-semibold text-slate-700">Rates saved in shop settings</p><p>Milk rate: {formatPaisa(references.vendorMilkRatePaisaPerUnit)} per adjusted liter · Delivery: {formatPaisa(references.vendorDeliveryFeePaisaPerUnit)} per received liter · Quality standard: {references.tsStandardPercent ?? 'Not set'}% TS</p></div>
    {localError && <Notice message={localError} />}
    <button id="record-purchase-button" disabled={busy} className="flex w-full items-center justify-center gap-2 rounded-xl bg-emerald-700 px-4 py-3 font-semibold text-white disabled:cursor-wait disabled:opacity-60"><ReceiptText className="h-4 w-4" />{busy ? 'Recording…' : 'Record milk purchase'}</button>
    {result && <div id="purchase-result" className="mt-5 rounded-2xl border border-emerald-200 bg-emerald-50 p-4 text-emerald-950"><p className="font-bold">Purchase recorded</p><p className="mt-2 text-2xl font-bold">{formatPaisa(result.amountOwedPaisa)}</p><p className="text-sm text-emerald-800">Amount now owed to the supplier</p><div className="mt-3 grid grid-cols-3 gap-2 text-sm"><ResultValue label="Adjusted liters" value={result.adjustedLiters} /><ResultValue label="SNF" value={`${result.snfPercent}%`} /><ResultValue label="TS" value={`${result.tsPercent}%`} /></div><p className="mt-3 text-xs leading-5 text-emerald-800">Stock increased by the physical liters received. Payment is based on the shop’s saved quality formula and delivery charge.</p></div>}
  </form>;
}

function ResultValue({ label, value }: { label: string; value: string }) {
  return <div><p className="text-xs text-emerald-700">{label}</p><p className="font-bold">{value}</p></div>;
}

function todayInput(): string {
  const date = new Date();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}
