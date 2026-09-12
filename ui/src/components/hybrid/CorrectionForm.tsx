import { useState } from 'react';
import type { EventDetailDto, EventMutationResultDto, OriginalEventValuesDto } from '../../bridge/contract.generated';
import { formatPaisa, paisaToRupeesInput } from '../../bridge/nativeBridge';
import { Dialog, Notice, TextInput } from './HybridUi';
import { correctionType, historyEventLabel } from './historyPresentation';
import { errorMessage, type CorrectionDraft } from './hybridModels';

export function CorrectionForm({ detail, busy, onClose, onSave }: { detail: EventDetailDto; busy: boolean; onClose(): void; onSave(draft: CorrectionDraft): Promise<EventMutationResultDto> }) {
  const { event, originalValues } = detail;
  const type = correctionType(event);
  const [entryMode, setEntryMode] = useState<CorrectionDraft['entryMode']>(originalValues?.quantity ? 'quantity' : 'value');
  const [values, setValues] = useState<Record<string, string>>(() => correctionValues(originalValues, event.channelCode));
  const [error, setError] = useState<string | null>(null);
  if (!type) return null;
  const value = (name: string) => values[name] ?? '';
  const set = (name: string) => (next: string) => setValues((current) => ({ ...current, [name]: next }));
  return <Dialog title={`Correct ${historyEventLabel(event.eventTypeCode)}`} onClose={onClose}><p className="mb-4 rounded-xl bg-amber-50 p-3 text-sm leading-5 text-amber-900">Change only what is wrong. The form contains the complete original transaction, and the app saves only the adjustment needed.</p>{originalValues && <OriginalValues type={type} values={originalValues} />}<form id="correction-form" onSubmit={(formEvent) => {
    formEvent.preventDefault(); setError(null);
    const draft: CorrectionDraft = { correctionTypeCode: type, originalEventId: event.eventId, occurredAt: event.occurredAt.slice(0, 10), entryMode, rawLiters: value('rawLiters'), fatPercent: value('fatPercent'), lrReading: value('lrReading'), milkLitersUsed: value('milkLitersUsed'), yogurtKgProduced: value('yogurtKgProduced'), unitRateRupees: value('unitRateRupees'), quantity: value('quantity'), valueRupees: value('valueRupees'), surchargeRupees: value('surchargeRupees'), channelCode: value('channelCode'), amountRupees: value('amountRupees'), reasonCode: value('reasonCode'), notes: value('notes') };
    void onSave(draft).catch((failure) => setError(errorMessage(failure)));
  }}>{type === 'purchase' && <><TextInput id="correction-raw-liters" label="Correct total milk received (liters)" value={value('rawLiters')} onChange={set('rawLiters')} inputMode="decimal" /><TextInput id="correction-fat-percent" label="Correct fat test (%)" value={value('fatPercent')} onChange={set('fatPercent')} inputMode="decimal" /><TextInput id="correction-lr-reading" label="Correct LR reading" value={value('lrReading')} onChange={set('lrReading')} inputMode="decimal" /></>}{type === 'production' && <><TextInput id="correction-milk-used" label="Correct milk used (liters)" value={value('milkLitersUsed')} onChange={set('milkLitersUsed')} inputMode="decimal" /><TextInput id="correction-yogurt-produced" label="Correct yogurt produced (kg)" value={value('yogurtKgProduced')} onChange={set('yogurtKgProduced')} inputMode="decimal" /></>}{type === 'monetary' && <TextInput id="correction-amount" label="Correct full amount (PKR)" value={value('amountRupees')} onChange={set('amountRupees')} inputMode="decimal" />}{type === 'spoilage' && <><TextInput id="correction-quantity" label="Correct full spoiled quantity" value={value('quantity')} onChange={set('quantity')} inputMode="decimal" /><label className="mb-4 block text-sm font-semibold text-slate-700">Correct reason<select id="correction-reason" value={value('reasonCode')} onChange={(change) => set('reasonCode')(change.target.value)} className="mt-1.5 w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5"><option value="spoiled_or_sour">Spoiled, sour, or curdled</option><option value="spill">Spilled</option><option value="contamination">Contaminated</option><option value="power_failure">Power failure</option><option value="equipment_failure">Equipment failure</option><option value="other">Other</option></select></label></>}{type === 'sale' && <SaleCorrectionFields entryMode={entryMode} setEntryMode={setEntryMode} value={value} set={set} />}<label className="mb-4 block text-sm font-semibold text-slate-700">Why is this being corrected? (optional)<textarea id="correction-notes" value={value('notes')} onChange={(change) => set('notes')(change.target.value)} rows={3} className="mt-1.5 w-full rounded-xl border border-slate-300 px-3 py-2.5" /></label><p className="mb-4 text-xs text-slate-500">Correction date: {event.occurredAt.slice(0, 10)}. To protect reports, this stays on the original business date.</p>{error && <Notice message={error} />}<div className="flex gap-3"><button type="button" onClick={onClose} className="flex-1 rounded-xl border border-slate-300 px-4 py-3 font-semibold">Cancel</button><button id="save-correction-button" disabled={busy} className="flex-1 rounded-xl bg-amber-600 px-4 py-3 font-semibold text-white disabled:opacity-60">{busy ? 'Saving…' : 'Save correction'}</button></div></form></Dialog>;
}

function correctionValues(original: OriginalEventValuesDto | null, channelCode: string | null): Record<string, string> {
  return {
    rawLiters: original?.rawLiters ?? '',
    fatPercent: original?.fatPercent ?? '',
    lrReading: original?.lrReading ?? '',
    milkLitersUsed: original?.milkLitersUsed ?? '',
    yogurtKgProduced: original?.yogurtKgProduced ?? '',
    unitRateRupees: original?.unitRatePaisa ? paisaToRupeesInput(original.unitRatePaisa) : '',
    quantity: original?.quantity ?? '',
    valueRupees: original?.valuePaisa ? paisaToRupeesInput(original.valuePaisa) : '',
    surchargeRupees: original?.surchargePaisa ? paisaToRupeesInput(original.surchargePaisa) : '',
    channelCode: original?.channelCode ?? channelCode ?? 'retail',
    amountRupees: original?.amountPaisa ? paisaToRupeesInput(original.amountPaisa) : '',
    reasonCode: original?.reasonCode ?? 'other',
  };
}

function OriginalValues({ type, values }: { type: NonNullable<ReturnType<typeof correctionType>>; values: OriginalEventValuesDto }) {
  const rows: Array<[string, string]> = type === 'purchase'
    ? [['Milk received', `${values.rawLiters ?? '—'} liters`], ['Fat test', `${values.fatPercent ?? '—'}%`], ['LR reading', values.lrReading ?? '—']]
    : type === 'production'
      ? [['Milk used', `${values.milkLitersUsed ?? '—'} liters`], ['Yogurt produced', `${values.yogurtKgProduced ?? '—'} kg`]]
      : type === 'sale'
        ? [['Selling rate', values.unitRatePaisa ? `${formatPaisa(values.unitRatePaisa)} per unit` : '—'], ['Quantity', values.quantity ?? '—'], ['Sale value', values.valuePaisa ? formatPaisa(values.valuePaisa) : '—'], ['Delivery charge', values.surchargePaisa ? formatPaisa(values.surchargePaisa) : 'PKR 0.00'], ['Sale type', plainValue(values.channelCode)]]
        : type === 'monetary'
          ? [['Amount', values.amountPaisa ? formatPaisa(values.amountPaisa) : '—']]
          : [['Quantity', values.quantity ?? '—'], ['Reason', plainValue(values.reasonCode)]];
  return <section id="correction-original-values" aria-label="Original values" className="mb-4 rounded-xl border border-slate-200 bg-slate-50 p-3"><h3 className="text-sm font-bold text-slate-900">Original values</h3><dl className="mt-2 grid grid-cols-2 gap-x-3 gap-y-2 text-sm">{rows.map(([label, value]) => <div key={label}><dt className="text-xs text-slate-500">{label}</dt><dd className="font-semibold text-slate-800">{value}</dd></div>)}</dl></section>;
}

function plainValue(value: string | null): string {
  if (!value) return '—';
  return value.replace(/[-_]/g, ' ').replace(/\b\w/g, (letter) => letter.toUpperCase());
}

function SaleCorrectionFields({ entryMode, setEntryMode, value, set }: { entryMode: CorrectionDraft['entryMode']; setEntryMode(value: CorrectionDraft['entryMode']): void; value(name: string): string; set(name: string): (value: string) => void }) {
  return <><TextInput id="correction-unit-rate" label="Correct selling rate (PKR per unit)" value={value('unitRateRupees')} onChange={set('unitRateRupees')} inputMode="decimal" /><div className="mb-4 flex rounded-xl bg-slate-100 p-1"><button id="correction-mode-quantity" type="button" onClick={() => setEntryMode('quantity')} className={`flex-1 rounded-lg px-3 py-2 text-sm font-bold ${entryMode === 'quantity' ? 'bg-white text-emerald-800 shadow-sm' : 'text-slate-600'}`}>By quantity</button><button id="correction-mode-value" type="button" onClick={() => setEntryMode('value')} className={`flex-1 rounded-lg px-3 py-2 text-sm font-bold ${entryMode === 'value' ? 'bg-white text-emerald-800 shadow-sm' : 'text-slate-600'}`}>By PKR value</button></div>{entryMode === 'quantity' ? <TextInput id="correction-quantity" label="Correct full quantity sold" value={value('quantity')} onChange={set('quantity')} inputMode="decimal" /> : <TextInput id="correction-sale-value" label="Correct sale value before delivery (PKR)" value={value('valueRupees')} onChange={set('valueRupees')} inputMode="decimal" />}<TextInput id="correction-surcharge" label="Correct delivery charge (PKR, leave blank for zero)" value={value('surchargeRupees')} onChange={set('surchargeRupees')} inputMode="decimal" required={false} /><label className="mb-4 block text-sm font-semibold text-slate-700">Correct sale type<select id="correction-channel" value={value('channelCode')} onChange={(event) => set('channelCode')(event.target.value)} className="mt-1.5 w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5"><option value="retail">Retail</option><option value="wholesale">Wholesale</option><option value="credit-retail">Credit retail</option><option value="credit-wholesale">Credit wholesale</option></select></label></>;
}
