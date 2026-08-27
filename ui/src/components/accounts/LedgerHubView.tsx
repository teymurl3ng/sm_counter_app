import React, { useState } from 'react';
import { Users, Truck, Store } from 'lucide-react';
import { motion } from 'motion/react';
import { MilkShopViewModel } from '../../viewmodels/useMilkShopViewModel';
import { CustomerLedgerView } from './CustomerLedgerView';
import { SupplierLedgerView } from '../suppliers/SupplierLedgerView';
import { WholesaleView } from '../wholesale/WholesaleView';

interface LedgerHubViewProps {
  vm: MilkShopViewModel;
  subTab?: SubTab;
  setSubTab?: (tab: SubTab) => void;
  onRequireProduct?: (action: string) => void;
  onRecordPayment?: (mode: 'inflow' | 'outflow', id: string, name: string, amount?: number) => void;
}

type SubTab = 'customers' | 'suppliers' | 'wholesale';

export const LedgerHubView: React.FC<LedgerHubViewProps> = ({ 
  vm, 
  subTab: controlledSubTab, 
  setSubTab: controlledSetSubTab,
  onRequireProduct,
  onRecordPayment
}) => {
  const [internalSubTab, setInternalSubTab] = useState<SubTab>('customers');

  const activeSubTab = controlledSubTab ?? internalSubTab;
  const handleSetSubTab = controlledSetSubTab ?? setInternalSubTab;

  return (
    <div className="space-y-4">
      {/* Segmented Top Pill Navigation */}
      <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 p-1 rounded-2xl flex items-center gap-1 shadow-xs">
        <button
          onClick={() => handleSetSubTab('customers')}
          className={`relative flex-1 py-2 px-3 rounded-xl text-xs font-bold flex items-center justify-center gap-1.5 transition-all duration-300 cursor-pointer select-none active:scale-95 ${
            activeSubTab === 'customers'
              ? 'text-white'
              : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200 hover:bg-slate-50 dark:hover:bg-slate-800'
          }`}
        >
          {activeSubTab === 'customers' && (
            <motion.div
              layoutId="ledgerSubTabIndicator"
              className="absolute inset-0 bg-[#135d46] dark:bg-emerald-600 rounded-xl shadow-xs"
              transition={{ type: 'spring', stiffness: 380, damping: 30 }}
            />
          )}
          <Users className="w-4 h-4 relative z-10" />
          <span className="relative z-10">Customers</span>
        </button>

        <button
          onClick={() => handleSetSubTab('suppliers')}
          className={`relative flex-1 py-2 px-3 rounded-xl text-xs font-bold flex items-center justify-center gap-1.5 transition-all duration-300 cursor-pointer select-none active:scale-95 ${
            activeSubTab === 'suppliers'
              ? 'text-white'
              : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200 hover:bg-slate-50 dark:hover:bg-slate-800'
          }`}
        >
          {activeSubTab === 'suppliers' && (
            <motion.div
              layoutId="ledgerSubTabIndicator"
              className="absolute inset-0 bg-[#135d46] dark:bg-emerald-600 rounded-xl shadow-xs"
              transition={{ type: 'spring', stiffness: 380, damping: 30 }}
            />
          )}
          <Truck className="w-4 h-4 relative z-10" />
          <span className="relative z-10">Suppliers</span>
        </button>

        <button
          onClick={() => handleSetSubTab('wholesale')}
          className={`relative flex-1 py-2 px-3 rounded-xl text-xs font-bold flex items-center justify-center gap-1.5 transition-all duration-300 cursor-pointer select-none active:scale-95 ${
            activeSubTab === 'wholesale'
              ? 'text-white'
              : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200 hover:bg-slate-50 dark:hover:bg-slate-800'
          }`}
        >
          {activeSubTab === 'wholesale' && (
            <motion.div
              layoutId="ledgerSubTabIndicator"
              className="absolute inset-0 bg-[#135d46] dark:bg-emerald-600 rounded-xl shadow-xs"
              transition={{ type: 'spring', stiffness: 380, damping: 30 }}
            />
          )}
          <Store className="w-4 h-4 relative z-10" />
          <span className="relative z-10">Wholesale</span>
        </button>
      </div>

      {/* Sub View Content */}
      {activeSubTab === 'customers' && (
        <CustomerLedgerView 
          vm={vm} 
          onRequireProduct={onRequireProduct} 
          onRecordPayment={(id, name, amount) => onRecordPayment?.('inflow', id, name, amount)}
        />
      )}
      {activeSubTab === 'suppliers' && (
        <SupplierLedgerView 
          vm={vm} 
          onRequireProduct={onRequireProduct} 
          onRecordPayment={(id, name, amount) => onRecordPayment?.('outflow', id, name, amount)}
        />
      )}
      {activeSubTab === 'wholesale' && (
        <WholesaleView vm={vm} onRequireProduct={onRequireProduct} />
      )}
    </div>
  );
};
