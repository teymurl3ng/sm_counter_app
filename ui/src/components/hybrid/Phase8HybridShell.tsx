import { useCallback, useEffect, useState } from 'react';
import type { BusinessSettingsDto, CustomerDto, CurrentSessionDto, MasterDataDto, ProductDto, SessionHandle, TransactionReferencesDto, VendorDto } from '../../bridge/contract.generated';
import { callNative, NativeBridgeError, paisaToRupeesInput, rupeesToPaisa, validateDecimalInput, verifyNativeContract } from '../../bridge/nativeBridge';
import { Login, OwnerSetup, RecoveryCodeScreen } from './AuthScreens';
import { CenteredMessage } from './HybridUi';
import { ManagementWorkspace } from './ManagementWorkspace';
import { employeeBridgeActions } from './employeeBridgeActions';
import { settingsBridgeActions } from './settingsBridgeActions';
import { errorMessage, type CustomerDraft, type HybridStage, type ProductDraft, type SupplierDraft } from './hybridModels';

export function Phase8HybridShell() {
  const [stage, setStage] = useState<HybridStage>('checking');
  const [session, setSession] = useState<SessionHandle | null>(null);
  const [currentUser, setCurrentUser] = useState<CurrentSessionDto | null>(null);
  const [masterData, setMasterData] = useState<MasterDataDto | null>(null);
  const [businessSettings, setBusinessSettings] = useState<BusinessSettingsDto | null>(null);
  const [transactionReferences, setTransactionReferences] = useState<TransactionReferencesDto | null>(null);
  const [recoveryCode, setRecoveryCode] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const loadAuthorizedData = useCallback(async (activeSession: SessionHandle, user: CurrentSessionDto) => {
    const [loadedMasterData, loadedReferences, loadedSettings] = await Promise.all([
      user.effectivePermissionCodes.includes('manage_master_data') ? callNative('query.masterData', {}, activeSession) : Promise.resolve(null),
      (user.effectivePermissionCodes.includes('attendance') || user.effectivePermissionCodes.includes('manage_business_settings')) ? callNative('query.transactionReferences', {}, activeSession) : Promise.resolve(null),
      user.effectivePermissionCodes.includes('manage_business_settings') ? callNative('query.businessSettings', {}, activeSession) : Promise.resolve(null),
    ]);
    setMasterData(loadedMasterData);
    setTransactionReferences(loadedReferences);
    setBusinessSettings(loadedSettings);
  }, []);

  const clearSession = useCallback(() => {
    setSession(null);
    setCurrentUser(null);
    setMasterData(null);
    setBusinessSettings(null);
    setTransactionReferences(null);
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

  const saveProduct = (draft: ProductDraft, productId?: ProductDto['productId']) => withMasterDataRefresh(async () => {
    if (!session) return;
    const payload = {
      name: draft.name,
      unitCode: draft.unitCode,
      defaultSaleRatePaisaPerUnit: draft.rateRupees.trim() ? rupeesToPaisa(draft.rateRupees) : null,
      inventoryTracking: draft.inventoryTracking,
      notes: draft.notes.trim() || null,
    };
    if (productId) await callNative('command.updateProduct', { productId, ...payload }, session);
    else await callNative('command.createProduct', payload, session);
  }, productId ? 'Product changes saved.' : 'Product added successfully.');

  const customerProfilePayload = (draft: CustomerDraft, accountId: CustomerDto['accountId']) => ({
    accountId,
    phone: draft.phone.trim() || null,
    areaRoute: draft.areaRoute.trim() || null,
    address: draft.address.trim() || null,
    notes: draft.notes.trim() || null,
    creditLimitPaisa: draft.creditLimitRupees.trim() ? rupeesToPaisa(draft.creditLimitRupees) : null,
    dailyQuota: draft.dailyQuota.trim() ? validateDecimalInput(draft.dailyQuota, 'the daily quantity') : null,
    active: draft.active,
  });

  const saveCustomer = async (draft: CustomerDraft, customer?: CustomerDto) => {
    try {
      await withMasterDataRefresh(async () => {
        if (!session) return;
        let accountId = customer?.accountId;
        if (!accountId) accountId = (await callNative('command.createCustomer', { name: draft.name }, session)).entityId;
        else if (draft.name.trim() !== customer?.name) await callNative('command.renameCustomer', { accountId, name: draft.name }, session);
        await callNative('command.saveCustomerProfile', customerProfilePayload(draft, accountId), session);
      }, customer ? 'Customer changes saved.' : 'Customer added successfully.');
    } catch (failure) {
      if (session && currentUser) await loadAuthorizedData(session, currentUser).catch(() => undefined);
      throw failure;
    }
  };

  const setCustomerActive = (customer: CustomerDto, active: boolean) => withMasterDataRefresh(async () => {
    if (!session) return;
    await callNative('command.saveCustomerProfile', customerProfilePayload({
      name: customer.name,
      phone: customer.phone ?? '',
      areaRoute: customer.areaRoute ?? '',
      address: customer.address ?? '',
      notes: customer.notes ?? '',
      creditLimitRupees: paisaToRupeesInput(customer.creditLimitPaisa),
      dailyQuota: customer.dailyQuota ?? '',
      active,
    }, customer.accountId), session);
  }, active ? `${customer.name} restored to active customers.` : `${customer.name} moved to inactive customers.`);

  const supplierProfilePayload = (draft: SupplierDraft, accountId: VendorDto['accountId']) => ({
    accountId,
    phone: draft.phone.trim() || null,
    location: draft.location.trim() || null,
    notes: draft.notes.trim() || null,
    active: draft.active,
  });

  const saveSupplier = async (draft: SupplierDraft, supplier?: VendorDto) => {
    try {
      await withMasterDataRefresh(async () => {
        if (!session) return;
        let accountId = supplier?.accountId;
        if (!accountId) accountId = (await callNative('command.createVendor', { name: draft.name }, session)).entityId;
        else if (draft.name.trim() !== supplier?.name) await callNative('command.renameVendor', { accountId, name: draft.name }, session);
        await callNative('command.saveVendorProfile', supplierProfilePayload(draft, accountId), session);
      }, supplier ? 'Supplier changes saved.' : 'Supplier added successfully.');
    } catch (failure) {
      if (session && currentUser) await loadAuthorizedData(session, currentUser).catch(() => undefined);
      throw failure;
    }
  };

  const setSupplierActive = (supplier: VendorDto, active: boolean) => withMasterDataRefresh(async () => {
    if (!session) return;
    await callNative('command.saveVendorProfile', supplierProfilePayload({
      name: supplier.name,
      phone: supplier.phone ?? '',
      location: supplier.location ?? '',
      notes: supplier.notes ?? '',
      active,
    }, supplier.accountId), session);
  }, active ? `${supplier.name} restored to active suppliers.` : `${supplier.name} moved to inactive suppliers.`);

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
  const settingsActions = settingsBridgeActions(session, withMasterDataRefresh);

  return <ManagementWorkspace
    busy={busy}
    currentUser={currentUser}
    error={error}
    masterData={masterData}
    businessSettings={businessSettings}
    transactionReferences={transactionReferences}
    notice={notice}
    onLogout={logout}
    onSaveProduct={saveProduct}
    onSetProductArchived={(product, archived) => withMasterDataRefresh(async () => {
      if (session) await callNative('command.setProductArchived', { productId: product.productId, archived }, session);
    }, archived ? `${product.name} moved to archived products.` : `${product.name} restored to active products.`)}
    onSaveCustomer={saveCustomer}
    onSetCustomerActive={setCustomerActive}
    onSaveSupplier={saveSupplier}
    onSetSupplierActive={setSupplierActive}
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
