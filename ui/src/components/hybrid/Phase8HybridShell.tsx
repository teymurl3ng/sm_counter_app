import { useCallback, useEffect, useState } from 'react';
import type { BusinessSettingsDto, CashierSummaryDto, CurrentSessionDto, DashboardDto, DeliveryDto, FinancialOverviewDto, IsoDateString, MasterDataDto, SessionHandle, SpoilageDto, TransactionReferencesDto } from '../../bridge/contract.generated';
import { callNative, NativeBridgeError, verifyNativeContract } from '../../bridge/nativeBridge';
import { Login, OwnerSetup, RecoveryCodeScreen } from './AuthScreens';
import { CenteredMessage } from './HybridUi';
import { ManagementWorkspace } from './ManagementWorkspace';
import { employeeBridgeActions } from './employeeBridgeActions';
import { dailyOperationsBridgeActions } from './dailyOperationsBridgeActions';
import { deliveryBridgeActions } from './deliveryBridgeActions';
import { ledgerBridgeActions } from './ledgerBridgeActions';
import { masterDataBridgeActions } from './masterDataBridgeActions';
import { settingsBridgeActions } from './settingsBridgeActions';
import { errorMessage, type HybridStage } from './hybridModels';

export function Phase8HybridShell() {
  const [stage, setStage] = useState<HybridStage>('checking');
  const [session, setSession] = useState<SessionHandle | null>(null);
  const [currentUser, setCurrentUser] = useState<CurrentSessionDto | null>(null);
  const [masterData, setMasterData] = useState<MasterDataDto | null>(null);
  const [businessSettings, setBusinessSettings] = useState<BusinessSettingsDto | null>(null);
  const [transactionReferences, setTransactionReferences] = useState<TransactionReferencesDto | null>(null);
  const [dashboard, setDashboard] = useState<DashboardDto | null>(null);
  const [cashierSummary, setCashierSummary] = useState<CashierSummaryDto | null>(null);
  const [financialOverview, setFinancialOverview] = useState<FinancialOverviewDto | null>(null);
  const [spoilageHistory, setSpoilageHistory] = useState<ReadonlyArray<SpoilageDto> | null>(null);
  const [deliveries, setDeliveries] = useState<ReadonlyArray<DeliveryDto> | null>(null);
  const [recoveryCode, setRecoveryCode] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const loadAuthorizedData = useCallback(async (activeSession: SessionHandle, user: CurrentSessionDto) => {
    const today = localDate(0);
    const tomorrow = localDate(1);
    const [loadedMasterData, loadedReferences, loadedSettings, loadedDashboard, loadedCashierSummary, loadedFinancialOverview, loadedSpoilageHistory, loadedDeliveries] = await Promise.all([
      user.effectivePermissionCodes.includes('manage_master_data') ? callNative('query.masterData', {}, activeSession) : Promise.resolve(null),
      user.effectivePermissionCodes.includes('view_operational') ? callNative('query.transactionReferences', {}, activeSession) : Promise.resolve(null),
      user.effectivePermissionCodes.includes('manage_business_settings') ? callNative('query.businessSettings', {}, activeSession) : Promise.resolve(null),
      user.effectivePermissionCodes.includes('view_operational') ? callNative('query.dashboard', { startDateInclusive: today, endDateExclusive: tomorrow }, activeSession) : Promise.resolve(null),
      user.effectivePermissionCodes.includes('sale_and_collection') ? callNative('query.cashierSummary', { startDateInclusive: today, endDateExclusive: tomorrow }, activeSession) : Promise.resolve(null),
      user.effectivePermissionCodes.includes('view_full_financial') ? callNative('query.financialOverview', { startDateInclusive: today, endDateExclusive: tomorrow }, activeSession) : Promise.resolve(null),
      user.effectivePermissionCodes.includes('production_and_spoilage') ? callNative('query.spoilageHistory', {}, activeSession) : Promise.resolve(null),
      user.effectivePermissionCodes.includes('delivery') ? callNative('query.deliveries', {}, activeSession) : Promise.resolve(null),
    ]);
    setMasterData(loadedMasterData);
    setTransactionReferences(loadedReferences);
    setBusinessSettings(loadedSettings);
    setDashboard(loadedDashboard);
    setCashierSummary(loadedCashierSummary);
    setFinancialOverview(loadedFinancialOverview);
    setSpoilageHistory(loadedSpoilageHistory);
    setDeliveries(loadedDeliveries);
  }, []);

  const clearSession = useCallback(() => {
    setSession(null);
    setCurrentUser(null);
    setMasterData(null);
    setBusinessSettings(null);
    setTransactionReferences(null);
    setDashboard(null);
    setCashierSummary(null);
    setFinancialOverview(null);
    setSpoilageHistory(null);
    setDeliveries(null);
    setRecoveryCode(null);
    setNotice(null);
    setStage('login');
  }, []);

  useEffect(() => {
    let cancelled = false;
    void (async () => {
      try {
        await verifyNativeContract();
        const status = await callNative('query.bootstrapStatus', {});
        if (!cancelled) setStage(status.needsOwnerBootstrap ? 'setup' : 'login');
      } catch (failure) {
        if (!cancelled) {
          setError(errorMessage(failure));
          setStage('blocked');
        }
      }
    })();
    return () => { cancelled = true; };
  }, []);

  useEffect(() => {
    const resume = () => {
      if (document.visibilityState !== 'visible' || !session) return;
      void (async () => {
        try {
          const user = await callNative('query.currentSession', {}, session);
          setCurrentUser(user);
          await loadAuthorizedData(session, user);
        } catch (failure) {
          if (failure instanceof NativeBridgeError && failure.code === 'session_expired') {
            clearSession();
            setError('Your session expired. Sign in again.');
          } else {
            setError(errorMessage(failure));
          }
        }
      })();
    };
    document.addEventListener('visibilitychange', resume);
    return () => document.removeEventListener('visibilitychange', resume);
  }, [clearSession, loadAuthorizedData, session]);

  const withMasterDataRefresh = async (operation: () => Promise<void>, success: string) => {
    if (!session || !currentUser) return;
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      await operation();
      await loadAuthorizedData(session, currentUser);
      setNotice(success);
    } catch (failure) {
      setError(errorMessage(failure));
      throw failure;
    } finally {
      setBusy(false);
    }
  };

  const withResultRefresh = async <T,>(operation: () => Promise<T>, success: string): Promise<T> => {
    if (!session || !currentUser) throw new Error('Sign in to continue.');
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const result = await operation();
      await loadAuthorizedData(session, currentUser);
      setNotice(success);
      return result;
    } catch (failure) {
      setError(errorMessage(failure));
      throw failure;
    } finally {
      setBusy(false);
    }
  };

  if (stage === 'checking') return <CenteredMessage title="Opening Saaf Milk" message="Checking the secure app connection…" />;
  if (stage === 'blocked') return <CenteredMessage title="Saaf Milk cannot open" message={error ?? 'The secure app connection is unavailable.'} />;
  if (stage === 'setup') return <OwnerSetup busy={busy} error={error} onSubmit={async (loginName, displayName, passphrase) => {
    setBusy(true);
    setError(null);
    try {
      const result = await callNative('command.auth.bootstrapOwner', { loginName, displayName, passphrase });
      setSession(result.sessionHandle);
      setCurrentUser(result.currentSession);
      await loadAuthorizedData(result.sessionHandle, result.currentSession);
      setRecoveryCode(result.oneTimeRecoveryCode);
      setStage('recovery');
    } catch (failure) {
      setError(errorMessage(failure));
    } finally {
      setBusy(false);
    }
  }} />;
  if (stage === 'login') return <Login busy={busy} error={error} onSubmit={async (loginName, passphrase) => {
    setBusy(true);
    setError(null);
    try {
      const result = await callNative('command.auth.login', { loginName, passphrase });
      setSession(result.sessionHandle);
      setCurrentUser(result.currentSession);
      await loadAuthorizedData(result.sessionHandle, result.currentSession);
      setStage('ready');
    } catch (failure) {
      setError(errorMessage(failure));
    } finally {
      setBusy(false);
    }
  }} />;
  if (stage === 'recovery' && recoveryCode) return <RecoveryCodeScreen recoveryCode={recoveryCode} onSaved={() => { setRecoveryCode(null); setStage('ready'); }} />;

  const logout = () => {
    if (!session) return;
    setBusy(true);
    setError(null);
    void callNative('command.auth.logout', {}, session)
      .then(clearSession)
      .catch((failure) => {
        if (failure instanceof NativeBridgeError && failure.code === 'session_expired') clearSession();
        else setError(errorMessage(failure));
      })
      .finally(() => setBusy(false));
  };

  const employeeActions = employeeBridgeActions(session, withMasterDataRefresh);
  const masterDataActions = masterDataBridgeActions(session, withMasterDataRefresh, async () => {
    if (session && currentUser) await loadAuthorizedData(session, currentUser);
  });
  const settingsActions = settingsBridgeActions(session, withMasterDataRefresh);
  const dailyOperationsActions = dailyOperationsBridgeActions(session, withResultRefresh);
  const deliveryActions = deliveryBridgeActions(session, withResultRefresh);
  const ledgerActions = ledgerBridgeActions(session);

  return <ManagementWorkspace
    busy={busy}
    currentUser={currentUser}
    error={error}
    masterData={masterData}
    dashboard={dashboard}
    cashierSummary={cashierSummary}
    financialOverview={financialOverview}
    spoilageHistory={spoilageHistory}
    deliveries={deliveries}
    businessSettings={businessSettings}
    transactionReferences={transactionReferences}
    notice={notice}
    onLogout={logout}
    onRecordPurchase={dailyOperationsActions.recordPurchase}
    onRecordSale={dailyOperationsActions.recordSale}
    onRecordProduction={dailyOperationsActions.recordProduction}
    onRecordExpense={dailyOperationsActions.recordExpense}
    onRecordPayment={dailyOperationsActions.recordPayment}
    onRecordSalaryPayment={dailyOperationsActions.recordSalaryPayment}
    onRecordSpoilage={dailyOperationsActions.recordSpoilage}
    onCreateDelivery={deliveryActions.createDelivery}
    onTransitionDelivery={deliveryActions.transitionDelivery}
    onLinkDeliverySale={deliveryActions.linkDeliverySale}
    onArchiveDelivery={deliveryActions.archiveDelivery}
    onLoadAccountLedger={ledgerActions.loadAccountLedger}
    onSaveProduct={masterDataActions.saveProduct}
    onSetProductArchived={masterDataActions.setProductArchived}
    onSaveCustomer={masterDataActions.saveCustomer}
    onSetCustomerActive={masterDataActions.setCustomerActive}
    onSaveSupplier={masterDataActions.saveSupplier}
    onSetSupplierActive={masterDataActions.setSupplierActive}
    onSaveEmployee={employeeActions.saveEmployee}
    onSetEmployeeActive={employeeActions.setEmployeeActive}
    onLoadAttendance={employeeActions.loadAttendance}
    onRecordAttendance={employeeActions.recordAttendance}
    onSaveStoreIdentity={settingsActions.saveStoreIdentity}
    onUpdateBusinessSettings={settingsActions.updateBusinessSettings}
    onSaveOpeningState={settingsActions.saveOpeningState}
    onCreateExpenseCategory={settingsActions.createExpenseCategory}
    onSetExpenseCategoryArchived={settingsActions.setExpenseCategoryArchived}
  />;
}

function localDate(daysFromToday: number): IsoDateString {
  const date = new Date();
  date.setDate(date.getDate() + daysFromToday);
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}` as IsoDateString;
}
