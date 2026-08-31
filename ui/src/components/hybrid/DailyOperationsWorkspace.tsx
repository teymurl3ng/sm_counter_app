import { useState } from 'react';
import type { CashierSummaryDto, DashboardDto, EventMutationResultDto, FinancialOverviewDto, PurchaseRecordedResultDto, SaleRecordedResultDto, SpoilageDto, TransactionReferencesDto } from '../../bridge/contract.generated';
import { formatPaisa } from '../../bridge/nativeBridge';
import { FilterButton, Notice, SummaryCard } from './HybridUi';
import type { ExpenseDraft, PaymentDraft, ProductionDraft, PurchaseDraft, SalaryPaymentDraft, SaleDraft, SpoilageDraft } from './hybridModels';
import { ExpenseForm } from './ExpenseForm';
import { PaymentForm } from './PaymentForm';
import { ProductionForm } from './ProductionForm';
import { PurchaseForm } from './PurchaseForm';
import { SaleForm } from './SaleForm';
import { SalaryPaymentForm } from './SalaryPaymentForm';
import { SpoilageForm } from './SpoilageForm';

export function DailyOperationsWorkspace({ busy, canCollectCustomer, canExpense, canPaySalary, canPaySupplier, canProduce, canPurchase, canSell, dashboard, cashierSummary, financialOverview, spoilageHistory, references, error, notice, onRecordExpense, onRecordPayment, onRecordProduction, onRecordPurchase, onRecordSalaryPayment, onRecordSale, onRecordSpoilage }: {
  busy: boolean;
  canCollectCustomer: boolean;
  canExpense: boolean;
  canPaySalary: boolean;
  canPaySupplier: boolean;
  canProduce: boolean;
  canPurchase: boolean;
  canSell: boolean;
  dashboard: DashboardDto | null;
  cashierSummary: CashierSummaryDto | null;
  financialOverview: FinancialOverviewDto | null;
  spoilageHistory: ReadonlyArray<SpoilageDto> | null;
  references: TransactionReferencesDto | null;
  error: string | null;
  notice: string | null;
  onRecordExpense(draft: ExpenseDraft): Promise<EventMutationResultDto>;
  onRecordPayment(draft: PaymentDraft): Promise<EventMutationResultDto>;
  onRecordProduction(draft: ProductionDraft): Promise<EventMutationResultDto>;
  onRecordPurchase(draft: PurchaseDraft): Promise<PurchaseRecordedResultDto>;
  onRecordSalaryPayment(draft: SalaryPaymentDraft): Promise<EventMutationResultDto>;
  onRecordSale(draft: SaleDraft): Promise<SaleRecordedResultDto>;
  onRecordSpoilage(draft: SpoilageDraft): Promise<EventMutationResultDto>;
}) {
  const [task, setTask] = useState<'purchase' | 'sale' | 'production' | 'spoilage' | 'expense' | 'payments' | 'salary'>(canPurchase ? 'purchase' : canSell ? 'sale' : canProduce ? 'production' : canExpense ? 'expense' : canPaySupplier || canCollectCustomer ? 'payments' : 'salary');
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
    {[canPurchase, canSell, canProduce, canProduce, canExpense, canPaySupplier || canCollectCustomer, canPaySalary].filter(Boolean).length > 1 && <div className="mb-5 flex overflow-x-auto rounded-xl bg-slate-200 p-1">{canPurchase && <FilterButton id="purchase-work-tab" active={task === 'purchase'} onClick={() => setTask('purchase')}>Receive milk</FilterButton>}{canSell && <FilterButton id="sale-work-tab" active={task === 'sale'} onClick={() => setTask('sale')}>Record sale</FilterButton>}{canProduce && <FilterButton id="production-work-tab" active={task === 'production'} onClick={() => setTask('production')}>Make yogurt</FilterButton>}{canProduce && <FilterButton id="spoilage-work-tab" active={task === 'spoilage'} onClick={() => setTask('spoilage')}>Spoilage</FilterButton>}{canExpense && <FilterButton id="expense-work-tab" active={task === 'expense'} onClick={() => setTask('expense')}>Expense</FilterButton>}{(canPaySupplier || canCollectCustomer) && <FilterButton id="payments-work-tab" active={task === 'payments'} onClick={() => setTask('payments')}>Payments</FilterButton>}{canPaySalary && <FilterButton id="salary-work-tab" active={task === 'salary'} onClick={() => setTask('salary')}>Pay salary</FilterButton>}</div>}
    {task === 'sale' && canSell && cashierSummary && <section className="mb-5 grid grid-cols-2 gap-3 sm:grid-cols-3"><SummaryCard label="Sales before delivery charges" value={formatPaisa(cashierSummary.salesValuePaisa)} />{cashierSummary.paymentAccounts.map((account) => <SummaryCard key={account.accountId} label={`${account.name} balance`} value={formatPaisa(account.canonicalBalancePaisa)} />)}</section>}
    {task === 'expense' && canExpense && financialOverview && <section className="mb-5 grid grid-cols-2 gap-3 sm:grid-cols-3"><SummaryCard label="Shop expenses today" value={formatPaisa(financialOverview.totals.operatingExpensesPaisa)} /></section>}
    {task === 'purchase' && canPurchase ? <PurchaseForm busy={busy} references={references} onRecordPurchase={onRecordPurchase} />
      : task === 'sale' && canSell ? <SaleForm busy={busy} references={references} onRecordSale={onRecordSale} />
        : task === 'production' && canProduce ? <ProductionForm busy={busy} milkAvailable={milk?.quantityOnHand ?? null} onRecordProduction={onRecordProduction} />
          : task === 'spoilage' && canProduce ? <SpoilageForm busy={busy} dashboard={dashboard} history={spoilageHistory} references={references} onRecordSpoilage={onRecordSpoilage} />
          : task === 'expense' && canExpense ? <ExpenseForm busy={busy} references={references} onRecordExpense={onRecordExpense} />
            : task === 'salary' && canPaySalary ? <SalaryPaymentForm busy={busy} references={references} onRecordSalaryPayment={onRecordSalaryPayment} />
              : <PaymentForm busy={busy} canCollectCustomer={canCollectCustomer} canPaySupplier={canPaySupplier} references={references} financialOverview={financialOverview} onRecordPayment={onRecordPayment} />}
  </>;
}
