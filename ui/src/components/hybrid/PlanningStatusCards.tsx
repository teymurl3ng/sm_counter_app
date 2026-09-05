import type { BudgetPlanDto, CollectionsTargetDto } from '../../bridge/contract.generated';
import { formatPaisa } from '../../bridge/nativeBridge';

export function BudgetPlanCard({ item }: { item: BudgetPlanDto }) {
  return <article id={`budget-comparison-${item.budgetId}`} className="p-4">
    <div className="flex items-start justify-between gap-3">
      <div><h3 className="font-bold">{item.categoryCode}</h3><p className="mt-1 text-xs text-slate-500">{item.periodStart} to before {item.periodEndExclusive}</p></div>
      <span className="rounded-full bg-emerald-50 px-2.5 py-1 text-xs font-bold text-emerald-800">Current</span>
    </div>
    <div className="mt-3 grid gap-2 sm:grid-cols-3">
      <PlanValue label={item.plannedLiters !== null ? 'Planned quantity' : 'Planned spending'} value={item.plannedLiters !== null ? `${item.plannedLiters} liters` : formatPaisa(item.plannedAmountPaisa)} />
      <PlanValue label="Recorded spending" value={formatPaisa(item.actualAmountPaisa)} />
      <PlanValue label="Actual minus plan" value={formatPaisa(item.variancePaisa)} />
    </div>
    {item.plannedLiters !== null && item.plannedAmountPaisa !== null && <p className="mt-3 text-sm text-slate-600">Expected buying cost: {formatPaisa(item.plannedAmountPaisa)}</p>}
    {item.salesTargetPaisa !== null && <div id={`sales-target-${item.budgetId}`} className="mt-3 rounded-xl bg-sky-50 p-3">
      <p className="text-xs font-bold uppercase tracking-wide text-sky-800">Sales target created by this milk plan</p>
      <div className="mt-2 grid gap-2 sm:grid-cols-3">
        <PlanValue label="Target sales" value={formatPaisa(item.salesTargetPaisa)} />
        <PlanValue label="Actual sales" value={formatPaisa(item.actualSalesPaisa)} />
        <PlanValue label="Sales minus target" value={formatPaisa(item.salesVariancePaisa)} />
      </div>
    </div>}
    <p className="mt-3 text-sm leading-5 text-slate-600">Recorded values come from saved shop activity in this plan period. A negative difference means actual activity is below the plan; a positive spending difference means spending is above the plan.</p>
  </article>;
}

export function CollectionsTargetCard({ item }: { item: CollectionsTargetDto }) {
  return <article id={`collection-comparison-${item.targetId}`} className="p-4">
    <div className="flex items-start justify-between gap-3">
      <div><h3 className="font-bold">Customer collections</h3><p className="mt-1 text-xs text-slate-500">Week of {item.weekStart}</p></div>
      <span className="rounded-full bg-sky-50 px-2.5 py-1 text-xs font-bold text-sky-800">Target</span>
    </div>
    <div className="mt-3 grid gap-2 sm:grid-cols-3">
      <PlanValue label="Target" value={formatPaisa(item.targetAmountPaisa)} />
      <PlanValue label="Collected" value={formatPaisa(item.actualCollectedPaisa)} />
      <PlanValue label="Collected minus target" value={formatPaisa(item.variancePaisa)} />
    </div>
  </article>;
}

function PlanValue({ label, value }: { label: string; value: string }) {
  const isCurrency = value.startsWith('PKR ');
  return <div className="min-w-0 rounded-lg bg-slate-50 px-3 py-2"><p className="text-xs leading-4 text-slate-600">{label}</p><p className={`mt-1 text-base font-bold text-slate-900 tabular-nums ${isCurrency ? 'whitespace-nowrap' : 'break-words'}`}>{isCurrency ? <>PKR&nbsp;{value.slice(4)}</> : value}</p></div>;
}
