import { useState } from 'react';
import type { CashierSummaryDto, DashboardDto, PurchaseRecordedResultDto, SaleRecordedResultDto, TransactionReferencesDto } from '../../bridge/contract.generated';
import { formatPaisa } from '../../bridge/nativeBridge';
import { FilterButton, Notice, SummaryCard } from './HybridUi';
import type { PurchaseDraft, SaleDraft } from './hybridModels';
import { PurchaseForm } from './PurchaseForm';
import { SaleForm } from './SaleForm';

export function DailyOperationsWorkspace({ busy, canPurchase, canSell, dashboard, cashierSummary, references, error, notice, onRecordPurchase, onRecordSale }: {
  busy: boolean;
  canPurchase: boolean;
  canSell: boolean;
  dashboard: DashboardDto | null;
  cashierSummary: CashierSummaryDto | null;
  references: TransactionReferencesDto | null;
  error: string | null;
  notice: string | null;
  onRecordPurchase(draft: PurchaseDraft): Promise<PurchaseRecordedResultDto>;
  onRecordSale(draft: SaleDraft): Promise<SaleRecordedResultDto>;
}) {
  const [task, setTask] = useState<'purchase' | 'sale'>(canPurchase ? 'purchase' : 'sale');
  const milk = dashboard?.stock.find((item) => item.name.toLowerCase() === 'milk');
  const yogurt = dashboard?.stock.find((item) => item.name.toLowerCase() === 'yogurt');
  return <>
    <section className="mb-5 grid grid-cols-2 gap-3 sm:grid-cols-4">
      <SummaryCard label="Milk in stock" value={milk ? `${milk.quantityOnHand} ${milk.unit}` : '—'} />
      <SummaryCard label="Yogurt in stock" value={yogurt ? `${yogurt.quantityOnHand} ${yogurt.unit}` : '—'} />
      <SummaryCard label="Milk bought today" value={dashboard ? `${dashboard.totals.purchasedMilkLiters} L` : '—'} />
      <SummaryCard label="Quantity sold today" value={dashboard?.totals.saleQuantity ?? '—'} />
    </section>
    {(notice || error) && <Notice message={notice ?? error ?? ''} success={Boolean(notice)} />}
    {canPurchase && canSell && <div className="mb-5 flex rounded-xl bg-slate-200 p-1"><FilterButton id="purchase-work-tab" active={task === 'purchase'} onClick={() => setTask('purchase')}>Receive milk</FilterButton><FilterButton id="sale-work-tab" active={task === 'sale'} onClick={() => setTask('sale')}>Record sale</FilterButton></div>}
    {task === 'sale' && canSell && cashierSummary && <section className="mb-5 grid grid-cols-2 gap-3 sm:grid-cols-3"><SummaryCard label="Sales before delivery charges" value={formatPaisa(cashierSummary.salesValuePaisa)} />{cashierSummary.paymentAccounts.map((account) => <SummaryCard key={account.accountId} label={`${account.name} balance`} value={formatPaisa(account.canonicalBalancePaisa)} />)}</section>}
    {task === 'purchase' && canPurchase
      ? <PurchaseForm busy={busy} references={references} onRecordPurchase={onRecordPurchase} />
      : <SaleForm busy={busy} references={references} onRecordSale={onRecordSale} />}
  </>;
}
