import { useState, type ReactNode } from 'react';
import { ClipboardCheck, LogOut, PackagePlus, Settings, ShoppingBasket, Truck, Users } from 'lucide-react';
import type { AccountReferenceDto, AttendanceDto, AttendanceStatusCode, BusinessSettingsDto, CashierSummaryDto, CustomerDto, CurrentSessionDto, DashboardDto, EmployeeDto, EventMutationResultDto, ExpenseCategoryDto, FinancialOverviewDto, MasterDataDto, ProductDto, PurchaseRecordedResultDto, SaleRecordedResultDto, TransactionReferencesDto, VendorDto } from '../../bridge/contract.generated';
import { CustomerProfilesScreen } from './CustomerProfilesScreen';
import { DailyOperationsWorkspace } from './DailyOperationsWorkspace';
import { EmployeeAttendanceScreen } from './EmployeeAttendanceScreen';
import { ProductManagementScreen } from './ProductManagementScreen';
import { SupplierProfilesScreen } from './SupplierProfilesScreen';
import { SettingsWorkspaceScreen } from './SettingsWorkspaceScreen';
import { plainRole, type BusinessRatesDraft, type CustomerDraft, type EmployeeDraft, type ExpenseDraft, type OpeningStateDraft, type PaymentDraft, type ProductDraft, type ProductionDraft, type PurchaseDraft, type SalaryPaymentDraft, type SaleDraft, type StoreIdentityDraft, type SupplierDraft } from './hybridModels';

type ManagementArea = 'operations' | 'products' | 'customers' | 'suppliers' | 'employees' | 'settings';

export function ManagementWorkspace({
  busy,
  currentUser,
  error,
  masterData,
  dashboard,
  cashierSummary,
  financialOverview,
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
}: {
  busy: boolean;
  currentUser: CurrentSessionDto | null;
  error: string | null;
  masterData: MasterDataDto | null;
  dashboard: DashboardDto | null;
  cashierSummary: CashierSummaryDto | null;
  financialOverview: FinancialOverviewDto | null;
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
  const canOperate = canPurchase || canSell || canProduce || canExpense || canPaySupplier || canPaySalary;
  const [area, setArea] = useState<ManagementArea>(canManage ? 'products' : canOperate ? 'operations' : 'employees');
  const title = area === 'operations' ? 'Daily work' : area === 'products' ? 'Products' : area === 'customers' ? 'Customers' : area === 'suppliers' ? 'Suppliers' : area === 'employees' ? 'Employees' : 'Settings';
  const attendanceEmployees = masterData?.employees.filter((employee) => employee.active).map(({ accountId, name }) => ({ accountId, name })) ?? transactionReferences?.employees ?? [];

  return <main className="min-h-screen bg-[#f7f6f0] pb-10 text-slate-900">
    <header className="bg-emerald-900 px-5 pb-7 pt-7 text-white shadow-lg">
      <div className="mx-auto flex max-w-4xl items-center justify-between gap-4">
        <div><p className="text-xs font-semibold uppercase tracking-[0.2em] text-emerald-200">Saaf Milk</p><h1 className="mt-1 text-2xl font-bold">{title}</h1><p id="signed-in-user" className="mt-1 text-sm text-emerald-100">Signed in as {currentUser?.displayName} · {plainRole(currentUser?.roleCode)}</p></div>
        <button id="logout-button" disabled={busy} onClick={onLogout} className="rounded-xl border border-emerald-600 p-3 text-emerald-50 disabled:opacity-50" aria-label="Sign out"><LogOut className="h-5 w-5" /></button>
      </div>
    </header>
    <nav className="border-b border-slate-200 bg-white px-4" aria-label="Management areas">
      <div className="mx-auto flex max-w-4xl gap-2 overflow-x-auto py-2">
        {canOperate && <AreaButton id="operations-nav" active={area === 'operations'} onClick={() => setArea('operations')}><ShoppingBasket className="h-4 w-4" />Daily work</AreaButton>}
        {canManage && <AreaButton id="products-nav" active={area === 'products'} onClick={() => setArea('products')}><PackagePlus className="h-4 w-4" />Products</AreaButton>}
        {canManage && <AreaButton id="customers-nav" active={area === 'customers'} onClick={() => setArea('customers')}><Users className="h-4 w-4" />Customers</AreaButton>}
        {canManage && <AreaButton id="suppliers-nav" active={area === 'suppliers'} onClick={() => setArea('suppliers')}><Truck className="h-4 w-4" />Suppliers</AreaButton>}
        {(canManage || canAttend) && <AreaButton id="employees-nav" active={area === 'employees'} onClick={() => setArea('employees')}><ClipboardCheck className="h-4 w-4" />Employees</AreaButton>}
        {(canManage || canManageSettings) && <AreaButton id="settings-nav" active={area === 'settings'} onClick={() => setArea('settings')}><Settings className="h-4 w-4" />Settings</AreaButton>}
      </div>
    </nav>
    <div className="mx-auto max-w-4xl px-4 py-6">
      {area === 'operations' && canOperate ? (
        <DailyOperationsWorkspace busy={busy} canCollectCustomer={canSell} canExpense={canExpense} canPaySalary={canPaySalary} canPaySupplier={canPaySupplier} canProduce={canProduce} canPurchase={canPurchase} canSell={canSell} dashboard={dashboard} cashierSummary={cashierSummary} financialOverview={financialOverview} references={transactionReferences} error={error} notice={notice} onRecordExpense={onRecordExpense} onRecordPayment={onRecordPayment} onRecordProduction={onRecordProduction} onRecordPurchase={onRecordPurchase} onRecordSalaryPayment={onRecordSalaryPayment} onRecordSale={onRecordSale} />
      ) : area === 'settings' && (canManage || canManageSettings) ? (
        <SettingsWorkspaceScreen busy={busy} canManageSettings={canManageSettings} categories={masterData?.expenseCategories ?? businessSettings?.expenseCategories ?? null} masterData={masterData} references={transactionReferences} settings={businessSettings} error={error} notice={notice} onSaveStore={onSaveStoreIdentity} onSaveRates={onUpdateBusinessSettings} onSaveOpening={onSaveOpeningState} onCreateCategory={onCreateExpenseCategory} onSetCategoryArchived={onSetExpenseCategoryArchived} />
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

function AreaButton({ id, active, onClick, children }: { id: string; active: boolean; onClick(): void; children: ReactNode }) {
  return <button id={id} onClick={onClick} className={`flex min-w-max flex-1 items-center justify-center gap-1.5 rounded-xl px-3 py-2.5 text-xs font-bold sm:text-sm ${active ? 'bg-emerald-50 text-emerald-800' : 'text-slate-500'}`}>{children}</button>;
}

function Unavailable() {
  return <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm"><h2 className="font-bold">Management is not available for this account</h2><p className="mt-2 text-sm text-slate-600">Ask the owner to use an account with permission to manage shop records.</p></section>;
}
