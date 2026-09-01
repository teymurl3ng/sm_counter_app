import { useState } from 'react';
import type { CashCountResultDto, Int64String } from '../../bridge/contract.generated';
import { Notice, TextInput } from './HybridUi';
import { errorMessage, type CashCountDraft } from './hybridModels';

export function CashCountForm({ busy, businessDate, cashAccountId, disabled, onDateChange, onSave }: { busy: boolean; businessDate: string; cashAccountId: Int64String | null; disabled: boolean; onDateChange(value: string): void; onSave(draft: CashCountDraft, accountId: Int64String): Promise<CashCountResultDto> }) {
  const [amount, setAmount] = useState('');
  const [notes, setNotes] = useState('');
  const [error, setError] = useState<string | null>(null);
  if (!cashAccountId) return <Notice message="The physical Cash drawer account is unavailable. Check the native account setup before counting cash." />;
  return <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm"><h2 className="text-lg font-bold">Count the cash drawer</h2><p className="mt-1 text-sm leading-6 text-slate-600">Physically count the notes and coins in the shop drawer first. Do not include bank or mobile-wallet balances.</p><form className="mt-5" id="cash-count-form" onSubmit={(event) => {
    event.preventDefault(); setError(null);
    void onSave({ businessDate, countedCashRupees: amount, notes }, cashAccountId).then(() => { setAmount(''); setNotes(''); }).catch((failure) => setError(errorMessage(failure)));
  }}><TextInput id="cash-count-date" label="Business date" value={businessDate} onChange={onDateChange} type="date" /><TextInput id="counted-cash-rupees" label="Physical drawer cash (PKR)" value={amount} onChange={setAmount} inputMode="decimal" /><label className="mb-4 block text-sm font-semibold text-slate-700">Count notes (optional)<textarea id="cash-count-notes" value={notes} onChange={(event) => setNotes(event.target.value)} rows={2} className="mt-1.5 w-full rounded-xl border border-slate-300 px-3 py-2.5" /></label>{error && <Notice message={error} />}<button id="save-cash-count-button" disabled={busy || disabled} className="w-full rounded-xl bg-emerald-800 px-4 py-3 font-bold text-white disabled:opacity-50">{disabled ? 'Date is closed' : busy ? 'Saving count…' : 'Save physical count and compare'}</button></form></section>;
}
