import { ArrowLeft, HandCoins } from 'lucide-react';
import type { AccountLedgerDto } from '../../bridge/contract.generated';
import { formatPaisa } from '../../bridge/nativeBridge';
import { balanceMeaning, eventLabel, ledgerKind, movementMeaning } from './ledgerPresentation';

export function AccountLedgerDetail({ ledger, canRecordPayment, onBack, onRecordPayment }: {
  ledger: AccountLedgerDto;
  canRecordPayment: boolean;
  onBack(): void;
  onRecordPayment(): void;
}) {
  const kind = ledgerKind(ledger);
  const balance = balanceMeaning(kind, ledger.currentBalancePaisa);
  return <section id="account-ledger-detail" className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
    <div className="border-b border-slate-100 p-5"><button id="ledger-back-button" onClick={onBack} className="mb-3 flex items-center gap-1 text-sm font-semibold text-emerald-800"><ArrowLeft className="h-4 w-4" />All {kind}s</button><div className="grid gap-3 sm:grid-cols-[minmax(0,1fr)_auto] sm:items-start"><div><h2 className="text-xl font-bold">{ledger.displayName}</h2><p className="mt-1 text-xs text-slate-500">{kind === 'customer' ? 'Customer' : 'Supplier'} account #{ledger.accountId}</p></div><div className="sm:text-right"><p className={`whitespace-nowrap text-xl font-bold tabular-nums ${balance.tone}`}>{balance.amount}</p><p className="mt-1 text-xs text-slate-500">{balance.label}</p></div></div>
      {canRecordPayment && <button id={`account-payment-${ledger.accountId}`} onClick={onRecordPayment} className="mt-4 flex items-center gap-2 rounded-xl bg-emerald-700 px-4 py-2.5 text-sm font-semibold text-white"><HandCoins className="h-4 w-4" />{kind === 'supplier' ? 'Record supplier payment' : 'Record customer payment'}</button>}
      {!canRecordPayment && <p className="mt-3 text-sm text-slate-600">This account is inactive. Its history remains visible, but new payments are disabled.</p>}
    </div>
    <div id="account-ledger-entries" className="divide-y divide-slate-100">
      {ledger.entries.length === 0 && <p className="p-5 text-sm text-slate-500">No money movements have been recorded for this account.</p>}
      {[...ledger.entries].reverse().map((entry) => {
        const entryBalance = balanceMeaning(kind, entry.balanceAfterPaisa);
        return <article key={entry.logId} id={`ledger-entry-${entry.eventId}`} className={`p-4 ${entry.isSuperseded ? 'bg-slate-50 text-slate-500' : ''}`}><div className="grid gap-2 sm:grid-cols-[minmax(0,1fr)_auto] sm:items-start"><div><h3 className="text-sm font-bold">{eventLabel(entry.eventTypeCode)}</h3><p className="mt-1 text-xs text-slate-500">{entry.occurredAt.slice(0, 10)} · Record #{entry.eventId}{entry.isCorrection ? ` · Corrects #${entry.correctsEventId}` : ''}{entry.isSuperseded ? ' · Replaced by correction' : ''}</p></div><div className="sm:text-right"><p className="whitespace-nowrap text-sm font-bold text-slate-800 tabular-nums">{formatPaisa(entry.movementPaisa)}</p><p className="mt-1 text-xs text-slate-500">{movementMeaning(kind, entry.movementPaisa)}</p></div></div>{entry.notes && <p className="mt-2 rounded-lg bg-slate-50 p-2 text-sm">{entry.notes}</p>}<p className="mt-2 text-sm text-slate-600">After this: <strong className={entryBalance.tone}>{entryBalance.label} · <span className="whitespace-nowrap tabular-nums">{entryBalance.amount}</span></strong></p></article>;
      })}
    </div>
  </section>;
}
