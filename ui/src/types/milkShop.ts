export type SalesChannel = 'cash_retail' | 'credit_delivery' | 'cash_wholesale' | 'credit_wholesale';
export type PaymentMethod = 'cash' | 'online' | 'bank_transfer' | 'credit';
export type AccountType = 'retail' | 'wholesale';
export type ExpenseCategory = 'Salary' | 'Rent' | 'Electricity' | 'Internet' | 'Water' | 'Poly Bags' | 'LPG' | 'Petrol' | 'Misc' | string;

export type SpoilageReason = 'Spoiled / Curdled / Sour' | 'Spilled / Accident' | 'Quality Issue' | 'Other';

export interface Employee {
  id: string;
  name: string;
  role: string;
  phone: string;
  salary: number;
  salaryType: 'monthly' | 'weekly' | 'daily';
  joinDate: string;
  isActive: boolean;
}

export interface AttendanceEntry {
  id: string;
  date: string;
  employeeId: string;
  employeeName: string;
  status: 'present' | 'absent' | 'half_day';
  notes?: string;
}

export interface Product {
  id: string;
  name: string;
  unit: 'L' | 'Kg' | 'Item' | 'Bottle' | string;
  salePrice: number;
  costRate: number;
  trackInInventory: boolean;
  isArchived?: boolean;
  purchased?: number;
  sold?: number;
  spoiled?: number;
  expectedStock?: number;
  currentStock?: number;
  isCoreMilk?: boolean;
  isCoreYogurt?: boolean;
}

export interface Customer {
  id: string;
  name: string;
  accountType: AccountType;
  phone: string;
  areaRoute: string;
  creditLimit: number;
  addressNote?: string;
  isActive: boolean;
  balance: number; // Positive means customer owes shop
  createdAt: string;
  lastDelivery?: string;
  defaultDailyQuota?: number; // liters
}

export interface Supplier {
  id: string;
  name: string;
  phone: string;
  addressLocation: string;
  notes?: string;
  isActive: boolean;
  balance: number; // Positive means shop owes supplier
  createdAt: string;
  lastDelivery?: string;
  defaultMilkRate?: number;
}

export interface Transaction {
  id: string;
  date: string;
  timestamp: number;
  type: 'sale' | 'customer_payment' | 'milk_purchase' | 'supplier_payment' | 'expense' | 'spoilage' | 'daily_close';
  salesChannel?: SalesChannel;
  contactId?: string;
  contactName?: string;
  contactType?: 'customer' | 'supplier' | 'employee' | 'other';
  productId?: string;
  productName?: string;
  quantity?: number;
  unit?: string;
  unitPrice?: number;
  discount?: number;
  totalAmount: number;
  cashAmount: number;
  creditAmount: number;
  paymentMethod: PaymentMethod;
  notes?: string;
  category?: string;
  attachment?: string; // Base64 string for bill/receipt image
  time?: string;
}

export interface DailyDeliveryEntry {
  customerId: string;
  customerName: string;
  quantity: number;
  rate: number;
  totalAmount: number;
  isDelivered: boolean;
}

export interface SupplierDeliveryEntry {
  supplierId: string;
  supplierName: string;
  productId?: string;
  productName?: string;
  liters: number;
  rate: number; // Base rate
  fat?: number;
  lr?: number;
  snf?: number;
  ts?: number;
  adjustedRate?: number;
  totalAmount: number;
  payNow: boolean;
  paymentMethod: PaymentMethod;
  amountPaid: number;
}

export interface WholesaleEntry {
  id: string;
  buyerName: string;
  productId: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  totalAmount: number;
  paymentType: 'cash' | 'credit';
  customerId?: string;
  notes?: string;
}

export interface ExpenseEntry {
  id: string;
  date: string;
  category: ExpenseCategory;
  description: string;
  amount: number;
  paymentMethod: PaymentMethod;
}

export interface SpoilageEntry {
  id: string;
  date: string;
  productId: string;
  productName: string;
  quantity: number;
  costRate: number;
  totalLoss: number;
  reason: SpoilageReason;
  notes?: string;
}

export interface DailyClose {
  id: string;
  date: string;
  closedAt: string;
  isLocked: boolean;
  supplierDeliveries: SupplierDeliveryEntry[];
  customerDeliveries: DailyDeliveryEntry[];
  retailSales: {
    productId: string;
    productName: string;
    quantity: number;
    salePrice: number;
    total: number;
  }[];
  wholesaleSales: WholesaleEntry[];
  expenses: ExpenseEntry[];
  spoilages: SpoilageEntry[];
  summary: {
    deliveryCreditRevenue: number;
    shopRetailCashRevenue: number;
    wholesaleRevenue: number;
    totalRevenue: number;
    totalCashInflows: number;
    totalCashOutflows: number;
    netCashDrawerChange: number;
    countedDrawerCash?: number;
    closingNotes?: string;
  };
}

export interface StockCountAudit {
  id: string;
  date: string;
  measuredCounts: {
    productId: string;
    productName: string;
    measuredQuantity: number;
    unit: string;
    costRate: number;
    variance?: number;
  }[];
  notes?: string;
  createdAt: string;
}

export interface BusinessSettings {
  storeName: string;
  startingCashInHand: number;
  openingStockValue: number;
  accountingStartDate: string;
  expenseCategories: string[];
  currencySymbol: string;
  theme?: 'light' | 'dark';
}

export interface AuditLog {
  id: string;
  action: 'CREATE' | 'UPDATE' | 'DELETE' | 'EXPORT' | 'RESTORE';
  category: 'SETTINGS' | 'DAILY_CLOSE' | 'PRODUCT' | 'CUSTOMER' | 'SUPPLIER' | 'EXPENSE' | 'SALE';
  description: string;
  timestamp: string;
  details?: string;
}
