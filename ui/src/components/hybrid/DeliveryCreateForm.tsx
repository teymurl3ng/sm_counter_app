import { useState } from 'react';
import { PackageCheck } from 'lucide-react';
import type { EntityMutationResultDto, Int64String, TransactionReferencesDto } from '../../bridge/contract.generated';
import { Notice, TextInput } from './HybridUi';
import { errorMessage, todayInput, type DeliveryDraft } from './hybridModels';

export function DeliveryCreateForm({ busy, references, onCreate }: {
  busy: boolean;
  references: TransactionReferencesDto;
  onCreate(draft: DeliveryDraft): Promise<EntityMutationResultDto>;
}) {
  const [customerAccountId, setCustomerAccountId] = useState('');
  const [productId, setProductId] = useState('');
  const [quantity, setQuantity] = useState('');
  const [route, setRoute] = useState('');
  const [businessDate, setBusinessDate] = useState(todayInput());
  const [saleNumber, setSaleNumber] = useState('');
  const [notes, setNotes] = useState('');
  const [localError, setLocalError] = useState<string | null>(null);
  const [createdId, setCreatedId] = useState<string | null>(null);
  const selectedProduct = references.products.find((item) => item.productId === productId);

  return <section className="rounded-2xl border border-slate-200 bg-white shadow-sm">
    <div className="border-b border-slate-100 p-5"><div className="flex items-center gap-3"><span className="rounded-xl bg-emerald-50 p-2 text-emerald-700"><PackageCheck className="h-5 w-5" /></span><div><h2 className="font-bold">Add a delivery</h2><p className="mt-0.5 text-sm text-slate-500">This creates a pending delivery. It does not record a sale or payment.</p></div></div></div>
    <form className="p-5" onSubmit={(event) => {
      event.preventDefault(); setLocalError(null); setCreatedId(null);
      if (!customerAccountId) { setLocalError('Choose a customer.'); return; }
      if (!productId) { setLocalError('Choose a product.'); return; }
      if (saleNumber.trim() && !/^[1-9][0-9]*$/.test(saleNumber.trim())) { setLocalError('Sale record number must be a positive whole number.'); return; }
      const draft: DeliveryDraft = {
        customerAccountId: customerAccountId as Int64String,
        productId: productId as Int64String,
        quantity,
        route,
        businessDate,
        saleEventId: saleNumber.trim() ? saleNumber.trim() as Int64String : null,
        notes,
      };
      void onCreate(draft).then((result) => {
        setCreatedId(result.entityId); setQuantity(''); setSaleNumber(''); setNotes('');
      }).catch((failure) => setLocalError(errorMessage(failure)));
    }}>
      <div className="grid gap-x-4 sm:grid-cols-2">
        <label className="mb-4 block text-sm font-semibold text-slate-700">Customer<select id="delivery-customer" required value={customerAccountId} onChange={(event) => setCustomerAccountId(event.target.value)} className="mt-1.5 w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5"><option value="">Choose customer</option>{references.customers.map((item) => <option key={item.accountId} value={item.accountId}>{item.name}</option>)}</select></label>
        <label className="mb-4 block text-sm font-semibold text-slate-700">Product<select id="delivery-product" required value={productId} onChange={(event) => setProductId(event.target.value)} className="mt-1.5 w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5"><option value="">Choose product</option>{references.products.map((item) => <option key={item.productId} value={item.productId}>{item.name} ({item.unit})</option>)}</select></label>
        <TextInput id="delivery-quantity" label={`Quantity${selectedProduct ? ` (${selectedProduct.unit})` : ''}`} value={quantity} onChange={setQuantity} inputMode="decimal" />
        <TextInput id="delivery-date" label="Business date" type="date" value={businessDate} onChange={setBusinessDate} />
        <TextInput id="delivery-route" label="Route or area (optional)" value={route} onChange={setRoute} required={false} />
        <TextInput id="delivery-sale-number" label="Sale record number (optional)" value={saleNumber} onChange={setSaleNumber} inputMode="decimal" required={false} />
      </div>
      <p className="-mt-2 mb-4 text-sm leading-5 text-slate-600">If the sale was already recorded, enter the number shown on its success message. Leave this blank when the delivery is being planned before the sale.</p>
      <label className="mb-4 block text-sm font-semibold text-slate-700">Delivery notes (optional)<textarea id="delivery-notes" rows={3} value={notes} onChange={(event) => setNotes(event.target.value)} className="mt-1.5 w-full resize-none rounded-xl border border-slate-300 bg-white px-3 py-2.5" /></label>
      {localError && <Notice message={localError} />}
      <button id="create-delivery-button" disabled={busy || references.customers.length === 0 || references.products.length === 0} className="w-full rounded-xl bg-emerald-700 px-4 py-3 font-semibold text-white disabled:cursor-wait disabled:opacity-60">{busy ? 'Saving…' : 'Add pending delivery'}</button>
      {createdId && <div id="delivery-created-result" className="mt-4 rounded-xl border border-emerald-200 bg-emerald-50 p-3 text-sm text-emerald-900"><strong>Delivery #{createdId}</strong> is now pending.</div>}
    </form>
  </section>;
}
