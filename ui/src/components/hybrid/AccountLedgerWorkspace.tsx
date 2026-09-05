import { useState } from 'react';
import type { AccountLedgerDto, EventMutationResultDto, FinancialOverviewDto, Int64String, TransactionReferencesDto } from '../../bridge/contract.generated';
import { AccountBalanceList } from './AccountBalanceList';
import { AccountLedgerDetail } from './AccountLedgerDetail';
import { FilterButton, Notice } from './HybridUi';
import { PaymentForm } from './PaymentForm';
import { errorMessage, type PaymentDraft } from './hybridModels';
import type { LedgerKind } from './ledgerPresentation';

type WorkspaceArea = 'history' | 'payment';
type PaymentTarget = { kind: PaymentDraft['kind']; accountId: Int64String } | null;

export function AccountLedgerWorkspace({ busy, overview, references, error, notice, onLoadLedger, onRecordPayment }: {
  busy: boolean;
  overview: FinancialOverviewDto | null;
  references: TransactionReferencesDto | null;
  error: string | null;
  notice: string | null;
  onLoadLedger(accountId: Int64String): Promise<AccountLedgerDto>;
  onRecordPayment(draft: PaymentDraft): Promise<EventMutationResultDto>;
}) {
  const [area, setArea] = useState<WorkspaceArea>('history');
  const [kind, setKind] = useState<LedgerKind>('customer');
  const [search, setSearch] = useState('');
  const [ledger, setLedger] = useState<AccountLedgerDto | null>(null);
  const [loading, setLoading] = useState(false);
  const [localError, setLocalError] = useState<string | null>(null);
  const [paymentTarget, setPaymentTarget] = useState<PaymentTarget>(null);

  if (!overview || !references) return <p className="rounded-2xl border border-slate-200 bg-white p-5 text-sm text-slate-500">Loading customer and supplier accounts…</p>;
  const accounts = kind === 'customer' ? overview.customers : overview.vendors;
  const activeIds = new Set((kind === 'customer' ? references.customers : references.vendors).map((item) => item.accountId));
  const openLedger = async (accountId: Int64String) => {
    setLoading(true); setLocalError(null);
    try { setLedger(await onLoadLedger(accountId)); }
    catch (failure) { setLocalError(errorMessage(failure)); }
    finally { setLoading(false); }
  };
  const openPayment = (target: PaymentTarget) => { setPaymentTarget(target); setArea('payment'); };
  const recordAndRefresh = async (draft: PaymentDraft) => {
    const result = await onRecordPayment(draft);
    if (ledger?.accountId === draft.partyAccountId) setLedger(await onLoadLedger(draft.partyAccountId));
    return result;
  };

  return <div className="space-y-5">
    {(notice || error || localError) && <Notice message={localError ?? error ?? notice ?? ''} success={Boolean(notice && !error && !localError)} />}
    <div className="grid grid-cols-2 rounded-xl bg-slate-200 p-1"><FilterButton id="accounts-history-tab" active={area === 'history'} onClick={() => setArea('history')}>Balances and history</FilterButton><FilterButton id="accounts-payments-tab" active={area === 'payment'} onClick={() => openPayment(null)}>Record payment</FilterButton></div>
    {area === 'payment' ? <PaymentForm key={`${paymentTarget?.kind ?? 'general'}-${paymentTarget?.accountId ?? ''}`} busy={busy} canCollectCustomer canPaySupplier references={references} financialOverview={overview} initialKind={paymentTarget?.kind} initialPartyAccountId={paymentTarget?.accountId ?? ''} onRecordPayment={recordAndRefresh} /> : <>
      <div className="grid grid-cols-2 rounded-xl bg-slate-200/70 p-1"><FilterButton id="account-customer-tab" active={kind === 'customer'} onClick={() => { setKind('customer'); setLedger(null); setSearch(''); }}>Customers</FilterButton><FilterButton id="account-supplier-tab" active={kind === 'supplier'} onClick={() => { setKind('supplier'); setLedger(null); setSearch(''); }}>Suppliers</FilterButton></div>
      {loading ? <p className="rounded-2xl border border-slate-200 bg-white p-5 text-sm text-slate-500">Loading account history…</p> : ledger ? <AccountLedgerDetail ledger={ledger} canRecordPayment={activeIds.has(ledger.accountId)} onBack={() => setLedger(null)} onRecordPayment={() => openPayment({ kind, accountId: ledger.accountId })} /> : <AccountBalanceList kind={kind} accounts={accounts} activeIds={activeIds} search={search} selectedId={null} onSearch={setSearch} onSelect={(accountId) => void openLedger(accountId)} />}
    </>}
  </div>;
}
