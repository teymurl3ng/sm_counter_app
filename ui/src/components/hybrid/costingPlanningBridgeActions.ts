import type {
  CostingManagementDto,
  IsoDateString,
  PlanningDto,
  SessionHandle,
  YearMonthString,
} from '../../bridge/contract.generated';
import { callNative, rupeesToPaisa, validateDecimalInput } from '../../bridge/nativeBridge';
import type {
  BudgetDraft,
  CollectionsTargetDraft,
  CostingPolicyDraft,
  LossAssessmentDraft,
  OpeningCostBasisDraft,
  YieldPolicyDraft,
} from './costingPlanningModels';

export type CostingPlanningBundle = { costing: CostingManagementDto; planning: PlanningDto };

export function costingPlanningBridgeActions(session: SessionHandle | null) {
  const requireSession = (): SessionHandle => {
    if (!session) throw new Error('Sign in as the owner or administrator to continue.');
    return session;
  };
  const load = async (): Promise<CostingPlanningBundle> => {
    const active = requireSession();
    const [costing, planning] = await Promise.all([
      callNative('query.costingManagement', {}, active),
      callNative('query.planning', {}, active),
    ]);
    return { costing, planning };
  };
  const reauthenticate = (passphrase: string) => callNative('command.auth.reauthenticate', { passphrase }, requireSession());
  const mutate = async (operation: () => Promise<unknown>): Promise<CostingPlanningBundle> => {
    await operation();
    return load();
  };

  const saveBudget = (draft: BudgetDraft) => mutate(() => callNative('command.upsertBudget', {
    categoryCode: draft.categoryCode,
    periodStart: draft.periodStart as IsoDateString,
    periodEndExclusive: draft.periodEndExclusive as IsoDateString,
    plannedLiters: draft.categoryCode === 'Milk purchases' ? validateDecimalInput(draft.plannedValue, 'planned milk liters') : null,
    plannedAmountPaisa: draft.categoryCode === 'Milk purchases' ? null : rupeesToPaisa(draft.plannedValue),
  }, requireSession()));

  const copyBudget = (draft: BudgetDraft) => mutate(() => callNative('command.copyPreviousBudget', {
    categoryCode: draft.categoryCode,
    periodStart: draft.periodStart as IsoDateString,
    periodEndExclusive: draft.periodEndExclusive as IsoDateString,
  }, requireSession()));

  const saveCollectionsTarget = (draft: CollectionsTargetDraft) => mutate(() => callNative('command.upsertCollectionsTarget', {
    weekStart: draft.weekStart as IsoDateString,
    targetAmountPaisa: rupeesToPaisa(draft.targetRupees),
  }, requireSession()));

  const saveCostingPolicy = (draft: CostingPolicyDraft, passphrase: string) => mutate(async () => {
    await reauthenticate(passphrase);
    return callNative('command.approveCostingPolicy', {
      calendarMonth: draft.calendarMonth as YearMonthString,
      normalCapacityKg: positiveDecimal(draft.normalCapacityKg, 'normal monthly capacity'),
      reviewReasonCode: draft.reviewReasonCode,
      components: draft.components.map((component) => ({
        componentCode: component.componentCode,
        sourceAmountPaisa: rupeesToPaisa(component.sourceAmountRupees),
        basisQuantityKg: positiveDecimal(component.basisQuantityKg, `${component.componentCode} basis quantity`),
      })),
      ...(draft.notes.trim() ? { notes: draft.notes.trim() } : {}),
      supersedesPolicyId: draft.supersedesPolicyId,
    }, requireSession());
  });

  const saveOpeningBasis = (draft: OpeningCostBasisDraft, passphrase: string) => mutate(async () => {
    await reauthenticate(passphrase);
    return callNative('command.recordOpeningCostBasis', {
      openingEventId: draft.openingEventId,
      productId: draft.productId,
      quantity: positiveDecimal(draft.quantity, 'opening quantity'),
      totalCostPaisa: positivePaisa(draft.totalCostRupees, 'opening stock cost'),
      basisKindCode: draft.basisKindCode,
      ...(draft.evidenceNotes.trim() ? { evidenceNotes: draft.evidenceNotes.trim() } : {}),
      supersedesBasisId: draft.supersedesBasisId,
    }, requireSession());
  });

  const saveYieldPolicy = (draft: YieldPolicyDraft, passphrase: string) => mutate(async () => {
    if (draft.acceptedProductionEventIds.length < 30) throw new Error('Choose at least 30 representative yogurt batches.');
    await reauthenticate(passphrase);
    return callNative('command.approveYieldPolicy', {
      acceptedProductionEventIds: draft.acceptedProductionEventIds,
      effectiveFrom: draft.effectiveFrom as IsoDateString,
      supersedesYieldPolicyId: draft.supersedesYieldPolicyId,
    }, requireSession());
  });

  const saveLossAssessment = (draft: LossAssessmentDraft, passphrase: string) => mutate(async () => {
    await reauthenticate(passphrase);
    return callNative('command.recordProductionLossAssessment', {
      productionEventId: draft.productionEventId,
      classificationCode: draft.classificationCode,
      abnormalMilkLiters: validateDecimalInput(draft.classificationCode === 'ordinary' ? '0' : draft.abnormalMilkLiters, 'identified abnormal milk quantity'),
      reasonCode: draft.reasonCode,
      ...(draft.notes.trim() ? { notes: draft.notes.trim() } : {}),
      supersedesAssessmentId: draft.supersedesAssessmentId,
    }, requireSession());
  });

  return { copyBudget, load, saveBudget, saveCollectionsTarget, saveCostingPolicy, saveLossAssessment, saveOpeningBasis, saveYieldPolicy };
}

export type CostingPlanningActions = ReturnType<typeof costingPlanningBridgeActions>;

function positiveDecimal(value: string, label: string) {
  const decimal = validateDecimalInput(value, label);
  if (/^0(?:\.0+)?$/.test(decimal)) throw new Error(`Enter ${label} greater than zero.`);
  return decimal;
}

function positivePaisa(value: string, label: string) {
  const paisa = rupeesToPaisa(value);
  if (paisa === '0') throw new Error(`Enter ${label} greater than zero.`);
  return paisa;
}
