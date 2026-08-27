import React, { useState } from 'react';
import { 
  Search, 
  Plus,
  Pencil,
  Trash2
} from 'lucide-react';
import { MilkShopViewModel } from '../../viewmodels/useMilkShopViewModel';
import { Supplier } from '../../types/milkShop';
import { Modal } from '../common/Modal';
import { ConfirmModal } from '../common/ConfirmModal';

interface SupplierLedgerViewProps {
  vm: MilkShopViewModel;
  onRequireProduct?: (action: string) => void;
  onRecordPayment?: (id: string, name: string, amount?: number) => void;
}

export const SupplierLedgerView: React.FC<SupplierLedgerViewProps> = ({ vm, onRequireProduct, onRecordPayment }) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [supplierToDelete, setSupplierToDelete] = useState<{ id: string; name: string } | null>(null);

  const handleOpenAddSupplier = () => {
    if (vm.products.length === 0) {
      onRequireProduct?.('add a new supplier');
      return;
    }
    setEditingSupplier(null);
    setNewName('');
    setNewPhone('');
    setNewRate(175);
    setNewVillage('');
    setIsAddSupplierOpen(true);
  };
  
  // Modals
  const [isAddSupplierOpen, setIsAddSupplierOpen] = useState(false);
  const [editingSupplier, setEditingSupplier] = useState<Supplier | null>(null);

  // Form: Add Supplier
  const [newName, setNewName] = useState('');
  const [newPhone, setNewPhone] = useState('');
  const [newRate, setNewRate] = useState<number | ''>(175);
  const [newVillage, setNewVillage] = useState('');

  const filteredSuppliers = vm.suppliers.filter(s => {
    return s.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
           s.phone.includes(searchTerm);
  });

  const openPayoutModal = (supplier: Supplier) => {
    onRecordPayment?.(supplier.id, supplier.name, supplier.balance > 0 ? supplier.balance : 5000);
  };

  const openEditSupplier = (sup: Supplier) => {
    setEditingSupplier(sup);
    setNewName(sup.name);
    setNewPhone(sup.phone);
    setNewRate(sup.defaultMilkRate ?? 175);
    setNewVillage(sup.addressLocation || '');
    setIsAddSupplierOpen(true);
  };

  const handleDeleteSupplier = (id: string, name: string) => {
    setSupplierToDelete({ id, name });
  };

  const handleAddSupplierSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newName.trim()) return;

    if (editingSupplier) {
      vm.updateSupplier(editingSupplier.id, {
        name: newName.trim(),
        phone: newPhone.trim(),
        addressLocation: newVillage.trim(),
        defaultMilkRate: Number(newRate || 0),
      });
    } else {
      vm.addSupplier({
        name: newName.trim(),
        phone: newPhone.trim(),
        addressLocation: newVillage.trim(),
        defaultMilkRate: Number(newRate || 0),
        isActive: true,
      });
    }

    setIsAddSupplierOpen(false);
    setEditingSupplier(null);
    setNewName('');
    setNewPhone('');
    setNewVillage('');
  };

  return (
    <div className="space-y-4 pb-16">
      {/* Top Metrics Cards */}
      <div className="grid grid-cols-2 gap-3">
        <div className="p-3.5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 shadow-xs">
          <span className="text-[10px] text-slate-500 dark:text-slate-400 font-bold uppercase block">Total Payable</span>
          <span className="text-base sm:text-lg font-extrabold font-mono text-rose-700 dark:text-rose-500">
            Rs. {vm.totalSupplierPayables.toLocaleString()}
          </span>
        </div>

        <div className="p-3.5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 shadow-xs">
          <span className="text-[10px] text-slate-500 dark:text-slate-400 font-bold uppercase block">Paid Out Today</span>
          <span className="text-base sm:text-lg font-extrabold font-mono text-emerald-700 dark:text-emerald-500">
            Rs. {vm.totalSupplierCashPaid.toLocaleString()}
          </span>
        </div>
      </div>

      {/* Control Bar */}
      <div className="flex flex-col sm:flex-row gap-2">
        <div className="relative flex-1">
          <Search className="w-4 h-4 text-slate-400 dark:text-slate-500 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Search supplier name or phone..."
            className="w-full bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl pl-9 pr-3 py-2.5 text-xs text-slate-900 dark:text-slate-100 placeholder:text-slate-400 dark:placeholder:text-slate-500 focus:outline-none focus:border-[#135d46] dark:focus:border-emerald-500"
          />
        </div>

        <button
          onClick={handleOpenAddSupplier}
          className="px-4 py-2.5 rounded-xl bg-[#135d46] dark:bg-emerald-600 hover:bg-[#0e4837] dark:hover:bg-emerald-700 text-white font-bold text-xs flex items-center justify-center gap-1.5 cursor-pointer shadow-xs"
        >
          <Plus className="w-4 h-4" />
          <span>New Supplier</span>
        </button>
      </div>

      {/* Mobile/Tablet Card List */}
      <div className="space-y-2.5">
        {filteredSuppliers.map((sup) => {
          const hasPayable = sup.balance > 0;
          return (
            <div
              key={sup.id}
              className={`p-3.5 rounded-2xl border flex items-center justify-between gap-3 shadow-xs ${
                hasPayable 
                  ? 'bg-rose-50/40 dark:bg-rose-900/10 border-rose-200 dark:border-rose-900/30' 
                  : 'bg-white dark:bg-slate-900 border-slate-200/80 dark:border-slate-800'
              }`}
            >
              <div>
                <h4 className="text-sm font-bold text-slate-900 dark:text-slate-100">{sup.name}</h4>
                <div className="text-[11px] text-slate-500 dark:text-slate-400 mt-1 space-x-2">
                  <span>Phone: {sup.phone || 'N/A'}</span>
                  <span>•</span>
                  <span>Rate: Rs.{sup.defaultMilkRate}/L</span>
                </div>

                <div className="mt-2 flex items-center gap-2 font-mono">
                  <span className="text-[10px] text-slate-500 dark:text-slate-400 uppercase font-bold">Payable:</span>
                  <span className={`text-sm font-extrabold ${hasPayable ? 'text-rose-700 dark:text-rose-500' : 'text-slate-600 dark:text-slate-400'}`}>
                    Rs. {sup.balance.toLocaleString()}
                  </span>
                </div>
              </div>

              <div className="flex items-center gap-2">
                <button
                  onClick={() => openPayoutModal(sup)}
                  className="px-3 py-2 rounded-xl bg-rose-600 hover:bg-rose-700 text-white font-bold text-xs cursor-pointer shadow-xs shrink-0"
                >
                  Record Payment
                </button>
                <button
                  onClick={() => openEditSupplier(sup)}
                  className="p-2 rounded-xl bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-600 dark:text-slate-400 transition-colors cursor-pointer"
                  title="Edit Supplier"
                >
                  <Pencil className="w-3.5 h-3.5" />
                </button>
                <button
                  onClick={() => handleDeleteSupplier(sup.id, sup.name)}
                  className="p-2 rounded-xl bg-rose-50 dark:bg-rose-900/20 hover:bg-rose-100 dark:hover:bg-rose-900/40 text-rose-600 dark:text-rose-400 transition-colors cursor-pointer"
                  title="Delete Supplier"
                >
                  <Trash2 className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>
          );
        })}
      </div>

      {/* Card List Removed internal modal triggers for payouts */}

      {/* Modal: Add Supplier */}
      <Modal
        isOpen={isAddSupplierOpen}
        onClose={() => {
          setIsAddSupplierOpen(false);
          setEditingSupplier(null);
        }}
        title={editingSupplier ? "Edit Milk Supplier" : "Register Milk Supplier"}
        subtitle={editingSupplier ? "Update supplier details and rate" : "Add a dairy supplier for milk procurement"}
      >
        <form onSubmit={handleAddSupplierSubmit} className="space-y-4 text-xs">
          <div>
            <label className="block text-slate-700 dark:text-slate-300 font-bold mb-1">Supplier Full Name</label>
            <input
              type="text"
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
              placeholder="e.g. Chaudhry Aslam"
              className="w-full bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl px-3 py-2.5 text-slate-900 dark:text-slate-100 focus:outline-none focus:border-[#135d46] dark:focus:border-emerald-500"
              required
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-slate-700 dark:text-slate-300 font-bold mb-1">Phone Number</label>
              <input
                type="text"
                value={newPhone}
                onChange={(e) => setNewPhone(e.target.value)}
                placeholder="0300-9876543"
                className="w-full bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl px-3 py-2.5 text-slate-900 dark:text-slate-100 focus:outline-none focus:border-[#135d46] dark:focus:border-emerald-500"
              />
            </div>
            <div>
              <label className="block text-slate-700 dark:text-slate-300 font-bold mb-1">Default Rate per Liter (Rs.)</label>
              <input
                type="number"
                min="1"
                value={newRate}
                onChange={(e) => setNewRate(e.target.value === '' ? '' : Number(e.target.value))}
                className="w-full bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl px-3 py-2.5 text-slate-900 dark:text-slate-100 font-mono font-bold focus:outline-none focus:border-[#135d46] dark:focus:border-emerald-500"
              />
            </div>
          </div>

          <div className="flex justify-end gap-2.5 pt-3">
            <button
              type="button"
              onClick={() => setIsAddSupplierOpen(false)}
              className="px-4 py-2 rounded-xl bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 font-bold cursor-pointer"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-5 py-2 rounded-xl bg-[#135d46] dark:bg-emerald-600 hover:bg-[#0e4837] dark:hover:bg-emerald-700 text-white font-bold cursor-pointer"
            >
              {editingSupplier ? 'Save Changes' : 'Add Supplier'}
            </button>
          </div>
        </form>
      </Modal>

      <ConfirmModal
        isOpen={Boolean(supplierToDelete)}
        onClose={() => setSupplierToDelete(null)}
        onConfirm={() => {
          if (supplierToDelete) {
            vm.deleteSupplier(supplierToDelete.id);
            setSupplierToDelete(null);
          }
        }}
        title="Delete Milk Supplier"
        message={`Are you sure you want to delete supplier "${supplierToDelete?.name}"? This action cannot be undone.`}
      />
    </div>
  );
};
