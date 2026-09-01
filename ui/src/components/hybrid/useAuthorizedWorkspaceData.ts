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

  const loadAuthorizedData = useCallback(async (session: SessionHandle, user: CurrentSessionDto) => {
    const today = localDate(0);
    const tomorrow = localDate(1);
    const [loadedMasterData, loadedReferences, loadedSettings, loadedDashboard, loadedCashierSummary, loadedFinancialOverview, loadedSpoilageHistory, loadedDeliveries, loadedEventHistory, loadedReconciliationArchive] = await Promise.all([
      user.effectivePermissionCodes.includes('manage_master_data') ? callNative('query.masterData', {}, session) : Promise.resolve(null),
      user.effectivePermissionCodes.includes('view_operational') ? callNative('query.transactionReferences', {}, session) : Promise.resolve(null),
      user.effectivePermissionCodes.includes('manage_business_settings') ? callNative('query.businessSettings', {}, session) : Promise.resolve(null),
      user.effectivePermissionCodes.includes('view_operational') ? callNative('query.dashboard', { startDateInclusive: today, endDateExclusive: tomorrow }, session) : Promise.resolve(null),
      user.effectivePermissionCodes.includes('sale_and_collection') ? callNative('query.cashierSummary', { startDateInclusive: today, endDateExclusive: tomorrow }, session) : Promise.resolve(null),
      user.effectivePermissionCodes.includes('view_full_financial') ? callNative('query.financialOverview', { startDateInclusive: today, endDateExclusive: tomorrow }, session) : Promise.resolve(null),
      user.effectivePermissionCodes.includes('production_and_spoilage') ? callNative('query.spoilageHistory', {}, session) : Promise.resolve(null),
      user.effectivePermissionCodes.includes('delivery') ? callNative('query.deliveries', {}, session) : Promise.resolve(null),
      user.effectivePermissionCodes.includes('view_operational') ? callNative('query.eventHistory', {}, session) : Promise.resolve(null),
      user.effectivePermissionCodes.includes('cash_count_and_close') ? callNative('query.reconciliationArchive', {}, session) : Promise.resolve(null),
    ]);
    setMasterData(loadedMasterData);
    setTransactionReferences(loadedReferences);
    setBusinessSettings(loadedSettings);
    setDashboard(loadedDashboard);
    setCashierSummary(loadedCashierSummary);
    setFinancialOverview(loadedFinancialOverview);
    setSpoilageHistory(loadedSpoilageHistory);
    setDeliveries(loadedDeliveries);
    setEventHistory(loadedEventHistory);
    setReconciliationArchive(loadedReconciliationArchive);
  }, []);

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
