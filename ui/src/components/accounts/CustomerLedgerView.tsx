import React, { useState, useMemo } from 'react';
import { 
  Search, 
  Plus,
  Pencil,
  Trash2,
  Bell,
  Phone,
  MessageSquare,
  Send
} from 'lucide-react';
import { MilkShopViewModel } from '../../viewmodels/useMilkShopViewModel';
import { Customer } from '../../types/milkShop';
import { Modal } from '../common/Modal';
import { Badge } from '../common/Badge';
import { ConfirmModal } from '../common/ConfirmModal';

interface CustomerLedgerViewProps {
  vm: MilkShopViewModel;
  onRequireProduct?: (action: string) => void;
  onRecordPayment?: (id: string, name: string, amount?: number) => void;
}

export const CustomerLedgerView: React.FC<CustomerLedgerViewProps> = ({ vm, onRequireProduct, onRecordPayment }) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [customerToDelete, setCustomerToDelete] = useState<{ id: string; name: string } | null>(null);
  const [reminderCustomer, setReminderCustomer] = useState<Customer | null>(null);

  const handleOpenAddCustomer = () => {
    if (vm.products.length === 0) {
      onRequireProduct?.('register a daily customer');
      return;
    }
    setEditingCustomer(null);
    setNewCustName('');
    setNewCustPhone('');
    setNewCustAddress('');
    setNewCustQuota(2);
    setIsAddCustomerOpen(true);
  };
  
  // Modals
  const [isAddCustomerOpen, setIsAddCustomerOpen] = useState(false);
  const [editingCustomer, setEditingCustomer] = useState<Customer | null>(null);

  // Form: Add Customer
  const [newCustName, setNewCustName] = useState('');
  const [newCustPhone, setNewCustPhone] = useState('');
  const [newCustRoute, setNewCustRoute] = useState('Route A - Model Town');
  const [newCustAddress, setNewCustAddress] = useState('');
  const [newCustQuota, setNewCustQuota] = useState<number | ''>(2);

  // Filtered customers
  const filteredCustomers = useMemo(() => {
    return vm.customers.filter(c => {
      const matchesSearch = c.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
                            c.phone.includes(searchTerm);
      return matchesSearch;
    });
  }, [vm.customers, searchTerm]);

  const openPaymentModal = (cust: Customer) => {
    onRecordPayment?.(cust.id, cust.name, cust.balance > 0 ? cust.balance : 1000);
  };

  const openEditCustomer = (cust: Customer) => {
    setEditingCustomer(cust);
    setNewCustName(cust.name);
    setNewCustPhone(cust.phone);
    setNewCustRoute(cust.areaRoute);
    setNewCustAddress(cust.addressNote || '');
    setNewCustQuota(cust.defaultDailyQuota || 2);
    setIsAddCustomerOpen(true);
  };

  const handleDeleteCustomer = (id: string, name: string) => {
    setCustomerToDelete({ id, name });
  };

  const handleAddCustomerSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newCustName.trim()) return;

    if (editingCustomer) {
      vm.updateCustomer(editingCustomer.id, {
        name: newCustName.trim(),
        phone: newCustPhone.trim(),
        areaRoute: newCustRoute,
        addressNote: newCustAddress.trim(),
        defaultDailyQuota: Number(newCustQuota || 0),
      });
    } else {
      vm.addCustomer({
        name: newCustName.trim(),
        phone: newCustPhone.trim(),
        accountType: 'retail',
        areaRoute: newCustRoute,
        addressNote: newCustAddress.trim(),
        defaultDailyQuota: Number(newCustQuota || 0),
        creditLimit: 15000,
        isActive: true,
      });
    }

    setIsAddCustomerOpen(false);
    setEditingCustomer(null);
    setNewCustName('');
    setNewCustPhone('');
    setNewCustAddress('');
  };

  return (
    <div className="space-y-4 pb-16">
      {/* Top Metrics Cards */}
      <div className="grid grid-cols-2 gap-3">
        <div className="p-3.5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 shadow-xs">
          <span className="text-[10px] text-slate-500 dark:text-slate-400 font-bold uppercase block">Total Receivables</span>
          <span className="text-base sm:text-lg font-extrabold font-mono text-amber-700 dark:text-amber-500">
            Rs. {vm.totalCustomerReceivables.toLocaleString()}
          </span>
        </div>

        <div className="p-3.5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 shadow-xs">
          <span className="text-[10px] text-slate-500 dark:text-slate-400 font-bold uppercase block">Collected Today</span>
          <span className="text-base sm:text-lg font-extrabold font-mono text-emerald-700 dark:text-emerald-500">
            Rs. {vm.totalCustomerCashCollected.toLocaleString()}
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
            placeholder="Search customer name or phone..."
            className="w-full bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl pl-9 pr-3 py-2.5 text-xs text-slate-900 dark:text-slate-100 placeholder:text-slate-400 dark:placeholder:text-slate-500 focus:outline-none focus:border-[#135d46] dark:focus:border-emerald-500"
          />
        </div>

        <button
          onClick={handleOpenAddCustomer}
          className="px-4 py-2.5 rounded-xl bg-[#135d46] dark:bg-emerald-600 hover:bg-[#0e4837] dark:hover:bg-emerald-700 text-white font-bold text-xs flex items-center justify-center gap-1.5 cursor-pointer shadow-xs"
        >
          <Plus className="w-4 h-4" />
          <span>New Customer</span>
        </button>
      </div>

      {/* Mobile/Tablet Card List */}
      <div className="space-y-2.5">
        {filteredCustomers.map((cust) => {
          const hasDue = cust.balance > 0;
          return (
            <div
              key={cust.id}
              className={`p-3.5 rounded-2xl border flex items-center justify-between gap-3 shadow-xs ${
                hasDue 
                  ? 'bg-amber-50/40 dark:bg-amber-900/10 border-amber-200 dark:border-amber-900/30' 
                  : 'bg-white dark:bg-slate-900 border-slate-200/80 dark:border-slate-800'
              }`}
            >
              <div>
                <div className="flex items-center gap-2">
                  <h4 className="text-sm font-bold text-slate-900 dark:text-slate-100">{cust.name}</h4>
                  <Badge variant={cust.accountType === 'wholesale' ? 'indigo' : 'neutral'}>
                    {cust.accountType}
                  </Badge>
                </div>

                <div className="text-[11px] text-slate-500 dark:text-slate-400 mt-1 space-x-2">
                  <span>Phone: {cust.phone || 'N/A'}</span>
                  <span>•</span>
                  <span>Route: {cust.areaRoute}</span>
                </div>

                <div className="mt-2 flex items-center gap-2 font-mono">
                  <span className="text-[10px] text-slate-500 dark:text-slate-400 uppercase font-bold">Balance:</span>
                  <span className={`text-sm font-extrabold ${hasDue ? 'text-amber-700 dark:text-amber-500' : 'text-emerald-700 dark:text-emerald-500'}`}>
                    Rs. {cust.balance.toLocaleString()}
                  </span>
                </div>
              </div>

              <div className="flex items-center gap-2">
                {cust.phone && (
                  <button
                    onClick={() => setReminderCustomer(cust)}
                    className="p-2 rounded-xl bg-amber-50 dark:bg-amber-900/20 hover:bg-amber-100 dark:hover:bg-amber-900/40 text-amber-700 dark:text-amber-500 transition-colors cursor-pointer"
                    title="Send Payment Reminder / Contact Customer"
                  >
                    <Bell className="w-3.5 h-3.5" />
                  </button>
                )}
                <button
                  onClick={() => openPaymentModal(cust)}
                  className="px-3 py-2 rounded-xl bg-[#135d46] dark:bg-emerald-600 hover:bg-[#0e4837] dark:hover:bg-emerald-700 text-white font-bold text-xs cursor-pointer shadow-xs shrink-0"
                >
                  Record Payment
                </button>
                <button
                  onClick={() => openEditCustomer(cust)}
                  className="p-2 rounded-xl bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-600 dark:text-slate-400 transition-colors cursor-pointer"
                  title="Edit Customer"
                >
                  <Pencil className="w-3.5 h-3.5" />
                </button>
                <button
                  onClick={() => handleDeleteCustomer(cust.id, cust.name)}
                  className="p-2 rounded-xl bg-rose-50 dark:bg-rose-900/20 hover:bg-rose-100 dark:hover:bg-rose-900/40 text-rose-600 dark:text-rose-400 transition-colors cursor-pointer"
                  title="Delete Customer"
                >
                  <Trash2 className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>
          );
        })}
      </div>

      {/* Card List Removed internal modal triggers for payments */}

      {/* Modal: Add Customer */}
      <Modal
        isOpen={isAddCustomerOpen}
        onClose={() => {
          setIsAddCustomerOpen(false);
          setEditingCustomer(null);
        }}
        title={editingCustomer ? "Edit Customer Details" : "Register New Customer"}
        subtitle={editingCustomer ? "Update customer profile and quota" : "Add a customer for home delivery or Khata credit"}
      >
        <form onSubmit={handleAddCustomerSubmit} className="space-y-4 text-xs">
          <div>
            <label className="block text-slate-700 dark:text-slate-300 font-bold mb-1">Customer Full Name</label>
            <input
              type="text"
              value={newCustName}
              onChange={(e) => setNewCustName(e.target.value)}
              placeholder="e.g. Tariq Mehmood"
              className="w-full bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl px-3 py-2.5 text-slate-900 dark:text-slate-100 focus:outline-none focus:border-[#135d46] dark:focus:border-emerald-500"
              required
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-slate-700 dark:text-slate-300 font-bold mb-1">Phone Number</label>
              <input
                type="text"
                value={newCustPhone}
                onChange={(e) => setNewCustPhone(e.target.value)}
                placeholder="0300-1234567"
                className="w-full bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl px-3 py-2.5 text-slate-900 dark:text-slate-100 focus:outline-none focus:border-[#135d46] dark:focus:border-emerald-500"
              />
            </div>
            <div>
              <label className="block text-slate-700 dark:text-slate-300 font-bold mb-1">Daily Milk Quota (Liters)</label>
              <input
                type="number"
                min="0"
                value={newCustQuota}
                onChange={(e) => setNewCustQuota(e.target.value === '' ? '' : Number(e.target.value))}
                className="w-full bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl px-3 py-2.5 text-slate-900 dark:text-slate-100 font-mono font-bold focus:outline-none focus:border-[#135d46] dark:focus:border-emerald-500"
              />
            </div>
          </div>

          <div>
            <label className="block text-slate-700 dark:text-slate-300 font-bold mb-1">Route / Area</label>
            <input
              type="text"
              value={newCustRoute}
              onChange={(e) => setNewCustRoute(e.target.value)}
              className="w-full bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl px-3 py-2.5 text-slate-900 dark:text-slate-100 focus:outline-none focus:border-[#135d46] dark:focus:border-emerald-500"
            />
          </div>

          <div className="flex justify-end gap-2.5 pt-3">
            <button
              type="button"
              onClick={() => setIsAddCustomerOpen(false)}
              className="px-4 py-2 rounded-xl bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 font-bold cursor-pointer"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-5 py-2 rounded-xl bg-[#135d46] dark:bg-emerald-600 hover:bg-[#0e4837] dark:hover:bg-emerald-700 text-white font-bold cursor-pointer"
            >
              {editingCustomer ? 'Save Changes' : 'Add Customer'}
            </button>
          </div>
        </form>
      </Modal>

      <ConfirmModal
        isOpen={Boolean(customerToDelete)}
        onClose={() => setCustomerToDelete(null)}
        onConfirm={() => {
          if (customerToDelete) {
            vm.deleteCustomer(customerToDelete.id);
            setCustomerToDelete(null);
          }
        }}
        title="Delete Customer Account"
        message={`Are you sure you want to delete customer "${customerToDelete?.name}"? This action cannot be undone.`}
      />

      <Modal
        isOpen={Boolean(reminderCustomer)}
        onClose={() => setReminderCustomer(null)}
        title="Send Payment Reminder & Contact"
      >
        {reminderCustomer && (
          <div className="space-y-4">
            <div className="bg-amber-50/70 dark:bg-amber-900/10 border border-amber-200/80 dark:border-amber-900/30 rounded-2xl p-4">
              <div className="flex items-center justify-between mb-1">
                <h4 className="font-bold text-slate-900 dark:text-slate-100">{reminderCustomer.name}</h4>
                <span className="text-xs font-mono font-bold text-amber-800 dark:text-amber-500 bg-amber-100 dark:bg-amber-900/40 px-2.5 py-0.5 rounded-full">
                  Due: Rs. {reminderCustomer.balance.toLocaleString()}
                </span>
              </div>
              <p className="text-xs text-slate-600 dark:text-slate-400">
                Phone: <span className="font-mono font-bold">{reminderCustomer.phone}</span>
              </p>
            </div>

            <p className="text-sm text-slate-600 dark:text-slate-400 font-medium">
              Choose how you would like to reach out to the customer regarding their pending payment:
            </p>

            <div className="space-y-2.5">
              <button
                type="button"
                onClick={() => {
                  const msg = encodeURIComponent(`Hello ${reminderCustomer.name}, this is a friendly reminder from our dairy shop regarding your outstanding balance of Rs. ${reminderCustomer.balance.toLocaleString()}. Please clear your dues at your earliest convenience. Thank you!`);
                  window.open(`https://wa.me/${reminderCustomer.phone.replace(/[^0-9]/g, '')}?text=${msg}`, '_blank');
                  setReminderCustomer(null);
                }}
                className="w-full flex items-center justify-between p-3.5 rounded-xl bg-emerald-50 dark:bg-emerald-900/10 hover:bg-emerald-100 dark:hover:bg-emerald-900/20 text-emerald-800 dark:text-emerald-400 font-bold transition-colors cursor-pointer border border-emerald-200/60 dark:border-emerald-900/30"
              >
                <div className="flex items-center gap-3">
                  <div className="p-2 rounded-lg bg-emerald-600 text-white">
                    <MessageSquare className="w-4 h-4" />
                  </div>
                  <div className="text-left">
                    <div className="text-sm">Send via WhatsApp</div>
                    <div className="text-xs font-normal text-emerald-700 dark:text-emerald-500">Opens WhatsApp with prefilled reminder message</div>
                  </div>
                </div>
                <Send className="w-4 h-4 text-emerald-600" />
              </button>

              <button
                type="button"
                onClick={() => {
                  const msg = encodeURIComponent(`Hello ${reminderCustomer.name}, gentle reminder from our dairy shop for your balance of Rs. ${reminderCustomer.balance.toLocaleString()}. Please clear dues when possible. Thank you!`);
                  window.open(`sms:${reminderCustomer.phone.replace(/[^0-9]/g, '')}?body=${msg}`, '_self');
                  setReminderCustomer(null);
                }}
                className="w-full flex items-center justify-between p-3.5 rounded-xl bg-sky-50 dark:bg-sky-900/10 hover:bg-sky-100 dark:hover:bg-sky-900/20 text-sky-800 dark:text-sky-400 font-bold transition-colors cursor-pointer border border-sky-200/60 dark:border-sky-900/30"
              >
                <div className="flex items-center gap-3">
                  <div className="p-2 rounded-lg bg-sky-600 text-white">
                    <MessageSquare className="w-4 h-4" />
                  </div>
                  <div className="text-left">
                    <div className="text-sm">Send SMS Text Message</div>
                    <div className="text-xs font-normal text-sky-700 dark:text-sky-500">Opens device text messaging app</div>
                  </div>
                </div>
                <Send className="w-4 h-4 text-sky-600" />
              </button>

              <button
                type="button"
                onClick={() => {
                  window.open(`tel:${reminderCustomer.phone.replace(/[^0-9]/g, '')}`, '_self');
                  setReminderCustomer(null);
                }}
                className="w-full flex items-center justify-between p-3.5 rounded-xl bg-indigo-50 dark:bg-indigo-900/10 hover:bg-indigo-100 dark:hover:bg-indigo-900/20 text-indigo-800 dark:text-indigo-400 font-bold transition-colors cursor-pointer border border-indigo-200/60 dark:border-indigo-900/30"
              >
                <div className="flex items-center gap-3">
                  <div className="p-2 rounded-lg bg-indigo-600 text-white">
                    <Phone className="w-4 h-4" />
                  </div>
                  <div className="text-left">
                    <div className="text-sm">Call Customer</div>
                    <div className="text-xs font-normal text-indigo-700 dark:text-indigo-500">Initiates direct phone call</div>
                  </div>
                </div>
                <Phone className="w-4 h-4 text-indigo-600" />
              </button>
            </div>

            <div className="flex justify-end pt-2">
              <button
                type="button"
                onClick={() => setReminderCustomer(null)}
                className="px-4 py-2 rounded-xl bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 font-bold cursor-pointer text-xs"
              >
                Cancel
              </button>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
};
