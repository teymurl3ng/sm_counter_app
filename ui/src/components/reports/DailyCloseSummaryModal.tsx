import React, { useState } from 'react';
import { 
  Calculator, 
  Lock, 
  TrendingUp, 
  Coins, 
  RotateCcw
} from 'lucide-react';
import { MilkShopViewModel } from '../../viewmodels/useMilkShopViewModel';
import { Modal } from '../common/Modal';
import { Badge } from '../common/Badge';

interface DailyCloseSummaryModalProps {
  isOpen: boolean;
  onClose: () => void;
  vm: MilkShopViewModel;
}

export const DailyCloseSummaryModal: React.FC<DailyCloseSummaryModalProps> = ({
  isOpen,
  onClose,
  vm,
}) => {
  const [closingNotes] = useState('');
  const [countedCash, setCountedCash] = useState<number | ''>(
    vm.countedDrawerCash !== undefined ? vm.countedDrawerCash : ''
  );

  const numericCountedCash = countedCash === '' ? vm.expectedDrawerCash : Number(countedCash);
  const variance = numericCountedCash - vm.expectedDrawerCash;

  const handleSaveCount = (e: React.FormEvent) => {
    e.preventDefault();
    vm.setCountedDrawerCash(numericCountedCash);
    vm.finalizeDailyClose(closingNotes);
    onClose();
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={`Daily Financial Close (${vm.selectedDate})`}
      subtitle="End-of-day register cash check & closing statement"
      maxWidth="xl"
    >
      <div className="space-y-4 text-xs">
        {/* Day Status Banner */}
        <div className={`p-3.5 rounded-2xl border flex items-center justify-between gap-3 ${
          vm.isDayLocked
            ? 'bg-slate-100 dark:bg-slate-800 border-slate-200 dark:border-slate-700 text-slate-800 dark:text-slate-200'
            : 'bg-emerald-50 dark:bg-emerald-950/30 border-emerald-200/80 dark:border-emerald-800/80 text-emerald-900 dark:text-emerald-400'
        }`}>
          <div className="flex items-center gap-2.5">
            <div className={`p-2 rounded-xl border ${
              vm.isDayLocked
                ? 'bg-slate-200 dark:bg-slate-700 text-slate-700 dark:text-slate-300 border-slate-300 dark:border-slate-600'
                : 'bg-emerald-100 dark:bg-emerald-900/40 text-emerald-800 dark:text-emerald-400 border-emerald-300 dark:border-emerald-800'
            }`}>
              {vm.isDayLocked ? <Lock className="w-4 h-4" /> : <Calculator className="w-4 h-4" />}
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="text-xs font-bold text-slate-900 dark:text-slate-100">
                  {vm.isDayLocked ? 'Day Register Locked' : 'Open Register Session'}
                </span>
                <Badge variant={vm.isDayLocked ? 'neutral' : 'emerald'}>
                  {vm.isDayLocked ? 'Closed' : 'Pending Close'}
                </Badge>
              </div>
            </div>
          </div>

          {vm.isDayLocked && (
            <button
              onClick={() => vm.resetForNewDay()}
              className="inline-flex items-center gap-1 px-3 py-1.5 rounded-xl bg-slate-200 dark:bg-slate-700 hover:bg-slate-300 dark:hover:bg-slate-600 text-slate-800 dark:text-slate-200 text-xs font-bold cursor-pointer transition-colors"
            >
              <RotateCcw className="w-3.5 h-3.5" />
              <span>New Day</span>
            </button>
          )}
        </div>

        {/* Financial Accounting Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          {/* Revenue Breakdown */}
          <div className="p-3.5 rounded-2xl bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-800 space-y-2 shadow-xs">
            <h4 className="text-xs font-bold text-slate-900 dark:text-slate-100 uppercase tracking-wider flex items-center gap-1.5">
              <TrendingUp className="w-3.5 h-3.5 text-emerald-600 dark:text-emerald-400" />
              <span>Total Sales & Inflows</span>
            </h4>
            <div className="space-y-1 font-mono text-slate-700 dark:text-slate-400">
              <div className="flex justify-between">
                <span>Counter Cash POS:</span>
                <span className="text-emerald-700 dark:text-emerald-400 font-bold">Rs. {vm.shopRetailCashRevenue.toLocaleString()}</span>
              </div>
              <div className="flex justify-between">
                <span>Wholesale Direct Cash:</span>
                <span className="text-emerald-700 dark:text-emerald-400 font-bold">Rs. {vm.wholesaleCashRevenue.toLocaleString()}</span>
              </div>
              <div className="flex justify-between">
                <span>Customer Collections:</span>
                <span className="text-emerald-700 dark:text-emerald-400 font-bold">Rs. {vm.totalCustomerCashCollected.toLocaleString()}</span>
              </div>
              <div className="flex justify-between pt-1 border-t border-slate-200 dark:border-slate-800 font-extrabold text-slate-900 dark:text-slate-100">
                <span>Total Cash Inflow:</span>
                <span className="text-emerald-700 dark:text-emerald-400">Rs. {vm.totalCashInflows.toLocaleString()}</span>
              </div>
            </div>
          </div>

          {/* Outflows */}
          <div className="p-3.5 rounded-2xl bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-800 space-y-2 shadow-xs">
            <h4 className="text-xs font-bold text-slate-900 dark:text-slate-100 uppercase tracking-wider flex items-center gap-1.5">
              <Coins className="w-3.5 h-3.5 text-rose-600 dark:text-rose-400" />
              <span>Drawer Cash Outflows</span>
            </h4>
            <div className="space-y-1 font-mono text-slate-700 dark:text-slate-400">
              <div className="flex justify-between">
                <span>Supplier Cash Payments:</span>
                <span className="text-rose-600 dark:text-rose-400 font-bold">Rs. {vm.totalSupplierCashPaid.toLocaleString()}</span>
              </div>
              <div className="flex justify-between">
                <span>Operating Expenses:</span>
                <span className="text-rose-600 dark:text-rose-400 font-bold">Rs. {vm.totalCashExpenses.toLocaleString()}</span>
              </div>
              <div className="flex justify-between pt-1 border-t border-slate-200 dark:border-slate-800 font-extrabold text-slate-900 dark:text-slate-100">
                <span>Expected Drawer Cash:</span>
                <span className="text-emerald-800 dark:text-emerald-400">Rs. {vm.expectedDrawerCash.toLocaleString()}</span>
              </div>
            </div>
          </div>
        </div>

        {/* Counted Drawer Cash Entry Form */}
        <form onSubmit={handleSaveCount} className="p-4 rounded-2xl bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-800 space-y-3 shadow-xs">
          <h4 className="text-xs font-bold text-slate-900 dark:text-slate-100 uppercase tracking-wider">
            Check Cash Drawer
          </h4>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-slate-700 dark:text-slate-300 font-bold mb-1">Physical Counted Cash (Rs.)</label>
              <input
                type="number"
                min="0"
                value={countedCash}
                onChange={(e) => setCountedCash(e.target.value === '' ? '' : Number(e.target.value))}
                placeholder={vm.expectedDrawerCash.toString()}
                className="w-full bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl px-3 py-2 text-sm font-extrabold font-mono text-emerald-800 dark:text-emerald-400 focus:outline-none focus:border-emerald-600 dark:focus:border-emerald-500"
              />
            </div>

            <div>
              <label className="block text-slate-700 dark:text-slate-300 font-bold mb-1">Calculated Cash Variance</label>
              <div className={`p-2 rounded-xl border text-sm font-extrabold font-mono ${
                variance === 0
                  ? 'bg-white dark:bg-slate-800 border-slate-200 dark:border-slate-700 text-emerald-700 dark:text-emerald-400'
                  : variance > 0
                  ? 'bg-emerald-50 dark:bg-emerald-950/30 border-emerald-200 dark:border-emerald-800/80 text-emerald-800 dark:text-emerald-400'
                  : 'bg-rose-50 dark:bg-rose-950/30 border-rose-200 dark:border-rose-800/80 text-rose-700 dark:text-rose-400'
              }`}>
                {variance > 0 ? `+ Rs. ${variance}` : variance < 0 ? `- Rs. ${Math.abs(variance)}` : 'Rs. 0 (Balanced)'}
              </div>
            </div>
          </div>

          <div className="flex justify-end gap-2.5 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 rounded-xl bg-slate-200 dark:bg-slate-800 hover:bg-slate-300 dark:hover:bg-slate-700 text-slate-800 dark:text-slate-200 font-bold cursor-pointer transition-colors"
            >
              Close
            </button>
            <button
              type="submit"
              className="px-5 py-2 rounded-xl bg-[#135d46] dark:bg-emerald-600 hover:bg-[#0e4837] dark:hover:bg-emerald-700 text-white font-bold cursor-pointer transition-all shadow-sm active:scale-98"
            >
              Confirm & Lock Day
            </button>
          </div>
        </form>
      </div>
    </Modal>
  );
};
