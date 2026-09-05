import { useState } from 'react';
import { ShoppingCart } from 'lucide-react';
import type { Int64String, SaleRecordedResultDto, TransactionReferencesDto } from '../../bridge/contract.generated';
import { formatPaisa, paisaToRupeesInput } from '../../bridge/nativeBridge';
import { Notice, TextInput } from './HybridUi';
import { errorMessage, todayInput, type SaleDraft } from './hybridModels';

export function SaleForm({ busy, references, onRecordSale }: {
  busy: boolean;
  references: TransactionReferencesDto | null;
  onRecordSale(draft: SaleDraft): Promise<SaleRecordedResultDto>;
}) {
  const [productId, setProductId] = useState('');
  const [occurredAt, setOccurredAt] = useState(todayInput());
  const [channelKind, setChannelKind] = useState<SaleDraft['channelKind']>('retail');
  const [entryMode, setEntryMode] = useState<SaleDraft['entryMode']>('quantity');
  const [unitRateRupees, setUnitRateRupees] = useState('');
  const [quantity, setQuantity] = useState('');
  const [valueRupees, setValueRupees] = useState('');
  const [paymentKind, setPaymentKind] = useState<SaleDraft['paymentKind']>('account');
  const [paymentAccountId, setPaymentAccountId] = useState('');
  const [customerAccountId, setCustomerAccountId] = useState('');
  const [surchargeEnabled, setSurchargeEnabled] = useState(false);
  const [surchargeRupees, setSurchargeRupees] = useState('');
  const [notes, setNotes] = useState('');
  const [localError, setLocalError] = useState<string | null>(null);
  const [result, setResult] = useState<SaleRecordedResultDto | null>(null);
  const product = references?.products.find((item) => item.productId === productId);

  if (!references) return <p className="rounded-2xl border border-slate-200 bg-white p-5 text-sm text-slate-500">Loading products, customers, and payment accounts…</p>;
  if (references.products.length === 0) return <Notice message="No active product is available for sale." />;

  const chooseProduct = (nextId: string) => {
    setProductId(nextId);
    const selected = references.products.find((item) => item.productId === nextId);
    const savedRate = selected?.name.toLowerCase() === 'milk' ? references.retailMilkRatePaisaPerUnit
      : selected?.name.toLowerCase() === 'yogurt' ? references.retailYogurtRatePaisaPerUnit : null;
    setUnitRateRupees(paisaToRupeesInput(savedRate));
  };

  return <section className="rounded-2xl border border-slate-200 bg-white shadow-sm">
    <div className="border-b border-slate-100 p-5"><div className="flex items-center gap-3"><span className="rounded-xl bg-emerald-50 p-2 text-emerald-700"><ShoppingCart className="h-5 w-5" /></span><div><h2 className="font-bold">Record a sale</h2><p className="mt-0.5 text-sm text-slate-500">The native app calculates stock and money movements after you save.</p></div></div></div>
    <form onSubmit={(event) => {
      event.preventDefault(); setLocalError(null); setResult(null);
      if (!productId) { setLocalError('Choose a product.'); return; }
      if (paymentKind === 'account' && !paymentAccountId) { setLocalError('Choose where the payment was received.'); return; }
      if (paymentKind === 'credit' && !customerAccountId) { setLocalError('Choose the customer receiving credit.'); return; }
      const draft: SaleDraft = {
        productId: productId as Int64String, occurredAt, channelKind, entryMode, unitRateRupees, quantity, valueRupees,
        paymentKind,
        paymentAccountId: paymentKind === 'account' ? paymentAccountId as Int64String : null,
        customerAccountId: paymentKind === 'credit' ? customerAccountId as Int64String : null,
        surchargeEnabled, surchargeRupees, notes,
      };
      void onRecordSale(draft).then((saved) => {
        setResult(saved); setQuantity(''); setValueRupees(''); setNotes('');
      }).catch((failure) => setLocalError(errorMessage(failure)));
    }} className="p-5">
      <div className="grid gap-x-4 sm:grid-cols-2">
        <label className="mb-4 block text-sm font-semibold text-slate-700">Product<select id="sale-product" required value={productId} onChange={(event) => chooseProduct(event.target.value)} className="mt-1.5 w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5"><option value="">Choose product</option>{references.products.map((item) => <option key={item.productId} value={item.productId}>{item.name} ({item.unit})</option>)}</select></label>
        <TextInput id="sale-date" label="Business date" type="date" value={occurredAt} onChange={setOccurredAt} />
        <label className="mb-4 block text-sm font-semibold text-slate-700">Sale type<select id="sale-channel" value={channelKind} onChange={(event) => setChannelKind(event.target.value as SaleDraft['channelKind'])} className="mt-1.5 w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5"><option value="retail">Retail</option><option value="wholesale">Wholesale</option></select></label>
        <TextInput id="sale-unit-rate" label={`Selling rate in PKR${product ? ` per ${product.unit}` : ''}`} value={unitRateRupees} onChange={setUnitRateRupees} inputMode="decimal" />
      </div>

      <ChoiceGroup label="How are you entering this sale?" firstId="sale-entry-quantity" firstLabel="By quantity" secondId="sale-entry-value" secondLabel="By PKR value" firstActive={entryMode === 'quantity'} onFirst={() => setEntryMode('quantity')} onSecond={() => setEntryMode('value')} />
      {entryMode === 'quantity'
        ? <TextInput id="sale-quantity" label={`Quantity sold${product ? ` (${product.unit})` : ''}`} value={quantity} onChange={setQuantity} inputMode="decimal" />
        : <TextInput id="sale-value" label="Sale value before delivery charge (PKR)" value={valueRupees} onChange={setValueRupees} inputMode="decimal" />}

      <ChoiceGroup label="How is the customer paying?" firstId="sale-payment-account" firstLabel="Paid now" secondId="sale-payment-credit" secondLabel="Customer credit" firstActive={paymentKind === 'account'} onFirst={() => setPaymentKind('account')} onSecond={() => setPaymentKind('credit')} />
      {paymentKind === 'account' ? <label className="mb-4 block text-sm font-semibold text-slate-700">Payment received in<select id="sale-payment-account-select" required value={paymentAccountId} onChange={(event) => setPaymentAccountId(event.target.value)} className="mt-1.5 w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5"><option value="">Choose cash or bank</option>{references.paymentAccounts.map((account) => <option key={account.accountId} value={account.accountId}>{account.name}</option>)}</select></label>
        : references.customers.length === 0 ? <Notice message="No active customer is available for a credit sale." />
          : <label className="mb-4 block text-sm font-semibold text-slate-700">Customer receiving credit<select id="sale-customer" required value={customerAccountId} onChange={(event) => setCustomerAccountId(event.target.value)} className="mt-1.5 w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5"><option value="">Choose customer</option>{references.customers.map((customer) => <option key={customer.accountId} value={customer.accountId}>{customer.name}</option>)}</select></label>}

      <label className="mb-4 flex items-center gap-3 rounded-xl bg-slate-50 p-3 text-sm font-semibold text-slate-700"><input id="sale-surcharge-enabled" type="checkbox" checked={surchargeEnabled} onChange={(event) => setSurchargeEnabled(event.target.checked)} className="h-5 w-5 accent-emerald-700" />Add delivery charge</label>
      {surchargeEnabled && <TextInput id="sale-surcharge" label={`Custom delivery charge in PKR (optional; saved default is ${formatPaisa(references.defaultDeliverySurchargePaisa)})`} value={surchargeRupees} onChange={setSurchargeRupees} inputMode="decimal" required={false} />}
      <label className="mb-4 block text-sm font-semibold text-slate-700">Notes (optional)<textarea id="sale-notes" value={notes} onChange={(event) => setNotes(event.target.value)} rows={3} className="mt-1.5 w-full resize-none rounded-xl border border-slate-300 bg-white px-3 py-2.5" /></label>
      {localError && <Notice message={localError} />}
      <button id="record-sale-button" disabled={busy} className="flex w-full items-center justify-center gap-2 rounded-xl bg-emerald-700 px-4 py-3 font-semibold text-white disabled:cursor-wait disabled:opacity-60"><ShoppingCart className="h-4 w-4" />{busy ? 'Recording…' : 'Record sale'}</button>
      {result && <div id="sale-result" className="mt-5 rounded-2xl border border-emerald-200 bg-emerald-50 p-4 text-emerald-950"><p className="font-bold">Sale recorded</p><p className="mt-2 whitespace-nowrap text-2xl font-bold tabular-nums">{formatPaisa(result.totalPaisa)}</p><p className="text-sm text-emerald-800">{paymentKind === 'credit' ? 'Now owed by the customer' : 'Recorded as received'}</p><div className="mt-3 grid grid-cols-3 gap-2 text-sm"><ResultValue label="Quantity" value={`${result.quantity}${product ? ` ${product.unit}` : ''}`} /><ResultValue label="Sale value" value={formatPaisa(result.valuePaisa)} /><ResultValue label="Delivery" value={formatPaisa(result.surchargePaisa)} /></div><p className="mt-3 text-sm leading-5 text-emerald-900">The sold quantity was removed from stock and the total was posted to the selected cash, bank, or customer account.</p></div>}
    </form>
  </section>;
}

function ChoiceGroup({ label, firstId, firstLabel, secondId, secondLabel, firstActive, onFirst, onSecond }: { label: string; firstId: string; firstLabel: string; secondId: string; secondLabel: string; firstActive: boolean; onFirst(): void; onSecond(): void }) {
  const style = (active: boolean) => `flex-1 rounded-lg px-3 py-2 text-sm font-bold ${active ? 'bg-white text-emerald-800 shadow-sm' : 'text-slate-600'}`;
  return <fieldset className="mb-4"><legend className="mb-1.5 text-sm font-semibold text-slate-700">{label}</legend><div className="flex rounded-xl bg-slate-100 p-1"><button id={firstId} type="button" onClick={onFirst} className={style(firstActive)}>{firstLabel}</button><button id={secondId} type="button" onClick={onSecond} className={style(!firstActive)}>{secondLabel}</button></div></fieldset>;
}

function ResultValue({ label, value }: { label: string; value: string }) {
  return <div><p className="text-xs text-emerald-700">{label}</p><p className="font-bold">{value}</p></div>;
}
