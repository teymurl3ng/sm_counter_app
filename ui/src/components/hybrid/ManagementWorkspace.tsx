import { useState } from 'react';
import { LogOut, PackagePlus, Users } from 'lucide-react';
import type { CustomerDto, CurrentSessionDto, MasterDataDto, ProductDto } from '../../bridge/contract.generated';
import { CustomerProfilesScreen } from './CustomerProfilesScreen';
import { ProductManagementScreen } from './ProductManagementScreen';
import { plainRole, type CustomerDraft, type ProductDraft } from './hybridModels';

export function ManagementWorkspace({ busy, currentUser, error, masterData, notice, onLogout, onSaveProduct, onSetProductArchived, onSaveCustomer, onSetCustomerActive }: {
  busy: boolean;
  currentUser: CurrentSessionDto | null;
  error: string | null;
  masterData: MasterDataDto | null;
  notice: string | null;
  onLogout(): void;
  onSaveProduct(draft: ProductDraft, productId?: ProductDto['productId']): Promise<void>;
  onSetProductArchived(product: ProductDto, archived: boolean): Promise<void>;
  onSaveCustomer(draft: CustomerDraft, customer?: CustomerDto): Promise<void>;
  onSetCustomerActive(customer: CustomerDto, active: boolean): Promise<void>;
}) {
  const [area, setArea] = useState<'products' | 'customers'>('products');
  const canManage = currentUser?.effectivePermissionCodes.includes('manage_master_data') === true;
  return <main className="min-h-screen bg-[#f7f6f0] pb-10 text-slate-900">
    <header className="bg-emerald-900 px-5 pb-7 pt-7 text-white shadow-lg"><div className="mx-auto flex max-w-4xl items-center justify-between gap-4"><div><p className="text-xs font-semibold uppercase tracking-[0.2em] text-emerald-200">Saaf Milk</p><h1 className="mt-1 text-2xl font-bold">{area === 'products' ? 'Products' : 'Customers'}</h1><p id="signed-in-user" className="mt-1 text-sm text-emerald-100">Signed in as {currentUser?.displayName} · {plainRole(currentUser?.roleCode)}</p></div><button id="logout-button" disabled={busy} onClick={onLogout} className="rounded-xl border border-emerald-600 p-3 text-emerald-50 disabled:opacity-50" aria-label="Sign out"><LogOut className="h-5 w-5" /></button></div></header>
    <nav className="border-b border-slate-200 bg-white px-4" aria-label="Management areas"><div className="mx-auto flex max-w-4xl gap-2 py-2"><button id="products-nav" onClick={() => setArea('products')} className={`flex flex-1 items-center justify-center gap-2 rounded-xl px-3 py-2.5 text-sm font-bold ${area === 'products' ? 'bg-emerald-50 text-emerald-800' : 'text-slate-500'}`}><PackagePlus className="h-4 w-4" />Products</button><button id="customers-nav" onClick={() => setArea('customers')} className={`flex flex-1 items-center justify-center gap-2 rounded-xl px-3 py-2.5 text-sm font-bold ${area === 'customers' ? 'bg-emerald-50 text-emerald-800' : 'text-slate-500'}`}><Users className="h-4 w-4" />Customers</button></div></nav>
    <div className="mx-auto max-w-4xl px-4 py-6">{!canManage ? <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm"><h2 className="font-bold">Management is not available for this account</h2><p className="mt-2 text-sm text-slate-600">Ask the owner to use an account with permission to manage shop records.</p></section> : area === 'products' ? <ProductManagementScreen busy={busy} masterData={masterData} error={error} notice={notice} onSave={onSaveProduct} onSetArchived={onSetProductArchived} /> : <CustomerProfilesScreen busy={busy} customers={masterData?.customers ?? null} error={error} notice={notice} onSave={onSaveCustomer} onSetActive={onSetCustomerActive} />}</div>
  </main>;
}
