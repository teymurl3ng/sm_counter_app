import { Search } from 'lucide-react';
import type { AccountPositionDto, Int64String } from '../../bridge/contract.generated';
import { balanceMeaning, type LedgerKind } from './ledgerPresentation';

export function AccountBalanceList({ kind, accounts, activeIds, search, selectedId, onSearch, onSelect }: {
  kind: LedgerKind;
  accounts: ReadonlyArray<AccountPositionDto>;
  activeIds: ReadonlySet<Int64String>;
  search: string;
  selectedId: Int64String | null;
  onSearch(value: string): void;
  onSelect(accountId: Int64String): void;
}) {
  const shown = accounts.filter((item) => item.name.toLowerCase().includes(search.trim().toLowerCase()));
  return <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
    <div className="border-b border-slate-100 p-4"><label className="relative block"><Search className="absolute left-3 top-3 h-4 w-4 text-slate-400" /><span className="sr-only">Search {kind}s</span><input id="account-ledger-search" value={search} onChange={(event) => onSearch(event.target.value)} placeholder={`Search ${kind} name`} className="w-full rounded-xl border border-slate-300 py-2.5 pl-9 pr-3 text-sm" /></label></div>
    <div id="account-balance-list" className="divide-y divide-slate-100">
      {shown.length === 0 && <p className="p-5 text-sm text-slate-500">No {kind} accounts found.</p>}
      {shown.map((account) => {
        const meaning = balanceMeaning(kind, account.canonicalBalancePaisa);
        const active = activeIds.has(account.accountId);
        return <button key={account.accountId} id={`ledger-account-${account.accountId}`} onClick={() => onSelect(account.accountId)} className={`flex w-full items-center justify-between gap-3 p-4 text-left ${selectedId === account.accountId ? 'bg-emerald-50' : 'bg-white'}`}>
          <span><strong className="block text-sm text-slate-900">{account.name}</strong><span className="mt-1 block text-xs text-slate-500">{active ? 'Active' : 'Inactive · history retained'}</span></span>
          <span className="text-right"><span className={`block whitespace-nowrap text-sm font-bold tabular-nums ${meaning.tone}`}>{meaning.amount}</span><span className="mt-1 block text-xs text-slate-500">{meaning.label}</span></span>
        </button>;
      })}
    </div>
  </section>;
}
