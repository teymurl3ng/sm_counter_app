import { useMemo, useState } from 'react';
import { Archive, Edit3, RefreshCw, RotateCcw, Search, UserPlus } from 'lucide-react';
import type { CustomerDto } from '../../bridge/contract.generated';
import { formatPaisa, paisaToRupeesInput, rupeesToPaisa, validateDecimalInput } from '../../bridge/nativeBridge';
import { Dialog, FilterButton, Notice, SummaryCard, TextInput } from './HybridUi';
import { errorMessage, type CustomerDraft } from './hybridModels';

export function CustomerProfilesScreen({ busy, customers, error, notice, onSave, onSetActive }: {
  busy: boolean;
  customers: ReadonlyArray<CustomerDto> | null;
  error: string | null;
  notice: string | null;
  onSave(draft: CustomerDraft, customer?: CustomerDto): Promise<void>;
  onSetActive(customer: CustomerDto, active: boolean): Promise<void>;
}) {
  const [showInactive, setShowInactive] = useState(false);
  const [search, setSearch] = useState('');
  const [editing, setEditing] = useState<CustomerDto | 'new' | null>(null);
  const [deactivateTarget, setDeactivateTarget] = useState<CustomerDto | null>(null);
  const allCustomers = customers ?? [];
  const activeCount = allCustomers.filter((customer) => customer.active).length;
  const inactiveCount = allCustomers.length - activeCount;
  const visibleCustomers = useMemo(() => {
    const query = search.trim().toLocaleLowerCase();
    return allCustomers.filter((customer) => {
      if (customer.active === showInactive) return false;
      if (!query) return true;
      return customer.name.toLocaleLowerCase().includes(query) ||
        (customer.phone ?? '').toLocaleLowerCase().includes(query) ||
        (customer.areaRoute ?? '').toLocaleLowerCase().includes(query);
    });
  }, [allCustomers, search, showInactive]);

  return <>
    <section className="mb-5 grid grid-cols-2 gap-3">
      <SummaryCard label="Active customers" value={activeCount.toString()} />
      <SummaryCard label="Inactive customers" value={inactiveCount.toString()} />
    </section>
    {(notice || error) && <Notice message={notice ?? error ?? ''} success={Boolean(notice)} />}
    <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
      <div className="border-b border-slate-100 p-4">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex rounded-xl bg-slate-100 p-1">
            <FilterButton id="active-customers-tab" active={!showInactive} onClick={() => setShowInactive(false)}>Active ({activeCount})</FilterButton>
            <FilterButton id="inactive-customers-tab" active={showInactive} onClick={() => setShowInactive(true)}>Inactive ({inactiveCount})</FilterButton>
          </div>
          <button id="add-customer-button" disabled={busy} onClick={() => setEditing('new')} className="flex items-center gap-2 rounded-xl bg-emerald-700 px-4 py-2.5 text-sm font-semibold text-white disabled:opacity-50"><UserPlus className="h-4 w-4" />Add customer</button>
        </div>
        <label className="relative mt-3 block"><Search className="absolute left-3 top-3 h-4 w-4 text-slate-400" /><input id="customer-search" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search name, phone, or route" className="w-full rounded-xl border border-slate-200 py-2.5 pl-9 pr-3 text-sm outline-none focus:border-emerald-600" /></label>
      </div>
      <div id="native-customer-list" className="divide-y divide-slate-100">
        {visibleCustomers.map((customer) => <CustomerRow key={customer.accountId} busy={busy} customer={customer} onEdit={() => setEditing(customer)} onDeactivate={() => setDeactivateTarget(customer)} onRestore={() => onSetActive(customer, true)} />)}
        {customers === null && <p className="p-5 text-sm text-slate-500"><RefreshCw className="mr-2 inline h-4 w-4 animate-spin" />Loading protected customer records…</p>}
        {customers !== null && visibleCustomers.length === 0 && <p className="p-5 text-sm text-slate-500">{search.trim() ? 'No customers match this search.' : showInactive ? 'No inactive customers.' : 'No active customers yet.'}</p>}
      </div>
    </section>
    {editing && <CustomerForm key={editing === 'new' ? 'new-customer' : editing.accountId} busy={busy} customer={editing === 'new' ? null : editing} onClose={() => setEditing(null)} onSave={async (draft) => { await onSave(draft, editing === 'new' ? undefined : editing); setEditing(null); }} />}
    {deactivateTarget && <Dialog title="Deactivate customer" onClose={() => setDeactivateTarget(null)}><div id="customer-deactivate-confirmation"><p className="text-sm leading-6 text-slate-600">Deactivate <strong>{deactivateTarget.name}</strong>? They will not be available for new credit sales, but their balance and complete history will remain safe.</p><div className="mt-5 flex gap-3"><button onClick={() => setDeactivateTarget(null)} className="flex-1 rounded-xl border border-slate-300 px-4 py-3 text-sm font-semibold">Keep active</button><button id="confirm-customer-deactivate" disabled={busy} onClick={() => void onSetActive(deactivateTarget, false).then(() => setDeactivateTarget(null)).catch(() => undefined)} className="flex-1 rounded-xl bg-amber-600 px-4 py-3 text-sm font-semibold text-white disabled:opacity-50">Deactivate</button></div></div></Dialog>}
  </>;
}

function CustomerRow({ busy, customer, onEdit, onDeactivate, onRestore }: { busy: boolean; customer: CustomerDto; onEdit(): void; onDeactivate(): void; onRestore(): Promise<void> }) {
  return <article data-customer-id={customer.accountId} className="p-4"><div className="flex items-start justify-between gap-3"><div className="min-w-0">
    <h2 className="font-bold text-slate-900">{customer.name}</h2>
    <p className={`mt-1 text-sm font-semibold ${customer.canonicalBalancePaisa.startsWith('-') ? 'text-emerald-700' : customer.canonicalBalancePaisa === '0' ? 'text-slate-600' : 'text-amber-700'}`}>Current balance: <span className="whitespace-nowrap tabular-nums">{formatPaisa(customer.canonicalBalancePaisa)}</span></p>
    <p className="text-sm leading-5 text-slate-600">Calculated from saved transactions; it cannot be edited here.</p>
    {customer.phone && <p className="mt-2 text-sm text-slate-600">Phone: {customer.phone}</p>}
    {customer.areaRoute && <p className="text-sm text-slate-600">Route/area: {customer.areaRoute}</p>}
    {customer.address && <p className="text-sm text-slate-600">Address: {customer.address}</p>}
    {customer.creditLimitPaisa !== null && <p className="text-sm text-slate-600">Credit limit: {formatPaisa(customer.creditLimitPaisa)}</p>}
    {customer.dailyQuota !== null && <p className="text-sm text-slate-600">Usual daily quantity: {customer.dailyQuota}</p>}
    {customer.notes && <p className="mt-2 text-sm text-slate-500">{customer.notes}</p>}
  </div><div className="flex shrink-0 gap-2"><button id={`edit-customer-${customer.accountId}`} disabled={busy} onClick={onEdit} className="rounded-lg border border-slate-200 p-2 text-slate-600 disabled:opacity-50" aria-label={`Edit ${customer.name}`}><Edit3 className="h-4 w-4" /></button>{customer.active ? <button id={`deactivate-customer-${customer.accountId}`} disabled={busy} onClick={onDeactivate} className="rounded-lg border border-amber-200 p-2 text-amber-700 disabled:opacity-50" aria-label={`Deactivate ${customer.name}`}><Archive className="h-4 w-4" /></button> : <button id={`restore-customer-${customer.accountId}`} disabled={busy} onClick={() => void onRestore().catch(() => undefined)} className="rounded-lg border border-emerald-200 p-2 text-emerald-700 disabled:opacity-50" aria-label={`Restore ${customer.name}`}><RotateCcw className="h-4 w-4" /></button>}</div></div></article>;
}

function CustomerForm({ busy, customer, onClose, onSave }: { busy: boolean; customer: CustomerDto | null; onClose(): void; onSave(draft: CustomerDraft): Promise<void> }) {
  const [name, setName] = useState(customer?.name ?? '');
  const [phone, setPhone] = useState(customer?.phone ?? '');
  const [areaRoute, setAreaRoute] = useState(customer?.areaRoute ?? '');
  const [address, setAddress] = useState(customer?.address ?? '');
  const [notes, setNotes] = useState(customer?.notes ?? '');
  const [creditLimitRupees, setCreditLimitRupees] = useState(paisaToRupeesInput(customer?.creditLimitPaisa ?? null));
  const [dailyQuota, setDailyQuota] = useState(customer?.dailyQuota ?? '');
  const [localError, setLocalError] = useState<string | null>(null);
  return <Dialog title={customer ? 'Edit customer' : 'Add customer'} onClose={onClose}><form onSubmit={(event) => { event.preventDefault(); setLocalError(null); try { if (creditLimitRupees.trim()) rupeesToPaisa(creditLimitRupees); if (dailyQuota.trim()) validateDecimalInput(dailyQuota, 'the daily quantity'); } catch (failure) { setLocalError(errorMessage(failure)); return; } void onSave({ name, phone, areaRoute, address, notes, creditLimitRupees, dailyQuota, active: customer?.active ?? true }).catch(() => undefined); }}>
    <TextInput id="customer-name" label="Customer name" value={name} onChange={setName} autoComplete="name" />
    <TextInput id="customer-phone" label="Phone (optional)" value={phone} onChange={setPhone} autoComplete="tel" required={false} />
    <TextInput id="customer-route" label="Route or area (optional)" value={areaRoute} onChange={setAreaRoute} required={false} />
    <label className="mb-4 block text-sm font-semibold text-slate-700">Address (optional)<textarea id="customer-address" value={address} onChange={(event) => setAddress(event.target.value)} rows={2} className="mt-1.5 w-full resize-none rounded-xl border border-slate-300 bg-white px-3 py-2.5" /></label>
    <TextInput id="customer-credit-limit" label="Credit limit in PKR (optional)" value={creditLimitRupees} onChange={setCreditLimitRupees} inputMode="decimal" required={false} />
    <TextInput id="customer-daily-quota" label="Usual daily quantity (optional)" value={dailyQuota} onChange={setDailyQuota} inputMode="decimal" required={false} />
    <label className="mb-4 block text-sm font-semibold text-slate-700">Notes (optional)<textarea id="customer-notes" value={notes} onChange={(event) => setNotes(event.target.value)} rows={3} className="mt-1.5 w-full resize-none rounded-xl border border-slate-300 bg-white px-3 py-2.5" /></label>
    <p className="mb-4 rounded-xl bg-sky-50 px-3 py-2 text-sm leading-5 text-sky-900">Customer balance is calculated from transactions and cannot be entered or changed on this form.</p>
    {localError && <Notice message={localError} />}
    <button id="save-customer-button" disabled={busy} className="w-full rounded-xl bg-emerald-700 px-4 py-3 font-semibold text-white disabled:cursor-wait disabled:opacity-60">{busy ? 'Saving…' : customer ? 'Save customer' : 'Add customer'}</button>
  </form></Dialog>;
}
