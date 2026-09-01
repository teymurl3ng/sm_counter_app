import type {
  CostComponentCode,
  CostReviewReasonCode,
  Int64String,
  LossClassificationCode,
  LossReasonCode,
  OpeningBasisKindCode,
} from '../../bridge/contract.generated';

export type CostingPlanningArea = 'planning' | 'yogurt-cost' | 'opening-cost' | 'yield-review';

export type BudgetDraft = {
  categoryCode: string;
  periodStart: string;
  periodEndExclusive: string;
  plannedValue: string;
};

export type CollectionsTargetDraft = {
  weekStart: string;
  targetRupees: string;
};

export type CostComponentDraft = {
  componentCode: CostComponentCode;
  sourceAmountRupees: string;
  basisQuantityKg: string;
};

export type CostingPolicyDraft = {
  calendarMonth: string;
  normalCapacityKg: string;
  reviewReasonCode: CostReviewReasonCode;
  components: ReadonlyArray<CostComponentDraft>;
  notes: string;
  supersedesPolicyId: Int64String | null;
};

export type OpeningCostBasisDraft = {
  openingEventId: Int64String;
  productId: Int64String;
  quantity: string;
  totalCostRupees: string;
  basisKindCode: OpeningBasisKindCode;
  evidenceNotes: string;
  supersedesBasisId: Int64String | null;
};

export type YieldPolicyDraft = {
  acceptedProductionEventIds: ReadonlyArray<Int64String>;
  effectiveFrom: string;
  supersedesYieldPolicyId: Int64String | null;
};

export type LossAssessmentDraft = {
  productionEventId: Int64String;
  classificationCode: LossClassificationCode;
  abnormalMilkLiters: string;
  reasonCode: LossReasonCode;
  notes: string;
  supersedesAssessmentId: Int64String | null;
};

export const PLANNING_CATEGORIES = [
  'Milk purchases',
  'Rent',
  'Electricity',
  'Internet',
  'Water',
  'Fuel',
  'Poly bags',
  'LPG',
  'Other',
] as const;

export const COST_COMPONENTS: ReadonlyArray<CostComponentCode> = [
  'culture',
  'packaging',
  'variable_energy',
  'direct_labor',
  'fixed_production_overhead',
];
