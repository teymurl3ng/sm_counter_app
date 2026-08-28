import { useState } from 'react';
import { HandCoins } from 'lucide-react';
import type { EventMutationResultDto, FinancialOverviewDto, Int64String, TransactionReferencesDto } from '../../bridge/contract.generated';
import { formatPaisa } from '../../bridge/nativeBridge';
import { FilterButton, Notice, TextInput } from './HybridUi';
import { errorMessage, todayInput, type PaymentDraft } from './hybridModels';

type RecordedPayment = PaymentDraft & EventMutationResultDto & { partyName: string; paymentAccountName: string };

export function PaymentForm({ busy, canCollectCustomer, canPaySupplier, references, financialOverview, onRecordPayment }: {
  busy: boolean;
  canCollectCustomer: boolean;
  canPaySupplier: boolean;
  references: TransactionReferencesDto | null;
  financialOverview: FinancialOverviewDto | null;
  onRecordPayment(draft: PaymentDraft): Promise<EventMutationResultDto>;
}) {
  const [kind, setKind] = useState<PaymentDraft['kind']>(canPaySupplier ? 'supplier' : 'customer');
  const [partyAccountId, setPartyAccountId] = useState('');
  const [paymentAccountId, setPaymentAccountId] = useState('');
  const [amountRupees, setAmountRupees] = useState('');
  const [occurredAt, setOccurredAt] = useState(todayInput());
  const [notes, setNotes] = useState('');
  const [localError, setLocalError] = useState<string | null>(null);
  const [recorded, setRecorded] = useState<RecordedPayment | null>(null);

  if (!references) return <p className="rounded-2xl border border-slate-200 bg-white p-5 text-sm text-slate-500">Loading customers, suppliers, and payment accounts…</p>;
  const parties = kind === 'supplier' ? references.vendors : references.customers;
  const balance = (kind === 'supplier' ? financialOverview?.vendors : financialOverview?.customers)?.find((item) => item.accountId === partyAccountId);
  const switchKind = (next: PaymentDraft['kind']) => { setKind(next); setPartyAccountId(''); setLocalError(null); setRecorded(null); };

  return <section className="rounded-2xl border border-slate-200 bg-white shadow-sm">
    <div className="border-b border-slate-100 p-5"><div className="flex items-center gap-3"><span className="rounded-xl bg-sky-50 p-2 text-sky-700"><HandCoins className="h-5 w-5" /></span><div><h2 className="font-bold">Supplier and customer payments</h2><p className="mt-0.5 text-sm text-slate-500">Record money paid out to a supplier or collected from a credit customer.</p></div></div></div>
    <form onSubmit={(event) => {
      event.preventDefault(); setLocalError(null); setRecorded(null);
      const party = parties.find((item) => item.accountId === partyAccountId);
      const payment = references.paymentAccounts.find((item) => item.accountId === paymentAccountId);
      if (!party) { setLocalError(`Choose ${kind === 'supplier' ? 'a supplier' : 'a customer'}.`); return; }
      if (!payment) { setLocalError(`Choose ${kind === 'supplier' ? 'where the payment came from' : 'where the money was received'}.`); return; }
      const draft: PaymentDraft = { kind, partyAccountId: partyAccountId as Int64String, paymentAccountId: paymentAccountId as Int64String, amountRupees, occurredAt, notes };
      void onRecordPayment(draft).then((saved) => {
        setRecorded({ ...draft, ...saved, partyName: party.name, paymentAccountName: payment.name });
        setAmountRupees(''); setNotes('');
      }).catch((failure) => setLocalError(errorMessage(failure)));
    }} className="p-5">
      {canPaySupplier && canCollectCustomer && <div className="mb-5 flex rounded-xl bg-slate-200 p-1"><FilterButton id="payment-mode-supplier" active={kind === 'supplier'} onClick={() => switchKind('supplier')}>Pay supplier</FilterButton><FilterButton id="payment-mode-customer" active={kind === 'customer'} onClick={() => switchKind('customer')}>Collect customer money</FilterButton></div>}
      {parties.length === 0 ? <Notice message={`No active ${kind === 'supplier' ? 'supplier' : 'customer'} is available.`} /> : <>
        <div className="grid gap-x-4 sm:grid-cols-2">
          <label className="mb-4 block text-sm font-semibold text-slate-700">{kind === 'supplier' ? 'Supplier being paid' : 'Customer paying the shop'}<select id="payment-party" required value={partyAccountId} onChange={(event) => setPartyAccountId(event.target.value)} className="mt-1.5 w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5"><option value="">Choose {kind}</option>{parties.map((item) => <option key={item.accountId} value={item.accountId}>{item.name}</option>)}</select></label>
          <label className="mb-4 block text-sm font-semibold text-slate-700">{kind === 'supplier' ? 'Pay from' : 'Money received in'}<select id="payment-account" required value={paymentAccountId} onChange={(event) => setPaymentAccountId(event.target.value)} className="mt-1.5 w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5"><option value="">Choose cash or bank</option>{references.paymentAccounts.map((item) => <option key={item.accountId} value={item.accountId}>{item.name}</option>)}</select></label>
          <TextInput id="payment-amount" label="Amount (PKR)" value={amountRupees} onChange={setAmountRupees} inputMode="decimal" />
          <TextInput id="payment-date" label="Business date" type="date" value={occurredAt} onChange={setOccurredAt} />
        </div>
        {balance && <div className="mb-4 rounded-xl bg-slate-50 p-3 text-sm text-slate-700"><span className="font-semibold">Current saved {kind} balance:</span> {formatPaisa(balance.canonicalBalancePaisa)}<p className="mt-1 text-xs text-slate-500">A positive supplier balance normally means the shop owes money. A positive customer balance normally means the customer owes the shop.</p></div>}
        <label className="mb-4 block text-sm font-semibold text-slate-700">Notes (optional)<textarea id="payment-notes" value={notes} onChange={(event) => setNotes(event.target.value)} rows={3} className="mt-1.5 w-full resize-none rounded-xl border border-slate-300 bg-white px-3 py-2.5" /></label>
        <p className="mb-4 text-xs leading-5 text-slate-500">The app updates the selected party and Cash/Bank balances together. It allows advances when the amount is greater than the current saved balance.</p>
        {localError && <Notice message={localError} />}
        <button id="record-payment-button" disabled={busy} className="flex w-full items-center justify-center gap-2 rounded-xl bg-emerald-700 px-4 py-3 font-semibold text-white disabled:cursor-wait disabled:opacity-60"><HandCoins className="h-4 w-4" />{busy ? 'Recording…' : kind === 'supplier' ? 'Record supplier payment' : 'Record customer collection'}</button>
        {recorded && <div id="payment-result" className="mt-5 rounded-2xl border border-emerald-200 bg-emerald-50 p-4 text-emerald-950"><p className="font-bold">{recorded.kind === 'supplier' ? 'Supplier payment recorded' : 'Customer collection recorded'}</p><p className="mt-2 text-sm text-emerald-800"><strong>PKR {recorded.amountRupees}</strong> {recorded.kind === 'supplier' ? `was paid to ${recorded.partyName} from ${recorded.paymentAccountName}` : `was collected from ${recorded.partyName} into ${recorded.paymentAccountName}`}.</p><p className="mt-2 text-xs text-emerald-700">Payment record #{recorded.eventId}</p></div>}
      </>}
    </form>
  </section>;
}
