import { useState } from 'react';
import { ReceiptText } from 'lucide-react';
import type { EventMutationResultDto, Int64String, TransactionReferencesDto } from '../../bridge/contract.generated';
import { Notice, TextInput } from './HybridUi';
import { errorMessage, todayInput, type ExpenseDraft } from './hybridModels';

type RecordedExpense = ExpenseDraft & EventMutationResultDto & { categoryName: string; paymentAccountName: string };

export function ExpenseForm({ busy, references, onRecordExpense }: {
  busy: boolean;
  references: TransactionReferencesDto | null;
  onRecordExpense(draft: ExpenseDraft): Promise<EventMutationResultDto>;
}) {
  const [expenseCategoryAccountId, setExpenseCategoryAccountId] = useState('');
  const [paymentAccountId, setPaymentAccountId] = useState('');
  const [amountRupees, setAmountRupees] = useState('');
  const [occurredAt, setOccurredAt] = useState(todayInput());
  const [notes, setNotes] = useState('');
  const [localError, setLocalError] = useState<string | null>(null);
  const [recorded, setRecorded] = useState<RecordedExpense | null>(null);

  if (!references) return <p className="rounded-2xl border border-slate-200 bg-white p-5 text-sm text-slate-500">Loading expense categories and payment accounts…</p>;
  if (references.expenseCategories.length === 0) return <Notice message="No active expense category is available. Ask an owner or manager to add or restore one." />;
  if (references.paymentAccounts.length === 0) return <Notice message="No cash or bank account is available for expense payment." />;

  return <section className="rounded-2xl border border-slate-200 bg-white shadow-sm">
    <div className="border-b border-slate-100 p-5"><div className="flex items-center gap-3"><span className="rounded-xl bg-amber-50 p-2 text-amber-700"><ReceiptText className="h-5 w-5" /></span><div><h2 className="font-bold">Record a shop expense</h2><p className="mt-0.5 text-sm text-slate-500">Choose what the shop paid for and where the money came from.</p></div></div></div>
    <form onSubmit={(event) => {
      event.preventDefault(); setLocalError(null); setRecorded(null);
      const category = references.expenseCategories.find((item) => item.accountId === expenseCategoryAccountId);
      const payment = references.paymentAccounts.find((item) => item.accountId === paymentAccountId);
      if (!category) { setLocalError('Choose an expense category.'); return; }
      if (!payment) { setLocalError('Choose the cash or bank account used to pay.'); return; }
      const draft: ExpenseDraft = {
        expenseCategoryAccountId: expenseCategoryAccountId as Int64String,
        paymentAccountId: paymentAccountId as Int64String,
        amountRupees, occurredAt, notes,
      };
      void onRecordExpense(draft).then((saved) => {
        setRecorded({ ...draft, ...saved, categoryName: category.displayName, paymentAccountName: payment.name });
        setAmountRupees(''); setNotes('');
      }).catch((failure) => setLocalError(errorMessage(failure)));
    }} className="p-5">
      <div className="grid gap-x-4 sm:grid-cols-2">
        <label className="mb-4 block text-sm font-semibold text-slate-700">Expense category<select id="expense-category" required value={expenseCategoryAccountId} onChange={(event) => setExpenseCategoryAccountId(event.target.value)} className="mt-1.5 w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5"><option value="">Choose category</option>{references.expenseCategories.map((item) => <option key={item.accountId} value={item.accountId}>{item.displayName}</option>)}</select></label>
        <label className="mb-4 block text-sm font-semibold text-slate-700">Paid from<select id="expense-payment-account" required value={paymentAccountId} onChange={(event) => setPaymentAccountId(event.target.value)} className="mt-1.5 w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5"><option value="">Choose cash or bank</option>{references.paymentAccounts.map((item) => <option key={item.accountId} value={item.accountId}>{item.name}</option>)}</select></label>
        <TextInput id="expense-amount" label="Amount paid (PKR)" value={amountRupees} onChange={setAmountRupees} inputMode="decimal" />
        <TextInput id="expense-date" label="Business date" type="date" value={occurredAt} onChange={setOccurredAt} />
      </div>
      <label className="mb-4 block text-sm font-semibold text-slate-700">Notes (optional)<textarea id="expense-notes" value={notes} onChange={(event) => setNotes(event.target.value)} rows={3} className="mt-1.5 w-full resize-none rounded-xl border border-slate-300 bg-white px-3 py-2.5" /></label>
      <p className="mb-4 text-sm leading-5 text-slate-600">The amount is removed from the selected cash or bank balance and recorded under the chosen expense category.</p>
      {localError && <Notice message={localError} />}
      <button id="record-expense-button" disabled={busy} className="flex w-full items-center justify-center gap-2 rounded-xl bg-emerald-700 px-4 py-3 font-semibold text-white disabled:cursor-wait disabled:opacity-60"><ReceiptText className="h-4 w-4" />{busy ? 'Recording…' : 'Record expense'}</button>
      {recorded && <div id="expense-result" className="mt-5 rounded-2xl border border-emerald-200 bg-emerald-50 p-4 text-emerald-950"><p className="font-bold">Expense recorded</p><p className="mt-2 text-sm text-emerald-800"><strong>PKR {recorded.amountRupees}</strong> was paid from <strong>{recorded.paymentAccountName}</strong> for <strong>{recorded.categoryName}</strong>.</p><p className="mt-2 text-xs text-emerald-700">Expense record #{recorded.eventId}</p></div>}
    </form>
  </section>;
}
