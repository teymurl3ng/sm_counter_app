import React, { useState } from 'react';
import { motion } from 'motion/react';
import { 
  Building2, 
  Smartphone, 
  Plus, 
  Download, 
  Upload, 
  Check, 
  AlertTriangle,
  Tag,
  Database,
  FileText,
  ShieldCheck,
  Save,
  Moon,
  Sun
} from 'lucide-react';
import { MilkShopViewModel } from '../../viewmodels/useMilkShopViewModel';

interface SettingsViewProps {
  vm: MilkShopViewModel;
  onClose?: () => void;
  onOpenDailyClose?: () => void;
}

type SettingsTab = 'business' | 'app';

export const SettingsView: React.FC<SettingsViewProps> = ({ vm, onClose, onOpenDailyClose }) => {
  const [activeTab, setActiveTab] = useState<SettingsTab>('business');

  // Business Settings Form State
  const [storeName, setStoreName] = useState(vm.settings.storeName);
  const [currencySymbol, setCurrencySymbol] = useState(vm.settings.currencySymbol || 'Rs.');
  const [startingCash, setStartingCash] = useState<number | ''>(vm.settings.startingCashInHand);
  const [openingStock, setOpeningStock] = useState<number | ''>(vm.settings.openingStockValue);
  const [isSavedNotice, setIsSavedNotice] = useState(false);

  // Category Add State
  const [newCategoryName, setNewCategoryName] = useState('');

  // Data Import / Export State
  const [importJsonText, setImportJsonText] = useState('');
  const [importStatus, setImportStatus] = useState<'idle' | 'success' | 'error'>('idle');
  const [isResetConfirming, setIsResetConfirming] = useState(false);

  const handleSaveBusinessSettings = (e: React.FormEvent) => {
    e.preventDefault();
    vm.setSettings({
      ...vm.settings,
      storeName,
      currencySymbol,
      startingCashInHand: Math.max(0, Number(startingCash || 0)),
      openingStockValue: Math.max(0, Number(openingStock || 0)),
    });
    setIsSavedNotice(true);
    setTimeout(() => setIsSavedNotice(false), 2500);
  };

  const handleAddCategorySubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (newCategoryName.trim()) {
      vm.addExpenseCategory(newCategoryName);
      setNewCategoryName('');
    }
  };

  const handleExportData = () => {
    const backupObj = {
      products: vm.products,
      customers: vm.customers,
      suppliers: vm.suppliers,
      settings: vm.settings,
      expenses: vm.expenses,
      wholesaleSales: vm.wholesaleSales,
      exportedAt: new Date().toISOString(),
    };
    const jsonStr = JSON.stringify(backupObj, null, 2);
    const blob = new Blob([jsonStr], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `dairy_pos_backup_${new Date().toISOString().split('T')[0]}.json`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const handleImportSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!importJsonText.trim()) return;
    const success = vm.importAllData(importJsonText);
    if (success) {
      setImportStatus('success');
      setImportJsonText('');
      setTimeout(() => setImportStatus('idle'), 3000);
    } else {
      setImportStatus('error');
    }
  };

  return (
    <div className="space-y-4">
      {/* Top Navigation Tabs */}
      <div className="bg-slate-100/80 dark:bg-slate-900/80 p-1.5 rounded-2xl flex items-center gap-1.5 border border-slate-200/80 dark:border-slate-800 relative">
        <button
          onClick={() => setActiveTab('business')}
          className={`relative flex-1 py-2 px-3 rounded-xl text-xs font-bold flex items-center justify-center gap-2 transition-all duration-300 cursor-pointer select-none active:scale-95 ${
            activeTab === 'business'
              ? 'text-white font-extrabold'
              : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-100 hover:bg-white/60 dark:hover:bg-slate-800/60'
          }`}
        >
          {activeTab === 'business' && (
            <motion.div
              layoutId="settingsSubTabIndicator"
              className="absolute inset-0 bg-[#135d46] rounded-xl shadow-xs"
              transition={{ type: 'spring', stiffness: 380, damping: 30 }}
            />
          )}
          <Building2 className="w-4 h-4 relative z-10" />
          <span className="relative z-10">Business Details</span>
        </button>

        <button
          onClick={() => setActiveTab('app')}
          className={`relative flex-1 py-2 px-3 rounded-xl text-xs font-bold flex items-center justify-center gap-2 transition-all duration-300 cursor-pointer select-none active:scale-95 ${
            activeTab === 'app'
              ? 'text-white font-extrabold'
              : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-100 hover:bg-white/60 dark:hover:bg-slate-800/60'
          }`}
        >
          {activeTab === 'app' && (
            <motion.div
              layoutId="settingsSubTabIndicator"
              className="absolute inset-0 bg-[#135d46] rounded-xl shadow-xs"
              transition={{ type: 'spring', stiffness: 380, damping: 30 }}
            />
          )}
          <Smartphone className="w-4 h-4 relative z-10" />
          <span className="relative z-10">General Settings</span>
        </button>
      </div>

      {/* BUSINESS SETTINGS TAB */}
      {activeTab === 'business' && (
        <div className="space-y-4">
          {/* Section 1: Store Identity & Finances */}
          <div className="p-4 sm:p-5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 shadow-xs space-y-4">
            <div className="flex items-center justify-between border-b border-slate-100 dark:border-slate-800 pb-3">
              <div>
                <h3 className="text-sm font-bold text-slate-900 dark:text-slate-100 flex items-center gap-2">
                  <Building2 className="w-4 h-4 text-[#135d46] dark:text-emerald-400" />
                  <span>Store Identity & Accounting</span>
                </h3>
                <p className="text-[11px] text-slate-500 dark:text-slate-400">Shop details, currency symbol, and starting balance</p>
              </div>

              {isSavedNotice && (
                <span className="text-xs text-emerald-700 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-500/10 border border-emerald-200 dark:border-emerald-800 px-3 py-1 rounded-full font-bold flex items-center gap-1 animate-fadeIn">
                  <Check className="w-3.5 h-3.5" />
                  Saved
                </span>
              )}
            </div>

            <form onSubmit={handleSaveBusinessSettings} className="space-y-4 text-xs">
              <div>
                <label className="block text-slate-700 dark:text-slate-300 font-bold mb-1">Store / Shop Name</label>
                <input
                  type="text"
                  value={storeName}
                  onChange={(e) => setStoreName(e.target.value)}
                  placeholder="e.g. Al-Rehman Pure Milk Center"
                  className="w-full bg-slate-50 dark:bg-slate-800/50 border border-slate-200 dark:border-slate-700 rounded-xl px-3.5 py-2.5 text-slate-900 dark:text-slate-100 font-medium focus:outline-none focus:border-[#135d46] dark:focus:border-emerald-500 focus:bg-white dark:focus:bg-slate-800"
                  required
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <div>
                  <label className="block text-slate-700 dark:text-slate-300 font-bold mb-1">Currency Symbol</label>
                  <input
                    type="text"
                    value={currencySymbol}
                    onChange={(e) => setCurrencySymbol(e.target.value)}
                    className="w-full bg-slate-50 dark:bg-slate-800/50 border border-slate-200 dark:border-slate-700 rounded-xl px-3.5 py-2.5 text-slate-900 dark:text-slate-100 font-mono font-bold focus:outline-none focus:border-[#135d46] dark:focus:border-emerald-500 focus:bg-white dark:focus:bg-slate-800"
                    required
                  />
                </div>
                <div>
                  <label className="block text-slate-700 dark:text-slate-300 font-bold mb-1">Starting Cash (Rs.)</label>
                  <input
                    type="number"
                    min="0"
                    value={startingCash}
                    onChange={(e) => setStartingCash(e.target.value === '' ? '' : Number(e.target.value))}
                    className="w-full bg-slate-50 dark:bg-slate-800/50 border border-slate-200 dark:border-slate-700 rounded-xl px-3.5 py-2.5 text-slate-900 dark:text-slate-100 font-mono font-bold focus:outline-none focus:border-[#135d46] dark:focus:border-emerald-500 focus:bg-white dark:focus:bg-slate-800"
                  />
                </div>
                <div>
                  <label className="block text-slate-700 dark:text-slate-300 font-bold mb-1">Opening Stock (Rs.)</label>
                  <input
                    type="number"
                    min="0"
                    value={openingStock}
                    onChange={(e) => setOpeningStock(e.target.value === '' ? '' : Number(e.target.value))}
                    className="w-full bg-slate-50 dark:bg-slate-800/50 border border-slate-200 dark:border-slate-700 rounded-xl px-3.5 py-2.5 text-slate-900 dark:text-slate-100 font-mono font-bold focus:outline-none focus:border-[#135d46] dark:focus:border-emerald-500 focus:bg-white dark:focus:bg-slate-800"
                  />
                </div>
              </div>

              <div className="flex justify-end pt-1">
                <button
                  type="submit"
                  className="px-4 py-2.5 rounded-xl bg-[#135d46] dark:bg-emerald-600 hover:bg-[#0e4837] dark:hover:bg-emerald-700 text-white font-bold flex items-center gap-1.5 cursor-pointer shadow-xs transition-colors"
                >
                  <Save className="w-4 h-4" />
                  <span>Save</span>
                </button>
              </div>
            </form>
          </div>

          {/* Section 2: Expense Categories Management */}

          {/* Section 3: Expense Categories Management */}
          <div className="p-4 sm:p-5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 shadow-xs space-y-4">
            <div className="border-b border-slate-100 dark:border-slate-800 pb-3">
              <h3 className="text-sm font-bold text-slate-900 dark:text-slate-100 flex items-center gap-2">
                <Tag className="w-4 h-4 text-[#135d46] dark:text-emerald-400" />
                <span>Expense Categories</span>
              </h3>
              <p className="text-[11px] text-slate-500 dark:text-slate-400">Custom categories for daily operational payouts</p>
            </div>

            <form onSubmit={handleAddCategorySubmit} className="flex gap-2 text-xs">
              <input
                type="text"
                value={newCategoryName}
                onChange={(e) => setNewCategoryName(e.target.value)}
                placeholder="New category name (e.g. Boiler Maintenance)"
                className="flex-1 bg-slate-50 dark:bg-slate-800/50 border border-slate-200 dark:border-slate-700 rounded-xl px-3 py-2 text-slate-900 dark:text-slate-100 focus:outline-none focus:border-[#135d46] dark:focus:border-emerald-500 focus:bg-white dark:focus:bg-slate-800"
              />
              <button
                type="submit"
                className="px-4 py-2 bg-[#135d46] dark:bg-emerald-600 hover:bg-[#0e4837] dark:hover:bg-emerald-700 text-white font-bold rounded-xl flex items-center gap-1 cursor-pointer shrink-0 transition-colors"
              >
                <Plus className="w-4 h-4" />
                <span>Add</span>
              </button>
            </form>

            <div className="flex flex-wrap gap-2">
              {vm.settings.expenseCategories.map((cat) => (
                <span
                  key={cat}
                  className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-100 dark:bg-slate-800 text-slate-800 dark:text-slate-200 text-xs font-medium border border-slate-200 dark:border-slate-700"
                >
                  <span>{cat}</span>
                  {vm.settings.expenseCategories.length > 1 && (
                    <button
                      type="button"
                      onClick={() => vm.removeExpenseCategory(cat)}
                      className="text-slate-400 hover:text-rose-600 p-0.5 cursor-pointer font-bold"
                      title="Remove category"
                    >
                      ×
                    </button>
                  )}
                </span>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* APP SETTINGS TAB */}
      {activeTab === 'app' && (
        <div className="space-y-4">
          {/* Theme Selector */}
          <div className="p-4 sm:p-5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 shadow-xs space-y-4">
            <div className="border-b border-slate-100 dark:border-slate-800 pb-3">
              <h3 className="text-sm font-bold text-slate-900 dark:text-slate-100 flex items-center gap-2">
                <Sun className="w-4 h-4 text-amber-500 dark:text-amber-400" />
                <span>Theme</span>
              </h3>
              <p className="text-[11px] text-slate-500 dark:text-slate-400">Choose your preferred visual style for the application</p>
            </div>

            <div className="flex gap-3">
              <button
                type="button"
                onClick={() => vm.setSettings({ ...vm.settings, theme: 'light' })}
                className={`flex-1 p-3 rounded-xl border flex items-center justify-center gap-2 transition-all ${
                  vm.settings.theme === 'light'
                    ? 'bg-emerald-50 dark:bg-emerald-500/10 border-emerald-500 text-emerald-700 dark:text-emerald-400'
                    : 'bg-slate-50 dark:bg-slate-800/50 border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-400'
                }`}
              >
                <Sun className="w-4 h-4" />
                <span className="text-xs font-bold">Light</span>
              </button>
              <button
                type="button"
                onClick={() => vm.setSettings({ ...vm.settings, theme: 'dark' })}
                className={`flex-1 p-3 rounded-xl border flex items-center justify-center gap-2 transition-all ${
                  vm.settings.theme === 'dark'
                    ? 'bg-slate-800 dark:bg-emerald-500/20 border-emerald-500 text-slate-100'
                    : 'bg-slate-50 dark:bg-slate-800/50 border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-400'
                }`}
              >
                <Moon className="w-4 h-4" />
                <span className="text-xs font-bold">Dark</span>
              </button>
            </div>
          </div>

          {/* Data Backup & Export */}
          <div className="p-4 sm:p-5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 shadow-xs space-y-4">
            <div className="border-b border-slate-100 dark:border-slate-800 pb-3">
              <h3 className="text-sm font-bold text-slate-900 dark:text-slate-100 flex items-center gap-2">
                <Database className="w-4 h-4 text-[#135d46] dark:text-emerald-400" />
                <span>Backup & Restore</span>
              </h3>
              <p className="text-[11px] text-slate-500 dark:text-slate-400">Securely export or restore your application records</p>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
              {/* Export Box */}
              <div className="p-4 rounded-xl bg-slate-50 dark:bg-slate-800/50 border border-slate-200/80 dark:border-slate-700 flex flex-col justify-between space-y-3">
                <div>
                  <span className="font-bold text-slate-900 dark:text-slate-100 block">Export Full Backup</span>
                  <p className="text-[11px] text-slate-500 dark:text-slate-400 mt-1 leading-relaxed">
                    Download a full backup of products, customer ledgers, supplier balances, and daily close records.
                  </p>
                </div>
                <button
                  type="button"
                  onClick={handleExportData}
                  className="w-full py-2.5 px-3 rounded-xl bg-[#135d46] dark:bg-emerald-600 hover:bg-[#0e4837] dark:hover:bg-emerald-700 text-white font-bold flex items-center justify-center gap-2 cursor-pointer transition-colors shadow-xs"
                >
                  <Download className="w-4 h-4" />
                  <span>Download Backup</span>
                </button>
              </div>

              {/* Import Box */}
              <div className="p-4 rounded-xl bg-slate-50 dark:bg-slate-800/50 border border-slate-200/80 dark:border-slate-700 space-y-3">
                <div>
                  <span className="font-bold text-slate-900 dark:text-slate-100 block">Restore from Backup</span>
                  <p className="text-[11px] text-slate-500 dark:text-slate-400 mt-1 leading-relaxed">
                    Paste your previously exported backup code here to restore your data.
                  </p>
                </div>

                <form onSubmit={handleImportSubmit} className="space-y-2">
                  <textarea
                    rows={2}
                    value={importJsonText}
                    onChange={(e) => setImportJsonText(e.target.value)}
                    placeholder="Paste backup code here..."
                    className="w-full bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl p-2.5 text-[11px] font-mono dark:text-slate-100 focus:outline-none focus:border-[#135d46] dark:focus:border-emerald-500"
                  />
                  {importStatus === 'success' && (
                    <p className="text-[11px] text-emerald-700 dark:text-emerald-400 font-bold">✓ Data restored successfully!</p>
                  )}
                  {importStatus === 'error' && (
                    <p className="text-[11px] text-rose-600 dark:text-rose-400 font-bold">✕ Invalid backup code provided.</p>
                  )}
                  <button
                    type="submit"
                    className="w-full py-2 px-3 rounded-xl bg-slate-800 dark:bg-slate-700 hover:bg-slate-900 dark:hover:bg-slate-600 text-white font-bold flex items-center justify-center gap-1.5 cursor-pointer transition-colors"
                  >
                    <Upload className="w-4 h-4" />
                    <span>Restore Data</span>
                  </button>
                </form>
              </div>
            </div>
          </div>

          {/* Reports Shortcut */}
          {onOpenDailyClose && (
            <div className="p-4 sm:p-5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 shadow-xs flex items-center justify-between gap-3">
              <div>
                <h4 className="text-sm font-bold text-slate-900 dark:text-slate-100 flex items-center gap-2">
                  <FileText className="w-4 h-4 text-emerald-700 dark:text-emerald-400" />
                  <span>Reports</span>
                </h4>
                <p className="text-[11px] text-slate-500 dark:text-slate-400">View today's cash summary and sales reports</p>
              </div>
              <button
                type="button"
                onClick={onOpenDailyClose}
                className="px-4 py-2 rounded-xl bg-emerald-700 dark:bg-emerald-600 hover:bg-emerald-800 dark:hover:bg-emerald-700 text-white font-bold text-xs cursor-pointer shadow-xs shrink-0 transition-colors"
              >
                Open Report
              </button>
            </div>
          )}

          {/* System & Storage Diagnostics */}
          <div className="p-4 sm:p-5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 shadow-xs space-y-3 text-xs">
            <div className="border-b border-slate-100 dark:border-slate-800 pb-2">
              <h3 className="text-sm font-bold text-slate-900 dark:text-slate-100 flex items-center gap-2">
                <ShieldCheck className="w-4 h-4 text-blue-600 dark:text-blue-400" />
                <span>Storage & Environment</span>
              </h3>
            </div>

            <div className="grid grid-cols-2 gap-3 text-[11px]">
              <div className="p-3 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700">
                <span className="text-slate-500 dark:text-slate-400 uppercase font-bold block text-[9px]">Storage Status</span>
                <span className="text-slate-900 dark:text-slate-100 font-mono font-bold">Device Storage</span>
              </div>
              <div className="p-3 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700">
                <span className="text-slate-500 dark:text-slate-400 uppercase font-bold block text-[9px]">Sync Status</span>
                <span className="text-emerald-700 dark:text-emerald-400 font-bold">Auto-saved locally</span>
              </div>
              <div className="p-3 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700">
                <span className="text-slate-500 dark:text-slate-400 uppercase font-bold block text-[9px]">Active Date</span>
                <span className="text-slate-900 dark:text-slate-100 font-mono font-bold">{vm.selectedDate}</span>
              </div>
              <div className="p-3 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700">
                <span className="text-slate-500 dark:text-slate-400 uppercase font-bold block text-[9px]">Audit Log Entries</span>
                <span className="text-slate-900 dark:text-slate-100 font-mono font-bold">{vm.auditLogs.length} records</span>
              </div>
            </div>
          </div>

          {/* Danger Zone: Wipe Data */}
          <div className="p-4 sm:p-5 rounded-2xl bg-rose-50/60 dark:bg-rose-950/20 border border-rose-200/80 dark:border-rose-900 shadow-xs space-y-3 text-xs">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
              <div>
                <h4 className="text-xs font-bold text-rose-900 dark:text-rose-400 uppercase tracking-wider flex items-center gap-1.5">
                  <AlertTriangle className="w-4 h-4 text-rose-600" />
                  <span>Reset & Data Wipe</span>
                </h4>
                <p className="text-[11px] text-rose-700 dark:text-rose-500 mt-0.5">Clear all transaction entries, customer balances, and reset app state</p>
              </div>

              {!isResetConfirming ? (
                <button
                  type="button"
                  onClick={() => setIsResetConfirming(true)}
                  className="px-4 py-2 rounded-xl bg-rose-600 hover:bg-rose-700 text-white font-bold cursor-pointer shadow-xs shrink-0 transition-colors"
                >
                  Reset All Data
                </button>
              ) : (
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => setIsResetConfirming(false)}
                    className="px-3 py-1.5 rounded-xl bg-slate-200 dark:bg-slate-700 text-slate-800 dark:text-slate-100 font-bold hover:bg-slate-300 dark:hover:bg-slate-600 cursor-pointer"
                  >
                    Cancel
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      vm.clearAllData();
                      setIsResetConfirming(false);
                    }}
                    className="px-4 py-1.5 rounded-xl bg-rose-700 hover:bg-rose-800 text-white font-bold cursor-pointer shadow-xs"
                  >
                    Confirm Wipe
                  </button>
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Close Action Button if rendered inside modal */}
      {onClose && (
        <div className="flex justify-end pt-3 border-t border-slate-200">
          <button
            type="button"
            onClick={onClose}
            className="px-5 py-2 rounded-xl bg-slate-200 hover:bg-slate-300 text-slate-800 font-bold text-xs cursor-pointer transition-colors"
          >
            Done
          </button>
        </div>
      )}
    </div>
  );
};
