import { useEffect, useState } from 'react';
import { BookOpen, Calculator, ClipboardCheck, DatabaseBackup, History as HistoryIcon, LayoutDashboard, LogOut, PackageCheck, PackagePlus, Scale, Settings, ShieldCheck, ShoppingBasket, Truck, Users } from 'lucide-react';
import type { AccountLedgerDto, AccountReferenceDto, AttendanceDto, AttendanceStatusCode, BusinessSettingsDto, CashCountResultDto, CashierSummaryDto, CustomerDto, CurrentSessionDto, DailyCloseResultDto, DashboardDto, DeliveryDto, DeliveryStatusCode, EmployeeDto, EntityMutationResultDto, EventDetailDto, EventHistoryDto, EventMutationResultDto, ExpenseCategoryDto, FinancialOverviewDto, Int64String, MasterDataDto, ProductDto, PurchaseRecordedResultDto, ReconciliationArchiveDto, SaleRecordedResultDto, SpoilageDto, TransactionReferencesDto, VendorDto } from '../../bridge/contract.generated';
import { AccountLedgerWorkspace } from './AccountLedgerWorkspace';
import { CustomerProfilesScreen } from './CustomerProfilesScreen';
import { DailyOperationsWorkspace } from './DailyOperationsWorkspace';
import { DeliveryWorkspaceScreen } from './DeliveryWorkspaceScreen';
import { HistoryWorkspace } from './HistoryWorkspace';
import { ReconciliationWorkspace } from './ReconciliationWorkspace';
import { EmployeeAttendanceScreen } from './EmployeeAttendanceScreen';
import { ProductManagementScreen } from './ProductManagementScreen';
import { SupplierProfilesScreen } from './SupplierProfilesScreen';
import { SettingsWorkspaceScreen } from './SettingsWorkspaceScreen';
import { DashboardReportsWorkspace } from './DashboardReportsWorkspace';
import { CostingPlanningWorkspace } from './CostingPlanningWorkspace';
import type { CostingPlanningActions } from './costingPlanningBridgeActions';
import type { ReportBundle } from './reportBridgeActions';
import type { SecurityActions } from './securityBridgeActions';
import { SecurityWorkspace } from './SecurityWorkspace';
import type { BackupActions } from './backupBridgeActions';
import { BackupRestoreWorkspace } from './BackupRestoreWorkspace';
import { HybridNavigation, type ManagementArea, type NavigationItem } from './HybridNavigation';
import { publishActiveArea } from '../../bridge/lifecycleBridge';
import { plainRole, type BusinessRatesDraft, type CashCountDraft, type CorrectionDraft, type CustomerDraft, type DeliveryDraft, type EmployeeDraft, type ExpenseDraft, type OpeningStateDraft, type PaymentDraft, type ProductDraft, type ProductionDraft, type PurchaseDraft, type ReopenDayDraft, type SalaryPaymentDraft, type SaleDraft, type SpoilageDraft, type StoreIdentityDraft, type SupplierDraft } from './hybridModels';

export function ManagementWorkspace({
  busy,
  currentUser,
  initialArea,
  error,
  masterData,
  dashboard,
  cashierSummary,
  financialOverview,
  spoilageHistory,
  deliveries,
  eventHistory,
  reconciliationArchive,
  businessSettings,
  transactionReferences,
  notice,
  onLogout,
  onRecordPurchase,
  onRecordSale,
  onRecordProduction,
  onRecordExpense,
  onRecordPayment,
  onRecordSalaryPayment,
  onRecordSpoilage,
  onCreateDelivery,
  onTransitionDelivery,
  onLinkDeliverySale,
  onArchiveDelivery,
  onLoadAccountLedger,
  onLoadEventDetail,
  onCorrectEvent,
  onRecordCashCount,
  onCloseBusinessDate,
  onReopenBusinessDate,
  onLoadReports,
  onSaveProduct,
  onSetProductArchived,
  onSaveCustomer,
  onSetCustomerActive,
  onSaveSupplier,
  onSetSupplierActive,
  onSaveEmployee,
  onSetEmployeeActive,
  onLoadAttendance,
  onRecordAttendance,
  onSaveStoreIdentity,
  onUpdateBusinessSettings,
  onSaveOpeningState,
  onCreateExpenseCategory,
  onSetExpenseCategoryArchived,
  costingPlanningActions,
  securityActions,
  onChangeOwnPassphrase,
  backupActions,
  onRestoreBackup,
}: {
  busy: boolean;
  currentUser: CurrentSessionDto | null;
  initialArea: ManagementArea | null;
  error: string | null;
  masterData: MasterDataDto | null;
  dashboard: DashboardDto | null;
  cashierSummary: CashierSummaryDto | null;
  financialOverview: FinancialOverviewDto | null;
  spoilageHistory: ReadonlyArray<SpoilageDto> | null;
  deliveries: ReadonlyArray<DeliveryDto> | null;
  eventHistory: ReadonlyArray<EventHistoryDto> | null;
  reconciliationArchive: ReconciliationArchiveDto | null;
  businessSettings: BusinessSettingsDto | null;
  transactionReferences: TransactionReferencesDto | null;
  notice: string | null;
  onLogout(): void;
  onRecordPurchase(draft: PurchaseDraft): Promise<PurchaseRecordedResultDto>;
  onRecordSale(draft: SaleDraft): Promise<SaleRecordedResultDto>;
  onRecordProduction(draft: ProductionDraft): Promise<EventMutationResultDto>;
  onRecordExpense(draft: ExpenseDraft): Promise<EventMutationResultDto>;
  onRecordPayment(draft: PaymentDraft): Promise<EventMutationResultDto>;
  onRecordSalaryPayment(draft: SalaryPaymentDraft): Promise<EventMutationResultDto>;
  onRecordSpoilage(draft: SpoilageDraft): Promise<EventMutationResultDto>;
  onCreateDelivery(draft: DeliveryDraft): Promise<EntityMutationResultDto>;
  onTransitionDelivery(delivery: DeliveryDto, status: DeliveryStatusCode, notes: string): Promise<void>;
  onLinkDeliverySale(deliveryId: DeliveryDto['deliveryId'], saleEventId: Int64String): Promise<void>;
  onArchiveDelivery(delivery: DeliveryDto): Promise<void>;
  onLoadAccountLedger(accountId: Int64String): Promise<AccountLedgerDto>;
  onLoadEventDetail(eventId: Int64String): Promise<EventDetailDto>;
  onCorrectEvent(draft: CorrectionDraft): Promise<EventMutationResultDto>;
  onRecordCashCount(draft: CashCountDraft, accountId: Int64String): Promise<CashCountResultDto>;
  onCloseBusinessDate(date: string, countId: Int64String, reason: string): Promise<DailyCloseResultDto>;
  onReopenBusinessDate(draft: ReopenDayDraft): Promise<DailyCloseResultDto>;
  onLoadReports(startDate: string, throughDate: string, canViewCashier: boolean, canViewFinancial: boolean): Promise<ReportBundle>;
  onSaveProduct(draft: ProductDraft, productId?: ProductDto['productId']): Promise<void>;
  onSetProductArchived(product: ProductDto, archived: boolean): Promise<void>;
  onSaveCustomer(draft: CustomerDraft, customer?: CustomerDto): Promise<void>;
  onSetCustomerActive(customer: CustomerDto, active: boolean): Promise<void>;
  onSaveSupplier(draft: SupplierDraft, supplier?: VendorDto): Promise<void>;
  onSetSupplierActive(supplier: VendorDto, active: boolean): Promise<void>;
  onSaveEmployee(draft: EmployeeDraft, employee?: EmployeeDto): Promise<void>;
  onSetEmployeeActive(employee: EmployeeDto, active: boolean): Promise<void>;
  onLoadAttendance(businessDate: string): Promise<ReadonlyArray<AttendanceDto>>;
  onRecordAttendance(employeeAccountId: AccountReferenceDto['accountId'], businessDate: string, statusCode: AttendanceStatusCode, notes: string): Promise<void>;
  onSaveStoreIdentity(draft: StoreIdentityDraft): Promise<void>;
  onUpdateBusinessSettings(draft: BusinessRatesDraft): Promise<void>;
  onSaveOpeningState(draft: OpeningStateDraft, passphrase: string, correction: boolean): Promise<void>;
  onCreateExpenseCategory(displayName: string): Promise<void>;
  onSetExpenseCategoryArchived(category: ExpenseCategoryDto, archived: boolean): Promise<void>;
  costingPlanningActions: CostingPlanningActions;
  securityActions: SecurityActions;
  onChangeOwnPassphrase(currentPassphrase: string, newPassphrase: string): Promise<void>;
  backupActions: BackupActions;
  onRestoreBackup(ticket: Parameters<BackupActions['restoreBackup']>[0]): ReturnType<BackupActions['restoreBackup']>;
}) {
  const canManage = currentUser?.effectivePermissionCodes.includes('manage_master_data') === true;
  const canAttend = currentUser?.effectivePermissionCodes.includes('attendance') === true;
  const canManageSettings = currentUser?.effectivePermissionCodes.includes('manage_business_settings') === true;
  const canPurchase = currentUser?.effectivePermissionCodes.includes('purchase') === true;
  const canSell = currentUser?.effectivePermissionCodes.includes('sale_and_collection') === true;
  const canProduce = currentUser?.effectivePermissionCodes.includes('production_and_spoilage') === true;
  const canExpense = currentUser?.effectivePermissionCodes.includes('expense') === true;
  const canPaySupplier = currentUser?.effectivePermissionCodes.includes('supplier_payment') === true;
  const canPaySalary = currentUser?.effectivePermissionCodes.includes('salary_payment') === true;
  const canDeliver = currentUser?.effectivePermissionCodes.includes('delivery') === true;
  const canViewFinancial = currentUser?.effectivePermissionCodes.includes('view_full_financial') === true;
  const canViewOperational = currentUser?.effectivePermissionCodes.includes('view_operational') === true;
  const canViewHistory = canViewOperational;
  const canCorrect = currentUser?.effectivePermissionCodes.includes('correction') === true;
  const canCashClose = currentUser?.effectivePermissionCodes.includes('cash_count_and_close') === true;
  const canReopen = currentUser?.effectivePermissionCodes.includes('reopen_day') === true;
  const canManageUsers = currentUser?.effectivePermissionCodes.includes('manage_staff_users') === true;
  const canViewAudit = currentUser?.effectivePermissionCodes.includes('view_security_audit') === true;
  const canExportBackup = currentUser?.effectivePermissionCodes.includes('create_export_backup') === true;
  const canRestoreBackup = currentUser?.effectivePermissionCodes.includes('restore_backup') === true;
  const canOperate = canPurchase || canSell || canProduce || canExpense || canPaySupplier || canPaySalary;
  const defaultArea: ManagementArea = canManage ? 'products' : canOperate ? 'operations' : 'employees';
  const navigationItems: NavigationItem[] = [
    canViewOperational && { area: 'dashboard', id: 'dashboard-nav', label: 'Dashboard', group: 'Everyday work', icon: LayoutDashboard },
    canOperate && { area: 'operations', id: 'operations-nav', label: 'Daily work', group: 'Everyday work', icon: ShoppingBasket },
    canDeliver && { area: 'deliveries', id: 'deliveries-nav', label: 'Deliveries', group: 'Everyday work', icon: PackageCheck },
    canViewFinancial && { area: 'accounts', id: 'accounts-nav', label: 'Accounts', group: 'Records & closing', icon: BookOpen },
    canViewHistory && { area: 'history', id: 'history-nav', label: 'History', group: 'Records & closing', icon: HistoryIcon },
    canCashClose && { area: 'cash-close', id: 'cash-close-nav', label: 'Cash close', group: 'Records & closing', icon: Scale },
    canManage && { area: 'products', id: 'products-nav', label: 'Products', group: 'People & stock', icon: PackagePlus },
    canManage && { area: 'customers', id: 'customers-nav', label: 'Customers', group: 'People & stock', icon: Users },
    canManage && { area: 'suppliers', id: 'suppliers-nav', label: 'Suppliers', group: 'People & stock', icon: Truck },
    (canManage || canAttend) && { area: 'employees', id: 'employees-nav', label: 'Employees', group: 'People & stock', icon: ClipboardCheck },
    canManageSettings && { area: 'costing-planning', id: 'costing-planning-nav', label: 'Cost & plans', group: 'Shop controls', icon: Calculator },
    (canManage || canManageSettings) && { area: 'settings', id: 'settings-nav', label: 'Settings', group: 'Shop controls', icon: Settings },
    { area: 'access', id: 'access-nav', label: 'Access', group: 'Shop controls', icon: ShieldCheck },
    canExportBackup && { area: 'backup', id: 'backup-nav', label: 'Backup', group: 'Shop controls', icon: DatabaseBackup },
  ].filter((item): item is NavigationItem => Boolean(item));
  const [area, setArea] = useState<ManagementArea>(() =>
    initialArea && navigationItems.some((item) => item.area === initialArea) ? initialArea : defaultArea,
  );
  const title = area === 'dashboard' ? 'Dashboard' : area === 'operations' ? 'Daily work' : area === 'deliveries' ? 'Deliveries' : area === 'accounts' ? 'Accounts' : area === 'history' ? 'History' : area === 'cash-close' ? 'Cash close' : area === 'products' ? 'Products' : area === 'customers' ? 'Customers' : area === 'suppliers' ? 'Suppliers' : area === 'employees' ? 'Employees' : area === 'costing-planning' ? 'Cost & plans' : area === 'access' ? 'Access & users' : area === 'backup' ? 'Backup & restore' : 'Settings';
  useEffect(() => publishActiveArea(area), [area]);
  const attendanceEmployees = masterData?.employees.filter((employee) => employee.active).map(({ accountId, name }) => ({ accountId, name })) ?? transactionReferences?.employees ?? [];

  return <main className="min-h-screen bg-[#f7f6f0] pb-24 text-slate-900 md:pb-10">
    <header className="bg-emerald-900 px-5 pb-7 pt-7 text-white shadow-lg" style={{ paddingTop: 'max(1.75rem, env(safe-area-inset-top))' }}>
      <div className="mx-auto flex max-w-4xl items-center justify-between gap-4">
        <div><p className="text-xs font-semibold uppercase tracking-[0.2em] text-emerald-200">Saaf Milk</p><h1 className="mt-1 text-2xl font-bold">{title}</h1><p id="signed-in-user" className="mt-1 text-sm text-emerald-100">Signed in as {currentUser?.displayName} · {plainRole(currentUser?.roleCode)}</p></div>
        <button id="logout-button" disabled={busy} onClick={onLogout} className="rounded-xl border border-emerald-600 p-3 text-emerald-50 disabled:opacity-50" aria-label="Sign out"><LogOut className="h-5 w-5" /></button>
      </div>
    </header>
    <HybridNavigation items={navigationItems} activeArea={area} onSelect={setArea} />
    <div className="mx-auto max-w-4xl px-4 py-6">
      {area === 'dashboard' && canViewOperational ? (
        <DashboardReportsWorkspace canViewCashier={canSell} canViewFinancial={canViewFinancial} onLoad={onLoadReports} />
      ) : area === 'operations' && canOperate ? (
        <DailyOperationsWorkspace busy={busy} canCollectCustomer={canSell} canExpense={canExpense} canPaySalary={canPaySalary} canPaySupplier={canPaySupplier} canProduce={canProduce} canPurchase={canPurchase} canSell={canSell} dashboard={dashboard} cashierSummary={cashierSummary} financialOverview={financialOverview} spoilageHistory={spoilageHistory} references={transactionReferences} error={error} notice={notice} onRecordExpense={onRecordExpense} onRecordPayment={onRecordPayment} onRecordProduction={onRecordProduction} onRecordPurchase={onRecordPurchase} onRecordSalaryPayment={onRecordSalaryPayment} onRecordSale={onRecordSale} onRecordSpoilage={onRecordSpoilage} />
      ) : area === 'deliveries' && canDeliver ? (
        <DeliveryWorkspaceScreen busy={busy} deliveries={deliveries} references={transactionReferences} error={error} notice={notice} onCreate={onCreateDelivery} onTransition={onTransitionDelivery} onLinkSale={onLinkDeliverySale} onArchive={onArchiveDelivery} />
      ) : area === 'accounts' && canViewFinancial ? (
        <AccountLedgerWorkspace busy={busy} overview={financialOverview} references={transactionReferences} error={error} notice={notice} onLoadLedger={onLoadAccountLedger} onRecordPayment={onRecordPayment} />
      ) : area === 'history' && canViewHistory ? (
        <HistoryWorkspace busy={busy} events={eventHistory} canCorrect={canCorrect} error={error} notice={notice} onLoadDetail={onLoadEventDetail} onCorrect={onCorrectEvent} />
      ) : area === 'cash-close' && canCashClose ? (
        <ReconciliationWorkspace busy={busy} archive={reconciliationArchive} cashAccountId={transactionReferences?.paymentAccounts.find((account) => account.name.trim().toLowerCase() === 'cash')?.accountId ?? null} canReopen={canReopen} error={error} notice={notice} onRecordCount={onRecordCashCount} onCloseDate={onCloseBusinessDate} onReopenDate={onReopenBusinessDate} />
      ) : area === 'settings' && (canManage || canManageSettings) ? (
        <SettingsWorkspaceScreen busy={busy} canManageSettings={canManageSettings} categories={masterData?.expenseCategories ?? businessSettings?.expenseCategories ?? null} masterData={masterData} references={transactionReferences} settings={businessSettings} error={error} notice={notice} onSaveStore={onSaveStoreIdentity} onSaveRates={onUpdateBusinessSettings} onSaveOpening={onSaveOpeningState} onCreateCategory={onCreateExpenseCategory} onSetCategoryArchived={onSetExpenseCategoryArchived} />
      ) : area === 'costing-planning' && canManageSettings ? (
        <CostingPlanningWorkspace actions={costingPlanningActions} settings={businessSettings} masterData={masterData} />
      ) : area === 'access' && currentUser ? (
        <SecurityWorkspace currentUser={currentUser} employees={masterData?.employees ?? []} actions={securityActions} canManageUsers={canManageUsers} canViewAudit={canViewAudit} onChangeOwnPassphrase={onChangeOwnPassphrase} />
      ) : area === 'backup' && canExportBackup ? (
        <BackupRestoreWorkspace actions={backupActions} canRestore={canRestoreBackup} onRestored={onRestoreBackup} />
      ) : area === 'employees' && (canManage || canAttend) ? (
        <EmployeeAttendanceScreen busy={busy} canManage={canManage} employees={masterData?.employees ?? null} attendanceEmployees={attendanceEmployees} error={error} notice={notice} onSaveEmployee={onSaveEmployee} onSetEmployeeActive={onSetEmployeeActive} onLoadAttendance={onLoadAttendance} onRecordAttendance={onRecordAttendance} />
      ) : !canManage ? <Unavailable /> : area === 'products' ? (
        <ProductManagementScreen busy={busy} masterData={masterData} error={error} notice={notice} onSave={onSaveProduct} onSetArchived={onSetProductArchived} />
      ) : area === 'customers' ? (
        <CustomerProfilesScreen busy={busy} customers={masterData?.customers ?? null} error={error} notice={notice} onSave={onSaveCustomer} onSetActive={onSetCustomerActive} />
      ) : (
        <SupplierProfilesScreen busy={busy} suppliers={masterData?.vendors ?? null} error={error} notice={notice} onSave={onSaveSupplier} onSetActive={onSetSupplierActive} />
      )}
    </div>
  </main>;
}

function Unavailable() {
  return <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm"><h2 className="font-bold">Management is not available for this account</h2><p className="mt-2 text-sm text-slate-600">Ask the owner to use an account with permission to manage shop records.</p></section>;
}
