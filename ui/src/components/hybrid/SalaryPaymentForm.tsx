import { useState } from 'react';
import { Banknote } from 'lucide-react';
import type { EventMutationResultDto, Int64String, TransactionReferencesDto } from '../../bridge/contract.generated';
import { Notice, TextInput } from './HybridUi';
import { errorMessage, todayInput, type SalaryPaymentDraft } from './hybridModels';

type RecordedSalaryPayment = SalaryPaymentDraft & EventMutationResultDto & { employeeName: string; paymentAccountName: string };

export function SalaryPaymentForm({ busy, references, onRecordSalaryPayment }: {
  busy: boolean;
  references: TransactionReferencesDto | null;
  onRecordSalaryPayment(draft: SalaryPaymentDraft): Promise<EventMutationResultDto>;
}) {
  const [employeeAccountId, setEmployeeAccountId] = useState('');
  const [paymentAccountId, setPaymentAccountId] = useState('');
  const [amountRupees, setAmountRupees] = useState('');
  const [occurredAt, setOccurredAt] = useState(todayInput());
  const [notes, setNotes] = useState('');
  const [localError, setLocalError] = useState<string | null>(null);
  const [recorded, setRecorded] = useState<RecordedSalaryPayment | null>(null);

  if (!references) return <p className="rounded-2xl border border-slate-200 bg-white p-5 text-sm text-slate-500">Loading employees and payment accounts…</p>;
  if (references.employees.length === 0) return <Notice message="No active employee is available. Add or restore an employee before recording salary." />;
  if (references.paymentAccounts.length === 0) return <Notice message="No cash or bank account is available for salary payment." />;

  return <section className="rounded-2xl border border-slate-200 bg-white shadow-sm">
    <div className="border-b border-slate-100 p-5"><div className="flex items-center gap-3"><span className="rounded-xl bg-violet-50 p-2 text-violet-700"><Banknote className="h-5 w-5" /></span><div><h2 className="font-bold">Record a salary payment</h2><p className="mt-0.5 text-sm text-slate-500">Record wages that were actually paid to an employee.</p></div></div></div>
    <form onSubmit={(event) => {
      event.preventDefault(); setLocalError(null); setRecorded(null);
      const employee = references.employees.find((item) => item.accountId === employeeAccountId);
      const payment = references.paymentAccounts.find((item) => item.accountId === paymentAccountId);
      if (!employee) { setLocalError('Choose an employee.'); return; }
      if (!payment) { setLocalError('Choose the cash or bank account used to pay.'); return; }
      const draft: SalaryPaymentDraft = {
        employeeAccountId: employeeAccountId as Int64String,
        paymentAccountId: paymentAccountId as Int64String,
        amountRupees, occurredAt, notes,
      };
      void onRecordSalaryPayment(draft).then((saved) => {
        setRecorded({ ...draft, ...saved, employeeName: employee.name, paymentAccountName: payment.name });
        setAmountRupees(''); setNotes('');
      }).catch((failure) => setLocalError(errorMessage(failure)));
    }} className="p-5">
      <div className="grid gap-x-4 sm:grid-cols-2">
        <label className="mb-4 block text-sm font-semibold text-slate-700">Employee paid<select id="salary-employee" required value={employeeAccountId} onChange={(event) => setEmployeeAccountId(event.target.value)} className="mt-1.5 w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5"><option value="">Choose employee</option>{references.employees.map((item) => <option key={item.accountId} value={item.accountId}>{item.name}</option>)}</select></label>
        <label className="mb-4 block text-sm font-semibold text-slate-700">Paid from<select id="salary-payment-account" required value={paymentAccountId} onChange={(event) => setPaymentAccountId(event.target.value)} className="mt-1.5 w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5"><option value="">Choose cash or bank</option>{references.paymentAccounts.map((item) => <option key={item.accountId} value={item.accountId}>{item.name}</option>)}</select></label>
        <TextInput id="salary-amount" label="Amount paid (PKR)" value={amountRupees} onChange={setAmountRupees} inputMode="decimal" />
        <TextInput id="salary-date" label="Business date" type="date" value={occurredAt} onChange={setOccurredAt} />
      </div>
      <label className="mb-4 block text-sm font-semibold text-slate-700">Notes (optional)<textarea id="salary-notes" value={notes} onChange={(event) => setNotes(event.target.value)} rows={3} className="mt-1.5 w-full resize-none rounded-xl border border-slate-300 bg-white px-3 py-2.5" /></label>
      <p className="mb-4 text-xs leading-5 text-slate-500">The amount is removed from the selected cash or bank balance and added to recorded salary costs for this employee. The app does not calculate salary owed.</p>
      {localError && <Notice message={localError} />}
      <button id="record-salary-button" disabled={busy} className="flex w-full items-center justify-center gap-2 rounded-xl bg-emerald-700 px-4 py-3 font-semibold text-white disabled:cursor-wait disabled:opacity-60"><Banknote className="h-4 w-4" />{busy ? 'Recording…' : 'Record salary payment'}</button>
      {recorded && <div id="salary-result" className="mt-5 rounded-2xl border border-emerald-200 bg-emerald-50 p-4 text-emerald-950"><p className="font-bold">Salary payment recorded</p><p className="mt-2 text-sm text-emerald-800"><strong>PKR {recorded.amountRupees}</strong> was paid to <strong>{recorded.employeeName}</strong> from <strong>{recorded.paymentAccountName}</strong>.</p><p className="mt-2 text-xs text-emerald-700">Salary record #{recorded.eventId}</p></div>}
    </form>
  </section>;
}
