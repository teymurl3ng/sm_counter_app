import { useMemo, useState } from 'react';
import { Archive, Plus, RefreshCw, RotateCcw, Search } from 'lucide-react';
import type { ExpenseCategoryDto } from '../../bridge/contract.generated';
import { Dialog, FilterButton, Notice, SummaryCard, TextInput } from './HybridUi';

export function ExpenseCategoriesPanel({ busy, categories, error, notice, onCreate, onSetArchived }: {
  busy: boolean;
  categories: ReadonlyArray<ExpenseCategoryDto> | null;
  error: string | null;
  notice: string | null;
  onCreate(displayName: string): Promise<void>;
  onSetArchived(category: ExpenseCategoryDto, archived: boolean): Promise<void>;
}) {
  const [showInactive, setShowInactive] = useState(false);
  const [search, setSearch] = useState('');
  const [adding, setAdding] = useState(false);
  const [archiveTarget, setArchiveTarget] = useState<ExpenseCategoryDto | null>(null);
  const all = categories ?? [];
  const activeCount = all.filter((category) => category.active).length;
  const visible = useMemo(() => {
    const query = search.trim().toLocaleLowerCase();
    return all.filter((category) => category.active !== showInactive && (!query || category.displayName.toLocaleLowerCase().includes(query)));
  }, [all, search, showInactive]);
  return <>
    <section className="mb-5 grid grid-cols-2 gap-3"><SummaryCard label="Active categories" value={activeCount.toString()} /><SummaryCard label="Inactive categories" value={(all.length - activeCount).toString()} /></section>
    {(notice || error) && <Notice message={notice ?? error ?? ''} success={Boolean(notice)} />}
    <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
      <div className="border-b border-slate-100 p-4">
        <div className="flex flex-wrap items-center justify-between gap-3"><div className="flex rounded-xl bg-slate-100 p-1"><FilterButton id="active-expense-categories-tab" active={!showInactive} onClick={() => setShowInactive(false)}>Active ({activeCount})</FilterButton><FilterButton id="inactive-expense-categories-tab" active={showInactive} onClick={() => setShowInactive(true)}>Inactive ({all.length - activeCount})</FilterButton></div><button id="add-expense-category-button" disabled={busy} onClick={() => setAdding(true)} className="flex items-center gap-2 rounded-xl bg-emerald-700 px-4 py-2.5 text-sm font-semibold text-white disabled:opacity-50"><Plus className="h-4 w-4" />Add category</button></div>
        <label className="relative mt-3 block"><Search className="absolute left-3 top-3 h-4 w-4 text-slate-400" /><input id="expense-category-search" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search expense categories" className="w-full rounded-xl border border-slate-200 py-2.5 pl-9 pr-3 text-sm outline-none focus:border-emerald-600" /></label>
      </div>
      <div id="native-expense-category-list" className="divide-y divide-slate-100">
        {visible.map((category) => <article key={category.accountId} data-expense-category-id={category.accountId} className="flex items-center justify-between gap-3 p-4"><div><h2 className="font-bold">{category.displayName}</h2><p className="text-xs text-slate-500">Used to classify shop expenses; past entries remain unchanged.</p></div>{category.active ? <button id={`archive-expense-category-${category.accountId}`} disabled={busy} onClick={() => setArchiveTarget(category)} className="rounded-lg border border-amber-200 p-2 text-amber-700 disabled:opacity-50" aria-label={`Deactivate ${category.displayName}`}><Archive className="h-4 w-4" /></button> : <button id={`restore-expense-category-${category.accountId}`} disabled={busy} onClick={() => void onSetArchived(category, false).catch(() => undefined)} className="rounded-lg border border-emerald-200 p-2 text-emerald-700 disabled:opacity-50" aria-label={`Restore ${category.displayName}`}><RotateCcw className="h-4 w-4" /></button>}</article>)}
        {categories === null && <p className="p-5 text-sm text-slate-500"><RefreshCw className="mr-2 inline h-4 w-4 animate-spin" />Loading protected categories…</p>}
        {categories !== null && visible.length === 0 && <p className="p-5 text-sm text-slate-500">{search.trim() ? 'No categories match this search.' : showInactive ? 'No inactive categories.' : 'No active expense categories yet.'}</p>}
      </div>
    </section>
    {adding && <AddCategoryDialog busy={busy} onClose={() => setAdding(false)} onCreate={async (name) => { await onCreate(name); setAdding(false); }} />}
    {archiveTarget && <Dialog title="Deactivate expense category" onClose={() => setArchiveTarget(null)}><div id="expense-category-archive-confirmation"><p className="text-sm leading-6 text-slate-600">Deactivate <strong>{archiveTarget.displayName}</strong>? It will disappear from new expense entry choices, but every past expense will keep this category.</p><div className="mt-5 flex gap-3"><button onClick={() => setArchiveTarget(null)} className="flex-1 rounded-xl border border-slate-300 px-4 py-3 text-sm font-semibold">Keep active</button><button id="confirm-expense-category-archive" disabled={busy} onClick={() => void onSetArchived(archiveTarget, true).then(() => setArchiveTarget(null)).catch(() => undefined)} className="flex-1 rounded-xl bg-amber-600 px-4 py-3 text-sm font-semibold text-white disabled:opacity-50">Deactivate</button></div></div></Dialog>}
  </>;
}

function AddCategoryDialog({ busy, onClose, onCreate }: { busy: boolean; onClose(): void; onCreate(name: string): Promise<void> }) {
  const [name, setName] = useState('');
  return <Dialog title="Add expense category" onClose={onClose}><form onSubmit={(event) => { event.preventDefault(); void onCreate(name).catch(() => undefined); }}><TextInput id="expense-category-name" label="Category name" value={name} onChange={setName} /><p className="mb-4 rounded-xl bg-sky-50 px-3 py-2 text-sm leading-5 text-sky-900">Examples: Electricity, Rent, Fuel, Cleaning. Categories cannot be renamed because old expenses must remain clear; deactivate one and create a replacement when needed.</p><button id="save-expense-category-button" disabled={busy} className="w-full rounded-xl bg-emerald-700 px-4 py-3 font-semibold text-white disabled:opacity-50">{busy ? 'Saving…' : 'Add expense category'}</button></form></Dialog>;
}
