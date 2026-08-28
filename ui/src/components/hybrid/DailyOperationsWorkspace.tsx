import { useState } from 'react';
import type { CashierSummaryDto, DashboardDto, EventMutationResultDto, FinancialOverviewDto, PurchaseRecordedResultDto, SaleRecordedResultDto, TransactionReferencesDto } from '../../bridge/contract.generated';
import { formatPaisa } from '../../bridge/nativeBridge';
import { FilterButton, Notice, SummaryCard } from './HybridUi';
import type { ExpenseDraft, PaymentDraft, ProductionDraft, PurchaseDraft, SaleDraft } from './hybridModels';
import { ExpenseForm } from './ExpenseForm';
import { PaymentForm } from './PaymentForm';
import { ProductionForm } from './ProductionForm';
import { PurchaseForm } from './PurchaseForm';
import { SaleForm } from './SaleForm';

export function DailyOperationsWorkspace({ busy, canCollectCustomer, canExpense, canPaySupplier, canProduce, canPurchase, canSell, dashboard, cashierSummary, financialOverview, references, error, notice, onRecordExpense, onRecordPayment, onRecordProduction, onRecordPurchase, onRecordSale }: {
  busy: boolean;
  canCollectCustomer: boolean;
  canExpense: boolean;
  canPaySupplier: boolean;
  canProduce: boolean;
  canPurchase: boolean;
  canSell: boolean;
  dashboard: DashboardDto | null;
  cashierSummary: CashierSummaryDto | null;
  financialOverview: FinancialOverviewDto | null;
  references: TransactionReferencesDto | null;
  error: string | null;
  notice: string | null;
  onRecordExpense(draft: ExpenseDraft): Promise<EventMutationResultDto>;
  onRecordPayment(draft: PaymentDraft): Promise<EventMutationResultDto>;
  onRecordProduction(draft: ProductionDraft): Promise<EventMutationResultDto>;
  onRecordPurchase(draft: PurchaseDraft): Promise<PurchaseRecordedResultDto>;
  onRecordSale(draft: SaleDraft): Promise<SaleRecordedResultDto>;
}) {
  const [task, setTask] = useState<'purchase' | 'sale' | 'production' | 'expense' | 'payments'>(canPurchase ? 'purchase' : canSell ? 'sale' : canProduce ? 'production' : canExpense ? 'expense' : 'payments');
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
    {[canPurchase, canSell, canProduce, canExpense, canPaySupplier || canCollectCustomer].filter(Boolean).length > 1 && <div className="mb-5 flex rounded-xl bg-slate-200 p-1">{canPurchase && <FilterButton id="purchase-work-tab" active={task === 'purchase'} onClick={() => setTask('purchase')}>Receive milk</FilterButton>}{canSell && <FilterButton id="sale-work-tab" active={task === 'sale'} onClick={() => setTask('sale')}>Record sale</FilterButton>}{canProduce && <FilterButton id="production-work-tab" active={task === 'production'} onClick={() => setTask('production')}>Make yogurt</FilterButton>}{canExpense && <FilterButton id="expense-work-tab" active={task === 'expense'} onClick={() => setTask('expense')}>Expense</FilterButton>}{(canPaySupplier || canCollectCustomer) && <FilterButton id="payments-work-tab" active={task === 'payments'} onClick={() => setTask('payments')}>Payments</FilterButton>}</div>}
    {task === 'sale' && canSell && cashierSummary && <section className="mb-5 grid grid-cols-2 gap-3 sm:grid-cols-3"><SummaryCard label="Sales before delivery charges" value={formatPaisa(cashierSummary.salesValuePaisa)} />{cashierSummary.paymentAccounts.map((account) => <SummaryCard key={account.accountId} label={`${account.name} balance`} value={formatPaisa(account.canonicalBalancePaisa)} />)}</section>}
    {task === 'expense' && canExpense && financialOverview && <section className="mb-5 grid grid-cols-2 gap-3 sm:grid-cols-3"><SummaryCard label="Shop expenses today" value={formatPaisa(financialOverview.totals.operatingExpensesPaisa)} /></section>}
    {task === 'purchase' && canPurchase ? <PurchaseForm busy={busy} references={references} onRecordPurchase={onRecordPurchase} />
      : task === 'sale' && canSell ? <SaleForm busy={busy} references={references} onRecordSale={onRecordSale} />
        : task === 'production' && canProduce ? <ProductionForm busy={busy} milkAvailable={milk?.quantityOnHand ?? null} onRecordProduction={onRecordProduction} />
          : task === 'expense' && canExpense ? <ExpenseForm busy={busy} references={references} onRecordExpense={onRecordExpense} />
            : <PaymentForm busy={busy} canCollectCustomer={canCollectCustomer} canPaySupplier={canPaySupplier} references={references} financialOverview={financialOverview} onRecordPayment={onRecordPayment} />}
  </>;
}
