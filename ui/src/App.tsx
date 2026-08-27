import { useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { useMilkShopViewModel } from './viewmodels/useMilkShopViewModel';
import { Navbar, ActiveTab } from './components/layout/Navbar';
import { DailyOperationsView } from './components/operations/DailyOperationsView';
import { LedgerHubView } from './components/accounts/LedgerHubView';
import { ManagementHubView } from './components/management/ManagementHubView';
import { ReportsView } from './components/reports/ReportsView';
import { SettingsView } from './components/settings/SettingsView';
import { DailyCloseSummaryModal } from './components/reports/DailyCloseSummaryModal';
import { NoProductErrorModal } from './components/common/NoProductErrorModal';
import { PaymentTransactionView } from './components/common/PaymentTransactionView';

const TAB_INDEX: Record<ActiveTab, number> = {
  operations: 0,
  ledgers: 1,
  management: 2,
  reports: 3,
  settings: 99,
};

const slideVariants = {
  enter: (direction: number) => ({
    x: direction === 0 ? 0 : (direction > 0 ? 15 : -15),
    opacity: 0,
  }),
  center: {
    x: 0,
    opacity: 1,
  },
  exit: (direction: number) => ({
    x: direction === 0 ? 0 : (direction < 0 ? 15 : -15),
    opacity: 0,
  })
};

const slideTransition = {
  x: { type: 'tween', duration: 0.07, ease: 'easeOut' },
  opacity: { duration: 0.05, ease: 'easeOut' }
} as const;

export default function App() {
  const vm = useMilkShopViewModel();
  const [activeTab, setActiveTab] = useState<ActiveTab>('operations');
  const [direction, setDirection] = useState(0);
  const [ledgerSubTab, setLedgerSubTab] = useState<'customers' | 'suppliers' | 'wholesale'>('customers');
  const [managementSubTab, setManagementSubTab] = useState<'inventory' | 'expenses' | 'employees'>('inventory');
  const [reportsSubTab, setReportsSubTab] = useState<'trends' | 'sales' | 'cashflow' | 'archives'>('trends');
  const [autoOpenAddProduct, setAutoOpenAddProduct] = useState(false);
  const [isNoProductModalOpen, setIsNoProductModalOpen] = useState(false);
  const [noProductActionName, setNoProductActionName] = useState('');
  const [isDailyCloseOpen, setIsDailyCloseOpen] = useState(false);
  const [paymentFlow, setPaymentFlow] = useState<{
    mode: 'inflow' | 'outflow';
    contactId: string;
    contactName: string;
    initialAmount?: number;
  } | null>(null);
 
  const handleSetActiveTab = (tab: ActiveTab, subTab?: string) => {
    // If clicking settings and it's already active, toggle it off by switching to operations
    if (tab === 'settings' && activeTab === 'settings') {
      setDirection(0);
      setActiveTab('operations');
      return;
    }
 
    if (activeTab === 'settings' || tab === 'settings') {
      setDirection(0);
    } else {
      const currentIndex = TAB_INDEX[activeTab] ?? 0;
      const nextIndex = TAB_INDEX[tab] ?? 0;
      setDirection(nextIndex > currentIndex ? 1 : -1);
    }
    setActiveTab(tab);
    
    if (tab === 'ledgers' && subTab) {
      setLedgerSubTab(subTab as any);
    } else if (tab === 'management' && subTab) {
      setManagementSubTab(subTab as any);
    } else if (tab === 'reports' && subTab) {
      setReportsSubTab(subTab as any);
    } else if (tab === 'reports' && !subTab) {
      // Default to sales if navigated via specific sales card (handled in Navbar)
      // but if just clicked via nav bar, we keep last or default
    }
  };

  const handleRequireProduct = (action: string) => {
    setNoProductActionName(action);
    setIsNoProductModalOpen(true);
  };

  const handleGoToAddProduct = () => {
    if (activeTab === 'settings') {
      setDirection(0);
    } else {
      const currentIndex = TAB_INDEX[activeTab] ?? 0;
      const nextIndex = TAB_INDEX['management'] ?? 0;
      setDirection(nextIndex > currentIndex ? 1 : -1);
    }
    setActiveTab('management');
    setManagementSubTab('inventory');
    setAutoOpenAddProduct(true);
  };

  if (paymentFlow) {
    return (
      <div className={vm.settings.theme === 'dark' ? 'dark' : ''}>
        <PaymentTransactionView 
          vm={vm}
          mode={paymentFlow.mode}
          contactId={paymentFlow.contactId}
          contactName={paymentFlow.contactName}
          initialAmount={paymentFlow.initialAmount}
          onBack={() => setPaymentFlow(null)}
        />
      </div>
    );
  }

  return (
    <div className={`${vm.settings.theme === 'dark' ? 'dark' : ''}`}>
      <div className="min-h-screen bg-[#f7f6f0] dark:bg-slate-950 text-slate-900 dark:text-slate-100 flex flex-col justify-between selection:bg-emerald-500/20 dark:selection:bg-emerald-500/40 selection:text-emerald-900 dark:selection:text-emerald-100 font-sans overflow-x-hidden">
      {/* Header */}
      <Navbar
        activeTab={activeTab}
        setActiveTab={handleSetActiveTab}
        vm={vm}
      />

      {/* Main Container */}
      <main className="max-w-4xl mx-auto px-4 py-3 flex-1 w-full pb-24 min-h-[65vh] overflow-x-hidden">
        {activeTab === 'operations' ? (
          <DailyOperationsView vm={vm} />
        ) : (
          <AnimatePresence initial={false} mode="wait" custom={direction}>
            <motion.div
              key={activeTab}
              custom={direction}
              variants={slideVariants}
              initial="enter"
              animate="center"
              exit="exit"
              transition={slideTransition}
              className="w-full"
            >
              {activeTab === 'ledgers' && (
                <LedgerHubView
                  vm={vm}
                  subTab={ledgerSubTab}
                  setSubTab={setLedgerSubTab}
                  onRequireProduct={handleRequireProduct}
                  onRecordPayment={(mode, id, name, amount) => {
                    setPaymentFlow({ mode, contactId: id, contactName: name, initialAmount: amount });
                  }}
                />
              )}
              {activeTab === 'management' && (
                <ManagementHubView 
                  vm={vm} 
                  subTab={managementSubTab}
                  setSubTab={setManagementSubTab}
                  autoOpenAddProduct={autoOpenAddProduct}
                  onResetAutoOpenAddProduct={() => setAutoOpenAddProduct(false)}
                  onRequireProduct={handleRequireProduct}
                />
              )}
              {activeTab === 'reports' && (
                <ReportsView 
                  vm={vm} 
                  activeSubTab={reportsSubTab}
                  onSubTabChange={setReportsSubTab}
                />
              )}
              {activeTab === 'settings' && (
                <SettingsView 
                  vm={vm} 
                  onOpenDailyClose={() => setIsDailyCloseOpen(true)}
                />
              )}
            </motion.div>
          </AnimatePresence>
        )}
      </main>

      {/* Reconciliation Modal */}
      <DailyCloseSummaryModal
        isOpen={isDailyCloseOpen}
        onClose={() => setIsDailyCloseOpen(false)}
        vm={vm}
      />

      {/* No Product Warning Modal */}
      <NoProductErrorModal
        isOpen={isNoProductModalOpen}
        onClose={() => setIsNoProductModalOpen(false)}
        onGoToAddProduct={handleGoToAddProduct}
        actionName={noProductActionName}
      />

      {/* Footer Bar */}
      <footer className="border-t border-slate-200/80 dark:border-slate-800 bg-white dark:bg-slate-900 py-2.5 text-[11px] text-slate-500 dark:text-slate-400 mb-16 shadow-xs">
        <div className="max-w-4xl mx-auto px-4 flex items-center justify-between gap-2">
          <span className="font-semibold text-slate-600 dark:text-slate-300">{vm.settings.storeName} • POS</span>
        </div>
      </footer>
    </div>
  </div>
);
}
