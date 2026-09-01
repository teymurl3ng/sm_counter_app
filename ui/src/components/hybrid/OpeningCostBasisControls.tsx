import { useState, type FormEvent } from 'react';
import type { BusinessSettingsDto, CostingManagementDto, MasterDataDto, OpeningCostBasisDto, OpeningBasisKindCode, ProductDto } from '../../bridge/contract.generated';
import { formatPaisa, paisaToRupeesInput } from '../../bridge/nativeBridge';
import { TextInput } from './HybridUi';
import type { OpeningCostBasisDraft } from './costingPlanningModels';
import { basisKindLabel } from './costingPlanningPresentation';

export function OpeningCostBasisControls({ busy, costing, settings, masterData, onSave }: {
  busy: boolean;
  costing: CostingManagementDto;
  settings: BusinessSettingsDto | null;
  masterData: MasterDataDto | null;
  onSave(draft: OpeningCostBasisDraft, passphrase: string): Promise<void>;
}) {
  const opening = settings?.openingState ?? null;
  const products = opening?.inventoryBalances.flatMap((balance) => {
    const product = masterData?.products.find((item) => item.productId === balance.productId);
    return product && (product.name === 'Milk' || product.name === 'Yogurt') && positive(balance.quantity) ? [{ product, quantity: balance.quantity }] : [];
  }) ?? [];
  const [draft, setDraft] = useState<OpeningCostBasisDraft | null>(null);
  const [passphrase, setPassphrase] = useState('');
  const [formError, setFormError] = useState<string | null>(null);
  const activeBases = costing.openingCostBases.filter((item) => !item.isSuperseded);

  const choose = (product: ProductDto, quantity: string, current?: OpeningCostBasisDto) => {
    if (!opening) return;
    setDraft({
      openingEventId: opening.eventId,
      productId: product.productId,
      quantity,
      totalCostRupees: paisaToRupeesInput(current?.totalCostPaisa ?? null),
      basisKindCode: (current?.basisKindCode as OpeningBasisKindCode | undefined) ?? 'documented_historical',
      evidenceNotes: current?.evidenceNotes ?? '',
      supersedesBasisId: current?.basisId ?? null,
    });
    setPassphrase('');
    setFormError(null);
  };
  const submit = (event: FormEvent) => {
    event.preventDefault();
    if (!draft) return;
    setFormError(null);
    void onSave(draft, passphrase).then(() => { setDraft(null); setPassphrase(''); }).catch((failure: unknown) => setFormError(failure instanceof Error ? failure.message : 'Could not save opening stock cost.'));
  };

  return <div id="opening-cost-basis-controls" className="grid gap-5 lg:grid-cols-[0.9fr_1.1fr]">
    <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
      <div className="border-b border-slate-100 p-5"><h2 className="text-lg font-bold">Stock that existed when the app started</h2><p className="mt-1 text-sm leading-6 text-slate-600">Give only the opening milk and yogurt a trustworthy cost. This does not create a fake purchase.</p></div>
      <div id="opening-cost-products" className="divide-y divide-slate-100">
        {!opening && <p className="p-5 text-sm text-slate-500">Record the shop’s opening balances in Settings first.</p>}
        {opening && products.length === 0 && <p className="p-5 text-sm text-slate-500">The opening record has no positive milk or yogurt quantity that needs a cost.</p>}
        {products.map(({ product, quantity }) => {
          const current = activeBases.find((item) => item.openingEventId === opening?.eventId && item.productId === product.productId);
          return <article key={product.productId} className="p-4">
            <div className="flex items-start justify-between gap-3"><div><h3 className="font-bold">{product.name}</h3><p className="mt-1 text-sm text-slate-600">Opening quantity: {quantity} {product.unit}</p></div><button id={`set-opening-cost-${product.productId}`} type="button" disabled={busy} onClick={() => choose(product, quantity, current)} className="rounded-lg border border-emerald-700 px-3 py-2 text-xs font-bold text-emerald-800 disabled:opacity-50">{current ? 'Correct cost' : 'Add cost'}</button></div>
            {current ? <div className="mt-3 rounded-xl bg-emerald-50 p-3"><p className="font-bold text-emerald-900">{formatPaisa(current.totalCostPaisa)} total</p><p className="mt-1 text-xs text-emerald-800">{basisKindLabel(current.basisKindCode)} · Basis #{current.basisId}</p></div> : <p className="mt-3 rounded-xl bg-amber-50 p-3 text-sm text-amber-900">Cost not recorded yet. Profit reports cannot fully value this opening stock.</p>}
          </article>;
        })}
      </div>
    </section>

    <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <h2 className="text-lg font-bold">{draft ? `Record ${productName(draft.productId, masterData)} opening cost` : 'Choose milk or yogurt'}</h2>
      {!draft ? <p className="mt-2 text-sm leading-6 text-slate-600">Use “Add cost” or “Correct cost” beside an opening stock item.</p> : <form id="opening-cost-basis-form" className="mt-5" onSubmit={submit}>
        {draft.supersedesBasisId && <p className="mb-4 rounded-xl bg-amber-50 p-3 text-sm text-amber-900">This correction will replace basis #{draft.supersedesBasisId}, while keeping it in history.</p>}
        <label className="mb-4 block text-sm font-semibold text-slate-700">Opening quantity (copied from opening balances)<input id="opening-cost-quantity" readOnly value={draft.quantity} className="mt-1.5 w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5 text-slate-600" /></label>
        <TextInput id="opening-total-cost" label="What this opening quantity cost in total (PKR)" value={draft.totalCostRupees} onChange={(totalCostRupees) => setDraft((current) => current && ({ ...current, totalCostRupees }))} inputMode="decimal" />
        <label className="mb-4 block text-sm font-semibold text-slate-700">Where did this cost come from?
          <select id="opening-cost-kind" value={draft.basisKindCode} onChange={(event) => setDraft((current) => current && ({ ...current, basisKindCode: event.target.value as OpeningBasisKindCode }))} className="mt-1.5 w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5">
            <option value="documented_historical">Documented bill or historical record</option>
            <option value="owner_estimate">Owner estimate</option>
          </select>
        </label>
        <label className="mb-4 block text-sm font-semibold text-slate-700">{draft.basisKindCode === 'owner_estimate' ? 'Explain how you estimated it' : 'Evidence or notes (optional)'}<textarea id="opening-cost-evidence" required={draft.basisKindCode === 'owner_estimate'} rows={3} value={draft.evidenceNotes} onChange={(event) => setDraft((current) => current && ({ ...current, evidenceNotes: event.target.value }))} className="mt-1.5 w-full rounded-xl border border-slate-300 px-3 py-2.5" /></label>
        <TextInput id="opening-cost-passphrase" label="Your owner/admin passphrase" value={passphrase} onChange={setPassphrase} type="password" autoComplete="current-password" />
        {formError && <p role="alert" className="mb-4 rounded-xl bg-rose-50 p-3 text-sm text-rose-800">{formError}</p>}
        <div className="grid grid-cols-2 gap-3"><button type="button" onClick={() => setDraft(null)} className="rounded-xl border border-slate-300 px-4 py-3 font-bold text-slate-700">Cancel</button><button id="save-opening-cost-button" disabled={busy} className="rounded-xl bg-emerald-800 px-4 py-3 font-bold text-white disabled:opacity-50">{busy ? 'Checking…' : 'Save cost'}</button></div>
      </form>}
    </section>
  </div>;
}

function positive(value: string): boolean {
  return Number(value) > 0;
}

function productName(productId: string, masterData: MasterDataDto | null): string {
  return masterData?.products.find((item) => item.productId === productId)?.name ?? 'stock';
}
