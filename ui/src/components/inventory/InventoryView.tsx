import React, { useState, useEffect } from 'react';
import { 
  CheckCircle2, 
  AlertCircle, 
  Plus, 
  Pencil, 
  Archive 
} from 'lucide-react';
import { motion } from 'motion/react';
import { MilkShopViewModel } from '../../viewmodels/useMilkShopViewModel';
import { Product } from '../../types/milkShop';
import { Modal } from '../common/Modal';
import { ConfirmModal } from '../common/ConfirmModal';

interface InventoryViewProps {
  vm: MilkShopViewModel;
  autoOpenAddProduct?: boolean;
  onResetAutoOpenAddProduct?: () => void;
  onRequireProduct?: (action: string) => void;
}

type TabType = 'active' | 'archived';

export const InventoryView: React.FC<InventoryViewProps> = ({ 
  vm, 
  autoOpenAddProduct, 
  onResetAutoOpenAddProduct,
  onRequireProduct
}) => {
  const [activeTab, setActiveTab] = useState<TabType>('active');
  const [productToArchive, setProductToArchive] = useState<{ id: string; name: string } | null>(null);
  
  // Count Stock Modal State
  const [isCountStockOpen, setIsCountStockOpen] = useState(false);
  const [auditCounts, setAuditCounts] = useState<{ [productId: string]: number | '' }>({});
  const [hasRecordedCount, setHasRecordedCount] = useState(false);

  // Add / Edit Product Modal State
  const [isProductModalOpen, setIsProductModalOpen] = useState(false);
  const [editingProduct, setEditingProduct] = useState<Product | null>(null);

  // Product Form Fields
  const [prodName, setProdName] = useState('');
  const [prodUnit, setProdUnit] = useState('L');
  const [prodSalePrice, setProdSalePrice] = useState<number | ''>(200);
  const [prodCostRate, setProdCostRate] = useState<number | ''>(160);
  const [prodStock, setProdStock] = useState<number | ''>(100);
  const [prodTrackStock, setProdTrackStock] = useState(true);
  const [prodIsArchived, setProdIsArchived] = useState(false);

  useEffect(() => {
    if (autoOpenAddProduct) {
      openAddProductModal();
      onResetAutoOpenAddProduct?.();
    }
  }, [autoOpenAddProduct]);

  const openCountStockModal = () => {
    if (vm.products.length === 0) {
      onRequireProduct?.('record a physical stock count');
      return;
    }
    const initial: { [productId: string]: number | '' } = {};
    vm.products.forEach(p => {
      initial[p.id] = p.currentStock || 0;
    });
    setAuditCounts(initial);
    setIsCountStockOpen(true);
  };

  const handleAuditSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    Object.entries(auditCounts).forEach(([prodId, measuredQty]) => {
      vm.updateProductStock(prodId, Number(measuredQty || 0));
    });
    setHasRecordedCount(true);
    setIsCountStockOpen(false);
  };

  const openAddProductModal = () => {
    setEditingProduct(null);
    setProdName('');
    setProdUnit('L');
    setProdSalePrice(200);
    setProdCostRate(160);
    setProdStock(100);
    setProdTrackStock(true);
    setProdIsArchived(false);
    setIsProductModalOpen(true);
  };

  const openEditProductModal = (prod: Product) => {
    setEditingProduct(prod);
    setProdName(prod.name);
    setProdUnit(prod.unit);
    setProdSalePrice(prod.salePrice);
    setProdCostRate(prod.costRate);
    setProdStock(prod.currentStock || 0);
    setProdTrackStock(prod.trackInInventory);
    setProdIsArchived(prod.isArchived || false);
    setIsProductModalOpen(true);
  };

  const handleProductSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!prodName.trim()) return;

    const salePrice = Number(prodSalePrice || 0);
    const costRate = Number(prodCostRate || 0);
    const stock = Number(prodStock || 0);

    if (editingProduct) {
      vm.updateProduct(editingProduct.id, {
        name: prodName.trim(),
        unit: prodUnit,
        salePrice,
        costRate,
        currentStock: stock,
        trackInInventory: prodTrackStock,
        isArchived: prodIsArchived,
      });
    } else {
      vm.addProduct({
        name: prodName.trim(),
        unit: prodUnit,
        salePrice,
        costRate,
        currentStock: stock,
        trackInInventory: prodTrackStock,
        isArchived: prodIsArchived,
      });
    }
    setIsProductModalOpen(false);
  };

  const activeProducts = vm.products.filter(p => !p.isArchived);
  const archivedProducts = vm.products.filter(p => p.isArchived);

  return (
    <div className="space-y-4 pb-16">
      {/* Header Row */}
      <div className="flex items-center justify-between gap-3">
        <div>
          <h2 className="text-xl sm:text-2xl font-extrabold text-slate-900 dark:text-slate-100 tracking-tight">
            Stock
          </h2>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
            Physical counts feed directly into P&L COGS
          </p>
        </div>

        <button
          onClick={openCountStockModal}
          className="px-3.5 py-2 rounded-xl bg-[#135d46] dark:bg-emerald-600 hover:bg-[#0e4837] dark:hover:bg-emerald-700 text-white font-bold text-xs flex items-center gap-1.5 cursor-pointer shadow-xs shrink-0 transition-colors"
        >
          <CheckCircle2 className="w-4 h-4" />
          <span>Count Stock</span>
        </button>
      </div>

      {/* Amber Warning Alert Banner */}
      {!hasRecordedCount && (
        <div className="p-3.5 rounded-2xl bg-amber-50/80 dark:bg-amber-900/10 border border-amber-200/80 dark:border-amber-900/30 flex items-start gap-3">
          <AlertCircle className="w-5 h-5 text-amber-700 dark:text-amber-500 shrink-0 mt-0.5" />
          <p className="text-xs text-amber-900 dark:text-amber-200 leading-relaxed">
            No physical inventory count recorded yet. Tap <strong className="font-bold cursor-pointer underline" onClick={openCountStockModal}>Count Stock</strong> to record this week's closing inventory.
          </p>
        </div>
      )}

      {/* Active / Archived Tab Pill Switcher */}
      <div className="bg-slate-100/80 dark:bg-slate-900 p-1 rounded-2xl flex items-center gap-1 border border-slate-200/60 dark:border-slate-800 relative">
        <button
          onClick={() => setActiveTab('active')}
          className={`relative flex-1 py-2 rounded-xl text-xs font-bold transition-all cursor-pointer text-center select-none active:scale-95 ${
            activeTab === 'active'
              ? 'text-[#135d46] dark:text-emerald-500'
              : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200'
          }`}
        >
          {activeTab === 'active' && (
            <motion.div
              layoutId="inventoryTabIndicator"
              className="absolute inset-0 bg-white dark:bg-slate-800 rounded-xl shadow-xs"
              transition={{ type: 'spring', stiffness: 380, damping: 30 }}
            />
          )}
          <span className="relative z-10">Active</span>
        </button>
        <button
          onClick={() => setActiveTab('archived')}
          className={`relative flex-1 py-2 rounded-xl text-xs font-bold transition-all cursor-pointer text-center select-none active:scale-95 ${
            activeTab === 'archived'
              ? 'text-[#135d46] dark:text-emerald-500'
              : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200'
          }`}
        >
          {activeTab === 'archived' && (
            <motion.div
              layoutId="inventoryTabIndicator"
              className="absolute inset-0 bg-white dark:bg-slate-800 rounded-xl shadow-xs"
              transition={{ type: 'spring', stiffness: 380, damping: 30 }}
            />
          )}
          <span className="relative z-10">Archived</span>
        </button>
      </div>

      {/* Subheader Row */}
      {activeTab === 'active' && (
        <>
          <div className="flex items-center justify-between pt-1">
            <span className="text-xs font-bold text-slate-700 dark:text-slate-300 uppercase tracking-wider">
              TRACKED PRODUCTS ({activeProducts.length})
            </span>
            <button
              onClick={openAddProductModal}
              className="text-xs font-bold text-[#135d46] dark:text-emerald-500 hover:underline flex items-center gap-1 cursor-pointer"
            >
              <Plus className="w-3.5 h-3.5" />
              <span>Add Product</span>
            </button>
          </div>

          {/* Product Cards List */}
          <div className="space-y-3">
            {activeProducts.map((prod) => (
              <div
                key={prod.id}
                className="p-4 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 shadow-xs space-y-3 hover:shadow-sm transition-all"
              >
                {/* Product Name, Unit Badge & Edit Button */}
                <div className="flex items-start justify-between">
                  <div>
                    <div className="flex items-center gap-2">
                      <h3 className="text-base font-bold text-slate-900 dark:text-slate-100">{prod.name}</h3>
                      <span className="bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400 px-2 py-0.5 rounded-md text-[11px] font-mono font-medium">
                        Unit: {prod.unit}
                      </span>
                    </div>
                    <p className="text-xs text-slate-500 dark:text-slate-400 font-medium mt-1">
                      Price: Rs. {prod.salePrice.toLocaleString()} · Cost: Rs. {prod.costRate.toLocaleString()}
                    </p>
                  </div>

                  <div className="flex items-center gap-1">
                    <button
                      onClick={() => openEditProductModal(prod)}
                      className="p-1.5 rounded-lg text-slate-400 dark:text-slate-500 hover:text-slate-700 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 cursor-pointer transition-colors"
                      title="Edit Product"
                    >
                      <Pencil className="w-4 h-4" />
                    </button>
                    <button
                      onClick={() => setProductToArchive({ id: prod.id, name: prod.name })}
                      className="p-1.5 rounded-lg text-slate-400 dark:text-slate-500 hover:text-amber-600 dark:hover:text-amber-400 hover:bg-amber-50 dark:hover:bg-amber-900/20 cursor-pointer transition-colors"
                      title="Archive Product"
                    >
                      <Archive className="w-4 h-4" />
                    </button>
                  </div>
                </div>

                {/* Divider Line */}
                <div className="border-t border-slate-100 dark:border-slate-800 pt-2.5">
                  {/* 4-Column Stock Stats Grid */}
                  <div className="grid grid-cols-4 gap-2 text-center">
                    <div>
                      <span className="text-[11px] text-slate-400 dark:text-slate-500 font-medium block mb-0.5">Purchased</span>
                      <span className="text-sm font-bold text-slate-800 dark:text-slate-200 font-mono">
                        {prod.purchased || 0}
                      </span>
                    </div>

                    <div>
                      <span className="text-[11px] text-slate-400 dark:text-slate-500 font-medium block mb-0.5">Sold</span>
                      <span className="text-sm font-bold text-[#135d46] dark:text-emerald-500 font-mono">
                        {prod.sold || 0}
                      </span>
                    </div>

                    <div>
                      <span className="text-[11px] text-slate-400 dark:text-slate-500 font-medium block mb-0.5">Spoiled</span>
                      <span className="text-sm font-bold text-rose-600 dark:text-rose-400 font-mono">
                        {prod.spoiled || 0}
                      </span>
                    </div>

                    <div>
                      <span className="text-[11px] text-slate-400 dark:text-slate-500 font-medium block mb-0.5">Expected</span>
                      <span className="text-sm font-bold text-[#135d46] dark:text-emerald-500 font-mono">
                        {prod.currentStock || 0}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            ))}

            {activeProducts.length === 0 && (
              <div className="p-8 text-center bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 text-slate-400 dark:text-slate-500 text-xs">
                No active products in inventory. Click <strong>+ Add Product</strong> to create one.
              </div>
            )}
          </div>
        </>
      )}

      {activeTab === 'archived' && (
        <div className="space-y-3">
          {archivedProducts.map((prod) => (
            <div
              key={prod.id}
              className="bg-white dark:bg-slate-900 rounded-2xl p-4 shadow-sm border border-slate-200/80 dark:border-slate-800 hover:shadow-md hover:border-slate-300 dark:hover:border-slate-700 transition-all cursor-pointer opacity-75"
              onClick={() => openEditProductModal(prod)}
            >
              <div className="flex justify-between items-start">
                <div>
                  <h3 className="text-sm font-extrabold text-slate-900 dark:text-slate-100 tracking-tight flex items-center gap-1.5">
                    {prod.name}
                  </h3>
                  <div className="text-[10px] text-slate-500 dark:text-slate-400 font-bold mt-0.5">
                    Sale: Rs.{prod.salePrice}/{prod.unit}
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <div className="text-right">
                    <span className="text-[10px] text-slate-500 dark:text-slate-400 font-bold uppercase tracking-wider block mb-0.5">
                      Stock
                    </span>
                    <span className="text-sm font-bold text-slate-600 dark:text-slate-400 font-mono">
                      {prod.currentStock || 0}
                    </span>
                  </div>
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      vm.updateProduct(prod.id, { isArchived: false });
                    }}
                    className="px-3 py-1.5 rounded-xl text-xs font-bold bg-[#135d46]/10 dark:bg-emerald-600/10 text-[#135d46] dark:text-emerald-500 hover:bg-[#135d46]/20 dark:hover:bg-emerald-600/20 cursor-pointer transition-colors"
                  >
                    Restore
                  </button>
                </div>
              </div>
            </div>
          ))}
          {archivedProducts.length === 0 && (
            <div className="p-8 text-center bg-white dark:bg-slate-900 rounded-2xl border border-slate-200/80 dark:border-slate-800 text-slate-500 dark:text-slate-400 text-xs">
              No archived products found.
            </div>
          )}
        </div>
      )}

      {/* Count Stock Modal */}
      <Modal
        isOpen={isCountStockOpen}
        onClose={() => setIsCountStockOpen(false)}
        title="Physical Stock Count"
        subtitle="Record measured physical inventory quantities"
      >
        <form onSubmit={handleAuditSubmit} className="space-y-4 text-xs">
          <div className="space-y-3">
            {vm.products.map((p) => (
              <div key={p.id} className="p-3.5 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 flex items-center justify-between gap-3">
                <div>
                  <span className="font-bold text-slate-900 dark:text-slate-100 block text-sm">{p.name}</span>
                  <span className="text-[11px] text-slate-500 dark:text-slate-400 font-mono">
                    System Expected: {p.currentStock} {p.unit}
                  </span>
                </div>
                <div className="flex items-center gap-2">
                  <input
                    type="number"
                    min="0"
                    value={auditCounts[p.id] !== undefined ? auditCounts[p.id] : (p.currentStock || 0)}
                    onChange={(e) => setAuditCounts({ ...auditCounts, [p.id]: e.target.value === '' ? '' : Number(e.target.value) })}
                    className="w-24 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-xl px-3 py-1.5 text-slate-900 dark:text-slate-100 font-mono font-bold text-sm text-right focus:outline-none focus:border-[#135d46] dark:focus:border-emerald-500"
                  />
                  <span className="text-slate-600 dark:text-slate-400 font-bold">{p.unit}</span>
                </div>
              </div>
            ))}
          </div>

          <div className="flex justify-end gap-2.5 pt-3 border-t border-slate-100 dark:border-slate-800">
            <button
              type="button"
              onClick={() => setIsCountStockOpen(false)}
              className="px-4 py-2 rounded-xl bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 font-bold cursor-pointer"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-5 py-2 rounded-xl bg-[#135d46] dark:bg-emerald-600 hover:bg-[#0e4837] dark:hover:bg-emerald-700 text-white font-bold cursor-pointer shadow-xs"
            >
              Save Stock Count
            </button>
          </div>
        </form>
      </Modal>

      {/* Add / Edit Product Modal */}
      <Modal
        isOpen={isProductModalOpen}
        onClose={() => setIsProductModalOpen(false)}
        title={editingProduct ? 'Edit Product' : 'Add New Product'}
        subtitle="Configure product details, pricing, and unit rate"
      >
        <form onSubmit={handleProductSubmit} className="space-y-4 text-xs">
          {!editingProduct && (!vm.products.some(p => p.isCoreMilk || p.name.toLowerCase().includes('milk')) || !vm.products.some(p => p.isCoreYogurt || p.name.toLowerCase().includes('yogurt'))) && (
            <div className="flex gap-2 mb-2 p-2 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg">
              <span className="text-[10px] font-bold text-slate-500 dark:text-slate-400 flex items-center">Quick Restore:</span>
              {!vm.products.some(p => p.isCoreMilk || p.name.toLowerCase().includes('milk')) && (
                <button
                  type="button"
                  onClick={() => {
                    setProdName('Fresh Milk');
                    setProdUnit('L');
                    setProdSalePrice(180);
                    setProdCostRate(150);
                    setProdStock(250);
                  }}
                  className="text-[10px] bg-emerald-100 dark:bg-emerald-900/40 hover:bg-emerald-200 dark:hover:bg-emerald-900/60 text-emerald-800 dark:text-emerald-400 font-bold px-2 py-1 rounded-md transition-colors cursor-pointer"
                >
                  Milk
                </button>
              )}
              {!vm.products.some(p => p.isCoreYogurt || p.name.toLowerCase().includes('yogurt')) && (
                <button
                  type="button"
                  onClick={() => {
                    setProdName('Yogurt');
                    setProdUnit('Kg');
                    setProdSalePrice(220);
                    setProdCostRate(185);
                    setProdStock(100);
                  }}
                  className="text-[10px] bg-cyan-100 dark:bg-cyan-900/40 hover:bg-cyan-200 dark:hover:bg-cyan-900/60 text-cyan-800 dark:text-cyan-400 font-bold px-2 py-1 rounded-md transition-colors cursor-pointer"
                >
                  Yogurt
                </button>
              )}
            </div>
          )}

          <div>
            <label className="block text-slate-700 dark:text-slate-300 font-bold mb-1">Product Name</label>
            <input
              type="text"
              value={prodName}
              onChange={(e) => setProdName(e.target.value)}
              placeholder="e.g. Milk, Yogurt, Desi Ghee"
              className="w-full bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl px-3.5 py-2.5 text-slate-900 dark:text-slate-100 focus:outline-none focus:border-[#135d46] dark:focus:border-emerald-500"
              required
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-slate-700 dark:text-slate-300 font-bold mb-1">Unit of Measurement</label>
              <select
                value={prodUnit}
                onChange={(e) => setProdUnit(e.target.value)}
                className="w-full bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl px-3 py-2.5 text-slate-900 dark:text-slate-100 font-medium focus:outline-none focus:border-[#135d46] dark:focus:border-emerald-500"
              >
                <option value="L">Liters (L)</option>
                <option value="Kg">Kilograms (Kg)</option>
                <option value="Item">Item / Piece</option>
                <option value="Bottle">Bottle (1L)</option>
                <option value="Pack">Pack / Bundle</option>
              </select>
            </div>

            <div>
              <label className="block text-slate-700 dark:text-slate-300 font-bold mb-1">Selling Price (Rs.)</label>
              <input
                type="number"
                min="0"
                value={prodSalePrice}
                onChange={(e) => setProdSalePrice(e.target.value === '' ? '' : Number(e.target.value))}
                className="w-full bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl px-3.5 py-2.5 text-slate-900 dark:text-slate-100 font-mono font-bold focus:outline-none focus:border-[#135d46] dark:focus:border-emerald-500"
                required
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-slate-700 dark:text-slate-300 font-bold mb-1">Cost Rate (Rs.)</label>
              <input
                type="number"
                min="0"
                value={prodCostRate}
                onChange={(e) => setProdCostRate(e.target.value === '' ? '' : Number(e.target.value))}
                className="w-full bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl px-3.5 py-2.5 text-slate-900 dark:text-slate-100 font-mono font-bold focus:outline-none focus:border-[#135d46] dark:focus:border-emerald-500"
                required
              />
            </div>

            <div>
              <label className="block text-slate-700 dark:text-slate-300 font-bold mb-1">Current Stock Quantity</label>
              <input
                type="number"
                min="0"
                value={prodStock}
                onChange={(e) => setProdStock(e.target.value === '' ? '' : Number(e.target.value))}
                className="w-full bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl px-3.5 py-2.5 text-slate-900 dark:text-slate-100 font-mono font-bold focus:outline-none focus:border-[#135d46] dark:focus:border-emerald-500"
              />
            </div>
          </div>

          <div className="flex items-center gap-4 py-2">
            <div className="flex items-center gap-2">
              <input
                type="checkbox"
                id="prodTrackStock"
                checked={prodTrackStock}
                onChange={(e) => setProdTrackStock(e.target.checked)}
                className="w-4 h-4 rounded text-[#135d46] dark:text-emerald-500 focus:ring-[#135d46] dark:focus:ring-emerald-500"
              />
              <label htmlFor="prodTrackStock" className="text-sm font-bold text-slate-700 dark:text-slate-300 cursor-pointer">
                Track Inventory
              </label>
            </div>
            {editingProduct && (
              <div className="flex items-center gap-2">
                <input
                  type="checkbox"
                  id="prodIsArchived"
                  checked={!prodIsArchived}
                  onChange={(e) => setProdIsArchived(!e.target.checked)}
                  className="w-4 h-4 rounded text-emerald-600 focus:ring-emerald-600 dark:focus:ring-emerald-500"
                />
                <label htmlFor="prodIsArchived" className="text-sm font-bold text-slate-700 dark:text-slate-300 cursor-pointer">
                  Active (Show in App)
                </label>
              </div>
            )}
          </div>

          <div className="flex items-center justify-end pt-2 border-t border-slate-100 dark:border-slate-800 gap-2">
            <button
              type="button"
              onClick={() => setIsProductModalOpen(false)}
              className="px-4 py-2 rounded-xl bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 font-bold cursor-pointer"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-5 py-2 rounded-xl bg-[#135d46] dark:bg-emerald-600 hover:bg-[#0e4837] dark:hover:bg-emerald-700 text-white font-bold cursor-pointer shadow-xs"
            >
              {editingProduct ? 'Save Changes' : 'Create Product'}
            </button>
          </div>
        </form>
      </Modal>

      <ConfirmModal
        isOpen={Boolean(productToArchive)}
        onClose={() => setProductToArchive(null)}
        onConfirm={() => {
          if (productToArchive) {
            vm.updateProduct(productToArchive.id, { isArchived: true });
            setProductToArchive(null);
          }
        }}
        title="Archive Product"
        message={`Are you sure you want to archive product "${productToArchive?.name}"? You can restore it anytime from the Archived tab.`}
        confirmText="Archive"
        isDestructive={false}
      />
    </div>
  );
};
