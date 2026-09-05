import { useState } from 'react';
import { Factory } from 'lucide-react';
import type { EventMutationResultDto } from '../../bridge/contract.generated';
import { Notice, TextInput } from './HybridUi';
import { errorMessage, todayInput, type ProductionDraft } from './hybridModels';

type RecordedBatch = ProductionDraft & EventMutationResultDto;

export function ProductionForm({ busy, milkAvailable, onRecordProduction }: {
  busy: boolean;
  milkAvailable: string | null;
  onRecordProduction(draft: ProductionDraft): Promise<EventMutationResultDto>;
}) {
  const [occurredAt, setOccurredAt] = useState(todayInput());
  const [milkLitersUsed, setMilkLitersUsed] = useState('');
  const [yogurtKgProduced, setYogurtKgProduced] = useState('');
  const [notes, setNotes] = useState('');
  const [localError, setLocalError] = useState<string | null>(null);
  const [recorded, setRecorded] = useState<RecordedBatch | null>(null);

  return <section className="rounded-2xl border border-slate-200 bg-white shadow-sm">
    <div className="border-b border-slate-100 p-5"><div className="flex items-center gap-3"><span className="rounded-xl bg-emerald-50 p-2 text-emerald-700"><Factory className="h-5 w-5" /></span><div><h2 className="font-bold">Record yogurt production</h2><p className="mt-0.5 text-sm text-slate-500">Record one completed batch using its physical input and output.</p></div></div></div>
    <form onSubmit={(event) => {
      event.preventDefault(); setLocalError(null); setRecorded(null);
      const draft = { occurredAt, milkLitersUsed, yogurtKgProduced, notes };
      void onRecordProduction(draft).then((saved) => {
        setRecorded({ ...draft, ...saved }); setMilkLitersUsed(''); setYogurtKgProduced(''); setNotes('');
      }).catch((failure) => setLocalError(errorMessage(failure)));
    }} className="p-5">
      <div className="mb-4 rounded-xl bg-slate-50 p-3 text-sm text-slate-600"><span className="font-semibold text-slate-800">Milk currently available:</span> {milkAvailable === null ? 'Loading…' : `${milkAvailable} liters`}</div>
      <div className="grid gap-x-4 sm:grid-cols-2">
        <TextInput id="production-date" label="Business date" type="date" value={occurredAt} onChange={setOccurredAt} />
        <TextInput id="production-milk-used" label="Milk used (liters)" value={milkLitersUsed} onChange={setMilkLitersUsed} inputMode="decimal" />
        <TextInput id="production-yogurt-produced" label="Yogurt produced (kg)" value={yogurtKgProduced} onChange={setYogurtKgProduced} inputMode="decimal" />
      </div>
      <label className="mb-4 block text-sm font-semibold text-slate-700">Batch notes (optional)<textarea id="production-notes" value={notes} onChange={(event) => setNotes(event.target.value)} rows={3} className="mt-1.5 w-full resize-none rounded-xl border border-slate-300 bg-white px-3 py-2.5" /></label>
      <p className="mb-4 text-sm leading-5 text-slate-600">The app stores this batch for stock, yield, and costing review. It does not automatically declare a financial loss.</p>
      {localError && <Notice message={localError} />}
      <button id="record-production-button" disabled={busy} className="flex w-full items-center justify-center gap-2 rounded-xl bg-emerald-700 px-4 py-3 font-semibold text-white disabled:cursor-wait disabled:opacity-60"><Factory className="h-4 w-4" />{busy ? 'Recording…' : 'Record production batch'}</button>
      {recorded && <div id="production-result" className="mt-5 rounded-2xl border border-emerald-200 bg-emerald-50 p-4 text-emerald-950"><p className="font-bold">Production batch recorded</p><p className="mt-2 text-sm text-emerald-800">Milk stock reduced by <strong>{recorded.milkLitersUsed} liters</strong> and Yogurt stock increased by <strong>{recorded.yogurtKgProduced} kg</strong>.</p><p className="mt-2 text-xs text-emerald-700">Batch record #{recorded.eventId}</p></div>}
    </form>
  </section>;
}
