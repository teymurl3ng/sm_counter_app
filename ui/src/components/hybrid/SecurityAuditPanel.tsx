import { useMemo, useState } from 'react';
import type { SecurityAuditDto, UserAdministrationDto } from '../../bridge/contract.generated';
import { FilterButton } from './HybridUi';
import { auditActionLabel, plainCode, roleLabel } from './securityPresentation';

type AuditFilter = 'all' | 'failed' | 'denied';

export function SecurityAuditPanel({ entries, users }: { entries: ReadonlyArray<SecurityAuditDto>; users: ReadonlyArray<UserAdministrationDto> }) {
  const [filter, setFilter] = useState<AuditFilter>('all');
  const visible = useMemo(() => [...entries].reverse().filter((entry) => filter === 'all' || (filter === 'failed' ? entry.resultCode === 'failure' : entry.actionCode === 'permission_denied')), [entries, filter]);
  const userName = (id: string | null) => id ? users.find((user) => user.userId === id)?.displayName ?? `Account ${id}` : null;
  return <section>
    <div className="mb-4"><h2 className="text-lg font-bold">Security history</h2><p className="mt-1 text-sm leading-6 text-slate-600">A permanent record of sign-ins and account changes. Passphrases and recovery codes are never shown here.</p></div>
    <div className="mb-4 flex rounded-xl bg-slate-100 p-1">
      <FilterButton id="audit-filter-all" active={filter === 'all'} onClick={() => setFilter('all')}>All</FilterButton>
      <FilterButton id="audit-filter-failed" active={filter === 'failed'} onClick={() => setFilter('failed')}>Failed</FilterButton>
      <FilterButton id="audit-filter-denied" active={filter === 'denied'} onClick={() => setFilter('denied')}>Permission denied</FilterButton>
    </div>
    <div id="security-audit-list" className="space-y-3">
      {visible.length === 0 && <p className="rounded-2xl border border-slate-200 bg-white p-5 text-sm text-slate-500">No matching security events.</p>}
      {visible.map((entry) => <article key={entry.auditId} className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
        <div className="flex flex-wrap items-start justify-between gap-3"><div><h3 className="font-bold">{auditActionLabel(entry.actionCode)}</h3><p className="mt-1 text-xs text-slate-500">{formatTime(entry.occurredAt)}</p></div><span className={`rounded-full px-2.5 py-1 text-xs font-bold ${entry.resultCode === 'success' ? 'bg-emerald-50 text-emerald-800' : 'bg-rose-50 text-rose-800'}`}>{plainCode(entry.resultCode)}</span></div>
        <p className="mt-3 text-sm text-slate-600">By {entry.actor.displayName ?? 'The app'}{entry.actor.roleCode ? ` (${roleLabel(entry.actor.roleCode)})` : ''}{entry.targetUserId ? ` · Account: ${userName(entry.targetUserId)}` : ''}</p>
        {entry.reasonCode && <p className="mt-2 text-sm text-rose-700">Reason: {plainCode(entry.reasonCode)}</p>}
        {entry.details && <p className="mt-2 break-words rounded-lg bg-slate-50 p-2 font-mono text-xs text-slate-600">{entry.details}</p>}
      </article>)}
    </div>
  </section>;
}

function formatTime(value: string): string {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
}
