import { useState, type FormEvent } from 'react';
import type { CostingManagementDto, Int64String, LossClassificationCode, LossReasonCode, ProductionLossAssessmentDto, YieldAlertDto } from '../../bridge/contract.generated';
import { TextInput } from './HybridUi';
import type { LossAssessmentDraft, YieldPolicyDraft } from './costingPlanningModels';
import { lossReasonLabel, yieldStatus } from './costingPlanningPresentation';

const LOSS_REASONS: ReadonlyArray<LossReasonCode> = ['spill', 'contamination', 'power_failure', 'equipment_failure', 'measurement_correction', 'other'];

export function YieldLossControls({ busy, costing, onSaveYieldPolicy, onSaveLossAssessment }: {
  busy: boolean;
  costing: CostingManagementDto;
  onSaveYieldPolicy(draft: YieldPolicyDraft, passphrase: string): Promise<void>;
  onSaveLossAssessment(draft: LossAssessmentDraft, passphrase: string): Promise<void>;
}) {
  const [selected, setSelected] = useState<ReadonlyArray<Int64String>>([]);
  const [effectiveFrom, setEffectiveFrom] = useState(tomorrow());
  const [yieldPassphrase, setYieldPassphrase] = useState('');
  const [yieldError, setYieldError] = useState<string | null>(null);
  const [lossDraft, setLossDraft] = useState<LossAssessmentDraft | null>(null);
  const [lossPassphrase, setLossPassphrase] = useState('');
  const [lossError, setLossError] = useState<string | null>(null);
  const activeYieldPolicy = costing.yieldPolicies.find((item) => !item.isSuperseded) ?? null;
  const activeAssessments = costing.productionLossAssessments.filter((item) => !item.isSuperseded);

  const toggle = (id: Int64String) => setSelected((current) => current.includes(id) ? current.filter((item) => item !== id) : [...current, id]);
  const submitYield = (event: FormEvent) => {
    event.preventDefault();
    setYieldError(null);
    void onSaveYieldPolicy({ acceptedProductionEventIds: selected, effectiveFrom, supersedesYieldPolicyId: activeYieldPolicy?.yieldPolicyId ?? null }, yieldPassphrase)
      .then(() => { setSelected([]); setYieldPassphrase(''); })
      .catch((failure: unknown) => setYieldError(failure instanceof Error ? failure.message : 'Could not approve the yield baseline.'));
  };
  const chooseLoss = (alert: YieldAlertDto) => {
    const current = activeAssessments.find((item) => item.productionEventId === alert.productionEventId);
    setLossDraft({
      productionEventId: alert.productionEventId,
      classificationCode: (current?.classificationCode as LossClassificationCode | undefined) ?? 'ordinary',
      abnormalMilkLiters: current?.abnormalMilkLiters ?? '0',
      reasonCode: (current?.reasonCode as LossReasonCode | undefined) ?? 'measurement_correction',
      notes: current?.notes ?? '',
      supersedesAssessmentId: current?.assessmentId ?? null,
    });
    setLossPassphrase('');
    setLossError(null);
  };
  const submitLoss = (event: FormEvent) => {
    event.preventDefault();
    if (!lossDraft) return;
    setLossError(null);
    void onSaveLossAssessment(lossDraft, lossPassphrase).then(() => { setLossDraft(null); setLossPassphrase(''); }).catch((failure: unknown) => setLossError(failure instanceof Error ? failure.message : 'Could not save the batch review.'));
  };

  return <div id="yield-loss-controls" className="space-y-5">
    <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <div className="flex flex-wrap items-start justify-between gap-4"><div><h2 className="text-lg font-bold">Shop-specific yogurt yield baseline</h2><p className="mt-1 max-w-2xl text-sm leading-6 text-slate-600">Choose at least 30 trustworthy batches covering at least 28 days. The app uses them to learn the shop’s normal yogurt output range.</p></div>{activeYieldPolicy && <div className="rounded-xl bg-emerald-50 px-4 py-3 text-sm text-emerald-900"><strong>Current baseline</strong><p>{activeYieldPolicy.lowerAlertLimit} to {activeYieldPolicy.upperAlertLimit} kg per liter</p><p className="mt-1 text-xs">Based on {activeYieldPolicy.acceptedBatchCount} batches</p></div>}</div>
      <form id="yield-policy-form" className="mt-5" onSubmit={submitYield}>
        <div className="mb-3 flex items-center justify-between"><p className="text-sm font-bold">Representative batches ({selected.length} selected)</p><button id="select-all-yield-batches" type="button" onClick={() => setSelected(costing.yieldAlerts.map((item) => item.productionEventId))} className="rounded-lg border border-slate-300 px-3 py-1.5 text-xs font-bold text-slate-700">Select all shown</button></div>
        <div id="yield-batch-selection" className="max-h-72 divide-y divide-slate-100 overflow-y-auto rounded-xl border border-slate-200">
          {costing.yieldAlerts.length === 0 && <p className="p-4 text-sm text-slate-500">No yogurt batches are available yet.</p>}
          {costing.yieldAlerts.map((alert) => <label key={alert.productionEventId} className="flex cursor-pointer items-center justify-between gap-3 p-3 text-sm"><span className="flex items-center gap-3"><input id={`yield-batch-${alert.productionEventId}`} type="checkbox" checked={selected.includes(alert.productionEventId)} onChange={() => toggle(alert.productionEventId)} className="h-4 w-4 rounded border-slate-300 text-emerald-700" /><span><strong>Batch #{alert.productionEventId}</strong><span className="block text-xs text-slate-500">Yield: {alert.yieldRatio} kg yogurt per liter milk</span></span></span><span className={`text-xs font-bold ${yieldStatus(alert.statusCode).tone}`}>{yieldStatus(alert.statusCode).label}</span></label>)}
        </div>
        <div className="mt-4 grid gap-3 sm:grid-cols-2"><TextInput id="yield-effective-from" label="Start using this baseline on" value={effectiveFrom} onChange={setEffectiveFrom} type="date" /><TextInput id="yield-policy-passphrase" label="Your owner/admin passphrase" value={yieldPassphrase} onChange={setYieldPassphrase} type="password" autoComplete="current-password" /></div>
        {activeYieldPolicy && <p className="mb-4 rounded-xl bg-amber-50 p-3 text-sm text-amber-900">Approving this replaces baseline #{activeYieldPolicy.yieldPolicyId}. The old baseline remains attached to earlier batches.</p>}
        {yieldError && <p role="alert" className="mb-4 rounded-xl bg-rose-50 p-3 text-sm text-rose-800">{yieldError}</p>}
        <button id="approve-yield-policy-button" disabled={busy || selected.length < 30} className="w-full rounded-xl bg-emerald-800 px-4 py-3 font-bold text-white disabled:opacity-50">{busy ? 'Checking…' : activeYieldPolicy ? 'Approve revised baseline' : 'Approve baseline'}</button>
      </form>
    </section>

    <div className="grid gap-5 lg:grid-cols-[1fr_1fr]">
      <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        <div className="border-b border-slate-100 p-5"><h2 className="text-lg font-bold">Batch warnings and reviews</h2><p className="mt-1 text-sm leading-6 text-slate-600">A warning asks a person to check the batch. It never records a loss by itself.</p></div>
        <div id="yield-alert-list" className="max-h-[38rem] divide-y divide-slate-100 overflow-y-auto">
          {costing.yieldAlerts.length === 0 && <p className="p-5 text-sm text-slate-500">No yogurt batches to review.</p>}
          {costing.yieldAlerts.map((alert) => {
            const status = yieldStatus(alert.statusCode);
            const assessment = activeAssessments.find((item) => item.productionEventId === alert.productionEventId);
            return <article key={alert.productionEventId} className="p-4"><div className="flex items-start justify-between gap-3"><div><h3 className="font-bold">Batch #{alert.productionEventId}</h3><p className="mt-1 text-sm text-slate-600">{alert.yieldRatio} kg yogurt per liter milk</p></div><button id={`review-loss-${alert.productionEventId}`} type="button" disabled={busy} onClick={() => chooseLoss(alert)} className="rounded-lg border border-slate-300 px-3 py-2 text-xs font-bold text-slate-700 disabled:opacity-50">{assessment ? 'Correct review' : 'Review'}</button></div><p className={`mt-3 text-sm font-bold ${status.tone}`}>{status.label}</p><p className="mt-1 text-xs leading-5 text-slate-500">{status.explanation}</p>{assessment && <AssessmentSummary assessment={assessment} />}</article>;
          })}
        </div>
      </section>

      <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
        <h2 className="text-lg font-bold">{lossDraft ? `Review batch #${lossDraft.productionEventId}` : 'Choose a batch to review'}</h2>
        {!lossDraft ? <p className="mt-2 text-sm leading-6 text-slate-600">Use the Review button beside a batch. Record only what you have actually checked.</p> : <form id="production-loss-form" className="mt-5" onSubmit={submitLoss}>
          {lossDraft.supersedesAssessmentId && <p className="mb-4 rounded-xl bg-amber-50 p-3 text-sm text-amber-900">This correction replaces review #{lossDraft.supersedesAssessmentId}, while keeping it in history.</p>}
          <label className="mb-4 block text-sm font-semibold text-slate-700">What did your review find?
            <select id="loss-classification" value={lossDraft.classificationCode} onChange={(event) => setLossDraft((current) => current && ({ ...current, classificationCode: event.target.value as LossClassificationCode, abnormalMilkLiters: event.target.value === 'ordinary' ? '0' : current.abnormalMilkLiters }))} className="mt-1.5 w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5">
              <option value="ordinary">Normal production variation</option><option value="abnormal">Identified unusual loss</option>
            </select>
          </label>
          {lossDraft.classificationCode === 'abnormal' && <TextInput id="abnormal-milk-liters" label="Milk lost because of this incident (liters)" value={lossDraft.abnormalMilkLiters} onChange={(abnormalMilkLiters) => setLossDraft((current) => current && ({ ...current, abnormalMilkLiters }))} inputMode="decimal" />}
          <label className="mb-4 block text-sm font-semibold text-slate-700">Reason
            <select id="loss-reason" value={lossDraft.reasonCode} onChange={(event) => setLossDraft((current) => current && ({ ...current, reasonCode: event.target.value as LossReasonCode }))} className="mt-1.5 w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5">{LOSS_REASONS.map((reason) => <option key={reason} value={reason}>{lossReasonLabel(reason)}</option>)}</select>
          </label>
          <label className="mb-4 block text-sm font-semibold text-slate-700">What did you check? (optional)<textarea id="loss-assessment-notes" rows={3} value={lossDraft.notes} onChange={(event) => setLossDraft((current) => current && ({ ...current, notes: event.target.value }))} className="mt-1.5 w-full rounded-xl border border-slate-300 px-3 py-2.5" /></label>
          <TextInput id="loss-assessment-passphrase" label="Your owner/admin passphrase" value={lossPassphrase} onChange={setLossPassphrase} type="password" autoComplete="current-password" />
          {lossError && <p role="alert" className="mb-4 rounded-xl bg-rose-50 p-3 text-sm text-rose-800">{lossError}</p>}
          <div className="grid grid-cols-2 gap-3"><button type="button" onClick={() => setLossDraft(null)} className="rounded-xl border border-slate-300 px-4 py-3 font-bold text-slate-700">Cancel</button><button id="save-loss-assessment-button" disabled={busy} className="rounded-xl bg-slate-900 px-4 py-3 font-bold text-white disabled:opacity-50">{busy ? 'Checking…' : 'Save review'}</button></div>
        </form>}
      </section>
    </div>
  </div>;
}

function AssessmentSummary({ assessment }: { assessment: ProductionLossAssessmentDto }) {
  return <div className="mt-3 rounded-xl bg-slate-50 p-3 text-xs text-slate-600"><strong className="text-slate-800">Reviewed: {assessment.classificationCode === 'abnormal' ? 'Unusual loss identified' : 'Normal variation'}</strong><p className="mt-1">{lossReasonLabel(assessment.reasonCode)}{assessment.classificationCode === 'abnormal' ? ` · ${assessment.abnormalMilkLiters} liters` : ''}</p></div>;
}

function tomorrow(): string {
  const date = new Date();
  date.setDate(date.getDate() + 1);
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}
