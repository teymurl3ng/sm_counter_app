export type AuthorizedDataSlice =
  | 'masterData'
  | 'transactionReferences'
  | 'businessSettings'
  | 'dashboard'
  | 'cashierSummary'
  | 'financialOverview'
  | 'spoilageHistory'
  | 'deliveries'
  | 'eventHistory'
  | 'reconciliationArchive';

export type RefreshPlan = ReadonlyArray<AuthorizedDataSlice>;

export type RunAndRefresh = <T>(
  operation: () => Promise<T>,
  success: string,
  refresh: RefreshPlan,
) => Promise<T>;

export const ALL_AUTHORIZED_DATA: RefreshPlan = [
  'masterData',
  'transactionReferences',
  'businessSettings',
  'dashboard',
  'cashierSummary',
  'financialOverview',
  'spoilageHistory',
  'deliveries',
  'eventHistory',
  'reconciliationArchive',
];
