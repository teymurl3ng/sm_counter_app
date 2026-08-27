import { useState, useEffect, useMemo, useRef } from 'react';
import {
  Product,
  Customer,
  Supplier,
  Employee,
  AttendanceEntry,
  DailyDeliveryEntry,
  SupplierDeliveryEntry,
  WholesaleEntry,
  ExpenseEntry,
  SpoilageEntry,
  BusinessSettings,
  AuditLog,
  PaymentMethod,
  DailyClose
} from '../types/milkShop';

const STORAGE_KEY_PREFIX = 'milkshop_data_clean_v8_prebaked';

// One-time auto-purge for stale legacy browser data
if (typeof window !== 'undefined') {
  const isCleaned = localStorage.getItem('milkshop_data_clean_v8_prebaked_purged');
  if (!isCleaned) {
    Object.keys(localStorage).forEach((key) => {
      if (key.includes('milkshop')) {
        localStorage.removeItem(key);
      }
    });
    localStorage.setItem('milkshop_data_clean_v8_prebaked_purged', 'true');
  }
}

const getTodayDateStr = () => new Date().toISOString().split('T')[0];

const INITIAL_PRODUCTS: Product[] = [
  {
    id: 'prod-milk',
    name: 'Fresh Milk',
    unit: 'L',
    salePrice: 180,
    costRate: 150,
    trackInInventory: true,
    currentStock: 250,
    purchased: 250,
    sold: 0,
    spoiled: 0,
    isCoreMilk: true
  },
  {
    id: 'prod-yogurt',
    name: 'Yogurt',
    unit: 'Kg',
    salePrice: 220,
    costRate: 185,
    trackInInventory: true,
    currentStock: 100,
    purchased: 100,
    sold: 0,
    spoiled: 0,
    isCoreYogurt: true
  },
  {
    id: 'prod-paneer',
    name: 'Paneer (Cottage Cheese)',
    unit: 'Kg',
    salePrice: 650,
    costRate: 520,
    trackInInventory: true,
    currentStock: 40,
    purchased: 40,
    sold: 0,
    spoiled: 0
  },
  {
    id: 'prod-cream',
    name: 'Fresh Cream',
    unit: 'Kg',
    salePrice: 450,
    costRate: 360,
    trackInInventory: true,
    currentStock: 30,
    purchased: 30,
    sold: 0,
    spoiled: 0
  },
  {
    id: 'prod-butter',
    name: 'Desi Butter',
    unit: 'Kg',
    salePrice: 900,
    costRate: 720,
    trackInInventory: true,
    currentStock: 25,
    purchased: 25,
    sold: 0,
    spoiled: 0
  },
  {
    id: 'prod-ghee',
    name: 'Desi Ghee',
    unit: 'Kg',
    salePrice: 1600,
    costRate: 1300,
    trackInInventory: true,
    currentStock: 15,
    purchased: 15,
    sold: 0,
    spoiled: 0
  }
];

const INITIAL_CUSTOMERS: Customer[] = [];

const INITIAL_SUPPLIERS: Supplier[] = [];

const INITIAL_EMPLOYEES: Employee[] = [];

const INITIAL_SETTINGS: BusinessSettings = {
  storeName: '',
  startingCashInHand: 0,
  openingStockValue: 0,
  accountingStartDate: getTodayDateStr(),
  expenseCategories: [
    'Salary',
    'Rent',
    'Electricity',
    'Internet',
    'Water',
    'Poly Bags',
    'LPG',
    'Petrol',
    'Misc'
  ],
  currencySymbol: 'Rs.',
  theme: 'light'
};

export function useMilkShopViewModel() {
  const [selectedDate, setSelectedDate] = useState<string>(getTodayDateStr());
  const [products, setProducts] = useState<Product[]>(() => {
    const saved = localStorage.getItem(`${STORAGE_KEY_PREFIX}_products`);
    return saved ? JSON.parse(saved) : INITIAL_PRODUCTS;
  });

  const [customers, setCustomers] = useState<Customer[]>(() => {
    const saved = localStorage.getItem(`${STORAGE_KEY_PREFIX}_customers`);
    return saved ? JSON.parse(saved) : INITIAL_CUSTOMERS;
  });

  const [suppliers, setSuppliers] = useState<Supplier[]>(() => {
    const saved = localStorage.getItem(`${STORAGE_KEY_PREFIX}_suppliers`);
    return saved ? JSON.parse(saved) : INITIAL_SUPPLIERS;
  });

  const [employees, setEmployees] = useState<Employee[]>(() => {
    const saved = localStorage.getItem(`${STORAGE_KEY_PREFIX}_employees`);
    return saved ? JSON.parse(saved) : INITIAL_EMPLOYEES;
  });

  const [settings, setSettings] = useState<BusinessSettings>(() => {
    const defaultExpenseCategories = [
      'Salary',
      'Rent',
      'Electricity',
      'Internet',
      'Water',
      'Poly Bags',
      'LPG',
      'Petrol',
      'Misc'
    ];
    const saved = localStorage.getItem(`${STORAGE_KEY_PREFIX}_settings`);
    if (saved) {
      const parsed = JSON.parse(saved);
      return {
        theme: 'light',
        ...parsed,
        expenseCategories: defaultExpenseCategories
      };
    }
    return {
      ...INITIAL_SETTINGS,
      theme: 'light',
      expenseCategories: defaultExpenseCategories
    };
  });

  // Daily operations state for the selected date
  const [supplierDeliveries, setSupplierDeliveries] = useState<SupplierDeliveryEntry[]>(() => {
    const saved = localStorage.getItem(`${STORAGE_KEY_PREFIX}_supplierDeliveries_${selectedDate}`);
    return saved ? JSON.parse(saved) : [];
  });

  const [attendances, setAttendances] = useState<AttendanceEntry[]>(() => {
    const saved = localStorage.getItem(`${STORAGE_KEY_PREFIX}_attendances_${selectedDate}`);
    return saved ? JSON.parse(saved) : [];
  });

  const [customerDeliveries, setCustomerDeliveries] = useState<DailyDeliveryEntry[]>(() => {
    const saved = localStorage.getItem(`${STORAGE_KEY_PREFIX}_customerDeliveries_${selectedDate}`);
    return saved ? JSON.parse(saved) : [];
  });

  const [retailSales, setRetailSales] = useState<{
    id: string;
    timestamp: string;
    productId: string;
    productName: string;
    quantity: number;
    salePrice: number;
    total: number;
  }[]>(() => {
    const saved = localStorage.getItem(`${STORAGE_KEY_PREFIX}_retailSales_${selectedDate}`);
    return saved ? JSON.parse(saved) : [];
  });

  const [wholesaleSales, setWholesaleSales] = useState<WholesaleEntry[]>(() => {
    const saved = localStorage.getItem(`${STORAGE_KEY_PREFIX}_wholesaleSales_${selectedDate}`);
    return saved ? JSON.parse(saved) : [];
  });

  const [expenses, setExpenses] = useState<ExpenseEntry[]>(() => {
    const saved = localStorage.getItem(`${STORAGE_KEY_PREFIX}_expenses_${selectedDate}`);
    return saved ? JSON.parse(saved) : [];
  });

  const [spoilages, setSpoilages] = useState<SpoilageEntry[]>(() => {
    const saved = localStorage.getItem(`${STORAGE_KEY_PREFIX}_spoilages_${selectedDate}`);
    return saved ? JSON.parse(saved) : [];
  });

  const [customerPayments, setCustomerPayments] = useState<{
    id: string;
    customerId: string;
    customerName: string;
    amount: number;
    paymentMethod: PaymentMethod;
    date: string;
    time?: string;
    notes?: string;
    attachment?: string;
  }[]>(() => {
    const saved = localStorage.getItem(`${STORAGE_KEY_PREFIX}_customerPayments_${selectedDate}`);
    return saved ? JSON.parse(saved) : [];
  });

  const [supplierPayments, setSupplierPayments] = useState<{
    id: string;
    supplierId: string;
    supplierName: string;
    amount: number;
    paymentMethod: PaymentMethod;
    date: string;
    time?: string;
    notes?: string;
    attachment?: string;
  }[]>(() => {
    const saved = localStorage.getItem(`${STORAGE_KEY_PREFIX}_supplierPayments_${selectedDate}`);
    return saved ? JSON.parse(saved) : [];
  });

  const [auditLogs, setAuditLogs] = useState<AuditLog[]>(() => {
    const saved = localStorage.getItem(`${STORAGE_KEY_PREFIX}_auditLogs`);
    return saved ? JSON.parse(saved) : [];
  });

  const [dailyCloses, setDailyCloses] = useState<DailyClose[]>(() => {
    const saved = localStorage.getItem(`${STORAGE_KEY_PREFIX}_dailyCloses`);
    return saved ? JSON.parse(saved) : [];
  });

  const [isDayLocked, setIsDayLocked] = useState<boolean>(() => {
    const saved = localStorage.getItem(`${STORAGE_KEY_PREFIX}_isDayLocked_${selectedDate}`);
    return saved ? JSON.parse(saved) : false;
  });
  const [countedDrawerCash, setCountedDrawerCash] = useState<number | undefined>(() => {
    const saved = localStorage.getItem(`${STORAGE_KEY_PREFIX}_countedDrawerCash_${selectedDate}`);
    return saved ? JSON.parse(saved) : undefined;
  });

  const loadedDateRef = useRef<string>(selectedDate);

  // Sync state when selectedDate changes to prevent cross-day pollution
  useEffect(() => {
    const getSaved = (keySuffix: string, fallback: any) => {
      const saved = localStorage.getItem(`${STORAGE_KEY_PREFIX}_${keySuffix}_${selectedDate}`);
      return saved ? JSON.parse(saved) : fallback;
    };

    setSupplierDeliveries(getSaved('supplierDeliveries', []));
    setAttendances(getSaved('attendances', []));
    setCustomerDeliveries(getSaved('customerDeliveries', []));
    setRetailSales(getSaved('retailSales', []));
    setWholesaleSales(getSaved('wholesaleSales', []));
    setExpenses(getSaved('expenses', []));
    setSpoilages(getSaved('spoilages', []));
    setCustomerPayments(getSaved('customerPayments', []));
    setSupplierPayments(getSaved('supplierPayments', []));
    
    const savedLock = localStorage.getItem(`${STORAGE_KEY_PREFIX}_isDayLocked_${selectedDate}`);
    setIsDayLocked(savedLock ? JSON.parse(savedLock) : false);

    const savedCounted = localStorage.getItem(`${STORAGE_KEY_PREFIX}_countedDrawerCash_${selectedDate}`);
    setCountedDrawerCash(savedCounted ? JSON.parse(savedCounted) : undefined);

    // Sync ref
    loadedDateRef.current = selectedDate;
  }, [selectedDate]);

  // Persistence effects
  useEffect(() => {
    localStorage.setItem(`${STORAGE_KEY_PREFIX}_products`, JSON.stringify(products));
  }, [products]);

  useEffect(() => {
    localStorage.setItem(`${STORAGE_KEY_PREFIX}_customers`, JSON.stringify(customers));
  }, [customers]);

  useEffect(() => {
    localStorage.setItem(`${STORAGE_KEY_PREFIX}_suppliers`, JSON.stringify(suppliers));
  }, [suppliers]);

  useEffect(() => {
    localStorage.setItem(`${STORAGE_KEY_PREFIX}_employees`, JSON.stringify(employees));
  }, [employees]);

  useEffect(() => {
    localStorage.setItem(`${STORAGE_KEY_PREFIX}_settings`, JSON.stringify(settings));
  }, [settings]);

  useEffect(() => {
    localStorage.setItem(`${STORAGE_KEY_PREFIX}_dailyCloses`, JSON.stringify(dailyCloses));
  }, [dailyCloses]);

  useEffect(() => {
    if (loadedDateRef.current !== selectedDate) return;
    localStorage.setItem(`${STORAGE_KEY_PREFIX}_isDayLocked_${selectedDate}`, JSON.stringify(isDayLocked));
  }, [isDayLocked, selectedDate]);

  useEffect(() => {
    if (loadedDateRef.current !== selectedDate) return;
    if (countedDrawerCash !== undefined) {
      localStorage.setItem(`${STORAGE_KEY_PREFIX}_countedDrawerCash_${selectedDate}`, JSON.stringify(countedDrawerCash));
    } else {
      localStorage.removeItem(`${STORAGE_KEY_PREFIX}_countedDrawerCash_${selectedDate}`);
    }
  }, [countedDrawerCash, selectedDate]);

  useEffect(() => {
    if (loadedDateRef.current !== selectedDate) return;
    localStorage.setItem(`${STORAGE_KEY_PREFIX}_supplierDeliveries_${selectedDate}`, JSON.stringify(supplierDeliveries));
  }, [supplierDeliveries, selectedDate]);

  useEffect(() => {
    if (loadedDateRef.current !== selectedDate) return;
    localStorage.setItem(`${STORAGE_KEY_PREFIX}_attendances_${selectedDate}`, JSON.stringify(attendances));
  }, [attendances, selectedDate]);

  useEffect(() => {
    if (loadedDateRef.current !== selectedDate) return;
    localStorage.setItem(`${STORAGE_KEY_PREFIX}_customerDeliveries_${selectedDate}`, JSON.stringify(customerDeliveries));
  }, [customerDeliveries, selectedDate]);

  useEffect(() => {
    if (loadedDateRef.current !== selectedDate) return;
    localStorage.setItem(`${STORAGE_KEY_PREFIX}_retailSales_${selectedDate}`, JSON.stringify(retailSales));
  }, [retailSales, selectedDate]);

  useEffect(() => {
    if (loadedDateRef.current !== selectedDate) return;
    localStorage.setItem(`${STORAGE_KEY_PREFIX}_wholesaleSales_${selectedDate}`, JSON.stringify(wholesaleSales));
  }, [wholesaleSales, selectedDate]);

  useEffect(() => {
    if (loadedDateRef.current !== selectedDate) return;
    localStorage.setItem(`${STORAGE_KEY_PREFIX}_expenses_${selectedDate}`, JSON.stringify(expenses));
  }, [expenses, selectedDate]);

  useEffect(() => {
    if (loadedDateRef.current !== selectedDate) return;
    localStorage.setItem(`${STORAGE_KEY_PREFIX}_spoilages_${selectedDate}`, JSON.stringify(spoilages));
  }, [spoilages, selectedDate]);

  useEffect(() => {
    if (loadedDateRef.current !== selectedDate) return;
    localStorage.setItem(`${STORAGE_KEY_PREFIX}_customerPayments_${selectedDate}`, JSON.stringify(customerPayments));
  }, [customerPayments, selectedDate]);

  useEffect(() => {
    if (loadedDateRef.current !== selectedDate) return;
    localStorage.setItem(`${STORAGE_KEY_PREFIX}_supplierPayments_${selectedDate}`, JSON.stringify(supplierPayments));
  }, [supplierPayments, selectedDate]);

  useEffect(() => {
    localStorage.setItem(`${STORAGE_KEY_PREFIX}_auditLogs`, JSON.stringify(auditLogs));
  }, [auditLogs]);

  // Derived financial & operational calculations
  const totalSupplierLiters = useMemo(() => {
    return supplierDeliveries.reduce((sum, item) => sum + item.liters, 0);
  }, [supplierDeliveries]);

  const totalSupplierAmount = useMemo(() => {
    return supplierDeliveries.reduce((sum, item) => sum + item.totalAmount, 0);
  }, [supplierDeliveries]);

  const totalSupplierCashPaid = useMemo(() => {
    return supplierDeliveries
      .filter(item => item.payNow && item.paymentMethod === 'cash')
      .reduce((sum, item) => sum + item.amountPaid, 0);
  }, [supplierDeliveries]);

  const totalSupplierOnlinePaid = useMemo(() => {
    return supplierDeliveries
      .filter(item => item.payNow && item.paymentMethod !== 'cash')
      .reduce((sum, item) => sum + item.amountPaid, 0);
  }, [supplierDeliveries]);

  const deliveryDeliveredCount = useMemo(() => {
    return customerDeliveries.filter(d => d.isDelivered).length;
  }, [customerDeliveries]);

  const deliveryPendingCount = useMemo(() => {
    return customerDeliveries.filter(d => !d.isDelivered).length;
  }, [customerDeliveries]);

  const deliveryDeliveredLiters = useMemo(() => {
    return customerDeliveries.filter(d => d.isDelivered).reduce((sum, d) => sum + d.quantity, 0);
  }, [customerDeliveries]);

  const deliveryCreditRevenue = useMemo(() => {
    return customerDeliveries.filter(d => d.isDelivered).reduce((sum, d) => sum + d.totalAmount, 0);
  }, [customerDeliveries]);

  const shopRetailCashRevenue = useMemo(() => {
    return retailSales.reduce((sum, item) => sum + item.total, 0);
  }, [retailSales]);

  const wholesaleCashRevenue = useMemo(() => {
    return wholesaleSales
      .filter(item => item.paymentType === 'cash')
      .reduce((sum, item) => sum + item.totalAmount, 0);
  }, [wholesaleSales]);

  const wholesaleCreditRevenue = useMemo(() => {
    return wholesaleSales
      .filter(item => item.paymentType === 'credit')
      .reduce((sum, item) => sum + item.totalAmount, 0);
  }, [wholesaleSales]);

  const totalWholesaleRevenue = wholesaleCashRevenue + wholesaleCreditRevenue;

  const totalRevenue = useMemo(() => {
    return deliveryCreditRevenue + shopRetailCashRevenue + totalWholesaleRevenue;
  }, [deliveryCreditRevenue, shopRetailCashRevenue, totalWholesaleRevenue]);

  const totalExpenses = useMemo(() => {
    return expenses.reduce((sum, e) => sum + e.amount, 0);
  }, [expenses]);

  const totalCashExpenses = useMemo(() => {
    return expenses.filter(e => e.paymentMethod === 'cash').reduce((sum, e) => sum + e.amount, 0);
  }, [expenses]);

  const totalSpoilageLoss = useMemo(() => {
    return spoilages.reduce((sum, s) => sum + s.totalLoss, 0);
  }, [spoilages]);

  const totalCustomerCashCollected = useMemo(() => {
    return customerPayments.filter(p => p.paymentMethod === 'cash').reduce((sum, p) => sum + p.amount, 0);
  }, [customerPayments]);

  const totalCustomerOnlineCollected = useMemo(() => {
    return customerPayments.filter(p => p.paymentMethod !== 'cash').reduce((sum, p) => sum + p.amount, 0);
  }, [customerPayments]);

  const totalSupplierPaymentMade = useMemo(() => {
    return supplierPayments.filter(p => p.paymentMethod === 'cash').reduce((sum, p) => sum + p.amount, 0);
  }, [supplierPayments]);

  const totalSupplierOnlinePaymentMade = useMemo(() => {
    return supplierPayments.filter(p => p.paymentMethod !== 'cash').reduce((sum, p) => sum + p.amount, 0);
  }, [supplierPayments]);

  // Cash Inflows & Outflows for Register Drawer
  const totalCashInflows = useMemo(() => {
    // Starting drawer + Retail sales cash + Wholesale cash sales + Customer cash debt recovered
    return shopRetailCashRevenue + wholesaleCashRevenue + totalCustomerCashCollected;
  }, [shopRetailCashRevenue, wholesaleCashRevenue, totalCustomerCashCollected]);

  const totalCashOutflows = useMemo(() => {
    // Supplier cash payouts + Cash expenses + Dedicated Supplier payments
    return totalSupplierCashPaid + totalCashExpenses + totalSupplierPaymentMade;
  }, [totalSupplierCashPaid, totalCashExpenses, totalSupplierPaymentMade]);

  const netCashDrawerChange = totalCashInflows - totalCashOutflows;
  const expectedDrawerCash = Math.max(0, settings.startingCashInHand + netCashDrawerChange);
  const drawerVariance = countedDrawerCash !== undefined ? countedDrawerCash - expectedDrawerCash : 0;

  // Total Outstanding Balances
  const totalCustomerReceivables = useMemo(() => {
    return customers.reduce((sum, c) => sum + (c.balance > 0 ? c.balance : 0), 0);
  }, [customers]);

  const totalSupplierPayables = useMemo(() => {
    return suppliers.reduce((sum, s) => sum + (s.balance > 0 ? s.balance : 0), 0);
  }, [suppliers]);

  // Log action helper
  const addLog = (action: AuditLog['action'], category: AuditLog['category'], description: string) => {
    const newLog: AuditLog = {
      id: `log-${Date.now()}`,
      action,
      category,
      description,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    };
    setAuditLogs(prev => [newLog, ...prev.slice(0, 49)]);
  };

  // Operations Handlers
  const addSupplierDelivery = (entry: Omit<SupplierDeliveryEntry, 'totalAmount'>) => {
    // Logic Guard: Prevent negative inputs
    const liters = Math.max(0, entry.liters);
    const rate = Math.max(0, entry.rate);
    const adjustedRate = entry.adjustedRate !== undefined ? Math.max(0, entry.adjustedRate) : undefined;
    const amountPaid = Math.max(0, entry.amountPaid);

    if (liters === 0) return;

    const activeRate = adjustedRate || rate;
    const totalAmount = liters * activeRate;
    const newEntry: SupplierDeliveryEntry = {
      ...entry,
      liters,
      rate,
      adjustedRate,
      amountPaid,
      totalAmount,
    };
    setSupplierDeliveries(prev => [newEntry, ...prev]);

    // Update supplier balance if not fully paid
    const unpaid = Math.max(0, totalAmount - (entry.payNow ? amountPaid : 0));
    if (unpaid > 0) {
      setSuppliers(prev => prev.map(s => {
        if (s.id === entry.supplierId) {
          return {
            ...s,
            balance: Math.max(0, s.balance + unpaid),
            lastDelivery: `Today (${liters} L)`
          };
        }
        return s;
      }));
    }

    // Update milk stock
    setProducts(prev => prev.map(p => {
      if (p.isCoreMilk || p.id === 'prod-milk') {
        return {
          ...p,
          currentStock: Math.max(0, (p.currentStock || 0) + liters),
          expectedStock: Math.max(0, (p.expectedStock || 0) + liters),
          purchased: (p.purchased || 0) + liters
        };
      }
      return p;
    }));

    addLog('CREATE', 'SUPPLIER', `Received ${liters}L milk from ${entry.supplierName} @ Rs. ${activeRate.toFixed(2)}/L`);
  };

  const toggleDeliveryDelivered = (customerId: string) => {
    setCustomerDeliveries(prev => prev.map(d => {
      if (d.customerId === customerId) {
        const nextState = !d.isDelivered;
        // Update customer balance
        setCustomers(custs => custs.map(c => {
          if (c.id === customerId) {
            const balanceDelta = nextState ? d.totalAmount : -d.totalAmount;
            return {
              ...c,
              balance: Math.max(0, c.balance + balanceDelta),
              lastDelivery: nextState ? 'Today' : c.lastDelivery
            };
          }
          return c;
        }));
        return { ...d, isDelivered: nextState };
      }
      return d;
    }));
  };

  const addRetailSale = (productId: string, quantity: number, customTotalPrice?: number) => {
    const prod = products.find(p => p.id === productId);
    if (!prod) return;

    const finalQuantity = customTotalPrice !== undefined 
      ? Number((customTotalPrice / prod.salePrice).toFixed(3)) 
      : quantity;

    const finalTotal = customTotalPrice !== undefined 
      ? customTotalPrice 
      : Number((quantity * prod.salePrice).toFixed(2));

    if (finalQuantity <= 0 || finalTotal <= 0) return;

    const now = new Date();
    const timeStr = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });

    const newSale = {
      id: `retail-${Date.now()}-${Math.random().toString(36).substr(2, 4)}`,
      timestamp: timeStr,
      productId: prod.id,
      productName: prod.name,
      quantity: finalQuantity,
      salePrice: prod.salePrice,
      total: finalTotal
    };

    setRetailSales(prev => [newSale, ...prev]);

    // Update product stock
    setProducts(prev => prev.map(p => {
      if (p.id === productId) {
        return {
          ...p,
          currentStock: Math.max(0, Number(((p.currentStock || 0) - finalQuantity).toFixed(3))),
          sold: Number(((p.sold || 0) + finalQuantity).toFixed(3))
        };
      }
      return p;
    }));

    addLog('CREATE', 'SALE', `Counter sale: ${finalQuantity} ${prod.unit} of ${prod.name} for Rs. ${finalTotal}`);
  };

  const deleteRetailSale = (id: string) => {
    const sale = retailSales.find(s => s.id === id);
    if (!sale) return;

    setRetailSales(prev => prev.filter(s => s.id !== id));

    // Revert product stock
    setProducts(prev => prev.map(p => {
      if (p.id === sale.productId) {
        return {
          ...p,
          currentStock: Number(((p.currentStock || 0) + sale.quantity).toFixed(3)),
          sold: Math.max(0, Number(((p.sold || 0) - sale.quantity).toFixed(3)))
        };
      }
      return p;
    }));

    addLog('DELETE', 'SALE', `Cancelled Counter sale of ${sale.quantity} of ${sale.productName}`);
  };

  const deleteSupplierDelivery = (index: number) => {
    const del = supplierDeliveries[index];
    if (!del) return;
    setSupplierDeliveries(prev => prev.filter((_, i) => i !== index));

    const unpaid = del.totalAmount - (del.payNow ? del.amountPaid : 0);
    if (unpaid > 0) {
      setSuppliers(prev => prev.map(s => {
        if (s.id === del.supplierId) {
          return {
            ...s,
            balance: Math.max(0, s.balance - unpaid)
          };
        }
        return s;
      }));
    }

    setProducts(prev => prev.map(p => {
      if (p.isCoreMilk || p.id === 'prod-milk') {
        return {
          ...p,
          currentStock: Math.max(0, (p.currentStock || 0) - del.liters),
          expectedStock: Math.max(0, (p.expectedStock || 0) - del.liters),
          purchased: Math.max(0, (p.purchased || 0) - del.liters)
        };
      }
      return p;
    }));

    addLog('DELETE', 'SUPPLIER', `Removed supplier delivery of ${del.liters}L from ${del.supplierName}`);
  };

  const deleteCustomerPayment = (id: string) => {
    const pay = customerPayments.find(p => p.id === id);
    if (!pay) return;
    setCustomerPayments(prev => prev.filter(p => p.id !== id));
    setCustomers(prev => prev.map(c => {
      if (c.id === pay.customerId) {
        return {
          ...c,
          balance: c.balance + pay.amount
        };
      }
      return c;
    }));
    addLog('DELETE', 'CUSTOMER', `Removed customer payment record of Rs. ${pay.amount}`);
  };

  const addWholesaleSale = (entry: Omit<WholesaleEntry, 'id' | 'totalAmount'>) => {
    // Logic Guard: Prevent negative inputs
    const quantity = Math.max(0, entry.quantity);
    const unitPrice = Math.max(0, entry.unitPrice);
    
    if (quantity <= 0) return;

    const totalAmount = quantity * unitPrice;
    const newEntry: WholesaleEntry = {
      ...entry,
      quantity,
      unitPrice,
      id: `ws-${Date.now()}`,
      totalAmount,
    };
    setWholesaleSales(prev => [newEntry, ...prev]);

    // If credit, add to customer balance
    if (entry.paymentType === 'credit' && entry.customerId) {
      setCustomers(prev => prev.map(c => {
        if (c.id === entry.customerId) {
          return {
            ...c,
            balance: Math.max(0, c.balance + totalAmount),
            lastDelivery: 'Today (Wholesale)'
          };
        }
        return c;
      }));
    }

    addLog('CREATE', 'SALE', `Wholesale order: ${quantity} units to ${entry.buyerName} (${entry.paymentType.toUpperCase()})`);
  };

  const addExpense = (expense: Omit<ExpenseEntry, 'id'>) => {
    // Logic Guard: Prevent negative amount
    const amount = Math.max(0, expense.amount);
    if (amount <= 0) return;

    const newEntry: ExpenseEntry = {
      ...expense,
      amount,
      id: `exp-${Date.now()}`,
    };
    setExpenses(prev => [newEntry, ...prev]);
    addLog('CREATE', 'EXPENSE', `Expense recorded: Rs. ${amount} for ${expense.description} (${expense.category})`);
  };

  const deleteExpense = (id: string) => {
    setExpenses(prev => prev.filter(e => e.id !== id));
    addLog('DELETE', 'EXPENSE', `Removed expense entry #${id}`);
  };

  const addSpoilage = (spoilage: Omit<SpoilageEntry, 'id' | 'totalLoss'>) => {
    // Logic Guard: Prevent negative inputs
    const quantity = Math.max(0, spoilage.quantity);
    const costRate = Math.max(0, spoilage.costRate);
    
    if (quantity <= 0) return;

    const totalLoss = quantity * costRate;
    const newEntry: SpoilageEntry = {
      ...spoilage,
      quantity,
      costRate,
      id: `spoil-${Date.now()}`,
      totalLoss,
    };
    setSpoilages(prev => [newEntry, ...prev]);

    // Update stock
    setProducts(prev => prev.map(p => {
      if (p.id === spoilage.productId) {
        return {
          ...p,
          currentStock: Math.max(0, (p.currentStock || 0) - quantity),
          spoiled: (p.spoiled || 0) + quantity
        };
      }
      return p;
    }));

    addLog('CREATE', 'PRODUCT', `Spoilage recorded: ${quantity} of ${spoilage.productName} (Loss: Rs. ${totalLoss})`);
  };

  const recordCustomerPayment = (customerId: string, amount: number, paymentMethod: PaymentMethod, notes?: string, attachment?: string, date?: string) => {
    const cust = customers.find(c => c.id === customerId);
    if (!cust || amount <= 0) return;

    const targetDate = date || selectedDate;
    const now = new Date();
    const timeStr = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

    const paymentRecord = {
      id: `cpay-${Date.now()}`,
      customerId,
      customerName: cust.name,
      amount,
      paymentMethod,
      date: targetDate,
      time: timeStr,
      notes,
      attachment
    };

    setCustomerPayments(prev => [paymentRecord, ...prev]);

    setCustomers(prev => prev.map(c => {
      if (c.id === customerId) {
        return {
          ...c,
          balance: Math.max(0, c.balance - amount),
        };
      }
      return c;
    }));

    addLog('UPDATE', 'CUSTOMER', `Received payment of Rs. ${amount} (${paymentMethod}) from ${cust.name}`);
    return paymentRecord;
  };

  const recordSupplierPayment = (supplierId: string, amount: number, paymentMethod: PaymentMethod, notes?: string, attachment?: string, date?: string) => {
    const sup = suppliers.find(s => s.id === supplierId);
    if (!sup || amount <= 0) return;

    const targetDate = date || selectedDate;
    const now = new Date();
    const timeStr = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

    const paymentRecord = {
      id: `spay-${Date.now()}`,
      supplierId,
      supplierName: sup.name,
      amount,
      paymentMethod,
      date: targetDate,
      time: timeStr,
      notes,
      attachment
    };

    setSupplierPayments(prev => [paymentRecord, ...prev]);

    setSuppliers(prev => prev.map(s => {
      if (s.id === supplierId) {
        return {
          ...s,
          balance: Math.max(0, s.balance - amount),
        };
      }
      return s;
    }));

    addLog('UPDATE', 'SUPPLIER', `Paid Rs. ${amount} (${paymentMethod}) to ${sup.name}${notes ? ` - ${notes}` : ''}`);
    return paymentRecord;
  };

  const addEmployee = (emp: Omit<Employee, 'id' | 'joinDate' | 'isActive'>) => {
    const sanitizedSalary = Math.max(0, emp.salary);
    const newEmp: Employee = {
      ...emp,
      salary: sanitizedSalary,
      id: `emp-${Date.now()}`,
      joinDate: getTodayDateStr(),
      isActive: true,
    };
    setEmployees(prev => [newEmp, ...prev]);
    addLog('CREATE', 'SETTINGS', `Added employee: ${newEmp.name} with salary Rs. ${sanitizedSalary}`);
  };

  const updateEmployee = (id: string, updated: Partial<Employee>) => {
    setEmployees(prev => prev.map(e => e.id === id ? { ...e, ...updated } : e));
    addLog('UPDATE', 'SETTINGS', `Updated employee: ${id}`);
  };

  const deleteEmployee = (id: string) => {
    setEmployees(prev => prev.filter(e => e.id !== id));
    addLog('DELETE', 'SETTINGS', `Deleted employee: ${id}`);
  };

  const recordAttendance = (employeeId: string, status: 'present' | 'absent' | 'half_day', notes?: string) => {
    setAttendances(prev => {
      const existing = prev.findIndex(a => a.employeeId === employeeId);
      const emp = employees.find(e => e.id === employeeId);
      if (!emp) return prev;
      
      const newEntry: AttendanceEntry = {
        id: `att-${Date.now()}-${employeeId}`,
        date: selectedDate,
        employeeId,
        employeeName: emp.name,
        status,
        notes
      };

      if (existing >= 0) {
        const copy = [...prev];
        copy[existing] = newEntry;
        return copy;
      }
      return [...prev, newEntry];
    });
    addLog('UPDATE', 'SETTINGS', `Recorded attendance for employee ${employeeId}`);
  };

  const updateCustomer = (id: string, updated: Partial<Customer>) => {
    setCustomers(prev => prev.map(c => c.id === id ? { ...c, ...updated } : c));
    addLog('UPDATE', 'CUSTOMER', `Updated customer #${id}`);
  };

  const deleteCustomer = (id: string) => {
    setCustomers(prev => prev.filter(c => c.id !== id));
    addLog('DELETE', 'CUSTOMER', `Removed customer #${id}`);
  };

  const updateSupplier = (id: string, updated: Partial<Supplier>) => {
    setSuppliers(prev => prev.map(s => s.id === id ? { ...s, ...updated } : s));
    addLog('UPDATE', 'SUPPLIER', `Updated supplier #${id}`);
  };

  const deleteSupplier = (id: string) => {
    setSuppliers(prev => prev.filter(s => s.id !== id));
    addLog('DELETE', 'SUPPLIER', `Removed supplier #${id}`);
  };

  const updateExpense = (id: string, updated: Partial<ExpenseEntry>) => {
    setExpenses(prev => prev.map(e => e.id === id ? { ...e, ...updated } : e));
    addLog('UPDATE', 'EXPENSE', `Updated expense entry #${id}`);
  };

  const updateWholesaleSale = (id: string, updated: Partial<WholesaleEntry>) => {
    setWholesaleSales(prev => prev.map(w => {
      if (w.id === id) {
        const next = { ...w, ...updated };
        if (updated.quantity !== undefined || updated.unitPrice !== undefined) {
          next.totalAmount = next.quantity * next.unitPrice;
        }
        return next;
      }
      return w;
    }));
    addLog('UPDATE', 'SALE', `Updated wholesale order #${id}`);
  };

  const deleteWholesaleSale = (id: string) => {
    setWholesaleSales(prev => prev.filter(w => w.id !== id));
    addLog('DELETE', 'SALE', `Removed wholesale order #${id}`);
  };

  const addCustomer = (customer: Omit<Customer, 'id' | 'createdAt' | 'balance'>) => {
    const sanitizedLimit = Math.max(0, customer.creditLimit || 0);
    const sanitizedQuota = customer.defaultDailyQuota !== undefined ? Math.max(0, customer.defaultDailyQuota) : undefined;

    const newCust: Customer = {
      ...customer,
      creditLimit: sanitizedLimit,
      defaultDailyQuota: sanitizedQuota,
      id: `cust-${Date.now()}`,
      balance: 0,
      createdAt: getTodayDateStr(),
    };
    setCustomers(prev => [newCust, ...prev]);

    // Also add default delivery if quota exists
    if (sanitizedQuota && sanitizedQuota > 0) {
      setCustomerDeliveries(prev => [...prev, {
        customerId: newCust.id,
        customerName: newCust.name,
        quantity: sanitizedQuota,
        rate: 220,
        totalAmount: sanitizedQuota * 220,
        isDelivered: false,
      }]);
    }

    addLog('CREATE', 'CUSTOMER', `Added new customer: ${newCust.name} (${newCust.areaRoute})`);
  };

  const addSupplier = (supplier: Omit<Supplier, 'id' | 'createdAt' | 'balance'>) => {
    const sanitizedRate = supplier.defaultMilkRate !== undefined ? Math.max(0, supplier.defaultMilkRate) : undefined;
    const newSup: Supplier = {
      ...supplier,
      defaultMilkRate: sanitizedRate,
      id: `sup-${Date.now()}`,
      balance: 0,
      createdAt: getTodayDateStr(),
    };
    setSuppliers(prev => [newSup, ...prev]);
    addLog('CREATE', 'SUPPLIER', `Added new milk supplier: ${newSup.name}`);
  };

  const updateProductStock = (productId: string, newStock: number) => {
    const sanitizedStock = Math.max(0, newStock);
    setProducts(prev => prev.map(p => {
      if (p.id === productId) {
        return {
          ...p,
          currentStock: sanitizedStock,
          expectedStock: sanitizedStock
        };
      }
      return p;
    }));
    addLog('UPDATE', 'PRODUCT', `Audited stock count for product #${productId} to ${sanitizedStock}`);
  };

  const recordYogurtProduction = (milkUsed: number, yogurtMade: number) => {
    const sMilkUsed = Math.max(0, milkUsed);
    const sYogurtMade = Math.max(0, yogurtMade);
    
    if (sMilkUsed <= 0 && sYogurtMade <= 0) return;

    setProducts(prev => prev.map(p => {
      if (p.isCoreMilk || p.id === 'prod-milk') {
        return {
          ...p,
          currentStock: Math.max(0, (p.currentStock || 0) - sMilkUsed),
          expectedStock: Math.max(0, (p.expectedStock || 0) - sMilkUsed)
        };
      }
      if (p.isCoreYogurt || p.id === 'prod-yogurt') {
        return {
          ...p,
          currentStock: (p.currentStock || 0) + sYogurtMade,
          expectedStock: (p.expectedStock || 0) + sYogurtMade,
          purchased: (p.purchased || 0) + sYogurtMade
        };
      }
      return p;
    }));
    addLog('CREATE', 'PRODUCT', `Produced ${sYogurtMade} Kg Yogurt from ${sMilkUsed} L Milk`);
  };

  const finalizeDailyClose = (closingNotes?: string) => {
    setIsDayLocked(true);
    
    const retailSummaryList = products.map(p => {
      const pSales = retailSales.filter(rs => rs.productId === p.id);
      const totalQty = pSales.reduce((sum, item) => sum + item.quantity, 0);
      const totalAmount = pSales.reduce((sum, item) => sum + item.total, 0);
      return {
        productId: p.id,
        productName: p.name,
        quantity: totalQty,
        salePrice: p.salePrice,
        total: totalAmount
      };
    }).filter(p => p.quantity > 0);

    const newClose: DailyClose = {
      id: `close-${selectedDate}-${Date.now()}`,
      date: selectedDate,
      closedAt: new Date().toISOString(),
      isLocked: true,
      supplierDeliveries,
      customerDeliveries,
      retailSales: retailSummaryList,
      wholesaleSales,
      expenses,
      spoilages,
      summary: {
        deliveryCreditRevenue,
        shopRetailCashRevenue,
        wholesaleRevenue: totalWholesaleRevenue,
        totalRevenue,
        totalCashInflows,
        totalCashOutflows,
        netCashDrawerChange,
        countedDrawerCash,
        closingNotes
      }
    };

    setDailyCloses(prev => {
      const filtered = prev.filter(c => c.date !== selectedDate);
      return [...filtered, newClose];
    });

    addLog('CREATE', 'DAILY_CLOSE', `Closed and locked register for ${selectedDate}. Net Drawer Cash: Rs. ${expectedDrawerCash}${closingNotes ? ` (${closingNotes})` : ''}`);
  };

  const reopenDailyClose = (dateStr: string) => {
    setDailyCloses(prev => prev.filter(c => c.date !== dateStr));
    if (selectedDate === dateStr) {
      setIsDayLocked(false);
    } else {
      localStorage.setItem(`${STORAGE_KEY_PREFIX}_isDayLocked_${dateStr}`, JSON.stringify(false));
    }
    addLog('UPDATE', 'DAILY_CLOSE', `Unlocked and reopened register for ${dateStr}`);
  };

  const resetForNewDay = () => {
    setIsDayLocked(false);
    setCountedDrawerCash(undefined);
    setSelectedDate(getTodayDateStr());
    addLog('CREATE', 'DAILY_CLOSE', 'Started new business session');
  };

  const addProduct = (product: Omit<Product, 'id'>) => {
    const isMilk = product.name.toLowerCase().includes('milk');
    const isYogurt = product.name.toLowerCase().includes('yogurt');
    
    const sanitizedStock = Math.max(0, product.currentStock ?? 0);
    const sanitizedSalePrice = Math.max(0, product.salePrice);
    const sanitizedCostRate = Math.max(0, product.costRate);

    const newProduct: Product = {
      ...product,
      id: `prod-${Date.now()}`,
      salePrice: sanitizedSalePrice,
      costRate: sanitizedCostRate,
      currentStock: sanitizedStock,
      expectedStock: sanitizedStock,
      isCoreMilk: isMilk,
      isCoreYogurt: isYogurt,
      purchased: 0,
      sold: 0,
      spoiled: 0,
    };
    setProducts(prev => [...prev, newProduct]);
    addLog('CREATE', 'PRODUCT', `Added product: ${newProduct.name} (${newProduct.salePrice}/${newProduct.unit})`);
  };

  const updateProduct = (id: string, updated: Partial<Product>) => {
    setProducts(prev => prev.map(p => p.id === id ? { ...p, ...updated } : p));
    addLog('UPDATE', 'PRODUCT', `Updated product details for #${id}`);
  };

  const deleteProduct = (id: string) => {
    const prod = products.find(p => p.id === id);
    if (prod && (prod.isCoreMilk || prod.isCoreYogurt || prod.id === 'prod-milk' || prod.id === 'prod-yogurt')) {
      alert('Cannot delete core items. You can toggle them inactive instead.');
      return;
    }

    // Check if part of any transactions
    const inRetail = retailSales.some(s => s.productId === id);
    const inWholesale = wholesaleSales.some(s => s.productId === id);
    const inSpoilage = spoilages.some(s => s.productId === id);
    const inHistorical = dailyCloses.some(dc => 
      dc.retailSales.some(s => s.productId === id) || 
      dc.wholesaleSales.some(s => s.productId === id) || 
      dc.spoilages.some(s => s.productId === id)
    );

    if (inRetail || inWholesale || inSpoilage || inHistorical) {
      alert('Cannot delete this product because it has associated transactions. Please edit it and uncheck "Active" to archive it instead.');
      return;
    }

    setProducts(prev => prev.filter(p => p.id !== id));
    addLog('DELETE', 'PRODUCT', `Removed product #${id}`);
  };

  const addExpenseCategory = (categoryName: string) => {
    if (!categoryName.trim()) return;
    const trimmed = categoryName.trim();
    if (settings.expenseCategories.includes(trimmed)) return;
    setSettings(prev => ({
      ...prev,
      expenseCategories: [...prev.expenseCategories, trimmed]
    }));
    addLog('UPDATE', 'SETTINGS', `Added expense category: ${trimmed}`);
  };

  const removeExpenseCategory = (categoryName: string) => {
    setSettings(prev => ({
      ...prev,
      expenseCategories: prev.expenseCategories.filter(c => c !== categoryName)
    }));
    addLog('UPDATE', 'SETTINGS', `Removed expense category: ${categoryName}`);
  };

  const importAllData = (jsonData: string): boolean => {
    try {
      const parsed = JSON.parse(jsonData);
      if (parsed.products) setProducts(parsed.products);
      if (parsed.customers) setCustomers(parsed.customers);
      if (parsed.suppliers) setSuppliers(parsed.suppliers);
      if (parsed.settings) setSettings(parsed.settings);
      if (parsed.expenses) setExpenses(parsed.expenses);
      if (parsed.wholesaleSales) setWholesaleSales(parsed.wholesaleSales);
      addLog('RESTORE', 'SETTINGS', 'Imported data backup successfully');
      return true;
    } catch {
      return false;
    }
  };

  const clearAllData = () => {
    Object.keys(localStorage).forEach((key) => {
      if (key.includes('milkshop')) {
        localStorage.removeItem(key);
      }
    });
    localStorage.setItem('milkshop_data_v3_cleaned', 'true');

    setSupplierDeliveries([]);
    setCustomerDeliveries([]);
    setRetailSales([]);
    setWholesaleSales([]);
    setExpenses([]);
    setSpoilages([]);
    setCustomerPayments([]);
    setAuditLogs([]);
    setDailyCloses([]);
    setIsDayLocked(false);
    setCountedDrawerCash(undefined);

    setCustomers([]);
    setSuppliers([]);
    setProducts(INITIAL_PRODUCTS);
    setSettings(INITIAL_SETTINGS);

    addLog('RESTORE', 'SETTINGS', 'Reset all data to clean state');
  };

  return {
    // State
    selectedDate,
    setSelectedDate,
    products,
    setProducts,
    customers,
    suppliers,
    employees,
    attendances,
    settings,
    setSettings,
    supplierDeliveries,
    customerDeliveries,
    retailSales,
    wholesaleSales,
    expenses,
    spoilages,
    customerPayments,
    supplierPayments,
    auditLogs,
    dailyCloses,
    isDayLocked,
    countedDrawerCash,
    setCountedDrawerCash,

    // Calculated Metrics
    totalSupplierLiters,
    totalSupplierAmount,
    totalSupplierCashPaid,
    totalSupplierOnlinePaid,
    deliveryDeliveredCount,
    deliveryPendingCount,
    deliveryDeliveredLiters,
    deliveryCreditRevenue,
    shopRetailCashRevenue,
    wholesaleCashRevenue,
    wholesaleCreditRevenue,
    totalWholesaleRevenue,
    totalRevenue,
    totalExpenses,
    totalCashExpenses,
    totalSpoilageLoss,
    totalCustomerCashCollected,
    totalCustomerOnlineCollected,
    totalCashInflows,
    totalCashOutflows,
    totalSupplierPaymentMade,
    totalSupplierOnlinePaymentMade,
    netCashDrawerChange,
    expectedDrawerCash,
    drawerVariance,
    totalCustomerReceivables,
    totalSupplierPayables,

    // Operations Handlers
    addSupplierDelivery,
    toggleDeliveryDelivered,
    addRetailSale,
    deleteRetailSale,
    deleteSupplierDelivery,
    deleteCustomerPayment,
    addWholesaleSale,
    addExpense,
    deleteExpense,
    addSpoilage,
    recordCustomerPayment,
    recordSupplierPayment,
    addCustomer,
    updateCustomer,
    deleteCustomer,
    addSupplier,
    updateSupplier,
    deleteSupplier,
    addEmployee,
    updateEmployee,
    deleteEmployee,
    recordAttendance,
    updateProductStock,
    recordYogurtProduction,
    addProduct,
    updateProduct,
    deleteProduct,
    addExpenseCategory,
    removeExpenseCategory,
    importAllData,
    finalizeDailyClose,
    reopenDailyClose,
    resetForNewDay,
    clearAllData,
    updateExpense,
    updateWholesaleSale,
    deleteWholesaleSale,
  };
}
export type MilkShopViewModel = ReturnType<typeof useMilkShopViewModel>;
