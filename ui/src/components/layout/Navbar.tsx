import React from 'react';
import { 
  Milk, 
  Users, 
  Boxes, 
  Calendar,
  Settings,
  TrendingUp,
  Truck,
  Check,
  ChevronRight,
  DollarSign
} from 'lucide-react';
import { motion } from 'motion/react';
import { MilkShopViewModel } from '../../viewmodels/useMilkShopViewModel';

export type ActiveTab = 'operations' | 'ledgers' | 'management' | 'reports' | 'settings';

interface NavbarProps {
  activeTab: ActiveTab;
  setActiveTab: (tab: ActiveTab, subTab?: string) => void;
  vm: MilkShopViewModel;
}

export const Navbar: React.FC<NavbarProps> = ({
  activeTab,
  setActiveTab,
  vm,
}) => {
  const navItems: { id: Exclude<ActiveTab, 'settings'>; label: string; icon: React.FC<{ className?: string }> }[] = [
    { id: 'operations', label: 'Operations', icon: Milk },
    { id: 'ledgers', label: 'Accounts', icon: Users },
    { id: 'management', label: 'Management', icon: Boxes },
    { id: 'reports', label: 'Reports', icon: TrendingUp },
  ];

  const currentTimeStr = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

  return (
    <>
      {/* Top Header Section */}
      <header className="px-4 pt-5 pb-1 max-w-4xl mx-auto w-full">
        {/* Title Bar */}
        <div className="flex items-start justify-between gap-3 mb-4">
          <div>
            <div className="flex items-center gap-2 flex-wrap">
              <h1 className="text-xl sm:text-2xl font-black text-slate-900 dark:text-slate-100 tracking-tight">
                {vm.settings.storeName || 'My Store'}
              </h1>
              <span className="inline-flex items-center gap-1 bg-emerald-100 dark:bg-emerald-500/10 text-emerald-800 dark:text-emerald-400 border border-emerald-200/80 dark:border-emerald-800/80 text-xs font-bold px-2.5 py-0.5 rounded-full">
                <Check className="w-3 h-3 stroke-[3]" />
                Saved
              </span>
            </div>

            <div className="flex items-center gap-1.5 text-xs text-slate-500 dark:text-slate-400 font-medium mt-1">
              <Calendar className="w-3.5 h-3.5 text-slate-400 dark:text-slate-500 shrink-0" />
              <label className="relative inline-flex items-center cursor-pointer hover:text-slate-800 dark:hover:text-slate-200 transition-colors">
                <span>{vm.selectedDate}</span>
                <input
                  type="date"
                  value={vm.selectedDate}
                  onChange={(e) => vm.setSelectedDate(e.target.value)}
                  className="absolute inset-0 opacity-0 cursor-pointer w-full"
                />
              </label>
              <span>·</span>
              <span>Saved at {currentTimeStr}</span>
            </div>
          </div>

          <button
            onClick={() => setActiveTab('settings')}
            title="Settings"
            className={`w-10 h-10 rounded-2xl border shadow-xs flex items-center justify-center active:scale-95 transition-all cursor-pointer shrink-0 ${
              activeTab === 'settings'
                ? 'bg-[#135d46] dark:bg-emerald-600 text-white border-[#135d46] dark:border-emerald-600'
                : 'bg-white dark:bg-slate-900 border-slate-200/90 dark:border-slate-800 text-slate-700 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-slate-800'
            }`}
          >
            <Settings className="w-4 h-4" />
          </button>
        </div>

        {/* 2x2 Metric Grid */}
        {activeTab === 'operations' && (
          <div className="grid grid-cols-2 gap-3 mb-2">
            {/* Card 1: Cash in Hand */}
            <div className="bg-[#135d46] dark:bg-emerald-700 text-white rounded-2xl p-3.5 sm:p-4 shadow-xs relative overflow-hidden flex flex-col justify-between min-h-[96px]">
              <div className="flex items-center justify-between">
                <span className="text-xs text-emerald-100/90 font-medium">Cash in Hand</span>
                <DollarSign className="w-4 h-4 text-emerald-200/90" />
              </div>
              <div className="my-1">
                <div className="text-xl sm:text-2xl font-black tracking-tight font-mono">
                  Rs {vm.expectedDrawerCash.toLocaleString()}
                </div>
              </div>
              <div className="text-[11px] text-emerald-100/80 font-medium">
                Today: +Rs {vm.shopRetailCashRevenue.toLocaleString()} net
              </div>
            </div>

            {/* Card 2: Today's Sales */}
            <button
              onClick={() => setActiveTab('reports', 'sales')}
              className="bg-white dark:bg-slate-900 rounded-2xl p-3.5 sm:p-4 shadow-xs border border-slate-200/80 dark:border-slate-800 flex flex-col justify-between min-h-[96px] text-left hover:border-emerald-400/80 dark:hover:border-emerald-500 transition-all cursor-pointer group"
            >
              <div className="flex items-center justify-between">
                <span className="text-xs text-slate-600 dark:text-slate-400 font-medium">Today's Sales</span>
                <TrendingUp className="w-4 h-4 text-emerald-600 dark:text-emerald-400 group-hover:scale-110 transition-transform" />
              </div>
              <div className="my-1">
                <div className="text-xl sm:text-2xl font-black text-slate-900 dark:text-slate-100 tracking-tight font-mono">
                  Rs {(vm.shopRetailCashRevenue + vm.wholesaleCreditRevenue).toLocaleString()}
                </div>
              </div>
              <div className="flex items-center justify-between text-[11px] text-slate-500 dark:text-slate-400 font-medium">
                <span className="truncate">Cash Rs {vm.shopRetailCashRevenue.toLocaleString()} · Credit Rs {vm.wholesaleCreditRevenue.toLocaleString()}</span>
                <ChevronRight className="w-3.5 h-3.5 text-slate-400 dark:text-slate-500 group-hover:translate-x-0.5 transition-transform shrink-0" />
              </div>
            </button>

            {/* Card 3: Customer Credit */}
            <button
              onClick={() => setActiveTab('ledgers', 'customers')}
              className="bg-white dark:bg-slate-900 rounded-2xl p-3.5 sm:p-4 shadow-xs border border-slate-200/80 dark:border-slate-800 flex flex-col justify-between min-h-[96px] text-left hover:border-amber-400/80 dark:hover:border-amber-500 transition-all cursor-pointer group"
            >
              <div className="flex items-center justify-between">
                <span className="text-xs text-slate-600 dark:text-slate-400 font-medium">Customer Credit</span>
                <Users className="w-4 h-4 text-amber-600 dark:text-amber-400" />
              </div>
              <div className="my-1">
                <div className="text-xl sm:text-2xl font-black text-amber-800 dark:text-amber-500 tracking-tight font-mono">
                  Rs {vm.totalCustomerReceivables.toLocaleString()}
                </div>
              </div>
              <div className="flex items-center justify-between text-[11px] text-slate-500 dark:text-slate-400 font-medium">
                <span>{vm.customerDeliveries.length} customers owe</span>
                <ChevronRight className="w-3.5 h-3.5 text-slate-400 dark:text-slate-500 group-hover:translate-x-0.5 transition-transform" />
              </div>
            </button>

            {/* Card 4: Supplier Payables */}
            <button
              onClick={() => setActiveTab('ledgers', 'suppliers')}
              className="bg-white dark:bg-slate-900 rounded-2xl p-3.5 sm:p-4 shadow-xs border border-slate-200/80 dark:border-slate-800 flex flex-col justify-between min-h-[96px] text-left hover:border-blue-400/80 dark:hover:border-blue-500 transition-all cursor-pointer group"
            >
              <div className="flex items-center justify-between">
                <span className="text-xs text-slate-600 dark:text-slate-400 font-medium">Supplier Payables</span>
                <Truck className="w-4 h-4 text-blue-600 dark:text-blue-400" />
              </div>
              <div className="my-1">
                <div className="text-xl sm:text-2xl font-black text-slate-900 dark:text-slate-100 tracking-tight font-mono">
                  Rs {vm.totalSupplierPayables.toLocaleString()}
                </div>
              </div>
              <div className="flex items-center justify-between text-[11px] text-slate-500 dark:text-slate-400 font-medium">
                <span>{vm.suppliers.length} suppliers</span>
                <ChevronRight className="w-3.5 h-3.5 text-slate-400 dark:text-slate-500 group-hover:translate-x-0.5 transition-transform" />
              </div>
            </button>
          </div>
        )}
      </header>

      {/* Bottom Navigation Bar - iPhone-style ultra-translucent crystal frosted glass */}
      <nav className="fixed bottom-5 inset-x-0 mx-auto max-w-sm w-[calc(100%-2.5rem)] z-50 bg-white/10 dark:bg-slate-900/60 backdrop-blur-md border border-white/20 dark:border-slate-800 shadow-[0_20px_50px_rgba(0,0,0,0.12)] rounded-3xl p-1.5 grid grid-cols-4 gap-1.5">
        {navItems.map((item) => {
          const Icon = item.icon;
          const isActive = activeTab === item.id;
          return (
            <button
              key={item.id}
              onClick={() => setActiveTab(item.id)}
              className={`relative flex flex-col items-center justify-center py-2.5 px-3 rounded-2xl transition-all duration-300 cursor-pointer select-none active:scale-95 ${
                isActive 
                  ? 'text-white font-extrabold' 
                  : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-100 font-medium'
              }`}
            >
              {isActive && (
                <motion.div
                  layoutId="navbarActiveIndicator"
                  className="absolute inset-0 bg-[#135d46] dark:bg-emerald-600 rounded-2xl -z-10 shadow-md shadow-[#135d46]/10 dark:shadow-emerald-900/20 border border-[#135d46]/10 dark:border-emerald-500/20"
                  transition={{ type: 'spring', stiffness: 320, damping: 28 }}
                />
              )}
              <Icon className="w-4.5 h-4.5 relative z-10" />
              <span className="text-[10px] mt-1 leading-none font-bold tracking-tight relative z-10">{item.label}</span>
            </button>
          );
        })}
      </nav>
    </>
  );
};
