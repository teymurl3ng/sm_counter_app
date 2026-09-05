import { useState } from 'react';
import { AlertTriangle, Banknote, ChevronDown, Droplets, Factory, HandCoins, ReceiptText, ShoppingCart, X, type LucideIcon } from 'lucide-react';
import type { CashierSummaryDto, DashboardDto, EventMutationResultDto, FinancialOverviewDto, PurchaseRecordedResultDto, SaleRecordedResultDto, SpoilageDto, TransactionReferencesDto } from '../../bridge/contract.generated';
import { formatPaisa } from '../../bridge/nativeBridge';
import { Notice, SummaryCard } from './HybridUi';
import type { ExpenseDraft, PaymentDraft, ProductionDraft, PurchaseDraft, SalaryPaymentDraft, SaleDraft, SpoilageDraft } from './hybridModels';
import { ExpenseForm } from './ExpenseForm';
import { PaymentForm } from './PaymentForm';
import { ProductionForm } from './ProductionForm';
import { PurchaseForm } from './PurchaseForm';
import { SaleForm } from './SaleForm';
import { SalaryPaymentForm } from './SalaryPaymentForm';
import { SpoilageForm } from './SpoilageForm';

type DailyTask = 'purchase' | 'sale' | 'production' | 'spoilage' | 'expense' | 'payments' | 'salary';

type DailyTaskItem = {
  task: DailyTask;
  id: string;
  label: string;
  icon: LucideIcon;
  available: boolean;
};

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
  const [task, setTask] = useState<DailyTask>(canPurchase ? 'purchase' : canSell ? 'sale' : canProduce ? 'production' : canExpense ? 'expense' : canPaySupplier || canCollectCustomer ? 'payments' : 'salary');
  const [taskNavigationOpen, setTaskNavigationOpen] = useState(false);
  const tasks = ([
    { task: 'purchase', id: 'purchase-work-tab', label: 'Receive milk', icon: Droplets, available: canPurchase },
    { task: 'sale', id: 'sale-work-tab', label: 'Record sale', icon: ShoppingCart, available: canSell },
    { task: 'production', id: 'production-work-tab', label: 'Make yogurt', icon: Factory, available: canProduce },
    { task: 'spoilage', id: 'spoilage-work-tab', label: 'Spoilage', icon: AlertTriangle, available: canProduce },
    { task: 'expense', id: 'expense-work-tab', label: 'Expense', icon: ReceiptText, available: canExpense },
    { task: 'payments', id: 'payments-work-tab', label: 'Payments', icon: HandCoins, available: canPaySupplier || canCollectCustomer },
    { task: 'salary', id: 'salary-work-tab', label: 'Pay salary', icon: Banknote, available: canPaySalary },
  ] satisfies ReadonlyArray<DailyTaskItem>).filter((item) => item.available);
  const activeTask = tasks.find((item) => item.task === task) ?? tasks[0];
  const ActiveTaskIcon = activeTask.icon;
  const milk = dashboard?.stock.find((item) => item.name.toLowerCase() === 'milk');
  const yogurt = dashboard?.stock.find((item) => item.name.toLowerCase() === 'yogurt');
  return <>
    {(notice || error) && <Notice message={notice ?? error ?? ''} success={Boolean(notice)} />}
    {tasks.length > 1 && <>
      <button id="daily-task-picker" type="button" aria-expanded={taskNavigationOpen} aria-controls="daily-task-navigation-panel" onClick={() => setTaskNavigationOpen(true)} className="mb-5 flex min-h-16 w-full items-center gap-3 rounded-2xl border border-slate-200 bg-white p-4 text-left shadow-sm">
        <span className="rounded-xl bg-emerald-50 p-2.5 text-emerald-700"><ActiveTaskIcon className="h-5 w-5" /></span>
        <span className="min-w-0 flex-1"><span className="block text-xs font-bold uppercase tracking-wide text-slate-500">Current task</span><strong className="mt-0.5 block text-base text-slate-900">{activeTask.label}</strong></span>
        <span className="flex items-center gap-1 text-sm font-bold text-emerald-800">Change<ChevronDown className="h-4 w-4" /></span>
      </button>
      <div id="daily-task-navigation-layer" className={`hybrid-overlay fixed inset-0 z-50 flex items-end bg-slate-950/45 transition-opacity md:items-center md:justify-center md:p-5 ${taskNavigationOpen ? 'visible opacity-100' : 'pointer-events-none invisible opacity-0'}`} aria-hidden={!taskNavigationOpen}>
        <button type="button" className="absolute inset-0" aria-label="Close daily task chooser" tabIndex={taskNavigationOpen ? 0 : -1} onClick={() => setTaskNavigationOpen(false)} />
        <section id="daily-task-navigation-panel" role="dialog" aria-modal="true" aria-labelledby="daily-task-navigation-title" className="hybrid-navigation-panel relative w-full overflow-y-auto rounded-t-3xl bg-[#f7f6f0] p-5 pb-8 shadow-2xl md:max-w-2xl md:rounded-3xl">
          <div className="flex items-start justify-between gap-4"><div><p className="text-xs font-bold uppercase tracking-wide text-emerald-700">Daily work</p><h2 id="daily-task-navigation-title" className="mt-1 text-xl font-bold text-slate-900">Choose a task</h2><p className="mt-1 text-sm text-slate-600">All tasks available to your account are shown here.</p></div><button id="close-daily-task-navigation" type="button" data-native-back-close aria-label="Close" tabIndex={taskNavigationOpen ? 0 : -1} onClick={() => setTaskNavigationOpen(false)} className="rounded-xl bg-white p-2.5 text-slate-500 shadow-sm"><X className="h-5 w-5" /></button></div>
          <div id="daily-task-navigation" className="mt-5 grid grid-cols-2 gap-2 sm:grid-cols-4" aria-label="Daily work tasks">
            {tasks.map((item) => <DailyTaskButton key={item.task} item={item} active={task === item.task} tabIndex={taskNavigationOpen ? 0 : -1} onClick={() => { setTask(item.task); setTaskNavigationOpen(false); window.scrollTo({ top: 0, behavior: 'auto' }); }} />)}
          </div>
        </section>
      </div>
    </>}
    <section className="mb-5 grid grid-cols-2 gap-3 sm:grid-cols-4">
      <SummaryCard label="Milk in stock" value={milk ? `${milk.quantityOnHand} ${milk.unit}` : '—'} />
      <SummaryCard label="Yogurt in stock" value={yogurt ? `${yogurt.quantityOnHand} ${yogurt.unit}` : '—'} />
      <SummaryCard label="Milk bought today" value={dashboard ? `${dashboard.totals.purchasedMilkLiters} L` : '—'} />
      <SummaryCard label="Quantity sold today" value={dashboard?.totals.saleQuantity ?? '—'} />
    </section>
    <section id="daily-task-panel" aria-live="polite">
      {task === 'sale' && canSell && cashierSummary && <section className="mb-5 grid grid-cols-2 gap-3 sm:grid-cols-3"><SummaryCard label="Sales before delivery charges" value={formatPaisa(cashierSummary.salesValuePaisa)} />{cashierSummary.paymentAccounts.map((account) => <SummaryCard key={account.accountId} label={`${account.name} balance`} value={formatPaisa(account.canonicalBalancePaisa)} />)}</section>}
      {task === 'expense' && canExpense && financialOverview && <section className="mb-5 grid grid-cols-2 gap-3 sm:grid-cols-3"><SummaryCard label="Shop expenses today" value={formatPaisa(financialOverview.totals.operatingExpensesPaisa)} /></section>}
      {task === 'purchase' && canPurchase ? <PurchaseForm busy={busy} references={references} onRecordPurchase={onRecordPurchase} />
        : task === 'sale' && canSell ? <SaleForm busy={busy} references={references} onRecordSale={onRecordSale} />
          : task === 'production' && canProduce ? <ProductionForm busy={busy} milkAvailable={milk?.quantityOnHand ?? null} onRecordProduction={onRecordProduction} />
            : task === 'spoilage' && canProduce ? <SpoilageForm busy={busy} dashboard={dashboard} history={spoilageHistory} references={references} onRecordSpoilage={onRecordSpoilage} />
              : task === 'expense' && canExpense ? <ExpenseForm busy={busy} references={references} onRecordExpense={onRecordExpense} />
                : task === 'salary' && canPaySalary ? <SalaryPaymentForm busy={busy} references={references} onRecordSalaryPayment={onRecordSalaryPayment} />
                  : <PaymentForm busy={busy} canCollectCustomer={canCollectCustomer} canPaySupplier={canPaySupplier} references={references} financialOverview={financialOverview} onRecordPayment={onRecordPayment} />}
    </section>
  </>;
}

function DailyTaskButton({ item, active, onClick, tabIndex }: { item: DailyTaskItem; active: boolean; onClick(): void; tabIndex?: number }) {
  const Icon = item.icon;
  return <button id={item.id} type="button" aria-pressed={active} aria-controls="daily-task-panel" tabIndex={tabIndex} onClick={onClick} className={`flex min-h-12 items-center gap-2 rounded-xl border px-3 py-2 text-left text-sm font-bold leading-5 transition-colors ${active ? 'border-emerald-300 bg-emerald-50 text-emerald-900 shadow-sm' : 'border-slate-200 bg-white text-slate-600'}`}><Icon className="h-5 w-5 shrink-0" /><span>{item.label}</span></button>;
}
