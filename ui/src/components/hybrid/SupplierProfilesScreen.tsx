import { useMemo, useState } from 'react';
import { Archive, Edit3, RefreshCw, RotateCcw, Search, Truck, UserPlus } from 'lucide-react';
import type { VendorDto } from '../../bridge/contract.generated';
import { formatPaisa } from '../../bridge/nativeBridge';
import { Dialog, FilterButton, Notice, SummaryCard, TextInput } from './HybridUi';
import type { SupplierDraft } from './hybridModels';

export function SupplierProfilesScreen({ busy, suppliers, error, notice, onSave, onSetActive }: {
  busy: boolean;
  suppliers: ReadonlyArray<VendorDto> | null;
  error: string | null;
  notice: string | null;
  onSave(draft: SupplierDraft, supplier?: VendorDto): Promise<void>;
  onSetActive(supplier: VendorDto, active: boolean): Promise<void>;
}) {
  const [showInactive, setShowInactive] = useState(false);
  const [search, setSearch] = useState('');
  const [editing, setEditing] = useState<VendorDto | 'new' | null>(null);
  const [deactivateTarget, setDeactivateTarget] = useState<VendorDto | null>(null);
  const allSuppliers = suppliers ?? [];
  const activeCount = allSuppliers.filter((supplier) => supplier.active).length;
  const inactiveCount = allSuppliers.length - activeCount;
  const visibleSuppliers = useMemo(() => {
    const query = search.trim().toLocaleLowerCase();
    return allSuppliers.filter((supplier) => {
      if (supplier.active === showInactive) return false;
      if (!query) return true;
      return supplier.name.toLocaleLowerCase().includes(query) ||
        (supplier.phone ?? '').toLocaleLowerCase().includes(query) ||
        (supplier.location ?? '').toLocaleLowerCase().includes(query);
    });
  }, [allSuppliers, search, showInactive]);

  return <>
    <section className="mb-5 grid grid-cols-2 gap-3">
      <SummaryCard label="Active suppliers" value={activeCount.toString()} />
      <SummaryCard label="Inactive suppliers" value={inactiveCount.toString()} />
    </section>
    {(notice || error) && <Notice message={notice ?? error ?? ''} success={Boolean(notice)} />}
    <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
      <div className="border-b border-slate-100 p-4">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex rounded-xl bg-slate-100 p-1">
            <FilterButton id="active-suppliers-tab" active={!showInactive} onClick={() => setShowInactive(false)}>Active ({activeCount})</FilterButton>
            <FilterButton id="inactive-suppliers-tab" active={showInactive} onClick={() => setShowInactive(true)}>Inactive ({inactiveCount})</FilterButton>
          </div>
          <button id="add-supplier-button" disabled={busy} onClick={() => setEditing('new')} className="flex items-center gap-2 rounded-xl bg-emerald-700 px-4 py-2.5 text-sm font-semibold text-white disabled:opacity-50"><UserPlus className="h-4 w-4" />Add supplier</button>
        </div>
        <label className="relative mt-3 block"><Search className="absolute left-3 top-3 h-4 w-4 text-slate-400" /><input id="supplier-search" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search name, phone, or location" className="w-full rounded-xl border border-slate-200 py-2.5 pl-9 pr-3 text-sm outline-none focus:border-emerald-600" /></label>
      </div>
      <div id="native-supplier-list" className="divide-y divide-slate-100">
        {visibleSuppliers.map((supplier) => <SupplierRow key={supplier.accountId} busy={busy} supplier={supplier} onEdit={() => setEditing(supplier)} onDeactivate={() => setDeactivateTarget(supplier)} onRestore={() => onSetActive(supplier, true)} />)}
        {suppliers === null && <p className="p-5 text-sm text-slate-500"><RefreshCw className="mr-2 inline h-4 w-4 animate-spin" />Loading protected supplier records…</p>}
        {suppliers !== null && visibleSuppliers.length === 0 && <p className="p-5 text-sm text-slate-500">{search.trim() ? 'No suppliers match this search.' : showInactive ? 'No inactive suppliers.' : 'No active suppliers yet.'}</p>}
      </div>
    </section>
    {editing && <SupplierForm key={editing === 'new' ? 'new-supplier' : editing.accountId} busy={busy} supplier={editing === 'new' ? null : editing} onClose={() => setEditing(null)} onSave={async (draft) => { await onSave(draft, editing === 'new' ? undefined : editing); setEditing(null); }} />}
    {deactivateTarget && <Dialog title="Deactivate supplier" onClose={() => setDeactivateTarget(null)}><div id="supplier-deactivate-confirmation"><p className="text-sm leading-6 text-slate-600">Deactivate <strong>{deactivateTarget.name}</strong>? They will not be available for new purchases or payments, but their balance and complete history will remain safe.</p><div className="mt-5 flex gap-3"><button onClick={() => setDeactivateTarget(null)} className="flex-1 rounded-xl border border-slate-300 px-4 py-3 text-sm font-semibold">Keep active</button><button id="confirm-supplier-deactivate" disabled={busy} onClick={() => void onSetActive(deactivateTarget, false).then(() => setDeactivateTarget(null)).catch(() => undefined)} className="flex-1 rounded-xl bg-amber-600 px-4 py-3 text-sm font-semibold text-white disabled:opacity-50">Deactivate</button></div></div></Dialog>}
  </>;
}

function SupplierRow({ busy, supplier, onEdit, onDeactivate, onRestore }: { busy: boolean; supplier: VendorDto; onEdit(): void; onDeactivate(): void; onRestore(): Promise<void> }) {
  return <article data-supplier-id={supplier.accountId} className="p-4"><div className="flex items-start justify-between gap-3"><div className="min-w-0">
    <div className="flex items-center gap-2"><Truck className="h-4 w-4 text-slate-400" /><h2 className="font-bold text-slate-900">{supplier.name}</h2></div>
    <p className={`mt-1 text-sm font-semibold ${supplier.canonicalBalancePaisa.startsWith('-') ? 'text-emerald-700' : supplier.canonicalBalancePaisa === '0' ? 'text-slate-600' : 'text-amber-700'}`}>Current balance: <span className="whitespace-nowrap tabular-nums">{formatPaisa(supplier.canonicalBalancePaisa)}</span></p>
    <p className="text-sm leading-5 text-slate-600">Calculated from saved purchases and payments; it cannot be edited here.</p>
    {supplier.phone && <p className="mt-2 text-sm text-slate-600">Phone: {supplier.phone}</p>}
    {supplier.location && <p className="text-sm text-slate-600">Location: {supplier.location}</p>}
    {supplier.notes && <p className="mt-2 text-sm text-slate-500">{supplier.notes}</p>}
  </div><div className="flex shrink-0 gap-2"><button id={`edit-supplier-${supplier.accountId}`} disabled={busy} onClick={onEdit} className="rounded-lg border border-slate-200 p-2 text-slate-600 disabled:opacity-50" aria-label={`Edit ${supplier.name}`}><Edit3 className="h-4 w-4" /></button>{supplier.active ? <button id={`deactivate-supplier-${supplier.accountId}`} disabled={busy} onClick={onDeactivate} className="rounded-lg border border-amber-200 p-2 text-amber-700 disabled:opacity-50" aria-label={`Deactivate ${supplier.name}`}><Archive className="h-4 w-4" /></button> : <button id={`restore-supplier-${supplier.accountId}`} disabled={busy} onClick={() => void onRestore().catch(() => undefined)} className="rounded-lg border border-emerald-200 p-2 text-emerald-700 disabled:opacity-50" aria-label={`Restore ${supplier.name}`}><RotateCcw className="h-4 w-4" /></button>}</div></div></article>;
}

function SupplierForm({ busy, supplier, onClose, onSave }: { busy: boolean; supplier: VendorDto | null; onClose(): void; onSave(draft: SupplierDraft): Promise<void> }) {
  const [name, setName] = useState(supplier?.name ?? '');
  const [phone, setPhone] = useState(supplier?.phone ?? '');
  const [location, setLocation] = useState(supplier?.location ?? '');
  const [notes, setNotes] = useState(supplier?.notes ?? '');
  return <Dialog title={supplier ? 'Edit supplier' : 'Add supplier'} onClose={onClose}><form onSubmit={(event) => { event.preventDefault(); void onSave({ name, phone, location, notes, active: supplier?.active ?? true }).catch(() => undefined); }}>
    <TextInput id="supplier-name" label="Supplier name" value={name} onChange={setName} autoComplete="organization" />
    <TextInput id="supplier-phone" label="Phone (optional)" value={phone} onChange={setPhone} autoComplete="tel" required={false} />
    <TextInput id="supplier-location" label="Location (optional)" value={location} onChange={setLocation} required={false} />
    <label className="mb-4 block text-sm font-semibold text-slate-700">Notes (optional)<textarea id="supplier-notes" value={notes} onChange={(event) => setNotes(event.target.value)} rows={3} className="mt-1.5 w-full resize-none rounded-xl border border-slate-300 bg-white px-3 py-2.5" /></label>
    <p className="mb-4 rounded-xl bg-sky-50 px-3 py-2 text-sm leading-5 text-sky-900">Supplier balance comes from purchases and payments. Milk buying rates are managed for the whole shop, not separately on each supplier.</p>
    <button id="save-supplier-button" disabled={busy} className="w-full rounded-xl bg-emerald-700 px-4 py-3 font-semibold text-white disabled:cursor-wait disabled:opacity-60">{busy ? 'Saving…' : supplier ? 'Save supplier' : 'Add supplier'}</button>
  </form></Dialog>;
}
