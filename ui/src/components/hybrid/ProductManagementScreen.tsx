import { useMemo, useState } from 'react';
import { Archive, Edit3, PackagePlus, RefreshCw, RotateCcw } from 'lucide-react';
import type { MasterDataDto, ProductDto, ProductUnitCode } from '../../bridge/contract.generated';
import { formatPaisa, paisaToRupeesInput, rupeesToPaisa } from '../../bridge/nativeBridge';
import { Dialog, FilterButton, Notice, SummaryCard, TextInput } from './HybridUi';
import { errorMessage, type ProductDraft } from './hybridModels';

export function ProductManagementScreen({ busy, masterData, error, notice, onSave, onSetArchived }: {
  busy: boolean;
  masterData: MasterDataDto | null;
  error: string | null;
  notice: string | null;
  onSave(draft: ProductDraft, productId?: ProductDto['productId']): Promise<void>;
  onSetArchived(product: ProductDto, archived: boolean): Promise<void>;
}) {
  const [showArchived, setShowArchived] = useState(false);
  const [editing, setEditing] = useState<ProductDto | 'new' | null>(null);
  const [archiveTarget, setArchiveTarget] = useState<ProductDto | null>(null);
  const products = masterData?.products ?? [];
  const activeCount = products.filter((product) => !product.archived).length;
  const archivedCount = products.length - activeCount;
  const visibleProducts = useMemo(() => products.filter((product) => product.archived === showArchived), [products, showArchived]);

  return <>
    <section className="mb-5 grid grid-cols-2 gap-3">
      <SummaryCard label="Active products" value={activeCount.toString()} />
      <SummaryCard label="Archived products" value={archivedCount.toString()} />
    </section>
    {(notice || error) && <Notice message={notice ?? error ?? ''} success={Boolean(notice)} />}
    <section className="mt-5 overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 p-4">
        <div className="flex rounded-xl bg-slate-100 p-1">
          <FilterButton id="active-products-tab" active={!showArchived} onClick={() => setShowArchived(false)}>Active ({activeCount})</FilterButton>
          <FilterButton id="archived-products-tab" active={showArchived} onClick={() => setShowArchived(true)}>Archived ({archivedCount})</FilterButton>
        </div>
        <button id="add-product-button" disabled={busy} onClick={() => setEditing('new')} className="flex items-center gap-2 rounded-xl bg-emerald-700 px-4 py-2.5 text-sm font-semibold text-white disabled:opacity-50"><PackagePlus className="h-4 w-4" /> Add product</button>
      </div>
      <div id="native-product-list" className="divide-y divide-slate-100">
        {visibleProducts.map((product) => <article key={product.productId} data-product-id={product.productId} className="p-4"><div className="flex items-start justify-between gap-3"><div className="min-w-0"><div className="flex flex-wrap items-center gap-2"><h2 className="font-bold text-slate-900">{product.name}</h2><span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs font-semibold text-slate-600">{product.unit}</span></div><p className="mt-1 text-sm text-slate-500">Current stock: {product.quantityOnHand} {product.unit}</p><p className="text-sm text-slate-500">Default selling rate: {formatPaisa(product.defaultSaleRatePaisaPerUnit)}</p><p className="text-sm text-slate-500">Stock tracking: {product.inventoryTracking ? 'On' : 'Off'}</p>{product.notes && <p className="mt-2 text-sm text-slate-600">{product.notes}</p>}</div><div className="flex shrink-0 gap-2"><button id={`edit-product-${product.productId}`} disabled={busy} onClick={() => setEditing(product)} className="rounded-lg border border-slate-200 p-2 text-slate-600 disabled:opacity-50" aria-label={`Edit ${product.name}`}><Edit3 className="h-4 w-4" /></button>{product.archived ? <button id={`restore-product-${product.productId}`} disabled={busy} onClick={() => void onSetArchived(product, false).catch(() => undefined)} className="rounded-lg border border-emerald-200 p-2 text-emerald-700 disabled:opacity-50" aria-label={`Restore ${product.name}`}><RotateCcw className="h-4 w-4" /></button> : <button id={`archive-product-${product.productId}`} disabled={busy} onClick={() => setArchiveTarget(product)} className="rounded-lg border border-amber-200 p-2 text-amber-700 disabled:opacity-50" aria-label={`Archive ${product.name}`}><Archive className="h-4 w-4" /></button>}</div></div></article>)}
        {!masterData && <p className="p-5 text-sm text-slate-500"><RefreshCw className="mr-2 inline h-4 w-4 animate-spin" />Loading protected product records…</p>}
        {masterData && visibleProducts.length === 0 && <p className="p-5 text-sm text-slate-500">{showArchived ? 'No archived products.' : 'No active products yet.'}</p>}
      </div>
    </section>
    {editing && <ProductForm key={editing === 'new' ? 'new' : editing.productId} busy={busy} product={editing === 'new' ? null : editing} onClose={() => setEditing(null)} onSave={async (draft) => { await onSave(draft, editing === 'new' ? undefined : editing.productId); setEditing(null); }} />}
    {archiveTarget && <Dialog title="Archive product" onClose={() => setArchiveTarget(null)}><div id="archive-confirmation"><p className="text-sm leading-6 text-slate-600">Archive <strong>{archiveTarget.name}</strong>? It will stop appearing in new transactions, but its old records will remain safe.</p><div className="mt-5 flex gap-3"><button onClick={() => setArchiveTarget(null)} className="flex-1 rounded-xl border border-slate-300 px-4 py-3 text-sm font-semibold">Keep active</button><button id="confirm-product-archive" disabled={busy} onClick={() => void onSetArchived(archiveTarget, true).then(() => setArchiveTarget(null)).catch(() => undefined)} className="flex-1 rounded-xl bg-amber-600 px-4 py-3 text-sm font-semibold text-white disabled:opacity-50">Archive</button></div></div></Dialog>}
  </>;
}

function ProductForm({ busy, product, onClose, onSave }: { busy: boolean; product: ProductDto | null; onClose(): void; onSave(draft: ProductDraft): Promise<void> }) {
  const [name, setName] = useState(product?.name ?? '');
  const [unitCode, setUnitCode] = useState<ProductUnitCode>(product?.unit === 'Kg' ? 'Kg' : 'Liters');
  const [rateRupees, setRateRupees] = useState(paisaToRupeesInput(product?.defaultSaleRatePaisaPerUnit ?? null));
  const [inventoryTracking, setInventoryTracking] = useState(product?.inventoryTracking ?? true);
  const [notes, setNotes] = useState(product?.notes ?? '');
  const [localError, setLocalError] = useState<string | null>(null);
  return <Dialog title={product ? 'Edit product' : 'Add product'} onClose={onClose}><form onSubmit={(event) => { event.preventDefault(); setLocalError(null); try { if (rateRupees.trim()) rupeesToPaisa(rateRupees); } catch (failure) { setLocalError(errorMessage(failure)); return; } void onSave({ name, unitCode, rateRupees, inventoryTracking, notes }).catch(() => undefined); }}>
    <TextInput id="product-name" label="Product name" value={name} onChange={setName} />
    <label className="mb-4 block text-sm font-semibold text-slate-700">Unit<select id="product-unit" value={unitCode} onChange={(event) => setUnitCode(event.target.value as ProductUnitCode)} className="mt-1.5 w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5"><option value="Liters">Liters</option><option value="Kg">Kg</option></select></label>
    <TextInput id="product-rate" label="Default selling rate in PKR (optional)" value={rateRupees} onChange={setRateRupees} inputMode="decimal" required={false} />
    <label className="mb-4 block text-sm font-semibold text-slate-700">Notes (optional)<textarea id="product-notes" value={notes} onChange={(event) => setNotes(event.target.value)} rows={3} className="mt-1.5 w-full resize-none rounded-xl border border-slate-300 bg-white px-3 py-2.5" /></label>
    <label className="mb-5 flex items-center gap-3 rounded-xl bg-slate-50 p-3 text-sm font-semibold text-slate-700"><input id="product-inventory-tracking" type="checkbox" checked={inventoryTracking} onChange={(event) => setInventoryTracking(event.target.checked)} className="h-5 w-5 accent-emerald-700" />Track this product’s stock</label>
    {localError && <Notice message={localError} />}
    <button id="save-product-button" disabled={busy} className="w-full rounded-xl bg-emerald-700 px-4 py-3 font-semibold text-white disabled:cursor-wait disabled:opacity-60">{busy ? 'Saving…' : product ? 'Save changes' : 'Add product'}</button>
  </form></Dialog>;
}
