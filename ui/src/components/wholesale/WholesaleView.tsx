import React, { useState } from 'react';
import { 
  Store, 
  Plus,
  Pencil,
  Trash2
} from 'lucide-react';
import { MilkShopViewModel } from '../../viewmodels/useMilkShopViewModel';
import { Modal } from '../common/Modal';
import { Badge } from '../common/Badge';
import { ConfirmModal } from '../common/ConfirmModal';

interface WholesaleViewProps {
  vm: MilkShopViewModel;
  onRequireProduct?: (action: string) => void;
}

export const WholesaleView: React.FC<WholesaleViewProps> = ({ vm, onRequireProduct }) => {
  const [isAddWholesaleOpen, setIsAddWholesaleOpen] = useState(false);
  const [editingWholesaleId, setEditingWholesaleId] = useState<string | null>(null);
  const [wholesaleToDeleteId, setWholesaleToDeleteId] = useState<string | null>(null);

  const handleOpenAddWholesale = () => {
    if (vm.products.length === 0) {
      onRequireProduct?.('create a wholesale customer dispatch');
      return;
    }
    setEditingWholesaleId(null);
    setBuyerName('');
    setSelectedProductId(activeProducts[0]?.id || '');
    setQuantity(50);
    setUnitPrice(205);
    setPaymentType('credit');
    setNotes('');
    setIsAddWholesaleOpen(true);
  };

  const activeProducts = vm.products.filter(p => !p.isArchived);

  // Form states
  const [buyerName, setBuyerName] = useState('');
  const [selectedProductId, setSelectedProductId] = useState(activeProducts[0]?.id || '');
  const [quantity, setQuantity] = useState<number | ''>(50);
  const [unitPrice, setUnitPrice] = useState<number | ''>(205);
  const [paymentType, setPaymentType] = useState<'cash' | 'credit'>('credit');
  const [notes, setNotes] = useState('');

  const openEditWholesale = (sale: any) => {
    setEditingWholesaleId(sale.id);
    setBuyerName(sale.buyerName);
    setSelectedProductId(sale.productId);
    setQuantity(sale.quantity);
    setUnitPrice(sale.unitPrice);
    setPaymentType(sale.paymentType);
    setNotes(sale.notes || '');
    setIsAddWholesaleOpen(true);
  };

  const handleDeleteWholesale = (id: string) => {
    setWholesaleToDeleteId(id);
  };

  const handleWholesaleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const prod = vm.products.find(p => p.id === selectedProductId);
    const qty = Number(quantity || 0);
    const price = Number(unitPrice || 0);
    if (!prod || !buyerName.trim() || qty <= 0 || price <= 0) return;

    if (editingWholesaleId) {
      vm.updateWholesaleSale(editingWholesaleId, {
        buyerName: buyerName.trim(),
        productId: prod.id,
        productName: prod.name,
        quantity: qty,
        unitPrice: price,
        paymentType,
        notes: notes.trim() || undefined,
      });
    } else {
      vm.addWholesaleSale({
        buyerName: buyerName.trim(),
        productId: prod.id,
        productName: prod.name,
        quantity: qty,
        unitPrice: price,
        paymentType,
        notes: notes.trim() || undefined,
      });
    }

    setIsAddWholesaleOpen(false);
    setEditingWholesaleId(null);
    setBuyerName('');
    setNotes('');
    setQuantity(50);
  };

  return (
    <div className="space-y-4 pb-16">
      {/* Metrics Header */}
      <div className="grid grid-cols-2 gap-3">
        <div className="p-3.5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 shadow-xs">
          <span className="text-[10px] text-slate-500 dark:text-slate-400 font-bold uppercase block">Wholesale Revenue</span>
          <span className="text-base sm:text-lg font-extrabold font-mono text-amber-700 dark:text-amber-500">
            Rs. {vm.totalWholesaleRevenue.toLocaleString()}
          </span>
        </div>

        <div className="p-3.5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 shadow-xs">
          <span className="text-[10px] text-slate-500 dark:text-slate-400 font-bold uppercase block">Wholesale Cash</span>
          <span className="text-base sm:text-lg font-extrabold font-mono text-emerald-700 dark:text-emerald-500">
            Rs. {vm.wholesaleCashRevenue.toLocaleString()}
          </span>
        </div>
      </div>

      {/* Action Header */}
      <div className="flex justify-between items-center">
        <h3 className="text-xs font-bold text-slate-900 dark:text-slate-100 uppercase tracking-wider flex items-center gap-1.5">
          <Store className="w-4 h-4 text-[#135d46] dark:text-emerald-500" />
          <span>Wholesale Bulk Sales</span>
        </h3>
        <button
          onClick={handleOpenAddWholesale}
          className="px-3 py-2 rounded-xl bg-[#135d46] dark:bg-emerald-600 hover:bg-[#0e4837] dark:hover:bg-emerald-700 text-white font-bold text-xs flex items-center gap-1 cursor-pointer shadow-xs"
        >
          <Plus className="w-4 h-4" />
          <span>New Dispatch</span>
        </button>
      </div>

      {/* Card List */}
      <div className="space-y-2.5">
        {vm.wholesaleSales.map((sale) => (
          <div key={sale.id} className="p-3.5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 shadow-xs flex items-center justify-between">
            <div>
              <div className="flex items-center gap-2">
                <h4 className="text-sm font-bold text-slate-900 dark:text-slate-100">{sale.buyerName}</h4>
                <Badge variant={sale.paymentType === 'cash' ? 'emerald' : 'amber'}>
                  {sale.paymentType === 'cash' ? 'Paid' : 'Credit'}
                </Badge>
              </div>
              <p className="text-[11px] text-slate-500 dark:text-slate-400 font-mono mt-1">
                {sale.productName} ({sale.quantity} units @ Rs. {sale.unitPrice})
              </p>
            </div>
            <div className="flex items-center gap-3">
              <span className="text-sm font-extrabold font-mono text-slate-900 dark:text-slate-100">
                Rs. {sale.totalAmount.toLocaleString()}
              </span>
              <div className="flex items-center gap-1">
                <button
                  onClick={() => openEditWholesale(sale)}
                  className="p-1.5 rounded-lg text-slate-400 dark:text-slate-500 hover:text-emerald-600 dark:hover:text-emerald-400 hover:bg-emerald-50 dark:hover:bg-emerald-900/20 cursor-pointer transition-colors"
                  title="Edit Wholesale Sale"
                >
                  <Pencil className="w-4 h-4" />
                </button>
                <button
                  onClick={() => handleDeleteWholesale(sale.id)}
                  className="p-1.5 rounded-lg text-slate-400 dark:text-slate-500 hover:text-rose-600 dark:hover:text-rose-400 hover:bg-rose-50 dark:hover:bg-rose-900/20 cursor-pointer transition-colors"
                  title="Delete Wholesale Sale"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Modal */}
      <Modal
        isOpen={isAddWholesaleOpen}
        onClose={() => {
          setIsAddWholesaleOpen(false);
          setEditingWholesaleId(null);
        }}
        title={editingWholesaleId ? "Edit Wholesale Dispatch" : "Record Wholesale Bulk Sale"}
        subtitle={editingWholesaleId ? "Update bulk supply details" : "Log bulk supply for sweet shops, tea stalls & bakeries"}
      >
        <form onSubmit={handleWholesaleSubmit} className="space-y-4 text-xs">
          <div>
            <label className="block text-slate-700 dark:text-slate-300 font-bold mb-1">Commercial Buyer Name</label>
            <input
              type="text"
              value={buyerName}
              onChange={(e) => setBuyerName(e.target.value)}
              placeholder="e.g. Al-Madina Sweets"
              className="w-full bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl px-3 py-2.5 text-slate-900 dark:text-slate-100 focus:outline-none focus:border-[#135d46] dark:focus:border-emerald-500"
              required
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-slate-700 dark:text-slate-300 font-bold mb-1">Product</label>
              <select
                value={selectedProductId}
                onChange={(e) => setSelectedProductId(e.target.value)}
                className="w-full bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl px-3 py-2 text-slate-900 dark:text-slate-100 focus:outline-none focus:border-[#135d46] dark:focus:border-emerald-500"
              >
                {activeProducts.map(p => (
                  <option key={p.id} value={p.id}>{p.name}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-slate-700 dark:text-slate-300 font-bold mb-1">Quantity</label>
              <input
                type="number"
                min="1"
                value={quantity}
                onChange={(e) => setQuantity(e.target.value === '' ? '' : Number(e.target.value))}
                className="w-full bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl px-3 py-2 text-slate-900 dark:text-slate-100 font-mono font-bold focus:outline-none focus:border-[#135d46] dark:focus:border-emerald-500"
                required
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-slate-700 dark:text-slate-300 font-bold mb-1">Wholesale Rate (Rs.)</label>
              <input
                type="number"
                min="1"
                value={unitPrice}
                onChange={(e) => setUnitPrice(e.target.value === '' ? '' : Number(e.target.value))}
                className="w-full bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl px-3 py-2 text-slate-900 dark:text-slate-100 font-mono font-bold focus:outline-none focus:border-[#135d46] dark:focus:border-emerald-500"
                required
              />
            </div>
            <div>
              <label className="block text-slate-700 dark:text-slate-300 font-bold mb-1">Payment Type</label>
              <select
                value={paymentType}
                onChange={(e) => setPaymentType(e.target.value as 'cash' | 'credit')}
                className="w-full bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl px-3 py-2 text-slate-900 dark:text-slate-100 focus:outline-none focus:border-[#135d46] dark:focus:border-emerald-500"
              >
                <option value="credit">Credit (Unpaid)</option>
                <option value="cash">Paid Now</option>
              </select>
            </div>
          </div>

          <div className="flex justify-end gap-2.5 pt-3">
            <button
              type="button"
              onClick={() => setIsAddWholesaleOpen(false)}
              className="px-4 py-2 rounded-xl bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 font-bold cursor-pointer"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-5 py-2 rounded-xl bg-[#135d46] dark:bg-emerald-600 hover:bg-[#0e4837] dark:hover:bg-emerald-700 text-white font-bold cursor-pointer"
            >
              {editingWholesaleId ? 'Save Changes' : 'Save Wholesale Order'}
            </button>
          </div>
        </form>
      </Modal>

      <ConfirmModal
        isOpen={Boolean(wholesaleToDeleteId)}
        onClose={() => setWholesaleToDeleteId(null)}
        onConfirm={() => {
          if (wholesaleToDeleteId) {
            vm.deleteWholesaleSale(wholesaleToDeleteId);
            setWholesaleToDeleteId(null);
          }
        }}
        title="Delete Wholesale Order"
        message="Are you sure you want to delete this wholesale record? This will adjust your inventory and balance records."
      />
    </div>
  );
};
