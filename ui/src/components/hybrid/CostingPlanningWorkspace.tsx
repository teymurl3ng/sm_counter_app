import { useEffect, useState } from 'react';
import { RefreshCw } from 'lucide-react';
import type { BusinessSettingsDto, MasterDataDto } from '../../bridge/contract.generated';
import { FilterButton, Notice } from './HybridUi';
import type { CostingPlanningActions, CostingPlanningBundle } from './costingPlanningBridgeActions';
import type { BudgetDraft, CollectionsTargetDraft, CostingPlanningArea, CostingPolicyDraft, LossAssessmentDraft, OpeningCostBasisDraft, YieldPolicyDraft } from './costingPlanningModels';
import { PlanningControls } from './PlanningControls';
import { CostingPolicyControls } from './CostingPolicyControls';
import { OpeningCostBasisControls } from './OpeningCostBasisControls';
import { YieldLossControls } from './YieldLossControls';

export function CostingPlanningWorkspace({ actions, settings, masterData }: {
  actions: CostingPlanningActions;
  settings: BusinessSettingsDto | null;
  masterData: MasterDataDto | null;
}) {
  const [area, setArea] = useState<CostingPlanningArea>('planning');
  const [bundle, setBundle] = useState<CostingPlanningBundle | null>(null);
  const [busy, setBusy] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const load = () => run(actions.load, null);
  useEffect(() => {
    let cancelled = false;
    setBusy(true);
    setError(null);
    void actions.load().then((value) => { if (!cancelled) setBundle(value); }).catch((failure: unknown) => { if (!cancelled) setError(message(failure)); }).finally(() => { if (!cancelled) setBusy(false); });
    return () => { cancelled = true; };
  }, [actions]);

  async function run(operation: () => Promise<CostingPlanningBundle>, success: string | null): Promise<void> {
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      setBundle(await operation());
      if (success) setNotice(success);
    } catch (failure) {
      setError(message(failure));
      throw failure;
    } finally {
      setBusy(false);
    }
  }

  const saveBudget = (draft: BudgetDraft) => run(() => actions.saveBudget(draft), 'Plan saved. The previous version is still in history.');
  const copyBudget = (draft: BudgetDraft) => run(() => actions.copyBudget(draft), 'The previous plan was copied into this period.');
  const saveTarget = (draft: CollectionsTargetDraft) => run(() => actions.saveCollectionsTarget(draft), 'Weekly collection target saved.');
  const saveCosting = (draft: CostingPolicyDraft, passphrase: string) => run(() => actions.saveCostingPolicy(draft, passphrase), 'Yogurt cost standard approved.');
  const saveOpening = (draft: OpeningCostBasisDraft, passphrase: string) => run(() => actions.saveOpeningBasis(draft, passphrase), 'Opening stock cost saved.');
  const saveYield = (draft: YieldPolicyDraft, passphrase: string) => run(() => actions.saveYieldPolicy(draft, passphrase), 'Shop-specific yield baseline approved.');
  const saveLoss = (draft: LossAssessmentDraft, passphrase: string) => run(() => actions.saveLossAssessment(draft, passphrase), 'Batch review saved.');

  return <div id="costing-planning-workspace">
    <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
      <div className="grid w-full grid-cols-2 gap-1 rounded-xl bg-slate-100 p-1 sm:w-auto sm:grid-cols-4">
        <FilterButton id="planning-tab" active={area === 'planning'} onClick={() => setArea('planning')}>Plans & targets</FilterButton>
        <FilterButton id="yogurt-cost-tab" active={area === 'yogurt-cost'} onClick={() => setArea('yogurt-cost')}>Yogurt cost</FilterButton>
        <FilterButton id="opening-cost-tab" active={area === 'opening-cost'} onClick={() => setArea('opening-cost')}>Opening stock cost</FilterButton>
        <FilterButton id="yield-review-tab" active={area === 'yield-review'} onClick={() => setArea('yield-review')}>Yield & loss review</FilterButton>
      </div>
      <button id="refresh-costing-planning" type="button" disabled={busy} onClick={() => void load().catch(() => undefined)} className="flex items-center gap-2 rounded-xl border border-slate-300 bg-white px-3 py-2 text-sm font-bold text-slate-700 disabled:opacity-50"><RefreshCw className={`h-4 w-4 ${busy ? 'animate-spin' : ''}`} />Refresh</button>
    </div>
    {error && <Notice message={error} />}
    {notice && <Notice message={notice} success />}
    {!bundle ? <section className="rounded-2xl border border-slate-200 bg-white p-6 text-center text-sm text-slate-600">{busy ? 'Loading cost and planning records…' : 'These records could not be loaded.'}</section>
      : area === 'planning' ? <PlanningControls busy={busy} planning={bundle.planning} onSaveBudget={saveBudget} onCopyBudget={copyBudget} onSaveCollectionsTarget={saveTarget} />
        : area === 'yogurt-cost' ? <CostingPolicyControls busy={busy} costing={bundle.costing} onSave={saveCosting} />
          : area === 'opening-cost' ? <OpeningCostBasisControls busy={busy} costing={bundle.costing} settings={settings} masterData={masterData} onSave={saveOpening} />
            : <YieldLossControls busy={busy} costing={bundle.costing} onSaveYieldPolicy={saveYield} onSaveLossAssessment={saveLoss} />}
  </div>;
}

function message(failure: unknown): string {
  return failure instanceof Error ? failure.message : 'Something went wrong. Try again.';
}
