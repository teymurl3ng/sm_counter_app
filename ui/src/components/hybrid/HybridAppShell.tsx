import { useMemo } from 'react';
import { Login, OwnerSetup, RecoveryCodeScreen } from './AuthScreens';
import { CenteredMessage } from './HybridUi';
import { ManagementWorkspace } from './ManagementWorkspace';
import { employeeBridgeActions } from './employeeBridgeActions';
import { dailyOperationsBridgeActions } from './dailyOperationsBridgeActions';
import { deliveryBridgeActions } from './deliveryBridgeActions';
import { ledgerBridgeActions } from './ledgerBridgeActions';
import { historyBridgeActions } from './historyBridgeActions';
import { reconciliationBridgeActions } from './reconciliationBridgeActions';
import { reportBridgeActions } from './reportBridgeActions';
import { masterDataBridgeActions } from './masterDataBridgeActions';
import { settingsBridgeActions } from './settingsBridgeActions';
import { costingPlanningBridgeActions } from './costingPlanningBridgeActions';
import { securityBridgeActions } from './securityBridgeActions';
import { backupBridgeActions } from './backupBridgeActions';
import { errorMessage } from './hybridModels';
import { useAuthorizedWorkspaceData } from './useAuthorizedWorkspaceData';
import { useHybridSession } from './useHybridSession';
import { useUsableViewport } from './useUsableViewport';
import type { RefreshPlan } from './authorizedDataRefresh';

export function HybridAppShell() {
  useUsableViewport();
  const {
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
  } = useAuthorizedWorkspaceData();
  const hybridSession = useHybridSession(loadAuthorizedData, clearAuthorizedData);
  const { stage, session, currentUser, recoveryCode, error, notice, busy, setError, setNotice, setBusy } = hybridSession;

  const reportActions = useMemo(() => reportBridgeActions(session), [session]);
  const costingPlanningActions = useMemo(() => costingPlanningBridgeActions(session), [session]);
  const securityActions = useMemo(() => securityBridgeActions(session), [session]);
  const backupActions = useMemo(() => backupBridgeActions(session), [session]);

  const withRefresh = async <T,>(operation: () => Promise<T>, success: string, refresh: RefreshPlan): Promise<T> => {
    if (!session || !currentUser) throw new Error('Sign in to continue.');
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const result = await operation();
      await refreshAuthorizedData(session, currentUser, refresh);
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
  if (stage === 'setup') return <OwnerSetup busy={busy} error={error} onSubmit={hybridSession.bootstrapOwner} />;
  if (stage === 'login') return <Login busy={busy} error={error} notice={notice} onSubmit={hybridSession.login} onRecover={hybridSession.recoverOwner} />;
  if (stage === 'recovery' && recoveryCode) return <RecoveryCodeScreen recoveryCode={recoveryCode} onSaved={hybridSession.acknowledgeRecoveryCode} />;

  const employeeActions = employeeBridgeActions(session, withRefresh);
  const masterDataActions = masterDataBridgeActions(session, withRefresh, async (refresh) => {
    if (session && currentUser) await refreshAuthorizedData(session, currentUser, refresh);
  });
  const settingsActions = settingsBridgeActions(session, withRefresh);
  const dailyOperationsActions = dailyOperationsBridgeActions(session, withRefresh);
  const deliveryActions = deliveryBridgeActions(session, withRefresh);
  const ledgerActions = ledgerBridgeActions(session);
  const historyActions = historyBridgeActions(session, withRefresh);
  const reconciliationActions = reconciliationBridgeActions(session, withRefresh);
  const restoreBackup = async (ticket: Parameters<typeof backupActions.restoreBackup>[0]) => {
    const result = await backupActions.restoreBackup(ticket);
    hybridSession.finishRestore();
    return result;
  };
  const restoreCloudBackup = async (ticket: Parameters<typeof backupActions.restoreCloudBackup>[0]) => {
    const result = await backupActions.restoreCloudBackup(ticket);
    hybridSession.finishRestore();
    return result;
  };
  return <ManagementWorkspace
    busy={busy}
    currentUser={currentUser}
    initialArea={hybridSession.restoredArea}
    error={error}
    masterData={masterData}
    dashboard={dashboard}
    cashierSummary={cashierSummary}
    financialOverview={financialOverview}
    spoilageHistory={spoilageHistory}
    deliveries={deliveries}
    eventHistory={eventHistory}
    reconciliationArchive={reconciliationArchive}
    businessSettings={businessSettings}
    transactionReferences={transactionReferences}
    notice={notice}
    onLogout={hybridSession.logout}
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
    onLoadEventDetail={historyActions.loadEventDetail}
    onCorrectEvent={historyActions.correctEvent}
    onRecordCashCount={reconciliationActions.recordCount}
    onCloseBusinessDate={reconciliationActions.closeDate}
    onReopenBusinessDate={reconciliationActions.reopenDate}
    onLoadReports={reportActions.loadReports}
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
    costingPlanningActions={costingPlanningActions}
    securityActions={securityActions}
    onChangeOwnPassphrase={hybridSession.changeOwnPassphrase}
    backupActions={backupActions}
    onRestoreBackup={restoreBackup}
    onRestoreCloudBackup={restoreCloudBackup}
  />;
}
