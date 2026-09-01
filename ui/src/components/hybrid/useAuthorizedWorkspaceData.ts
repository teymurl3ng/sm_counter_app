import { useCallback, useState } from 'react';
import type {
  BusinessSettingsDto,
  CashierSummaryDto,
  CurrentSessionDto,
  DashboardDto,
  DeliveryDto,
  EventHistoryDto,
  FinancialOverviewDto,
  IsoDateString,
  MasterDataDto,
  ReconciliationArchiveDto,
  SessionHandle,
  SpoilageDto,
  TransactionReferencesDto,
} from '../../bridge/contract.generated';
import { callNative } from '../../bridge/nativeBridge';
import { ALL_AUTHORIZED_DATA, type RefreshPlan } from './authorizedDataRefresh';

export function useAuthorizedWorkspaceData() {
  const [masterData, setMasterData] = useState<MasterDataDto | null>(null);
  const [businessSettings, setBusinessSettings] = useState<BusinessSettingsDto | null>(null);
  const [transactionReferences, setTransactionReferences] = useState<TransactionReferencesDto | null>(null);
  const [dashboard, setDashboard] = useState<DashboardDto | null>(null);
  const [cashierSummary, setCashierSummary] = useState<CashierSummaryDto | null>(null);
  const [financialOverview, setFinancialOverview] = useState<FinancialOverviewDto | null>(null);
  const [spoilageHistory, setSpoilageHistory] = useState<ReadonlyArray<SpoilageDto> | null>(null);
  const [deliveries, setDeliveries] = useState<ReadonlyArray<DeliveryDto> | null>(null);
  const [eventHistory, setEventHistory] = useState<ReadonlyArray<EventHistoryDto> | null>(null);
  const [reconciliationArchive, setReconciliationArchive] = useState<ReconciliationArchiveDto | null>(null);

  const refreshAuthorizedData = useCallback(async (session: SessionHandle, user: CurrentSessionDto, plan: RefreshPlan) => {
    const today = localDate(0);
    const tomorrow = localDate(1);
    const requested = new Set(plan);
    const permissions = new Set(user.effectivePermissionCodes);
    const tasks: Array<Promise<unknown>> = [];

    if (requested.has('masterData')) tasks.push(permissions.has('manage_master_data') ? callNative('query.masterData', {}, session).then(setMasterData) : Promise.resolve(setMasterData(null)));
    if (requested.has('transactionReferences')) tasks.push(permissions.has('view_operational') ? callNative('query.transactionReferences', {}, session).then(setTransactionReferences) : Promise.resolve(setTransactionReferences(null)));
    if (requested.has('businessSettings')) tasks.push(permissions.has('manage_business_settings') ? callNative('query.businessSettings', {}, session).then(setBusinessSettings) : Promise.resolve(setBusinessSettings(null)));
    if (requested.has('dashboard')) tasks.push(permissions.has('view_operational') ? callNative('query.dashboard', { startDateInclusive: today, endDateExclusive: tomorrow }, session).then(setDashboard) : Promise.resolve(setDashboard(null)));
    if (requested.has('cashierSummary')) tasks.push(permissions.has('sale_and_collection') ? callNative('query.cashierSummary', { startDateInclusive: today, endDateExclusive: tomorrow }, session).then(setCashierSummary) : Promise.resolve(setCashierSummary(null)));
    if (requested.has('financialOverview')) tasks.push(permissions.has('view_full_financial') ? callNative('query.financialOverview', { startDateInclusive: today, endDateExclusive: tomorrow }, session).then(setFinancialOverview) : Promise.resolve(setFinancialOverview(null)));
    if (requested.has('spoilageHistory')) tasks.push(permissions.has('production_and_spoilage') ? callNative('query.spoilageHistory', {}, session).then(setSpoilageHistory) : Promise.resolve(setSpoilageHistory(null)));
    if (requested.has('deliveries')) tasks.push(permissions.has('delivery') ? callNative('query.deliveries', {}, session).then(setDeliveries) : Promise.resolve(setDeliveries(null)));
    if (requested.has('eventHistory')) tasks.push(permissions.has('view_operational') ? callNative('query.eventHistory', {}, session).then(setEventHistory) : Promise.resolve(setEventHistory(null)));
    if (requested.has('reconciliationArchive')) tasks.push(permissions.has('cash_count_and_close') ? callNative('query.reconciliationArchive', {}, session).then(setReconciliationArchive) : Promise.resolve(setReconciliationArchive(null)));

    await Promise.all(tasks);
  }, []);

  const loadAuthorizedData = useCallback(
    (session: SessionHandle, user: CurrentSessionDto) => refreshAuthorizedData(session, user, ALL_AUTHORIZED_DATA),
    [refreshAuthorizedData],
  );

  const clearAuthorizedData = useCallback(() => {
    setMasterData(null);
    setBusinessSettings(null);
    setTransactionReferences(null);
    setDashboard(null);
    setCashierSummary(null);
    setFinancialOverview(null);
    setSpoilageHistory(null);
    setDeliveries(null);
    setEventHistory(null);
    setReconciliationArchive(null);
  }, []);

  return {
    masterData,
    businessSettings,
    transactionReferences,
    dashboard,
    cashierSummary,
    financialOverview,
    spoilageHistory,
    deliveries,
    eventHistory,
    reconciliationArchive,
    loadAuthorizedData,
    refreshAuthorizedData,
    clearAuthorizedData,
  };
}

function localDate(daysFromToday: number): IsoDateString {
  const date = new Date();
  date.setDate(date.getDate() + daysFromToday);
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}` as IsoDateString;
}
