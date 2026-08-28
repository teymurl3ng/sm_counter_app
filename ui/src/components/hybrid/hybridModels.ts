import type { ProductUnitCode } from '../../bridge/contract.generated';

export type HybridStage = 'checking' | 'setup' | 'login' | 'recovery' | 'ready' | 'blocked';

export type ProductDraft = {
  name: string;
  unitCode: ProductUnitCode;
  rateRupees: string;
  inventoryTracking: boolean;
  notes: string;
};

export type CustomerDraft = {
  name: string;
  phone: string;
  areaRoute: string;
  address: string;
  notes: string;
  creditLimitRupees: string;
  dailyQuota: string;
  active: boolean;
};

export type SupplierDraft = {
  name: string;
  phone: string;
  location: string;
  notes: string;
  active: boolean;
};

export type EmployeeDraft = {
  name: string;
  roleLabel: string;
  phone: string;
  referenceSalaryRupees: string;
  salaryPeriodCode: string;
  joinDate: string;
  active: boolean;
};

export function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : 'The app could not complete this request.';
}

export function plainRole(roleCode: string | undefined): string {
  if (roleCode === 'owner') return 'Owner';
  if (roleCode === 'manager') return 'Manager';
  if (roleCode === 'cashier') return 'Cashier';
  if (roleCode === 'delivery') return 'Delivery staff';
  return roleCode ?? 'User';
}
