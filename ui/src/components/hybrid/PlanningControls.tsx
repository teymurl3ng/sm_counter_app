import { useState, type FormEvent } from 'react';
import type { PlanningDto } from '../../bridge/contract.generated';
import { TextInput } from './HybridUi';
import { nextMondayInput, nextMonthInput, periodEnd } from './costingPlanningPresentation';
import { PLANNING_CATEGORIES, type BudgetDraft, type CollectionsTargetDraft } from './costingPlanningModels';
import { BudgetPlanCard, CollectionsTargetCard } from './PlanningStatusCards';

export function PlanningControls({ busy, planning, onSaveBudget, onCopyBudget, onSaveCollectionsTarget }: {
  busy: boolean;
  planning: PlanningDto;
  onSaveBudget(draft: BudgetDraft): Promise<void>;
  onCopyBudget(draft: BudgetDraft): Promise<void>;
  onSaveCollectionsTarget(draft: CollectionsTargetDraft): Promise<void>;
}) {
  const initialStart = nextMondayInput();
  const [budget, setBudget] = useState<BudgetDraft>({ categoryCode: 'Milk purchases', periodStart: initialStart, periodEndExclusive: periodEnd(initialStart, 'Milk purchases'), plannedValue: '' });
  const [target, setTarget] = useState<CollectionsTargetDraft>({ weekStart: initialStart, targetRupees: '' });
  const [formError, setFormError] = useState<string | null>(null);

  const updatePeriod = (categoryCode: string, periodStart: string) => setBudget((current) => ({ ...current, categoryCode, periodStart, periodEndExclusive: periodEnd(periodStart, categoryCode) }));
  const updateCategory = (categoryCode: string) => {
    const start = categoryCode === 'Milk purchases' ? nextMondayInput() : `${nextMonthInput()}-01`;
    updatePeriod(categoryCode, start);
  };
  const submit = (event: FormEvent, action: () => Promise<void>) => {
    event.preventDefault();
    setFormError(null);
    void action().catch((failure: unknown) => setFormError(failure instanceof Error ? failure.message : 'Could not save the plan.'));
  };
  const activeBudgets = planning.budgets.filter((item) => !item.archived);
  const activeTargets = planning.collectionTargets.filter((item) => !item.archived);

  return <div id="planning-controls" className="grid gap-5 lg:grid-cols-[1fr_1fr]">
    <div className="space-y-5">
      <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
        <h2 className="text-lg font-bold">Set a spending or milk plan</h2>
        <p className="mt-1 text-sm leading-6 text-slate-600">Milk is planned one week at a time in liters. Other shop costs are planned one month at a time in PKR.</p>
        <form id="budget-plan-form" className="mt-5" onSubmit={(event) => submit(event, () => onSaveBudget(budget))}>
          <label className="mb-4 block text-sm font-semibold text-slate-700">What are you planning?
            <select id="budget-category" value={budget.categoryCode} onChange={(event) => updateCategory(event.target.value)} className="mt-1.5 w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5">
              {PLANNING_CATEGORIES.map((category) => <option key={category}>{category}</option>)}
            </select>
          </label>
          <TextInput id="budget-period-start" label={budget.categoryCode === 'Milk purchases' ? 'Week starts on Monday' : 'Month starts'} value={budget.periodStart} onChange={(value) => updatePeriod(budget.categoryCode, value)} type="date" />
          <p className="-mt-2 mb-4 text-xs text-slate-500">This plan ends before {budget.periodEndExclusive}.</p>
          <TextInput id="budget-planned-value" label={budget.categoryCode === 'Milk purchases' ? 'Planned milk to buy (liters)' : 'Planned amount (PKR)'} value={budget.plannedValue} onChange={(plannedValue) => setBudget((current) => ({ ...current, plannedValue }))} inputMode="decimal" />
          <div className="grid grid-cols-2 gap-3">
            <button id="copy-previous-budget-button" type="button" disabled={busy} onClick={() => void onCopyBudget(budget).catch((failure: unknown) => setFormError(failure instanceof Error ? failure.message : 'Could not copy the earlier plan.'))} className="rounded-xl border border-emerald-700 px-3 py-3 text-sm font-bold text-emerald-800 disabled:opacity-50">Copy previous</button>
            <button id="save-budget-button" disabled={busy} className="rounded-xl bg-emerald-800 px-3 py-3 text-sm font-bold text-white disabled:opacity-50">{busy ? 'Saving…' : 'Save plan'}</button>
          </div>
        </form>
      </section>

      <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
        <h2 className="text-lg font-bold">Set a weekly collection target</h2>
        <p className="mt-1 text-sm leading-6 text-slate-600">This is how much customer debt you hope to collect. It is separate from the sales target.</p>
        <form id="collections-target-form" className="mt-5" onSubmit={(event) => submit(event, () => onSaveCollectionsTarget(target))}>
          <TextInput id="collection-week-start" label="Week starts on Monday" value={target.weekStart} onChange={(weekStart) => setTarget((current) => ({ ...current, weekStart }))} type="date" />
          <TextInput id="collection-target-amount" label="Target to collect (PKR)" value={target.targetRupees} onChange={(targetRupees) => setTarget((current) => ({ ...current, targetRupees }))} inputMode="decimal" />
          <button id="save-collections-target-button" disabled={busy} className="w-full rounded-xl bg-slate-900 px-4 py-3 font-bold text-white disabled:opacity-50">{busy ? 'Saving…' : 'Save collection target'}</button>
        </form>
      </section>
      {formError && <p role="alert" className="rounded-xl bg-rose-50 p-3 text-sm text-rose-800">{formError}</p>}
    </div>

    <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
      <div className="border-b border-slate-100 p-5"><h2 className="text-lg font-bold">Saved plans</h2><p className="mt-1 text-sm text-slate-500">Changing a plan keeps the old version for accountability.</p></div>
      <div id="saved-plans-list" className="divide-y divide-slate-100">
        {activeBudgets.length === 0 && activeTargets.length === 0 && <p className="p-5 text-sm text-slate-500">No current plans have been saved.</p>}
        {activeBudgets.map((item) => <BudgetPlanCard key={`budget-${item.budgetId}`} item={item} />)}
        {activeTargets.map((item) => <CollectionsTargetCard key={`target-${item.targetId}`} item={item} />)}
      </div>
      <p className="border-t border-slate-100 bg-slate-50 p-4 text-xs leading-5 text-slate-600">Plans are for comparison only. Saving one does not create a purchase, expense, sale, or payment.</p>
    </section>
  </div>;
}
