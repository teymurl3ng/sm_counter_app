import React from 'react';
import { PackageX, ArrowRight } from 'lucide-react';
import { Modal } from './Modal';

interface NoProductErrorModalProps {
  isOpen: boolean;
  onClose: () => void;
  onGoToAddProduct: () => void;
  actionName?: string;
}

export const NoProductErrorModal: React.FC<NoProductErrorModalProps> = ({
  isOpen,
  onClose,
  onGoToAddProduct,
  actionName = 'perform this action',
}) => {
  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Product Catalog Required"
      subtitle="Stock setup required before proceeding"
    >
      <div className="space-y-4 text-xs">
        <div className="p-4 rounded-2xl bg-amber-50 dark:bg-amber-950/30 border border-amber-200 dark:border-amber-800 text-amber-900 dark:text-amber-400 flex items-start gap-3 shadow-xs">
          <div className="p-2 rounded-xl bg-amber-100 dark:bg-amber-900/40 text-amber-800 dark:text-amber-300 shrink-0">
            <PackageX className="w-5 h-5" />
          </div>
          <div className="space-y-1">
            <h4 className="font-extrabold text-sm text-amber-950 dark:text-amber-200">No Products in Inventory</h4>
            <p className="text-amber-800 dark:text-amber-400 text-xs leading-relaxed">
              You must add at least one product (e.g., Fresh Milk, Yogurt, Paneer) to your inventory before you can {actionName}.
            </p>
          </div>
        </div>

        <div className="flex justify-end gap-2.5 pt-2">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2.5 rounded-xl bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 font-bold cursor-pointer transition-colors"
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={() => {
              onClose();
              onGoToAddProduct();
            }}
            className="px-5 py-2.5 rounded-xl bg-[#135d46] dark:bg-emerald-600 hover:bg-[#0e4837] dark:hover:bg-emerald-700 text-white font-bold cursor-pointer flex items-center gap-2 shadow-xs transition-all active:scale-95"
          >
            <span>Add Product Now</span>
            <ArrowRight className="w-4 h-4" />
          </button>
        </div>
      </div>
    </Modal>
  );
};
