import type { Int64String, ProductUnitCode, SalePaymentKindCode } from '../../bridge/contract.generated';

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

export type StoreIdentityDraft = {
  storeName: string;
  phone: string;
  address: string;
};

export type BusinessRatesDraft = {
  vendorMilkRateRupees: string;
  vendorDeliveryFeeRupees: string;
  tsStandardPercent: string;
  retailMilkRateRupees: string;
  retailYogurtRateRupees: string;
  defaultDeliverySurchargeRupees: string;
};

export type OpeningStateDraft = {
  openingDate: string;
  productQuantities: Record<Int64String, string>;
  accountBalancesRupees: Record<Int64String, string>;
  notes: string;
};

export type PurchaseDraft = {
  vendorAccountId: Int64String;
  occurredAt: string;
  rawLiters: string;
  fatPercent: string;
  lrReading: string;
  notes: string;
};

export type SaleDraft = {
  productId: Int64String;
  occurredAt: string;
  channelKind: 'retail' | 'wholesale';
  entryMode: 'quantity' | 'value';
  unitRateRupees: string;
  quantity: string;
  valueRupees: string;
  paymentKind: SalePaymentKindCode;
  paymentAccountId: Int64String | null;
  customerAccountId: Int64String | null;
  surchargeEnabled: boolean;
  surchargeRupees: string;
  notes: string;
};

export type ProductionDraft = {
  occurredAt: string;
  milkLitersUsed: string;
  yogurtKgProduced: string;
  notes: string;
};

export type ExpenseDraft = {
  expenseCategoryAccountId: Int64String;
  paymentAccountId: Int64String;
  amountRupees: string;
  occurredAt: string;
  notes: string;
};

export type PaymentDraft = {
  kind: 'supplier' | 'customer';
  partyAccountId: Int64String;
  paymentAccountId: Int64String;
  amountRupees: string;
  occurredAt: string;
  notes: string;
};

export type SalaryPaymentDraft = {
  employeeAccountId: Int64String;
  paymentAccountId: Int64String;
  amountRupees: string;
  occurredAt: string;
  notes: string;
};

export type SpoilageDraft = {
  productId: Int64String;
  quantity: string;
  occurredAt: string;
  reasonCode: string;
  notes: string;
};

export type DeliveryDraft = {
  customerAccountId: Int64String;
  productId: Int64String;
  quantity: string;
  route: string;
  businessDate: string;
  saleEventId: Int64String | null;
  notes: string;
};

export function todayInput(): string {
  const date = new Date();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}

export type OpeningAccountOption = {
  accountId: Int64String;
  name: string;
  kind: 'Cash or bank' | 'Customer owes shop' | 'Shop owes supplier';
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
