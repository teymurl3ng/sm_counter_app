import React, { useState } from 'react';
import { Boxes, Receipt, Users } from 'lucide-react';
import { motion } from 'motion/react';
import { MilkShopViewModel } from '../../viewmodels/useMilkShopViewModel';
import { InventoryView } from '../inventory/InventoryView';
import { ExpensesView } from '../expenses/ExpensesView';
import { EmployeesView } from './EmployeesView';

interface ManagementHubViewProps {
  vm: MilkShopViewModel;
  subTab?: SubTab;
  setSubTab?: (tab: SubTab) => void;
  autoOpenAddProduct?: boolean;
  onResetAutoOpenAddProduct?: () => void;
  onRequireProduct?: (action: string) => void;
}

type SubTab = 'inventory' | 'expenses' | 'employees';

export const ManagementHubView: React.FC<ManagementHubViewProps> = ({ 
  vm, 
  subTab: controlledSubTab, 
  setSubTab: controlledSetSubTab,
  autoOpenAddProduct,
  onResetAutoOpenAddProduct,
  onRequireProduct
}) => {
  const [internalSubTab, setInternalSubTab] = useState<SubTab>('inventory');

  const activeSubTab = controlledSubTab ?? internalSubTab;
  const handleSetSubTab = controlledSetSubTab ?? setInternalSubTab;

  return (
    <div className="space-y-4">
      {/* Segmented Top Pill Navigation */}
      <div className="bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 p-1 rounded-2xl flex items-center gap-1 shadow-xs">
        <button
          onClick={() => handleSetSubTab('inventory')}
          className={`relative flex-1 py-2 px-2.5 rounded-xl text-xs font-bold flex items-center justify-center gap-1.5 transition-all duration-300 cursor-pointer select-none active:scale-95 ${
            activeSubTab === 'inventory'
              ? 'text-white'
              : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200 hover:bg-slate-50 dark:hover:bg-slate-800'
          }`}
        >
          {activeSubTab === 'inventory' && (
            <motion.div
              layoutId="managementSubTabIndicator"
              className="absolute inset-0 bg-[#135d46] dark:bg-emerald-600 rounded-xl shadow-xs"
              transition={{ type: 'spring', stiffness: 380, damping: 30 }}
            />
          )}
          <Boxes className="w-4 h-4 relative z-10" />
          <span className="relative z-10 hidden sm:inline">Stock</span>
        </button>
        <button
          onClick={() => handleSetSubTab('expenses')}
          className={`relative flex-1 py-2 px-2.5 rounded-xl text-xs font-bold flex items-center justify-center gap-1.5 transition-all duration-300 cursor-pointer select-none active:scale-95 ${
            activeSubTab === 'expenses'
              ? 'text-white'
              : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200 hover:bg-slate-50 dark:hover:bg-slate-800'
          }`}
        >
          {activeSubTab === 'expenses' && (
            <motion.div
              layoutId="managementSubTabIndicator"
              className="absolute inset-0 bg-[#135d46] dark:bg-emerald-600 rounded-xl shadow-xs"
              transition={{ type: 'spring', stiffness: 380, damping: 30 }}
            />
          )}
          <Receipt className="w-4 h-4 relative z-10" />
          <span className="relative z-10 hidden sm:inline">Expenses</span>
        </button>
        <button
          onClick={() => handleSetSubTab('employees')}
          className={`relative flex-1 py-2 px-2.5 rounded-xl text-xs font-bold flex items-center justify-center gap-1.5 transition-all duration-300 cursor-pointer select-none active:scale-95 ${
            activeSubTab === 'employees'
              ? 'text-white'
              : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200 hover:bg-slate-50 dark:hover:bg-slate-800'
          }`}
        >
          {activeSubTab === 'employees' && (
            <motion.div
              layoutId="managementSubTabIndicator"
              className="absolute inset-0 bg-[#135d46] dark:bg-emerald-600 rounded-xl shadow-xs"
              transition={{ type: 'spring', stiffness: 380, damping: 30 }}
            />
          )}
          <Users className="w-4 h-4 relative z-10" />
          <span className="relative z-10 hidden sm:inline">Employees</span>
        </button>
      </div>

      {/* Sub View Content */}
      {activeSubTab === 'inventory' && (
        <InventoryView
          vm={vm}
          autoOpenAddProduct={autoOpenAddProduct}
          onResetAutoOpenAddProduct={onResetAutoOpenAddProduct}
          onRequireProduct={onRequireProduct}
        />
      )}
      {activeSubTab === 'expenses' && <ExpensesView vm={vm} />}
      {activeSubTab === 'employees' && <EmployeesView vm={vm} />}
    </div>
  );
};
