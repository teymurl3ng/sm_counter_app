import React, { useState } from 'react';
import { 
  Milk, 
  Truck, 
  CheckCircle2, 
  Clock, 
  AlertTriangle, 
  UserCheck,
  Zap,
  Plus,
  Flame,
  Layers,
  Sparkles,
  Package,
  Droplet,
  X
} from 'lucide-react';
import { MilkShopViewModel } from '../../viewmodels/useMilkShopViewModel';
import { Badge } from '../common/Badge';
import { Modal } from '../common/Modal';
import { ConfirmModal } from '../common/ConfirmModal';
import { PaymentMethod } from '../../types/milkShop';

interface DailyOperationsViewProps {
  vm: MilkShopViewModel;
}

export const DailyOperationsView: React.FC<DailyOperationsViewProps> = ({ vm }) => {
  const [txToDelete, setTxToDelete] = useState<{ id: string | number; type: string; title: string } | null>(null);
  // Modal states
  const [isMilkIntakeOpen, setIsMilkIntakeOpen] = useState(false);
  const [isRetailSaleOpen, setIsRetailSaleOpen] = useState(false);
  const [isSpoilageOpen, setIsSpoilageOpen] = useState(false);

  // Form states - Supplier Intake
  const [intakeSupplierId, setIntakeSupplierId] = useState(vm.suppliers[0]?.id || '');
  const [intakeLiters, setIntakeLiters] = useState<number | ''>(100);
  const [intakeRate, setIntakeRate] = useState<number | ''>(vm.suppliers[0]?.defaultMilkRate || 175);
  const [intakePayNow, setIntakePayNow] = useState(false);
  const [intakeAmountPaid, setIntakeAmountPaid] = useState<number | ''>(0);

  // Form states - Retail Sale
  const activeProducts = vm.products.filter(p => !p.isArchived);
  const [selectedProductId, setSelectedProductId] = useState(activeProducts[0]?.id || '');
  const [saleQuantity, setSaleQuantity] = useState<number | ''>(1);
  const [saleInputMode, setSaleInputMode] = useState<'quantity' | 'money'>('quantity');
  const [saleMoneyAmount, setSaleMoneyAmount] = useState<number | ''>(50);

  // Form states - Yogurt Production
  const [isYogurtProductionOpen, setIsYogurtProductionOpen] = useState(false);
  const [yogurtMilkUsed, setYogurtMilkUsed] = useState<number | ''>(50);
  const [yogurtMade, setYogurtMade] = useState<number | ''>(48);

  // Form states - Spoilage
  const [spoilageProductId, setSpoilageProductId] = useState(activeProducts[0]?.id || '');
  const [spoilageQuantity, setSpoilageQuantity] = useState<number | ''>(1);
  const [spoilageNotes, setSpoilageNotes] = useState('');

  // Milk TS Quality states
  const [intakeFat, setIntakeFat] = useState<number | ''>('');
  const [intakeLr, setIntakeLr] = useState<number | ''>('');

  // Formula helper
  const calculateMilkQuality = (fat: number, lr: number) => {
    if (!fat || !lr) return { snf: 0, ts: 0, adjustedRate: 0 };
    const snf = (lr / 4) + (0.21 * fat) + 0.36;
    const ts = fat + snf;
    const baseRate = Number(intakeRate || 0);
    const adjustedRate = (baseRate / 13) * ts;
    return { snf, ts, adjustedRate };
  };

  // Auto-sync supplier rate when supplier changes
  const handleSupplierSelectChange = (id: string) => {
    setIntakeSupplierId(id);
    const sup = vm.suppliers.find(s => s.id === id);
    if (sup?.defaultMilkRate) {
      setIntakeRate(sup.defaultMilkRate);
    }
  };

  const handleAddIntakeSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const sup = vm.suppliers.find(s => s.id === intakeSupplierId);
    const liters = Number(intakeLiters || 0);
    const baseRate = Number(intakeRate || 0);
    if (!sup || liters <= 0 || baseRate <= 0) return;
    
    let finalRate = baseRate;
    let fat = Number(intakeFat || 0);
    let lr = Number(intakeLr || 0);
    let snf = 0;
    let ts = 0;
    
    if (fat > 0 && lr > 0) {
      const quality = calculateMilkQuality(fat, lr);
      snf = quality.snf;
      ts = quality.ts;
      finalRate = quality.adjustedRate;
    }

    vm.addSupplierDelivery({
      supplierId: sup.id,
      supplierName: sup.name,
      liters,
      rate: baseRate,
      fat,
      lr,
      snf,
      ts,
      adjustedRate: finalRate,
      payNow: intakePayNow,
      paymentMethod: 'cash' as PaymentMethod,
      amountPaid: intakePayNow ? Number(intakeAmountPaid || liters * finalRate) : 0,
    });

    setIsMilkIntakeOpen(false);
    setIntakeLiters(100);
    setIntakeAmountPaid(0);
    setIntakePayNow(false);
    setIntakeFat('');
    setIntakeLr('');
  };

  const handleYogurtSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const used = Number(yogurtMilkUsed || 0);
    const made = Number(yogurtMade || 0);
    if (used <= 0 || made <= 0) return;
    vm.recordYogurtProduction(used, made);
    setIsYogurtProductionOpen(false);
    setYogurtMilkUsed(50);
    setYogurtMade(48);
  };

  const handleRetailSaleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedProductId) return;
    
    if (saleInputMode === 'money') {
      const money = Number(saleMoneyAmount || 0);
      if (money <= 0) return;
      vm.addRetailSale(selectedProductId, 0, money);
    } else {
      const qty = Number(saleQuantity || 0);
      if (qty <= 0) return;
      vm.addRetailSale(selectedProductId, qty);
    }
    
    setIsRetailSaleOpen(false);
    setSaleQuantity(1);
    setSaleMoneyAmount(50);
  };

  const handleSpoilageSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const prod = vm.products.find(p => p.id === spoilageProductId);
    const qty = Number(spoilageQuantity || 0);
    if (!prod || qty <= 0) return;

    vm.addSpoilage({
      date: vm.selectedDate,
      productId: prod.id,
      productName: prod.name,
      quantity: qty,
      costRate: prod.costRate,
      reason: 'Spoiled / Curdled / Sour',
      notes: spoilageNotes,
    });

    setIsSpoilageOpen(false);
    setSpoilageQuantity(1);
    setSpoilageNotes('');
  };

  return (
    <div className="space-y-4">
      {/* Centered Large Quick Sale Button */}
      <button
        onClick={() => setIsRetailSaleOpen(true)}
        className="w-full bg-[#135d46] dark:bg-emerald-600 hover:bg-[#0e4836] dark:hover:bg-emerald-700 active:scale-98 transition-all text-white rounded-2xl py-6 px-6 shadow-sm flex flex-col items-center justify-center gap-1 text-center cursor-pointer group"
      >
        <div className="w-12 h-12 rounded-full bg-white/10 flex items-center justify-center text-emerald-300 mb-1 group-hover:scale-105 transition-all">
          <Zap className="w-6 h-6 animate-pulse" />
        </div>
        <span className="text-base font-extrabold tracking-tight">
          Record Quick Counter Sale
        </span>
        <span className="text-xs text-emerald-200/75 max-w-sm">
          Tap here to rapidly log individual retail cash sales (by quantity or by money)
        </span>
      </button>

      {/* Quick Action Buttons */}
      <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
        <button
          onClick={() => setIsMilkIntakeOpen(true)}
          className="p-3.5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 hover:bg-slate-50 dark:hover:bg-slate-800/50 text-slate-900 dark:text-slate-100 font-bold flex items-center justify-between shadow-xs cursor-pointer active:scale-98 transition-all"
        >
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-amber-100 dark:bg-amber-900/30 text-amber-800 dark:text-amber-400">
              <Milk className="w-4 h-4" />
            </div>
            <div className="text-left">
              <span className="text-xs font-bold block">Log Milk Purchase</span>
              <span className="text-[10px] text-slate-500 dark:text-slate-400">Log new supplier procurement</span>
            </div>
          </div>
          <Plus className="w-4 h-4 text-slate-400 dark:text-slate-500" />
        </button>

        <button
          onClick={() => setIsYogurtProductionOpen(true)}
          className="p-3.5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 hover:bg-slate-50 dark:hover:bg-slate-800/50 text-slate-900 dark:text-slate-100 font-bold flex items-center justify-between shadow-xs cursor-pointer active:scale-98 transition-all"
        >
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-cyan-100 dark:bg-cyan-900/30 text-cyan-800 dark:text-cyan-400">
              <Layers className="w-4 h-4" />
            </div>
            <div className="text-left">
              <span className="text-xs font-bold block">Produce Yogurt</span>
              <span className="text-[10px] text-slate-500 dark:text-slate-400">Convert milk to yogurt</span>
            </div>
          </div>
          <Plus className="w-4 h-4 text-slate-400 dark:text-slate-500" />
        </button>

        <button
          onClick={() => setIsSpoilageOpen(true)}
          className="p-3.5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 hover:bg-slate-50 dark:hover:bg-slate-800/50 text-slate-900 dark:text-slate-100 font-bold flex items-center justify-between shadow-xs cursor-pointer active:scale-98 transition-all"
        >
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-rose-100 dark:bg-rose-900/30 text-rose-800 dark:text-rose-400">
              <AlertTriangle className="w-4 h-4" />
            </div>
            <div className="text-left">
              <span className="text-xs font-bold block">Log Spoilage</span>
              <span className="text-[10px] text-slate-500 dark:text-slate-400">Record spoiled stock</span>
            </div>
          </div>
          <Plus className="w-4 h-4 text-slate-400 dark:text-slate-500" />
        </button>
      </div>

      {/* Operation Lists Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {/* Customer Route Deliveries Checklist */}
        <div className="p-4 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 space-y-3 shadow-xs">
          <div className="flex items-center justify-between">
            <h3 className="text-xs font-bold text-slate-900 dark:text-slate-100 uppercase tracking-wider flex items-center gap-1.5">
              <UserCheck className="w-4 h-4 text-emerald-600 dark:text-emerald-400" />
              <span>Deliveries</span>
            </h3>
            <span className="text-[10px] text-slate-500 dark:text-slate-400 font-mono font-bold">
              {vm.deliveryDeliveredCount} Delivered
            </span>
          </div>

          <div className="space-y-2 max-h-72 overflow-y-auto pr-1">
            {vm.customerDeliveries.map((delivery) => (
              <div
                key={delivery.customerId}
                className={`p-2.5 rounded-xl border flex items-center justify-between gap-2.5 ${
                  delivery.isDelivered
                    ? 'bg-slate-50 dark:bg-slate-800 border-slate-200 dark:border-slate-700'
                    : 'bg-amber-50/60 dark:bg-amber-900/20 border-amber-200/80 dark:border-amber-800/80'
                }`}
              >
                <div>
                  <span className={`text-xs font-bold block ${delivery.isDelivered ? 'text-slate-700 dark:text-slate-300' : 'text-amber-900 dark:text-amber-400'}`}>
                    {delivery.customerName}
                  </span>
                  <span className="text-[11px] text-slate-500 dark:text-slate-400 font-mono">
                    {delivery.quantity} L @ Rs.{delivery.rate} = Rs.{delivery.totalAmount}
                  </span>
                </div>

                <button
                  onClick={() => vm.toggleDeliveryDelivered(delivery.customerId)}
                  className={`px-3 py-1.5 rounded-xl text-xs font-bold flex items-center gap-1 cursor-pointer transition-all ${
                    delivery.isDelivered
                      ? 'bg-emerald-100 dark:bg-emerald-900/40 text-emerald-800 dark:text-emerald-300 border border-emerald-300 dark:border-emerald-800'
                      : 'bg-[#135d46] dark:bg-emerald-600 text-white hover:bg-[#0e4837] dark:hover:bg-emerald-700'
                  }`}
                >
                  {delivery.isDelivered ? (
                    <>
                      <CheckCircle2 className="w-3.5 h-3.5" />
                      <span>Delivered</span>
                    </>
                  ) : (
                    <>
                      <Clock className="w-3.5 h-3.5" />
                      <span>Mark Delivered</span>
                    </>
                  )}
                </button>
              </div>
            ))}

            {vm.customerDeliveries.length === 0 && (
              <div className="p-6 text-center border border-dashed border-slate-200 dark:border-slate-700 rounded-xl text-slate-400 dark:text-slate-500 text-xs">
                No customer deliveries scheduled for today yet.
              </div>
            )}
          </div>
        </div>

        {/* Supplier Procurement Batches */}
        <div className="p-4 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 space-y-3 shadow-xs">
          <div className="flex items-center justify-between">
            <h3 className="text-xs font-bold text-slate-900 dark:text-slate-100 uppercase tracking-wider flex items-center gap-1.5">
              <Truck className="w-4 h-4 text-emerald-600 dark:text-emerald-400" />
              <span>Procured Batches Today</span>
            </h3>
            <button
              onClick={() => setIsMilkIntakeOpen(true)}
              className="text-xs font-bold text-emerald-700 dark:text-emerald-400 hover:underline cursor-pointer"
            >
              + Add Batch
            </button>
          </div>

          <div className="space-y-2 max-h-72 overflow-y-auto pr-1">
            {vm.supplierDeliveries.map((item, idx) => (
              <div key={idx} className="p-2.5 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200/80 dark:border-slate-700 flex items-center justify-between">
                <div>
                  <span className="text-xs font-bold text-slate-800 dark:text-slate-200 block">{item.supplierName}</span>
                  <span className="text-[11px] text-slate-500 dark:text-slate-400 font-mono">
                    {item.liters} L @ Rs. {item.rate}/L = Rs. {item.totalAmount.toLocaleString()}
                  </span>
                </div>
                <Badge variant={item.payNow ? 'emerald' : 'rose'}>
                  {item.payNow ? 'Paid' : 'Credit'}
                </Badge>
              </div>
            ))}

            {vm.supplierDeliveries.length === 0 && (
              <div className="p-6 text-center border border-dashed border-slate-200 dark:border-slate-700 rounded-xl text-slate-400 dark:text-slate-500 text-xs">
                No milk intake batches logged today yet.
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Today's Transactions & Payment Log */}
      <div className="p-4 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 space-y-3 shadow-xs">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-1.5">
            <Zap className="w-4 h-4 text-emerald-600 dark:text-emerald-400" />
            <h3 className="text-xs font-bold text-slate-900 dark:text-slate-100 uppercase tracking-wider">
              Today's Transactions & Payment Log
            </h3>
          </div>
          <span className="text-[10px] bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400 px-2 py-0.5 rounded-md font-mono font-bold">
            Live Drawer
          </span>
        </div>

        <div className="space-y-2 max-h-80 overflow-y-auto pr-1">
          {/* Combine all today's operational transactions chronologically */}
          {[
            ...vm.retailSales.map((sale, idx) => ({
              id: `retail-${sale.productId}-${idx}`,
              type: 'Counter Sale',
              isInflow: true,
              isCredit: false,
              title: `Counter Sale: ${sale.productName}`,
              subtitle: `${sale.quantity} units @ Rs.${sale.salePrice}`,
              amount: sale.total,
              badge: 'Cash',
              badgeColor: 'bg-emerald-100 dark:bg-emerald-900/30 text-emerald-800 dark:text-emerald-400 border-emerald-200 dark:border-emerald-800',
            })),
            ...vm.wholesaleSales.map((ws) => ({
              id: ws.id,
              type: 'Wholesale Order',
              isInflow: ws.paymentType === 'cash',
              isCredit: ws.paymentType === 'credit',
              title: `Wholesale to ${ws.buyerName}`,
              subtitle: `${ws.quantity} units @ Rs.${ws.unitPrice}`,
              amount: ws.totalAmount,
              badge: ws.paymentType === 'cash' ? 'Cash Received' : 'Ledger Credit',
              badgeColor: ws.paymentType === 'cash' ? 'bg-emerald-100 dark:bg-emerald-900/30 text-emerald-800 dark:text-emerald-400 border-emerald-200 dark:border-emerald-800' : 'bg-amber-100 dark:bg-amber-900/30 text-amber-800 dark:text-amber-400 border-amber-200 dark:border-amber-800',
            })),
            ...vm.customerPayments.map((pay) => ({
              id: pay.id,
              type: 'Collection',
              isInflow: true,
              isCredit: false,
              title: `Debt Recovered: ${pay.customerName}`,
              subtitle: `Via ${pay.paymentMethod.toUpperCase()}${pay.notes ? ` - ${pay.notes}` : ''}`,
              amount: pay.amount,
              badge: pay.paymentMethod.toUpperCase(),
              badgeColor: 'bg-emerald-100 dark:bg-emerald-900/30 text-emerald-800 dark:text-emerald-400 border-emerald-200 dark:border-emerald-800',
            })),
            ...vm.supplierDeliveries.map((del, idx) => ({
              id: `sup-${idx}`,
              type: 'Procurement',
              isInflow: false,
              isCredit: !del.payNow,
              title: `Procured from ${del.supplierName}`,
              subtitle: `${del.liters}L @ Rs.${del.rate}/L`,
              amount: del.totalAmount,
              badge: del.payNow ? 'Paid Cash' : 'Supplier Credit',
              badgeColor: del.payNow ? 'bg-rose-100 dark:bg-rose-900/30 text-rose-800 dark:text-rose-400 border-rose-200 dark:border-rose-800' : 'bg-slate-100 dark:bg-slate-800 text-slate-800 dark:text-slate-200 border-slate-200 dark:border-slate-700',
            })),
            ...vm.expenses.map((exp) => ({
              id: exp.id,
              type: 'Expense',
              isInflow: false,
              isCredit: false,
              title: exp.description || `Expense: ${exp.category}`,
              subtitle: `Category: ${exp.category} • ${exp.paymentMethod.toUpperCase()}`,
              amount: exp.amount,
              badge: exp.paymentMethod.toUpperCase(),
              badgeColor: 'bg-rose-100 dark:bg-rose-900/30 text-rose-800 dark:text-rose-400 border-rose-200 dark:border-rose-800',
            })),
          ].length === 0 ? (
            <div className="p-8 text-center border border-dashed border-slate-200 dark:border-slate-700 rounded-xl text-slate-400 dark:text-slate-500 text-xs">
              No transactions or payments registered yet today. 
            </div>
          ) : (
            [
              ...vm.retailSales.map((sale) => ({
                id: sale.id,
                type: 'Counter Sale',
                isInflow: true,
                isCredit: false,
                title: `Counter Sale: ${sale.productName}`,
                subtitle: `${sale.quantity} units @ Rs.${sale.salePrice} • ${sale.timestamp}`,
                amount: sale.total,
                badge: 'Cash',
                badgeColor: 'bg-emerald-100 dark:bg-emerald-900/30 text-emerald-800 dark:text-emerald-400 border-emerald-200 dark:border-emerald-800',
              })),
              ...vm.wholesaleSales.map((ws) => ({
                id: ws.id,
                type: 'Wholesale Order',
                isInflow: ws.paymentType === 'cash',
                isCredit: ws.paymentType === 'credit',
                title: `Wholesale to ${ws.buyerName}`,
                subtitle: `${ws.quantity} units @ Rs.${ws.unitPrice}`,
                amount: ws.totalAmount,
                badge: ws.paymentType === 'cash' ? 'Cash Received' : 'Ledger Credit',
                badgeColor: ws.paymentType === 'cash' ? 'bg-emerald-100 dark:bg-emerald-900/30 text-emerald-800 dark:text-emerald-400 border-emerald-200 dark:border-emerald-800' : 'bg-amber-100 dark:bg-amber-900/30 text-amber-800 dark:text-amber-400 border-amber-200 dark:border-amber-800',
              })),
              ...vm.customerPayments.map((pay) => ({
                id: pay.id,
                type: 'Collection',
                isInflow: true,
                isCredit: false,
                title: `Debt Recovered: ${pay.customerName}`,
                subtitle: `Via ${pay.paymentMethod.toUpperCase()}${pay.notes ? ` - ${pay.notes}` : ''}`,
                amount: pay.amount,
                badge: pay.paymentMethod.toUpperCase(),
                badgeColor: 'bg-emerald-100 dark:bg-emerald-900/30 text-emerald-800 dark:text-emerald-400 border-emerald-200 dark:border-emerald-800',
              })),
              ...vm.supplierDeliveries.map((del, idx) => ({
                id: `sup-${idx}`,
                deliveryIndex: idx,
                type: 'Procurement',
                isInflow: false,
                isCredit: !del.payNow,
                title: `Procured from ${del.supplierName}`,
                subtitle: `${del.liters}L @ Rs.${del.rate}/L`,
                amount: del.totalAmount,
                badge: del.payNow ? 'Paid Cash' : 'Supplier Credit',
                badgeColor: del.payNow ? 'bg-rose-100 dark:bg-rose-900/30 text-rose-800 dark:text-rose-400 border-rose-200 dark:border-rose-800' : 'bg-slate-100 dark:bg-slate-800 text-slate-800 dark:text-slate-200 border-slate-200 dark:border-slate-700',
              })),
              ...vm.expenses.map((exp) => ({
                id: exp.id,
                type: 'Expense',
                isInflow: false,
                isCredit: false,
                title: exp.description || `Expense: ${exp.category}`,
                subtitle: `Category: ${exp.category} • ${exp.paymentMethod.toUpperCase()}`,
                amount: exp.amount,
                badge: exp.paymentMethod.toUpperCase(),
                badgeColor: 'bg-rose-100 dark:bg-rose-900/30 text-rose-800 dark:text-rose-400 border-rose-200 dark:border-rose-800',
              })),
            ].map((tx, idx) => (
              <div key={tx.id || idx} className="p-3 rounded-xl bg-slate-50 dark:bg-slate-800/50 border border-slate-200/80 dark:border-slate-700 flex items-center justify-between gap-3 text-xs">
                <div className="space-y-1">
                  <div className="flex items-center gap-1.5 flex-wrap">
                    <span className="font-bold text-slate-800 dark:text-slate-200">{tx.title}</span>
                    <span className={`text-[9px] px-1.5 py-0.5 rounded-md border font-mono font-bold ${tx.badgeColor}`}>
                      {tx.badge}
                    </span>
                  </div>
                  <p className="text-[11px] text-slate-500 dark:text-slate-400">{tx.subtitle}</p>
                </div>

                <div className="text-right flex items-center gap-2">
                  <span className={`font-mono font-black text-sm shrink-0 ${
                    tx.isCredit 
                      ? 'text-slate-500 dark:text-slate-500' 
                      : tx.isInflow 
                        ? 'text-emerald-700 dark:text-emerald-400' 
                        : 'text-rose-600 dark:text-rose-400'
                  }`}>
                    {tx.isCredit ? '' : tx.isInflow ? '+' : '-'}Rs.{tx.amount.toLocaleString()}
                  </span>
                  {tx.type === 'Expense' && (
                    <button
                      onClick={() => setTxToDelete({ id: tx.id, type: 'Expense', title: tx.title })}
                      className="p-1 text-slate-400 hover:text-rose-600 dark:hover:text-rose-400 rounded-lg hover:bg-rose-50 dark:hover:bg-rose-900/30 cursor-pointer"
                      title="Delete Expense"
                    >
                      <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                      </svg>
                    </button>
                  )}
                  {tx.type === 'Counter Sale' && (
                    <button
                      onClick={() => setTxToDelete({ id: tx.id, type: 'Counter Sale', title: tx.title })}
                      className="p-1 text-slate-400 hover:text-rose-600 dark:hover:text-rose-400 rounded-lg hover:bg-rose-50 dark:hover:bg-rose-900/30 cursor-pointer"
                      title="Delete Counter Sale"
                    >
                      <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                      </svg>
                    </button>
                  )}
                  {tx.type === 'Wholesale Order' && (
                    <button
                      onClick={() => setTxToDelete({ id: tx.id, type: 'Wholesale Order', title: tx.title })}
                      className="p-1 text-slate-400 hover:text-rose-600 dark:hover:text-rose-400 rounded-lg hover:bg-rose-50 dark:hover:bg-rose-900/30 cursor-pointer"
                      title="Delete Wholesale Order"
                    >
                      <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                      </svg>
                    </button>
                  )}
                  {tx.type === 'Collection' && (
                    <button
                      onClick={() => setTxToDelete({ id: tx.id, type: 'Collection', title: tx.title })}
                      className="p-1 text-slate-400 hover:text-rose-600 dark:hover:text-rose-400 rounded-lg hover:bg-rose-50 dark:hover:bg-rose-900/30 cursor-pointer"
                      title="Delete Collection"
                    >
                      <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                      </svg>
                    </button>
                  )}
                  {tx.type === 'Procurement' && (
                    <button
                      onClick={() => setTxToDelete({ id: (tx as any).deliveryIndex, type: 'Procurement', title: tx.title })}
                      className="p-1 text-slate-400 hover:text-rose-600 dark:hover:text-rose-400 rounded-lg hover:bg-rose-50 dark:hover:bg-rose-900/30 cursor-pointer"
                      title="Delete Procurement"
                    >
                      <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                      </svg>
                    </button>
                  )}
                </div>
              </div>
            ))
          )}
        </div>
      </div>

      {/* Modal: Record Supplier Milk Intake */}
      <Modal
        isOpen={isMilkIntakeOpen}
        onClose={() => setIsMilkIntakeOpen(false)}
        title="Record Milk Intake"
        subtitle="Log milk received from supplier"
      >
        <form onSubmit={handleAddIntakeSubmit} className="space-y-4 text-xs">
          <div>
            <label className="block text-slate-700 dark:text-slate-300 font-bold mb-1">Select Supplier</label>
            <select
              value={intakeSupplierId}
              onChange={(e) => handleSupplierSelectChange(e.target.value)}
              className="w-full bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl px-3 py-2.5 text-slate-900 dark:text-slate-100 text-sm focus:outline-none focus:border-emerald-600 dark:focus:border-emerald-500"
            >
              {vm.suppliers.map(s => (
                <option key={s.id} value={s.id}>{s.name} (Rate: Rs. {s.defaultMilkRate}/L)</option>
              ))}
            </select>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-slate-700 dark:text-slate-300 font-bold mb-1">Liters Received</label>
              <input
                type="number"
                min="1"
                value={intakeLiters}
                onChange={(e) => setIntakeLiters(e.target.value === '' ? '' : Number(e.target.value))}
                className="w-full bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl px-3 py-2.5 text-sm font-bold font-mono text-emerald-800 dark:text-emerald-400 focus:outline-none focus:border-emerald-600 dark:focus:border-emerald-500"
                required
              />
            </div>
            <div>
              <label className="block text-slate-700 dark:text-slate-300 font-bold mb-1">Base Rate / 13 TS (Rs.)</label>
              <input
                type="number"
                min="1"
                value={intakeRate}
                onChange={(e) => setIntakeRate(e.target.value === '' ? '' : Number(e.target.value))}
                className="w-full bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl px-3 py-2.5 text-sm font-bold font-mono text-slate-900 dark:text-slate-100 focus:outline-none focus:border-emerald-600 dark:focus:border-emerald-500"
                required
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-slate-700 dark:text-slate-300 font-bold mb-1">FAT (%)</label>
              <input
                type="number"
                min="0"
                step="0.1"
                value={intakeFat}
                onChange={(e) => setIntakeFat(e.target.value === '' ? '' : Number(e.target.value))}
                className="w-full bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl px-3 py-2.5 text-sm font-bold font-mono text-emerald-800 dark:text-emerald-400 focus:outline-none focus:border-emerald-600 dark:focus:border-emerald-500"
                placeholder="e.g. 5.5"
              />
            </div>
            <div>
              <label className="block text-slate-700 dark:text-slate-300 font-bold mb-1">LR</label>
              <input
                type="number"
                min="0"
                step="0.1"
                value={intakeLr}
                onChange={(e) => setIntakeLr(e.target.value === '' ? '' : Number(e.target.value))}
                className="w-full bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl px-3 py-2.5 text-sm font-bold font-mono text-emerald-800 dark:text-emerald-400 focus:outline-none focus:border-emerald-600 dark:focus:border-emerald-500"
                placeholder="e.g. 26"
              />
            </div>
          </div>

          {(() => {
            const fat = Number(intakeFat || 0);
            const lr = Number(intakeLr || 0);
            const baseRate = Number(intakeRate || 0);
            const liters = Number(intakeLiters || 0);
            const { snf, ts, adjustedRate } = calculateMilkQuality(fat, lr);
            const finalRate = fat > 0 && lr > 0 ? adjustedRate : baseRate;
            
            return (
              <div className="p-3 rounded-xl bg-slate-50 dark:bg-slate-800/50 border border-slate-200 dark:border-slate-700 space-y-2">
                {fat > 0 && lr > 0 && (
                  <>
                    <div className="flex justify-between items-center text-xs text-slate-500 dark:text-slate-400">
                      <span>SNF: <strong className="text-slate-700 dark:text-slate-200">{snf.toFixed(2)}</strong></span>
                      <span>TS: <strong className="text-slate-700 dark:text-slate-200">{ts.toFixed(2)}</strong></span>
                    </div>
                    <div className="flex justify-between items-center text-xs text-slate-500 dark:text-slate-400 pb-2 border-b border-slate-200 dark:border-slate-700">
                      <span>Adjusted Rate (TS 13):</span>
                      <strong className="text-emerald-700 dark:text-emerald-400">Rs. {adjustedRate.toFixed(2)}</strong>
                    </div>
                  </>
                )}
                <div className="flex justify-between items-center">
                  <span className="text-slate-600 dark:text-slate-400 font-bold">Total Batch Cost:</span>
                  <span className="text-base font-extrabold font-mono text-emerald-800 dark:text-emerald-400">
                    Rs. {(liters * finalRate).toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: 0 })}
                  </span>
                </div>
              </div>
            );
          })()}

          <div className="flex items-center gap-2">
            <input
              type="checkbox"
              id="intakePayNow"
              checked={intakePayNow}
              onChange={(e) => setIntakePayNow(e.target.checked)}
              className="w-4 h-4 rounded text-emerald-600 dark:text-emerald-500 focus:ring-0 cursor-pointer"
            />
            <label htmlFor="intakePayNow" className="text-slate-800 dark:text-slate-200 font-bold cursor-pointer">
              Pay Cash Now
            </label>
          </div>

          <div className="flex justify-end gap-2.5 pt-3">
            <button
              type="button"
              onClick={() => setIsMilkIntakeOpen(false)}
              className="px-4 py-2 rounded-xl bg-slate-200 dark:bg-slate-800 text-slate-700 dark:text-slate-300 font-medium cursor-pointer hover:bg-slate-300 dark:hover:bg-slate-700"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-5 py-2 rounded-xl bg-[#135d46] dark:bg-emerald-600 hover:bg-[#0e4837] dark:hover:bg-emerald-700 text-white font-bold cursor-pointer"
            >
              Save Intake
            </button>
          </div>
        </form>
      </Modal>

      {/* Yogurt Production Modal */}
      <Modal
        isOpen={isYogurtProductionOpen}
        onClose={() => setIsYogurtProductionOpen(false)}
        title="Produce Yogurt"
        subtitle="Convert milk into yogurt"
      >
        <form onSubmit={handleYogurtSubmit} className="space-y-4 text-xs">
          <div className="p-3 rounded-xl bg-cyan-50 dark:bg-cyan-900/20 border border-cyan-200 dark:border-cyan-800 text-cyan-800 dark:text-cyan-400 space-y-2">
            <p className="font-bold flex items-center gap-1.5"><Layers className="w-4 h-4" /> Production Flow</p>
            <p className="text-[10px]">This will subtract the entered liters of Milk from your stock, and add the generated Kg of Yogurt to your stock.</p>
          </div>

          <div>
            <label className="block text-slate-700 dark:text-slate-300 font-bold mb-1">Milk Used (Liters)</label>
            <input
              type="number"
              min="0.1"
              step="any"
              value={yogurtMilkUsed}
              onChange={(e) => setYogurtMilkUsed(e.target.value === '' ? '' : Number(e.target.value))}
              className="w-full bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl px-3 py-2.5 text-sm font-bold font-mono text-cyan-800 dark:text-cyan-400 focus:outline-none focus:border-cyan-600 dark:focus:border-cyan-500"
              required
            />
          </div>

          <div>
            <label className="block text-slate-700 dark:text-slate-300 font-bold mb-1">Yogurt Made (Kg)</label>
            <input
              type="number"
              min="0.1"
              step="any"
              value={yogurtMade}
              onChange={(e) => setYogurtMade(e.target.value === '' ? '' : Number(e.target.value))}
              className="w-full bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl px-3 py-2.5 text-sm font-bold font-mono text-emerald-800 dark:text-emerald-400 focus:outline-none focus:border-emerald-600 dark:focus:border-emerald-500"
              required
            />
          </div>

          <div className="flex justify-end gap-2.5 pt-3">
            <button
              type="button"
              onClick={() => setIsYogurtProductionOpen(false)}
              className="px-4 py-2 rounded-xl bg-slate-200 dark:bg-slate-800 text-slate-700 dark:text-slate-300 font-medium cursor-pointer hover:bg-slate-300 dark:hover:bg-slate-700"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-5 py-2 rounded-xl bg-cyan-600 hover:bg-cyan-700 text-white font-bold cursor-pointer"
            >
              Save Production
            </button>
          </div>
        </form>
      </Modal>

      {/* Immersive Full Screen Counter POS Terminal */}
      {/* Immersive Full Screen Counter POS Terminal */}
      {isRetailSaleOpen && (
        <div className="fixed inset-0 bg-slate-100 dark:bg-slate-950 z-[100] flex flex-col overflow-hidden animate-in fade-in zoom-in-95 duration-150">
          {/* Header */}
          <div className="bg-[#114b39] dark:bg-slate-900 text-white px-4 md:px-6 py-3 flex justify-between items-center shrink-0 shadow-md border-b border-emerald-950/20 dark:border-slate-800">
            <div className="flex items-center gap-2.5">
              <div className="w-8 h-8 rounded-xl bg-white/10 flex items-center justify-center text-emerald-300 shrink-0">
                <Zap className="w-4 h-4 animate-pulse" />
              </div>
              <div>
                <h2 className="text-xs md:text-sm font-black tracking-tight text-white uppercase">POS Terminal</h2>
                <p className="text-[9px] md:text-[10px] text-emerald-200/80 font-bold">Counter Cash Sales</p>
              </div>
            </div>
            <button
              onClick={() => setIsRetailSaleOpen(false)}
              className="px-3 py-1.5 rounded-full bg-rose-600 hover:bg-rose-700 active:scale-95 text-white transition-all duration-150 cursor-pointer flex items-center justify-center gap-1 text-[10px] md:text-xs font-black shadow-sm"
            >
              <X className="w-3.5 h-3.5" />
              <span>EXIT POS</span>
            </button>
          </div>

          {/* =========================================================================
              MOBILE ONLY VIEW (Visible on screens < lg)
              ========================================================================= */}
          <div className="flex-1 flex flex-col lg:hidden overflow-y-auto bg-slate-50 dark:bg-slate-950 pb-8">
            <div className="p-4 space-y-6">
              {/* Product selection list / grid */}
              <div className="space-y-2">
                <div className="flex justify-between items-center">
                  <h3 className="text-xs font-black uppercase tracking-wider text-slate-500 dark:text-slate-400">1. Select Product</h3>
                  <span className="text-[10px] text-slate-400 dark:text-slate-500 font-bold">Tap to choose</span>
                </div>
                <div className="grid grid-cols-2 gap-2">
                  {activeProducts.map((prod) => {
                    const isSelected = selectedProductId === prod.id;
                    let ProductIcon = Milk;
                    if (prod.name.toLowerCase().includes('milk')) ProductIcon = Milk;
                    else if (prod.name.toLowerCase().includes('yogurt') || prod.name.toLowerCase().includes('dahi')) ProductIcon = Droplet;
                    else if (prod.name.toLowerCase().includes('paneer') || prod.name.toLowerCase().includes('cheese')) ProductIcon = Package;
                    else if (prod.name.toLowerCase().includes('cream') || prod.name.toLowerCase().includes('malai')) ProductIcon = Sparkles;
                    else if (prod.name.toLowerCase().includes('butter') || prod.name.toLowerCase().includes('makkhan')) ProductIcon = Layers;
                    else if (prod.name.toLowerCase().includes('ghee')) ProductIcon = Flame;

                    return (
                      <button
                        key={prod.id}
                        type="button"
                        onClick={() => setSelectedProductId(prod.id)}
                        className={`p-3 rounded-xl border-2 text-left flex flex-col justify-between transition-all active:scale-98 cursor-pointer relative overflow-hidden ${
                          isSelected 
                            ? 'border-emerald-600 bg-emerald-50/30 dark:bg-emerald-900/20' 
                            : 'border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900'
                        }`}
                      >
                        <div className="flex justify-between items-center w-full">
                          <div className={`p-1.5 rounded-lg ${isSelected ? 'bg-emerald-600 text-white' : 'bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300'}`}>
                            <ProductIcon className="w-4 h-4" />
                          </div>
                          <span className="text-[9px] text-slate-400 dark:text-slate-500 font-bold font-mono">
                            {prod.currentStock} {prod.unit} left
                          </span>
                        </div>
                        <div className="mt-2">
                          <span className="font-extrabold text-xs block truncate text-slate-800 dark:text-slate-100">
                            {prod.name}
                          </span>
                          <span className="text-[10px] text-slate-500 dark:text-slate-400 font-bold font-mono">
                            Rs. {prod.salePrice}/{prod.unit}
                          </span>
                        </div>
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Mode toggle */}
              <div className="space-y-3 bg-white dark:bg-slate-900 p-4 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-xs">
                <div className="flex items-center justify-between">
                  <h3 className="text-xs font-black uppercase tracking-wider text-slate-500 dark:text-slate-400 font-bold">2. Sale Method</h3>
                  <div className="flex p-0.5 bg-slate-100 dark:bg-slate-800 rounded-lg border border-slate-200 dark:border-slate-700">
                    <button
                      type="button"
                      onClick={() => setSaleInputMode('quantity')}
                      className={`px-3 py-1 rounded-md text-[10px] font-bold transition-all cursor-pointer ${
                        saleInputMode === 'quantity'
                          ? 'bg-white dark:bg-slate-700 text-emerald-800 dark:text-emerald-400 shadow-2xs'
                          : 'text-slate-500 dark:text-slate-400'
                      }`}
                    >
                      By Qty
                    </button>
                    <button
                      type="button"
                      onClick={() => setSaleInputMode('money')}
                      className={`px-3 py-1 rounded-md text-[10px] font-bold transition-all cursor-pointer ${
                        saleInputMode === 'money'
                          ? 'bg-white dark:bg-slate-700 text-emerald-800 dark:text-emerald-400 shadow-2xs'
                          : 'text-slate-500 dark:text-slate-400'
                      }`}
                    >
                      By Cash
                    </button>
                  </div>
                </div>

                {saleInputMode === 'quantity' ? (
                  <div className="space-y-3">
                    <div className="grid grid-cols-4 gap-1.5">
                      {[0.25, 0.5, 1, 1.5, 2, 5, 10].map((amt) => (
                        <button
                          key={amt}
                          type="button"
                          onClick={() => setSaleQuantity(amt)}
                          className={`py-2 rounded-lg border text-xs font-mono font-bold transition-all active:scale-95 cursor-pointer ${
                            saleQuantity === amt
                              ? 'bg-emerald-600 border-emerald-600 text-white'
                              : 'bg-white dark:bg-slate-800 border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-200'
                          }`}
                        >
                          {amt} {vm.products.find(p => p.id === selectedProductId)?.unit || 'units'}
                        </button>
                      ))}
                    </div>

                    <div className="relative rounded-xl shadow-2xs">
                      <input
                        type="number"
                        min="0.01"
                        step="any"
                        value={saleQuantity}
                        onChange={(e) => setSaleQuantity(e.target.value === '' ? '' : Number(e.target.value))}
                        className="w-full bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl pl-4 pr-12 py-2.5 text-xs font-bold font-mono text-emerald-800 dark:text-emerald-400 focus:outline-none focus:border-emerald-600 dark:focus:border-emerald-500"
                        placeholder="Type quantity"
                      />
                      <div className="absolute inset-y-0 right-0 pr-4 flex items-center pointer-events-none text-slate-400 dark:text-slate-500 font-bold font-mono text-xs">
                        {vm.products.find(p => p.id === selectedProductId)?.unit || 'L'}
                      </div>
                    </div>
                  </div>
                ) : (
                  <div className="space-y-3">
                    <div className="grid grid-cols-4 gap-1.5">
                      {[10, 20, 50, 100, 150, 200, 500].map((amt) => (
                        <button
                          key={amt}
                          type="button"
                          onClick={() => setSaleMoneyAmount(amt)}
                          className={`py-2 rounded-lg border text-xs font-mono font-bold transition-all active:scale-95 cursor-pointer ${
                            saleMoneyAmount === amt
                              ? 'bg-emerald-600 border-emerald-600 text-white'
                              : 'bg-white dark:bg-slate-800 border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-200'
                          }`}
                        >
                          Rs.{amt}
                        </button>
                      ))}
                    </div>

                    <div className="relative rounded-xl shadow-2xs">
                      <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none text-slate-400 dark:text-slate-500 font-bold font-mono text-xs">
                        Rs.
                      </div>
                      <input
                        type="number"
                        min="1"
                        value={saleMoneyAmount}
                        onChange={(e) => setSaleMoneyAmount(e.target.value === '' ? '' : Number(e.target.value))}
                        className="w-full bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl pl-10 pr-4 py-2.5 text-xs font-bold font-mono text-emerald-800 dark:text-emerald-400 focus:outline-none focus:border-emerald-600 dark:focus:border-emerald-500"
                        placeholder="Type cash amount"
                      />
                    </div>
                  </div>
                )}
              </div>

              {/* Receipt Preview */}
              {(() => {
                const activeProd = vm.products.find(p => p.id === selectedProductId);
                const isQtyMode = saleInputMode === 'quantity';
                const sQty = Number(saleQuantity || 0);
                const sMoney = Number(saleMoneyAmount || 0);
                const finalAmount = activeProd 
                  ? (isQtyMode ? Number((sQty * activeProd.salePrice).toFixed(2)) : sMoney)
                  : 0;
                const finalQuantity = activeProd
                  ? (isQtyMode ? sQty : Number((sMoney / activeProd.salePrice).toFixed(3)))
                  : 0;

                return (
                  <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl shadow-sm p-4 space-y-3 font-mono text-xs">
                    <div className="text-center border-b border-dashed border-slate-200 dark:border-slate-700 pb-2">
                      <span className="text-[10px] font-black tracking-widest text-emerald-700 dark:text-emerald-400 uppercase block">COUNTER CASH RECEIPT</span>
                    </div>

                    {activeProd ? (
                      <div className="space-y-2">
                        <div className="flex justify-between">
                          <span className="text-slate-400 dark:text-slate-500">Item:</span>
                          <span className="font-bold text-slate-800 dark:text-slate-100">{activeProd.name}</span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-slate-400 dark:text-slate-500">Rate:</span>
                          <span className="text-slate-700 dark:text-slate-300">Rs. {activeProd.salePrice}/{activeProd.unit}</span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-slate-400 dark:text-slate-500">Quantity:</span>
                          <span className="font-bold text-emerald-700 dark:text-emerald-400">{finalQuantity.toFixed(3)} {activeProd.unit}</span>
                        </div>
                        <div className="border-t border-dashed border-slate-200 dark:border-slate-700 pt-2 flex justify-between items-center text-sm">
                          <span className="font-bold text-slate-700 dark:text-slate-300">TOTAL:</span>
                          <span className="font-black text-emerald-800 dark:text-emerald-400">Rs. {finalAmount.toLocaleString()}</span>
                        </div>
                      </div>
                    ) : (
                      <div className="text-center text-slate-400 dark:text-slate-500 text-xs py-2">Select an item above</div>
                    )}
                  </div>
                );
              })()}

              {/* Mobile CTA Footer */}
              <div className="space-y-2">
                <button
                  type="button"
                  onClick={handleRetailSaleSubmit}
                  className="w-full py-3.5 rounded-xl bg-[#135d46] dark:bg-emerald-600 hover:bg-[#0e4837] dark:hover:bg-emerald-700 text-white font-black text-xs shadow-md transition-all flex items-center justify-center gap-2 cursor-pointer"
                >
                  <CheckCircle2 className="w-4 h-4" />
                  <span>CONFIRM & SAVE SALE (CASH)</span>
                </button>
                <button
                  type="button"
                  onClick={() => setIsRetailSaleOpen(false)}
                  className="w-full py-3 rounded-xl bg-slate-200 dark:bg-slate-800 hover:bg-slate-300 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 font-bold text-xs cursor-pointer text-center"
                >
                  CLOSE TERMINAL
                </button>
              </div>
            </div>
          </div>

          {/* =========================================================================
              DESKTOP ONLY VIEW (Visible on screens >= lg)
              ========================================================================= */}
          <div className="hidden lg:flex flex-1 flex-row overflow-hidden">
            {/* Left Panel: Configuration Grid (60%) */}
            <div className="flex-1 overflow-y-auto p-6 space-y-6 bg-white dark:bg-slate-900 h-[calc(100vh-76px)]">
              {/* Step 1: Select Product */}
              <div className="space-y-3">
                <h3 className="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                  1. Select Dairy Item
                </h3>
                <div className="grid grid-cols-3 gap-3">
                  {activeProducts.map((prod) => {
                    const isSelected = selectedProductId === prod.id;
                    let ProductIcon = Milk;
                    if (prod.name.toLowerCase().includes('milk')) ProductIcon = Milk;
                    else if (prod.name.toLowerCase().includes('yogurt') || prod.name.toLowerCase().includes('dahi')) ProductIcon = Droplet;
                    else if (prod.name.toLowerCase().includes('paneer') || prod.name.toLowerCase().includes('cheese')) ProductIcon = Package;
                    else if (prod.name.toLowerCase().includes('cream') || prod.name.toLowerCase().includes('malai')) ProductIcon = Sparkles;
                    else if (prod.name.toLowerCase().includes('butter') || prod.name.toLowerCase().includes('makkhan')) ProductIcon = Layers;
                    else if (prod.name.toLowerCase().includes('ghee')) ProductIcon = Flame;

                    return (
                      <button
                        key={prod.id}
                        type="button"
                        onClick={() => setSelectedProductId(prod.id)}
                        className={`p-4 rounded-xl border-2 text-left flex flex-col justify-between transition-all cursor-pointer h-28 relative overflow-hidden group ${
                          isSelected 
                            ? 'border-emerald-600 bg-emerald-50/20 dark:bg-emerald-900/20 shadow-sm' 
                            : 'border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 hover:border-slate-300 dark:hover:border-slate-700 hover:bg-slate-50/50 dark:hover:bg-slate-800/50'
                        }`}
                      >
                        <div className="flex justify-between items-start w-full">
                          <div className={`p-2 rounded-lg ${isSelected ? 'bg-emerald-600 text-white' : 'bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300'} transition-colors`}>
                            <ProductIcon className="w-5 h-5" />
                          </div>
                          <span className="text-[10px] text-slate-400 dark:text-slate-500 font-bold font-mono">
                            {prod.currentStock} {prod.unit} left
                          </span>
                        </div>
                        <div className="mt-2">
                          <span className={`font-black text-xs block truncate ${isSelected ? 'text-emerald-900 dark:text-emerald-300' : 'text-slate-800 dark:text-slate-100'}`}>
                            {prod.name}
                          </span>
                          <span className="text-[10px] text-slate-500 dark:text-slate-400 font-bold font-mono">
                            Rs. {prod.salePrice}/{prod.unit}
                          </span>
                        </div>
                        {isSelected && (
                          <div className="absolute top-0 right-0 w-3.5 h-3.5 bg-emerald-600 rounded-bl-lg flex items-center justify-center">
                            <div className="w-1.5 h-1.5 bg-white rounded-full"></div>
                          </div>
                        )}
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Step 2: Choose Input Mode & Amounts */}
              <div className="space-y-4">
                <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 border-t border-slate-100 dark:border-slate-800 pt-5">
                  <h3 className="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                    2. Select Mode & Value
                  </h3>
                  
                  <div className="flex p-1 bg-slate-100 dark:bg-slate-800 rounded-xl border border-slate-200 dark:border-slate-700 max-w-sm shrink-0">
                    <button
                      type="button"
                      onClick={() => setSaleInputMode('quantity')}
                      className={`px-4 py-1.5 rounded-lg text-xs font-bold transition-all cursor-pointer ${
                        saleInputMode === 'quantity'
                          ? 'bg-white dark:bg-slate-700 text-emerald-800 dark:text-emerald-400 shadow-xs'
                          : 'text-slate-500 dark:text-slate-400 hover:text-slate-800 dark:hover:text-slate-200'
                      }`}
                    >
                      By Weight / Volume
                    </button>
                    <button
                      type="button"
                      onClick={() => setSaleInputMode('money')}
                      className={`px-4 py-1.5 rounded-lg text-xs font-bold transition-all cursor-pointer ${
                        saleInputMode === 'money'
                          ? 'bg-white dark:bg-slate-700 text-emerald-800 dark:text-emerald-400 shadow-xs'
                          : 'text-slate-500 dark:text-slate-400 hover:text-slate-800 dark:hover:text-slate-200'
                      }`}
                    >
                      By Money Amount
                    </button>
                  </div>
                </div>

                {saleInputMode === 'quantity' ? (
                  <div className="space-y-4">
                    <div className="space-y-2">
                      <label className="block text-[11px] text-slate-400 dark:text-slate-500 font-bold uppercase">Standard Quantity Presets</label>
                      <div className="grid grid-cols-8 gap-2">
                        {[0.25, 0.5, 1, 1.5, 2, 2.5, 5, 10].map((amt) => (
                          <button
                            key={amt}
                            type="button"
                            onClick={() => setSaleQuantity(amt)}
                            className={`py-3 rounded-xl border text-xs font-mono font-black active:scale-95 transition-all cursor-pointer ${
                              saleQuantity === amt
                                ? 'bg-emerald-600 border-emerald-600 text-white shadow-xs'
                                : 'bg-white dark:bg-slate-800 border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-200 hover:border-slate-300 dark:hover:border-slate-600 hover:bg-slate-50 dark:hover:bg-slate-700'
                            }`}
                          >
                            {amt} {vm.products.find(p => p.id === selectedProductId)?.unit || 'units'}
                          </button>
                        ))}
                      </div>
                    </div>

                    <div className="max-w-xs space-y-1">
                      <label className="block text-[11px] text-slate-400 dark:text-slate-500 font-bold uppercase">Or Type Custom Quantity</label>
                      <div className="relative rounded-xl shadow-xs">
                        <input
                          type="number"
                          min="0.01"
                          step="any"
                          value={saleQuantity}
                          onChange={(e) => setSaleQuantity(e.target.value === '' ? '' : Number(e.target.value))}
                          className="w-full bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl pl-4 pr-12 py-3 text-sm font-bold font-mono text-emerald-800 dark:text-emerald-400 focus:outline-none focus:border-emerald-600 dark:focus:border-emerald-500"
                        />
                        <div className="absolute inset-y-0 right-0 pr-4 flex items-center pointer-events-none text-slate-400 dark:text-slate-500 font-bold font-mono">
                          {vm.products.find(p => p.id === selectedProductId)?.unit || 'L'}
                        </div>
                      </div>
                    </div>
                  </div>
                ) : (
                  <div className="space-y-4">
                    <div className="space-y-2">
                      <label className="block text-[11px] text-slate-400 dark:text-slate-500 font-bold uppercase">Standard Money Presets</label>
                      <div className="grid grid-cols-8 gap-2">
                        {[10, 20, 50, 100, 120, 150, 200, 500].map((amt) => (
                          <button
                            key={amt}
                            type="button"
                            onClick={() => setSaleMoneyAmount(amt)}
                            className={`py-3 rounded-xl border text-xs font-mono font-black active:scale-95 transition-all cursor-pointer ${
                              saleMoneyAmount === amt
                                ? 'bg-emerald-600 border-emerald-600 text-white shadow-xs'
                                : 'bg-white dark:bg-slate-800 border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-200 hover:border-slate-300 dark:hover:border-slate-600 hover:bg-slate-50 dark:hover:bg-slate-700'
                            }`}
                          >
                            Rs.{amt}
                          </button>
                        ))}
                      </div>
                    </div>

                    <div className="max-w-xs space-y-1">
                      <label className="block text-[11px] text-slate-400 dark:text-slate-500 font-bold uppercase">Or Type Custom Rupees</label>
                      <div className="relative rounded-xl shadow-xs">
                        <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none text-slate-400 dark:text-slate-500 font-bold font-mono">
                          Rs.
                        </div>
                        <input
                          type="number"
                          min="1"
                          value={saleMoneyAmount}
                          onChange={(e) => setSaleMoneyAmount(e.target.value === '' ? '' : Number(e.target.value))}
                          className="w-full bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl pl-10 pr-4 py-3 text-sm font-bold font-mono text-emerald-800 dark:text-emerald-400 focus:outline-none focus:border-emerald-600 dark:focus:border-emerald-500"
                        />
                      </div>
                    </div>
                  </div>
                )}
              </div>
            </div>

            {/* Right Panel: POS Live Billing Invoice Checkout Summary (40%) */}
            <div className="w-[420px] bg-slate-50 dark:bg-slate-900 border-l border-slate-200 dark:border-slate-800 p-6 flex flex-col justify-between shrink-0 overflow-y-auto h-[calc(100vh-76px)]">
              <div className="space-y-6">
                <h3 className="text-xs font-black uppercase tracking-wider text-slate-500 dark:text-slate-400 font-bold">
                  Live Cashier Invoice
                </h3>

                {/* Simulated POS Invoice paper card */}
                {(() => {
                  const activeProd = vm.products.find(p => p.id === selectedProductId);
                  const isQtyMode = saleInputMode === 'quantity';
                  const sQty = Number(saleQuantity || 0);
                  const sMoney = Number(saleMoneyAmount || 0);
                  const finalAmount = activeProd 
                    ? (isQtyMode ? Number((sQty * activeProd.salePrice).toFixed(2)) : sMoney)
                    : 0;
                  const finalQuantity = activeProd
                    ? (isQtyMode ? sQty : Number((sMoney / activeProd.salePrice).toFixed(3)))
                    : 0;

                  return (
                    <div className="bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-2xl shadow-sm p-5 space-y-4 relative overflow-hidden">
                      <div className="absolute top-0 inset-x-0 h-1.5 bg-slate-100 dark:bg-slate-900 flex gap-1 overflow-hidden">
                        {Array.from({ length: 20 }).map((_, i) => (
                          <div key={i} className="w-4 h-4 rotate-45 bg-slate-200/50 dark:bg-slate-700/50 -mt-2 shrink-0"></div>
                        ))}
                      </div>

                      <div className="pt-2 text-center border-b border-dashed border-slate-200 dark:border-slate-700 pb-4">
                        <span className="text-[10px] font-black tracking-widest text-emerald-700 dark:text-emerald-400 uppercase block">MILK SHOP COUNTER CASH</span>
                        <span className="text-[9px] text-slate-400 dark:text-slate-500 font-bold font-mono uppercase">{new Date().toLocaleDateString(undefined, { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}</span>
                      </div>

                      {activeProd ? (
                        <div className="space-y-3 font-mono">
                          <div className="flex justify-between items-start text-xs">
                            <div className="space-y-0.5">
                              <span className="font-black text-slate-800 dark:text-slate-100 text-sm block">{activeProd.name}</span>
                              <span className="text-slate-400 dark:text-slate-500 text-[10px] block font-bold">
                                Rate: Rs. {activeProd.salePrice} per {activeProd.unit}
                              </span>
                            </div>
                            <span className="font-bold text-slate-800 dark:text-slate-100 text-right text-xs">
                              Rs.{activeProd.salePrice}/{activeProd.unit}
                            </span>
                          </div>

                          <div className="flex justify-between items-center text-xs border-t border-slate-100 dark:border-slate-700 pt-3">
                            <span className="text-slate-500 dark:text-slate-400 font-bold uppercase">Volume/Weight:</span>
                            <span className="font-black text-emerald-800 dark:text-emerald-400 text-sm bg-emerald-50 dark:bg-emerald-900/30 px-2.5 py-1 rounded-lg">
                              {finalQuantity.toFixed(3)} {activeProd.unit}
                            </span>
                          </div>

                          <div className="flex justify-between items-center text-xs border-t border-slate-100 dark:border-slate-700 pt-3">
                            <span className="text-slate-500 dark:text-slate-400 font-bold uppercase">Billing Channel:</span>
                            <span className="font-bold text-slate-600 dark:text-slate-300 bg-slate-100 dark:bg-slate-800 px-2 py-0.5 rounded-md">
                              Cash POS
                            </span>
                          </div>

                          <div className="border-t-2 border-dashed border-slate-300 dark:border-slate-600 pt-4 flex justify-between items-center">
                            <span className="text-sm font-black text-slate-700 dark:text-slate-300 uppercase">NET PAYABLE:</span>
                            <span className="text-2xl font-black text-emerald-800 dark:text-emerald-400">
                              Rs. {finalAmount.toLocaleString()}
                            </span>
                          </div>
                        </div>
                      ) : (
                        <div className="p-8 text-center text-slate-400 dark:text-slate-500 text-xs">
                          Please select a product first.
                        </div>
                      )}
                    </div>
                  );
                })()}
              </div>

              {/* Action buttons footer */}
              <div className="space-y-2.5 pt-6 border-t border-slate-200 dark:border-slate-800">
                <button
                  type="button"
                  onClick={handleRetailSaleSubmit}
                  className="w-full py-4 rounded-xl bg-[#135d46] dark:bg-emerald-600 hover:bg-[#0e4837] dark:hover:bg-emerald-700 active:scale-98 text-white font-black text-sm shadow-md cursor-pointer transition-all flex items-center justify-center gap-2"
                >
                  <CheckCircle2 className="w-5 h-5" />
                  <span>RECORD CASH SALE & CONFIRM</span>
                </button>
                <button
                  type="button"
                  onClick={() => setIsRetailSaleOpen(false)}
                  className="px-4 py-3 rounded-xl bg-slate-200 dark:bg-slate-800 text-slate-700 dark:text-slate-300 font-bold text-xs transition-colors cursor-pointer w-full text-center hover:bg-slate-300 dark:hover:bg-slate-700"
                >
                  CANCEL TRANSACTION
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Modal: Spoilage */}
      <Modal
        isOpen={isSpoilageOpen}
        onClose={() => setIsSpoilageOpen(false)}
        title="Log Spoilage"
        subtitle="Record wasted or spoiled stock"
      >
        <form onSubmit={handleSpoilageSubmit} className="space-y-4 text-xs">
          <div>
            <label className="block text-slate-700 dark:text-slate-300 font-bold mb-1">Select Product</label>
            <select
              value={spoilageProductId}
              onChange={(e) => setSpoilageProductId(e.target.value)}
              className="w-full bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl px-3 py-2.5 text-slate-900 dark:text-slate-100 text-sm focus:outline-none focus:border-rose-500"
            >
              {activeProducts.map(p => (
                <option key={p.id} value={p.id}>{p.name} (Cost: Rs. {p.costRate}/{p.unit})</option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-slate-700 dark:text-slate-300 font-bold mb-1">Quantity Lost</label>
            <input
              type="number"
              min="1"
              value={spoilageQuantity}
              onChange={(e) => setSpoilageQuantity(e.target.value === '' ? '' : Number(e.target.value))}
              className="w-full bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl px-3 py-2.5 text-sm font-bold font-mono text-rose-600 dark:text-rose-400 focus:outline-none focus:border-rose-500"
              required
            />
          </div>

          <div className="flex justify-end gap-2.5 pt-3">
            <button
              type="button"
              onClick={() => setIsSpoilageOpen(false)}
              className="px-4 py-2 rounded-xl bg-slate-200 dark:bg-slate-800 text-slate-700 dark:text-slate-300 font-medium cursor-pointer hover:bg-slate-300 dark:hover:bg-slate-700"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-5 py-2 rounded-xl bg-rose-600 hover:bg-rose-700 text-white font-bold cursor-pointer"
            >
              Save Spoilage
            </button>
          </div>
        </form>
      </Modal>

      <ConfirmModal
        isOpen={Boolean(txToDelete)}
        onClose={() => setTxToDelete(null)}
        onConfirm={() => {
          if (txToDelete) {
            if (txToDelete.type === 'Expense') {
              vm.deleteExpense(txToDelete.id as string);
            } else if (txToDelete.type === 'Counter Sale') {
              vm.deleteRetailSale(txToDelete.id as string);
            } else if (txToDelete.type === 'Wholesale Order') {
              vm.deleteWholesaleSale(txToDelete.id as string);
            } else if (txToDelete.type === 'Collection') {
              vm.deleteCustomerPayment(txToDelete.id as string);
            } else if (txToDelete.type === 'Procurement') {
              vm.deleteSupplierDelivery(txToDelete.id as number);
            }
            setTxToDelete(null);
          }
        }}
        title={`Delete ${txToDelete?.type || 'Record'}`}
        message={`Are you sure you want to delete "${txToDelete?.title}"? This will update your balances and records accordingly.`}
      />
    </div>
  );
};
