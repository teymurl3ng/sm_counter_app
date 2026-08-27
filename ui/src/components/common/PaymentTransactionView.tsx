import React, { useState, useRef } from 'react';
import { 
  ArrowLeft, 
  Calendar, 
  Clock, 
  CreditCard, 
  FileText, 
  Image as ImageIcon, 
  Share2, 
  Camera,
  CheckCircle2,
  X
} from 'lucide-react';
import { MilkShopViewModel } from '../../viewmodels/useMilkShopViewModel';
import { PaymentMethod } from '../../types/milkShop';
import { motion } from 'motion/react';
import html2canvas from 'html2canvas';
import { jsPDF } from 'jspdf';

interface PaymentTransactionViewProps {
  vm: MilkShopViewModel;
  mode: 'inflow' | 'outflow'; // inflow: customer pays shop, outflow: shop pays supplier
  contactId: string;
  contactName: string;
  initialAmount?: number;
  onBack: () => void;
}

export const PaymentTransactionView: React.FC<PaymentTransactionViewProps> = ({ 
  vm, 
  mode, 
  contactId, 
  contactName, 
  initialAmount = 0,
  onBack 
}) => {
  const [amount, setAmount] = useState<number | ''>(initialAmount);
  const [date, setDate] = useState(new Date().toISOString().split('T')[0]);
  const [time, setTime] = useState(new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false }));
  const [method, setMethod] = useState<PaymentMethod>('cash');
  const [notes, setNotes] = useState('');
  const [attachment, setAttachment] = useState<string | null>(null);
  const [isSuccess, setIsSuccess] = useState(false);
  const [recordedPayment, setRecordedPayment] = useState<any>(null);
  const [isExporting, setIsExporting] = useState(false);
  
  const receiptRef = useRef<HTMLDivElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onloadend = () => {
        setAttachment(reader.result as string);
      };
      reader.readAsDataURL(file);
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (amount === '' || amount <= 0) return;

    let result;
    if (mode === 'inflow') {
      result = vm.recordCustomerPayment(contactId, amount, method, notes, attachment || undefined, date);
    } else {
      result = vm.recordSupplierPayment(contactId, amount, method, notes, attachment || undefined, date);
    }

    if (result) {
      setRecordedPayment(result);
      setIsSuccess(true);
    }
  };

  const downloadAsImage = async () => {
    if (!receiptRef.current || isExporting) return;
    setIsExporting(true);
    try {
      const canvas = await html2canvas(receiptRef.current, {
        scale: 2,
        backgroundColor: '#ffffff',
        logging: false,
        useCORS: true,
        onclone: (clonedDoc) => {
          // Crucial fix: html2canvas crashes when parsing modern CSS colors like oklch.
          // Since the receipt uses 100% inline styles for layout and colors, 
          // we can safely strip all document stylesheets in the clone to avoid the crash.
          const styles = clonedDoc.getElementsByTagName('style');
          for (let i = styles.length - 1; i >= 0; i--) {
            styles[i].remove();
          }
          const links = clonedDoc.getElementsByTagName('link');
          for (let i = links.length - 1; i >= 0; i--) {
            if (links[i].rel === 'stylesheet') {
              links[i].remove();
            }
          }
        }
      });
      const dataUrl = canvas.toDataURL('image/png');
      const link = document.createElement('a');
      link.href = dataUrl;
      link.download = `Receipt_${contactName.replace(/\s+/g, '_')}_${date}.png`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    } catch (err) {
      console.error('Failed to export image:', err);
    } finally {
      setIsExporting(false);
    }
  };

  const downloadAsPDF = async () => {
    if (!receiptRef.current || isExporting) return;
    setIsExporting(true);
    try {
      const canvas = await html2canvas(receiptRef.current, {
        scale: 2,
        backgroundColor: '#ffffff',
        logging: false,
        useCORS: true,
        onclone: (clonedDoc) => {
          // Stripping stylesheets in the clone to prevent oklch parsing errors
          const styles = clonedDoc.getElementsByTagName('style');
          for (let i = styles.length - 1; i >= 0; i--) {
            styles[i].remove();
          }
          const links = clonedDoc.getElementsByTagName('link');
          for (let i = links.length - 1; i >= 0; i--) {
            if (links[i].rel === 'stylesheet') {
              links[i].remove();
            }
          }
        }
      });
      const imgData = canvas.toDataURL('image/png');
      const pdf = new jsPDF('p', 'mm', 'a4');
      const imgProps = pdf.getImageProperties(imgData);
      const pdfWidth = pdf.internal.pageSize.getWidth();
      const pdfHeight = (imgProps.height * pdfWidth) / imgProps.width;
      
      pdf.addImage(imgData, 'PNG', 0, 0, pdfWidth, pdfHeight);
      pdf.save(`Receipt_${contactName.replace(/\s+/g, '_')}_${date}.pdf`);
    } catch (err) {
      console.error('Failed to export PDF:', err);
    } finally {
      setIsExporting(false);
    }
  };

  const shareReceipt = () => {
    const msg = encodeURIComponent(
      `*Payment Receipt*\n\n` +
      `*Entity:* ${contactName}\n` +
      `*Amount:* Rs. ${amount}\n` +
      `*Date:* ${date}\n` +
      `*Method:* ${method.toUpperCase()}\n` +
      `*Note:* ${notes || 'N/A'}\n\n` +
      `Thank you for your business!`
    );
    window.open(`https://wa.me/?text=${msg}`, '_blank');
  };

  if (isSuccess && recordedPayment) {
    return (
      <div className="fixed inset-0 z-[100] bg-[#f7f6f0] dark:bg-slate-950 flex flex-col overflow-y-auto">
        <div className="w-full max-w-xl mx-auto p-4 md:p-8 flex-1 flex flex-col items-center justify-center">
          <motion.div 
            initial={{ scale: 0.9, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            className="w-full bg-white dark:bg-slate-900 rounded-3xl shadow-xl overflow-hidden border border-slate-200 dark:border-slate-800"
          >
            <div className="p-8 text-center border-b border-dashed border-slate-200 dark:border-slate-800 bg-emerald-50/30 dark:bg-emerald-950/20">
              <div className="w-16 h-16 bg-emerald-100 dark:bg-emerald-900/40 text-emerald-600 dark:text-emerald-400 rounded-full flex items-center justify-center mx-auto mb-4">
                <CheckCircle2 className="w-10 h-10" />
              </div>
              <h2 className="text-2xl font-black text-slate-900 dark:text-slate-100">Payment Recorded!</h2>
              <p className="text-slate-500 dark:text-slate-400 text-sm mt-1">Transaction successfully saved to ledger</p>
            </div>

            <div 
              ref={receiptRef} 
              style={{ 
                backgroundColor: '#ffffff', 
                padding: '32px',
                width: '100%',
                maxWidth: '480px',
                margin: '0 auto',
                fontFamily: 'system-ui, -apple-system, sans-serif'
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '32px' }}>
                <div style={{ textAlign: 'left' }}>
                  <h3 style={{ fontSize: '20px', fontWeight: '900', color: '#135d46', margin: '0' }}>{vm.settings.storeName || 'Milk Shop'}</h3>
                  <p style={{ fontSize: '10px', color: '#94a3b8', fontWeight: '700', textTransform: 'uppercase', letterSpacing: '0.05em', margin: '0' }}>Official Digital Receipt</p>
                </div>
                <div style={{ textAlign: 'right' }}>
                  <p style={{ fontSize: '12px', fontWeight: '900', color: '#0f172a', margin: '0' }}>Ref: #{recordedPayment.id.split('-')[1]}</p>
                  <p style={{ fontSize: '10px', color: '#64748b', fontWeight: '700', margin: '0' }}>{date} | {time}</p>
                </div>
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '16px 0', borderBottom: '1px solid #f1f5f9' }}>
                  <span style={{ fontSize: '10px', color: '#94a3b8', fontWeight: '700', textTransform: 'uppercase' }}>{mode === 'inflow' ? 'Received From' : 'Paid To'}</span>
                  <span style={{ fontSize: '14px', fontWeight: '900', color: '#0f172a' }}>{contactName}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '16px 0', borderBottom: '1px solid #f1f5f9' }}>
                  <span style={{ fontSize: '10px', color: '#94a3b8', fontWeight: '700', textTransform: 'uppercase' }}>Method</span>
                  <span style={{ fontSize: '14px', fontWeight: '900', color: '#0f172a', textTransform: 'uppercase' }}>{method.replace('_', ' ')}</span>
                </div>
                {notes && (
                  <div style={{ padding: '16px 0', borderBottom: '1px solid #f1f5f9' }}>
                    <span style={{ fontSize: '10px', color: '#94a3b8', fontWeight: '700', textTransform: 'uppercase', display: 'block', marginBottom: '4px' }}>Description</span>
                    <p style={{ fontSize: '14px', fontWeight: '500', color: '#334155', fontStyle: 'italic', margin: '0' }}>"{notes}"</p>
                  </div>
                )}
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '20px 24px', backgroundColor: '#f8fafc', borderRadius: '16px', marginTop: '16px', border: '1px solid #f1f5f9' }}>
                  <span style={{ fontSize: '14px', fontWeight: '900', color: '#0f172a', textTransform: 'uppercase' }}>Total Amount</span>
                  <span style={{ fontSize: '24px', fontWeight: '900', color: '#135d46' }}>Rs. {Number(amount).toLocaleString()}</span>
                </div>
              </div>

              <div style={{ marginTop: '48px', textAlign: 'center' }}>
                <p style={{ fontSize: '10px', color: '#cbd5e1', fontWeight: '700', textTransform: 'uppercase', margin: '0' }}>Thank you for your business</p>
              </div>
            </div>

            <div className="p-6 bg-slate-50 dark:bg-slate-900/50 border-t border-slate-200 dark:border-slate-800 grid grid-cols-2 gap-3">
              <button 
                onClick={downloadAsImage}
                disabled={isExporting}
                className="flex items-center justify-center gap-2 p-4 rounded-2xl bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-300 font-black text-xs hover:bg-slate-100 dark:hover:bg-slate-700 transition-all active:scale-95 disabled:opacity-50 cursor-pointer shadow-sm"
              >
                <ImageIcon className="w-4 h-4" />
                {isExporting ? 'Exporting...' : 'Save Image'}
              </button>
              <button 
                onClick={downloadAsPDF}
                disabled={isExporting}
                className="flex items-center justify-center gap-2 p-4 rounded-2xl bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-300 font-black text-xs hover:bg-slate-100 dark:hover:bg-slate-700 transition-all active:scale-95 disabled:opacity-50 cursor-pointer shadow-sm"
              >
                <FileText className="w-4 h-4" />
                {isExporting ? 'Exporting...' : 'Get PDF'}
              </button>
              <button 
                onClick={shareReceipt}
                className="flex items-center justify-center gap-2 p-4 rounded-2xl bg-[#135d46] dark:bg-emerald-600 text-white font-black text-sm hover:bg-[#0e4837] dark:hover:bg-emerald-700 transition-all active:scale-95 cursor-pointer col-span-2 shadow-lg shadow-emerald-900/20"
              >
                <Share2 className="w-5 h-5" />
                Share on WhatsApp
              </button>
              <button 
                onClick={onBack}
                className="flex items-center justify-center gap-2 p-4 rounded-2xl text-slate-400 dark:text-slate-500 font-bold text-xs hover:text-slate-600 dark:hover:text-slate-300 transition-colors cursor-pointer col-span-2 mt-2"
              >
                Close & Return to Ledger
              </button>
            </div>
          </motion.div>
        </div>
      </div>
    );
  }

  return (
    <div className="fixed inset-0 z-[100] bg-[#f7f6f0] dark:bg-slate-950 flex flex-col">
      {/* Header */}
      <header className="bg-white dark:bg-slate-900 border-b border-slate-200 dark:border-slate-800 p-4 sticky top-0 z-10 flex items-center gap-4">
        <button onClick={onBack} className="p-2 rounded-xl hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-600 dark:text-slate-400 cursor-pointer transition-colors">
          <ArrowLeft className="w-6 h-6" />
        </button>
        <div>
          <h1 className="text-xl font-black text-slate-900 dark:text-slate-100">Record {mode === 'inflow' ? 'Payment In' : 'Payment Out'}</h1>
          <p className="text-xs text-slate-500 dark:text-slate-400 font-bold uppercase tracking-tight">Transaction for: {contactName}</p>
        </div>
      </header>

      <div className="flex-1 overflow-y-auto">
        <form onSubmit={handleSubmit} className="w-full max-w-3xl mx-auto p-4 md:p-8 space-y-6 pb-32">
          {/* Amount Input Section */}
          <section className="bg-white dark:bg-slate-900 rounded-[2rem] p-8 md:p-10 shadow-sm border border-slate-200 dark:border-slate-800">
            <label className="text-[10px] font-black text-slate-400 dark:text-slate-500 uppercase tracking-widest block mb-4">Transaction Amount (PKR)</label>
            <div className="relative">
              <span className="absolute left-0 top-1/2 -translate-y-1/2 text-3xl font-black text-slate-200 dark:text-slate-800">Rs.</span>
              <input 
                type="number"
                value={amount}
                onChange={(e) => setAmount(e.target.value === '' ? '' : Number(e.target.value))}
                placeholder="0.00"
                className="w-full pl-14 py-4 text-5xl md:text-6xl font-black text-[#135d46] dark:text-emerald-500 bg-transparent focus:outline-none placeholder:text-slate-50 dark:placeholder:text-slate-800 appearance-none"
                required
                autoFocus
              />
            </div>
          </section>

          {/* Details Grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <section className="bg-white dark:bg-slate-900 rounded-[1.5rem] p-6 shadow-sm border border-slate-200 dark:border-slate-800">
              <label className="text-[10px] font-black text-slate-400 dark:text-slate-500 uppercase tracking-widest block mb-4">Date & Time</label>
              <div className="space-y-3">
                <div className="flex items-center gap-4 p-4 bg-slate-50 dark:bg-slate-800/50 rounded-2xl border border-slate-100 dark:border-slate-800 focus-within:border-emerald-200 dark:focus-within:border-emerald-800 transition-colors">
                  <Calendar className="w-5 h-5 text-slate-400 dark:text-slate-500" />
                  <input 
                    type="date" 
                    value={date} 
                    onChange={(e) => setDate(e.target.value)}
                    className="bg-transparent text-base font-bold text-slate-700 dark:text-slate-200 focus:outline-none w-full"
                  />
                </div>
                <div className="flex items-center gap-4 p-4 bg-slate-50 dark:bg-slate-800/50 rounded-2xl border border-slate-100 dark:border-slate-800 focus-within:border-emerald-200 dark:focus-within:border-emerald-800 transition-colors">
                  <Clock className="w-5 h-5 text-slate-400 dark:text-slate-500" />
                  <input 
                    type="time" 
                    value={time} 
                    onChange={(e) => setTime(e.target.value)}
                    className="bg-transparent text-base font-bold text-slate-700 dark:text-slate-200 focus:outline-none w-full"
                  />
                </div>
              </div>
            </section>

            <section className="bg-white dark:bg-slate-900 rounded-[1.5rem] p-6 shadow-sm border border-slate-200 dark:border-slate-800">
              <label className="text-[10px] font-black text-slate-400 dark:text-slate-500 uppercase tracking-widest block mb-4">Payment Method</label>
              <div className="grid grid-cols-2 gap-3">
                {(['cash', 'online', 'bank_transfer'] as PaymentMethod[]).map((m) => (
                  <button
                    key={m}
                    type="button"
                    onClick={() => setMethod(m)}
                    className={`p-4 rounded-2xl border text-xs font-black transition-all flex flex-col items-center justify-center gap-2 cursor-pointer ${
                      method === m 
                        ? 'bg-[#135d46] dark:bg-emerald-600 border-[#135d46] dark:border-emerald-600 text-white shadow-lg shadow-emerald-900/20' 
                        : 'bg-slate-50 dark:bg-slate-800 border-slate-200 dark:border-slate-700 text-slate-500 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-700'
                    }`}
                  >
                    <CreditCard className="w-5 h-5" />
                    <span>{m === 'bank_transfer' ? 'Bank' : m.charAt(0).toUpperCase() + m.slice(1)}</span>
                  </button>
                ))}
              </div>
            </section>
          </div>

          {/* Attachment & Notes */}
          <section className="bg-white dark:bg-slate-900 rounded-[1.5rem] p-6 shadow-sm border border-slate-200 dark:border-slate-800">
            <label className="text-[10px] font-black text-slate-400 dark:text-slate-500 uppercase tracking-widest block mb-4">Reference & Proof</label>
            <div className="space-y-6">
              <textarea 
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                placeholder="What is this payment for? (e.g. Invoice #123, Advance salary...)"
                className="w-full bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-2xl p-5 text-base text-slate-700 dark:text-slate-200 placeholder:text-slate-300 dark:placeholder:text-slate-600 focus:outline-none focus:border-emerald-200 dark:focus:border-emerald-800 min-h-[120px] transition-colors"
              />
              
              <div className="flex flex-col gap-3">
                <span className="text-[10px] font-black text-slate-400 dark:text-slate-500 uppercase tracking-widest">Attachment (Bill / Screenshot)</span>
                {attachment ? (
                  <div className="relative w-full aspect-video rounded-[1.5rem] overflow-hidden border-2 border-slate-100 dark:border-slate-800 shadow-inner">
                    <img src={attachment} alt="Attachment" className="w-full h-full object-contain bg-slate-900" />
                    <button 
                      type="button"
                      onClick={() => setAttachment(null)}
                      className="absolute top-4 right-4 p-2 bg-rose-500 text-white rounded-full shadow-xl hover:bg-rose-600 transition-colors cursor-pointer"
                    >
                      <X className="w-5 h-5" />
                    </button>
                  </div>
                ) : (
                  <button
                    type="button"
                    onClick={() => fileInputRef.current?.click()}
                    className="w-full py-12 border-2 border-dashed border-slate-200 dark:border-slate-800 rounded-[1.5rem] flex flex-col items-center justify-center gap-3 text-slate-300 dark:text-slate-600 hover:text-[#135d46] dark:hover:text-emerald-500 hover:border-[#135d46] dark:hover:border-emerald-500 hover:bg-emerald-50/30 dark:hover:bg-emerald-950/20 transition-all group cursor-pointer"
                  >
                    <Camera className="w-10 h-10 group-hover:scale-110 transition-transform" />
                    <span className="text-xs font-black uppercase tracking-widest">Take Photo / Upload Bill</span>
                  </button>
                )}
                <input 
                  type="file" 
                  ref={fileInputRef} 
                  onChange={handleFileChange} 
                  className="hidden" 
                  accept="image/*"
                />
              </div>
            </div>
          </section>
        </form>
      </div>

      {/* Bottom Bar */}
      <div className="bg-white dark:bg-slate-900 border-t border-slate-200 dark:border-slate-800 p-4 md:p-6 shadow-[0_-10px_20px_rgba(0,0,0,0.03)] dark:shadow-none">
        <div className="max-w-3xl mx-auto grid grid-cols-2 gap-4">
          <button 
            type="button"
            onClick={onBack}
            className="p-5 rounded-2xl bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 font-black text-sm hover:bg-slate-200 dark:hover:bg-slate-700 transition-all active:scale-95 cursor-pointer"
          >
            Cancel
          </button>
          <button 
            onClick={(e) => {
              const form = (e.target as HTMLButtonElement).closest('div')?.parentElement?.parentElement?.querySelector('form');
              if (form) form.requestSubmit();
            }}
            className="p-5 rounded-2xl bg-[#135d46] dark:bg-emerald-600 text-white font-black text-sm shadow-xl shadow-emerald-900/20 dark:shadow-none hover:bg-[#0e4837] dark:hover:bg-emerald-700 transition-all active:scale-95 cursor-pointer"
          >
            Save Transaction
          </button>
        </div>
      </div>
    </div>
  );
};
