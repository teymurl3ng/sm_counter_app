import { useState, type FormEvent } from 'react';
import type { CostComponentCode, CostingManagementDto, CostingPolicyDto, CostReviewReasonCode } from '../../bridge/contract.generated';
import { formatPaisa, paisaToRupeesInput } from '../../bridge/nativeBridge';
import { TextInput } from './HybridUi';
import { COST_COMPONENTS, type CostingPolicyDraft } from './costingPlanningModels';
import { componentLabel, decimalPaisaPerKg, nextMonthInput, reviewReasonLabel } from './costingPlanningPresentation';

const REVIEW_REASONS: ReadonlyArray<CostReviewReasonCode> = ['monthly', 'component_change', 'capacity_change', 'process_change', 'quarterly_variance'];

export function CostingPolicyControls({ busy, costing, onSave }: {
  busy: boolean;
  costing: CostingManagementDto;
  onSave(draft: CostingPolicyDraft, passphrase: string): Promise<void>;
}) {
  const [draft, setDraft] = useState<CostingPolicyDraft>(() => emptyDraft());
  const [passphrase, setPassphrase] = useState('');
  const [formError, setFormError] = useState<string | null>(null);
  const active = costing.policies.filter((item) => !item.isSuperseded);

  const submit = (event: FormEvent) => {
    event.preventDefault();
    setFormError(null);
    void onSave(draft, passphrase).then(() => {
      setDraft(emptyDraft());
      setPassphrase('');
    }).catch((failure: unknown) => setFormError(failure instanceof Error ? failure.message : 'Could not save the yogurt cost standard.'));
  };
  const setCapacity = (normalCapacityKg: string) => setDraft((current) => ({
    ...current,
    normalCapacityKg,
    components: current.components.map((component) => component.componentCode === 'fixed_production_overhead' ? { ...component, basisQuantityKg: normalCapacityKg } : component),
  }));
  const revise = (policy: CostingPolicyDto) => {
    setDraft({
      calendarMonth: nextMonthInput(),
      normalCapacityKg: policy.normalCapacityKg,
      reviewReasonCode: 'monthly',
      components: COST_COMPONENTS.map((componentCode) => {
        const saved = policy.components.find((item) => item.componentCode === componentCode);
        return { componentCode, sourceAmountRupees: paisaToRupeesInput(saved?.sourceAmountPaisa ?? null), basisQuantityKg: saved?.basisQuantityKg ?? (componentCode === 'fixed_production_overhead' ? policy.normalCapacityKg : '1') };
      }),
      notes: '',
      supersedesPolicyId: policy.policyId,
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  return <div id="costing-policy-controls" className="grid gap-5 lg:grid-cols-[1.1fr_0.9fr]">
    <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <h2 className="text-lg font-bold">Approve next month’s yogurt-making cost</h2>
      <p className="mt-1 text-sm leading-6 text-slate-600">Enter the expected cost and the amount of yogurt each cost covers. The app calculates one standard cost per kg and keeps old months unchanged.</p>
      {draft.supersedesPolicyId && <p className="mt-4 rounded-xl bg-amber-50 p-3 text-sm text-amber-900">You are correcting standard #{draft.supersedesPolicyId}. The earlier version will remain in history.</p>}
      <form id="costing-policy-form" className="mt-5" onSubmit={submit}>
        <TextInput id="costing-calendar-month" label="Month this standard applies to" value={draft.calendarMonth} onChange={(calendarMonth) => setDraft((current) => ({ ...current, calendarMonth }))} type="month" />
        <TextInput id="costing-normal-capacity" label="Normal yogurt production for the month (kg)" value={draft.normalCapacityKg} onChange={setCapacity} inputMode="decimal" />
        <label className="mb-5 block text-sm font-semibold text-slate-700">Why are you reviewing it?
          <select id="costing-review-reason" value={draft.reviewReasonCode} onChange={(event) => setDraft((current) => ({ ...current, reviewReasonCode: event.target.value as CostReviewReasonCode }))} className="mt-1.5 w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5">
            {REVIEW_REASONS.map((reason) => <option key={reason} value={reason}>{reviewReasonLabel(reason)}</option>)}
          </select>
        </label>
        <div className="space-y-4">
          {draft.components.map((component, index) => <fieldset key={component.componentCode} className="rounded-xl border border-slate-200 p-4">
            <legend className="px-1 text-sm font-bold text-slate-800">{componentLabel(component.componentCode)}</legend>
            <div className="grid gap-3 sm:grid-cols-2">
              <TextInput id={`cost-component-amount-${component.componentCode}`} label="Expected total cost (PKR)" value={component.sourceAmountRupees} onChange={(value) => setDraft((current) => ({ ...current, components: replaceComponent(current.components, index, { sourceAmountRupees: value }) }))} inputMode="decimal" />
              <TextInput id={`cost-component-basis-${component.componentCode}`} label="This cost covers (kg)" value={component.basisQuantityKg} onChange={(value) => setDraft((current) => ({ ...current, components: replaceComponent(current.components, index, { basisQuantityKg: value }) }))} inputMode="decimal" />
            </div>
            {component.componentCode === 'fixed_production_overhead' && <p className="-mt-2 text-sm leading-5 text-slate-600">This must match normal monthly production. The form keeps both values together.</p>}
          </fieldset>)}
        </div>
        <label className="mt-5 block text-sm font-semibold text-slate-700">Notes (optional)<textarea id="costing-policy-notes" rows={3} value={draft.notes} onChange={(event) => setDraft((current) => ({ ...current, notes: event.target.value }))} className="mt-1.5 w-full rounded-xl border border-slate-300 px-3 py-2.5" /></label>
        <TextInput id="costing-policy-passphrase" label="Your owner/admin passphrase" value={passphrase} onChange={setPassphrase} type="password" autoComplete="current-password" />
        {formError && <p role="alert" className="mb-4 rounded-xl bg-rose-50 p-3 text-sm text-rose-800">{formError}</p>}
        <button id="save-costing-policy-button" disabled={busy} className="w-full rounded-xl bg-emerald-800 px-4 py-3 font-bold text-white disabled:opacity-50">{busy ? 'Checking and saving…' : draft.supersedesPolicyId ? 'Approve corrected standard' : 'Approve monthly standard'}</button>
      </form>
    </section>

    <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
      <div className="border-b border-slate-100 p-5"><h2 className="text-lg font-bold">Approved standards</h2><p className="mt-1 text-sm text-slate-500">Each month stays tied to the cost approved for that month.</p></div>
      <div id="approved-costing-policies" className="divide-y divide-slate-100">
        {active.length === 0 && <p className="p-5 text-sm text-slate-500">No yogurt cost standard has been approved yet.</p>}
        {active.map((policy) => <article key={policy.policyId} className="p-4">
          <div className="flex items-start justify-between gap-3"><div><h3 className="font-bold">{policy.calendarMonth}</h3><p className="mt-1 text-xs text-slate-500">{reviewReasonLabel(policy.reviewReasonCode)} · Standard #{policy.policyId}</p></div><button type="button" id={`revise-costing-policy-${policy.policyId}`} disabled={busy} onClick={() => revise(policy)} className="rounded-lg border border-slate-300 px-2.5 py-1.5 text-xs font-bold text-slate-700 disabled:opacity-50">Correct</button></div>
          <p className="mt-3 text-xl font-bold text-emerald-800">{decimalPaisaPerKg(policy.conversionCostPaisaPerKg)}</p>
          <p className="text-sm text-slate-600">Based on {policy.normalCapacityKg} kg normal monthly production</p>
          <details className="mt-3 text-sm"><summary className="cursor-pointer font-semibold text-slate-700">See entered costs</summary><div className="mt-2 space-y-1">{policy.components.map((component) => <p key={component.componentCode} className="flex justify-between gap-3 text-slate-600"><span>{componentLabel(component.componentCode as CostComponentCode)}</span><span>{formatPaisa(component.sourceAmountPaisa)} / {component.basisQuantityKg} kg</span></p>)}</div></details>
        </article>)}
      </div>
    </section>
  </div>;
}

function emptyDraft(): CostingPolicyDraft {
  return {
    calendarMonth: nextMonthInput(),
    normalCapacityKg: '1',
    reviewReasonCode: 'monthly',
    components: COST_COMPONENTS.map((componentCode) => ({ componentCode, sourceAmountRupees: '', basisQuantityKg: '1' })),
    notes: '',
    supersedesPolicyId: null,
  };
}

function replaceComponent(components: CostingPolicyDraft['components'], index: number, update: Partial<{ sourceAmountRupees: string; basisQuantityKg: string }>) {
  return components.map((component, current) => current === index ? { ...component, ...update } : component);
}
