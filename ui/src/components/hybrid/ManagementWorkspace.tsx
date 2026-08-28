import { useState, type ReactNode } from 'react';
import { ClipboardCheck, LogOut, PackagePlus, Truck, Users } from 'lucide-react';
import type { AccountReferenceDto, AttendanceDto, AttendanceStatusCode, CustomerDto, CurrentSessionDto, EmployeeDto, MasterDataDto, ProductDto, TransactionReferencesDto, VendorDto } from '../../bridge/contract.generated';
import { CustomerProfilesScreen } from './CustomerProfilesScreen';
import { EmployeeAttendanceScreen } from './EmployeeAttendanceScreen';
import { ProductManagementScreen } from './ProductManagementScreen';
import { SupplierProfilesScreen } from './SupplierProfilesScreen';
import { plainRole, type CustomerDraft, type EmployeeDraft, type ProductDraft, type SupplierDraft } from './hybridModels';

type ManagementArea = 'products' | 'customers' | 'suppliers' | 'employees';

export function ManagementWorkspace({
  busy,
  currentUser,
  error,
  masterData,
  transactionReferences,
  notice,
  onLogout,
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
}: {
  busy: boolean;
  currentUser: CurrentSessionDto | null;
  error: string | null;
  masterData: MasterDataDto | null;
  transactionReferences: TransactionReferencesDto | null;
  notice: string | null;
  onLogout(): void;
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
}) {
  const canManage = currentUser?.effectivePermissionCodes.includes('manage_master_data') === true;
  const canAttend = currentUser?.effectivePermissionCodes.includes('attendance') === true;
  const [area, setArea] = useState<ManagementArea>(canManage ? 'products' : 'employees');
  const title = area === 'products' ? 'Products' : area === 'customers' ? 'Customers' : area === 'suppliers' ? 'Suppliers' : 'Employees';
  const attendanceEmployees = masterData?.employees.filter((employee) => employee.active).map(({ accountId, name }) => ({ accountId, name })) ?? transactionReferences?.employees ?? [];

  return <main className="min-h-screen bg-[#f7f6f0] pb-10 text-slate-900">
    <header className="bg-emerald-900 px-5 pb-7 pt-7 text-white shadow-lg">
      <div className="mx-auto flex max-w-4xl items-center justify-between gap-4">
        <div><p className="text-xs font-semibold uppercase tracking-[0.2em] text-emerald-200">Saaf Milk</p><h1 className="mt-1 text-2xl font-bold">{title}</h1><p id="signed-in-user" className="mt-1 text-sm text-emerald-100">Signed in as {currentUser?.displayName} · {plainRole(currentUser?.roleCode)}</p></div>
        <button id="logout-button" disabled={busy} onClick={onLogout} className="rounded-xl border border-emerald-600 p-3 text-emerald-50 disabled:opacity-50" aria-label="Sign out"><LogOut className="h-5 w-5" /></button>
      </div>
    </header>
    <nav className="border-b border-slate-200 bg-white px-4" aria-label="Management areas">
      <div className="mx-auto flex max-w-4xl gap-2 py-2">
        {canManage && <AreaButton id="products-nav" active={area === 'products'} onClick={() => setArea('products')}><PackagePlus className="h-4 w-4" />Products</AreaButton>}
        {canManage && <AreaButton id="customers-nav" active={area === 'customers'} onClick={() => setArea('customers')}><Users className="h-4 w-4" />Customers</AreaButton>}
        {canManage && <AreaButton id="suppliers-nav" active={area === 'suppliers'} onClick={() => setArea('suppliers')}><Truck className="h-4 w-4" />Suppliers</AreaButton>}
        {(canManage || canAttend) && <AreaButton id="employees-nav" active={area === 'employees'} onClick={() => setArea('employees')}><ClipboardCheck className="h-4 w-4" />Employees</AreaButton>}
      </div>
    </nav>
    <div className="mx-auto max-w-4xl px-4 py-6">
      {area === 'employees' && (canManage || canAttend) ? (
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
  return <button id={id} onClick={onClick} className={`flex flex-1 items-center justify-center gap-1.5 rounded-xl px-2 py-2.5 text-xs font-bold sm:text-sm ${active ? 'bg-emerald-50 text-emerald-800' : 'text-slate-500'}`}>{children}</button>;
}

function Unavailable() {
  return <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm"><h2 className="font-bold">Management is not available for this account</h2><p className="mt-2 text-sm text-slate-600">Ask the owner to use an account with permission to manage shop records.</p></section>;
}
