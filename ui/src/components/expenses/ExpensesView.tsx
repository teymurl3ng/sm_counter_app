import React, { useState } from 'react';
import { 
  Receipt, 
  Plus, 
  Trash2,
  Edit
} from 'lucide-react';
import { MilkShopViewModel } from '../../viewmodels/useMilkShopViewModel';
import { ExpenseCategory, PaymentMethod } from '../../types/milkShop';
import { Modal } from '../common/Modal';
import { Badge } from '../common/Badge';
import { ConfirmModal } from '../common/ConfirmModal';

interface ExpensesViewProps {
  vm: MilkShopViewModel;
}

export const ExpensesView: React.FC<ExpensesViewProps> = ({ vm }) => {
  const [isAddExpenseOpen, setIsAddExpenseOpen] = useState(false);
  const [expenseToDeleteId, setExpenseToDeleteId] = useState<string | null>(null);

  // Form states
  const [editingExpenseId, setEditingExpenseId] = useState<string | null>(null);
  const [category, setCategory] = useState<ExpenseCategory>(vm.settings.expenseCategories[0] || 'Salaries & Staff Advances');
  const [description, setDescription] = useState('');
  const [amount, setAmount] = useState<number | ''>(1000);
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('cash');

  const handleExpenseSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const expenseAmt = Number(amount || 0);
    if (!description.trim() || expenseAmt <= 0) return;

    if (editingExpenseId) {
      vm.updateExpense(editingExpenseId, {
        category,
        description: description.trim(),
        amount: expenseAmt,
        paymentMethod,
      });
    } else {
      vm.addExpense({
        date: vm.selectedDate,
        category,
        description: description.trim(),
        amount: expenseAmt,
        paymentMethod,
      });
    }

    setIsAddExpenseOpen(false);
    resetForm();
  };

  const resetForm = () => {
    setEditingExpenseId(null);
    setCategory(vm.settings.expenseCategories[0] || 'Salaries & Staff Advances');
    setDescription('');
    setAmount(1000);
    setPaymentMethod('cash');
  };

  const handleEditExpense = (exp: any) => {
    setEditingExpenseId(exp.id);
    setCategory(exp.category);
    setDescription(exp.description);
    setAmount(exp.amount);
    setPaymentMethod(exp.paymentMethod);
    setIsAddExpenseOpen(true);
  };

  const handleDeleteExpense = (id: string) => {
    setExpenseToDeleteId(id);
  };

  return (
    <div className="space-y-4 pb-16">
      {/* Metrics Header */}
      <div className="grid grid-cols-2 gap-3">
        <div className="p-3.5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 shadow-xs">
          <span className="text-[10px] text-slate-500 dark:text-slate-400 font-bold uppercase block">Total Expenses</span>
          <span className="text-base sm:text-lg font-extrabold font-mono text-rose-700 dark:text-rose-500">
            Rs. {vm.totalExpenses.toLocaleString()}
          </span>
        </div>

        <div className="p-3.5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 shadow-xs">
          <span className="text-[10px] text-slate-500 dark:text-slate-400 font-bold uppercase block">Cash Out of Drawer</span>
          <span className="text-base sm:text-lg font-extrabold font-mono text-amber-700 dark:text-amber-500">
            Rs. {vm.totalCashExpenses.toLocaleString()}
          </span>
        </div>
      </div>

      {/* Action Header */}
      <div className="flex justify-between items-center">
        <h3 className="text-xs font-bold text-slate-900 dark:text-slate-100 uppercase tracking-wider flex items-center gap-1.5">
          <Receipt className="w-4 h-4 text-rose-600 dark:text-rose-500" />
          <span>Shop Expenses & Wages</span>
        </h3>
        <button
          onClick={() => setIsAddExpenseOpen(true)}
          className="px-3 py-2 rounded-xl bg-rose-600 dark:bg-rose-600 hover:bg-rose-700 dark:hover:bg-rose-700 text-white font-bold text-xs flex items-center gap-1 cursor-pointer shadow-xs"
        >
          <Plus className="w-4 h-4" />
          <span>Record Expense</span>
        </button>
      </div>

      {/* Expense Card List */}
      <div className="space-y-2.5">
        {vm.expenses.map((exp) => (
          <div key={exp.id} className="p-3.5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 shadow-xs flex items-center justify-between">
            <div>
              <div className="flex items-center gap-2">
                <Badge variant="rose">{exp.category}</Badge>
                <span className="text-xs text-slate-500 dark:text-slate-400">({exp.paymentMethod})</span>
              </div>
              <p className="text-xs font-bold text-slate-900 dark:text-slate-100 mt-1">{exp.description}</p>
            </div>
            <div className="flex items-center gap-3">
              <span className="text-sm font-extrabold font-mono text-rose-700 dark:text-rose-500">
                Rs. {exp.amount.toLocaleString()}
              </span>
              <div className="flex items-center gap-1">
                <button
                  onClick={() => handleEditExpense(exp)}
                  className="text-slate-400 dark:text-slate-500 hover:text-emerald-600 dark:hover:text-emerald-400 p-1 cursor-pointer"
                >
                  <Edit className="w-4 h-4" />
                </button>
                <button
                  onClick={() => handleDeleteExpense(exp.id)}
                  className="text-slate-400 dark:text-slate-500 hover:text-rose-600 dark:hover:text-rose-400 p-1 cursor-pointer"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            </div>
          </div>
        ))}

        {vm.expenses.length === 0 && (
          <div className="p-6 text-center border border-dashed border-slate-200 dark:border-slate-800 rounded-xl text-slate-500 dark:text-slate-400 text-xs bg-white dark:bg-slate-900">
            No expense records logged today.
          </div>
        )}
      </div>

      {/* Modal */}
      <Modal
        isOpen={isAddExpenseOpen}
        onClose={() => {
          setIsAddExpenseOpen(false);
          resetForm();
        }}
        title={editingExpenseId ? "Edit Shop Expense" : "Record Shop Expense / Staff Advance"}
        subtitle={editingExpenseId ? "Update expense details" : "Log electricity, fuel, bags, or staff cash advance"}
      >
        <form onSubmit={handleExpenseSubmit} className="space-y-4 text-xs">
          <div>
            <label className="block text-slate-700 dark:text-slate-300 font-bold mb-1">Expense Category</label>
            <select
              value={category}
              onChange={(e) => setCategory(e.target.value as ExpenseCategory)}
              className="w-full bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl px-3 py-2.5 text-slate-900 dark:text-slate-100 focus:outline-none focus:border-rose-600 dark:focus:border-rose-500"
            >
              {vm.settings.expenseCategories.map(cat => (
                <option key={cat} value={cat}>{cat}</option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-slate-700 dark:text-slate-300 font-bold mb-1">Description / Notes</label>
            <input
              type="text"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="e.g. Generator Diesel 10L"
              className="w-full bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl px-3 py-2.5 text-slate-900 dark:text-slate-100 focus:outline-none focus:border-rose-600 dark:focus:border-rose-500"
              required
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-slate-700 dark:text-slate-300 font-bold mb-1">Amount (Rs.)</label>
              <input
                type="number"
                min="1"
                value={amount}
                onChange={(e) => setAmount(e.target.value === '' ? '' : Number(e.target.value))}
                className="w-full bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl px-3 py-2.5 text-slate-900 dark:text-slate-100 font-mono font-bold focus:outline-none focus:border-rose-600 dark:focus:border-rose-500"
                required
              />
            </div>

            <div>
              <label className="block text-slate-700 dark:text-slate-300 font-bold mb-1">Payment Method</label>
              <select
                value={paymentMethod}
                onChange={(e) => setPaymentMethod(e.target.value as PaymentMethod)}
                className="w-full bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl px-3 py-2.5 text-slate-900 dark:text-slate-100 focus:outline-none focus:border-rose-600 dark:focus:border-rose-500"
              >
                <option value="cash">Drawer Cash Out</option>
                <option value="jazzcash">JazzCash / Online</option>
                <option value="bank">Bank Transfer</option>
              </select>
            </div>
          </div>

          <div className="flex justify-end gap-2.5 pt-3">
            <button
              type="button"
              onClick={() => setIsAddExpenseOpen(false)}
              className="px-4 py-2 rounded-xl bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 font-bold cursor-pointer"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-5 py-2 rounded-xl bg-rose-600 dark:bg-rose-600 hover:bg-rose-700 dark:hover:bg-rose-700 text-white font-bold cursor-pointer"
            >
              {editingExpenseId ? 'Save Changes' : 'Save Expense'}
            </button>
          </div>
        </form>
      </Modal>

      <ConfirmModal
        isOpen={Boolean(expenseToDeleteId)}
        onClose={() => setExpenseToDeleteId(null)}
        onConfirm={() => {
          if (expenseToDeleteId) {
            vm.deleteExpense(expenseToDeleteId);
            setExpenseToDeleteId(null);
          }
        }}
        title="Delete Expense Record"
        message="Are you sure you want to delete this expense record? This action will adjust your daily drawer balance accordingly."
      />
    </div>
  );
};
