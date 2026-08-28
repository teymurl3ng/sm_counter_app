import type { ReactNode } from 'react';
import { CheckCircle2, ShieldCheck, X } from 'lucide-react';

export function Dialog({ title, onClose, children }: { title: string; onClose(): void; children: ReactNode }) {
  return (
    <div className="fixed inset-0 z-50 flex items-end bg-slate-950/40 p-0 sm:items-center sm:justify-center sm:p-4">
      <section role="dialog" aria-modal="true" className="max-h-[92vh] w-full overflow-y-auto rounded-t-3xl bg-white p-5 shadow-2xl sm:max-w-lg sm:rounded-3xl">
        <div className="mb-5 flex items-center justify-between">
          <h2 className="text-xl font-bold">{title}</h2>
          <button type="button" onClick={onClose} className="rounded-lg p-2 text-slate-500" aria-label="Close"><X className="h-5 w-5" /></button>
        </div>
        {children}
      </section>
    </div>
  );
}

export function TextInput({ id, label, value, onChange, type = 'text', autoComplete, inputMode, required = true }: {
  id: string;
  label: string;
  value: string;
  onChange(value: string): void;
  type?: string;
  autoComplete?: string;
  inputMode?: 'decimal';
  required?: boolean;
}) {
  return (
    <label className="mb-4 block text-sm font-semibold text-slate-700">{label}
      <input id={id} required={required} value={value} onChange={(event) => onChange(event.target.value)} type={type} autoComplete={autoComplete} inputMode={inputMode} className="mt-1.5 w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5 outline-none focus:border-emerald-600 focus:ring-2 focus:ring-emerald-100" />
    </label>
  );
}

export function FilterButton({ id, active, onClick, children }: { id: string; active: boolean; onClick(): void; children: ReactNode }) {
  return <button type="button" id={id} onClick={onClick} className={`rounded-lg px-3 py-2 text-xs font-bold ${active ? 'bg-white text-emerald-800 shadow-sm' : 'text-slate-500'}`}>{children}</button>;
}

export function SummaryCard({ label, value }: { label: string; value: string }) {
  return <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm"><p className="text-xs font-semibold uppercase tracking-wide text-slate-500">{label}</p><p className="mt-1 text-2xl font-bold text-slate-900">{value}</p></div>;
}

export function Notice({ message, success = false }: { message: string; success?: boolean }) {
  return <p role="status" className={`mb-4 flex items-start gap-2 rounded-xl px-3 py-2 text-sm ${success ? 'bg-emerald-50 text-emerald-800' : 'bg-rose-50 text-rose-800'}`}>{success && <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0" />}{message}</p>;
}

export function CenteredMessage({ title, message }: { title: string; message: string }) {
  return <main className="flex min-h-screen items-center justify-center bg-[#f7f6f0] px-5 text-slate-900"><section className="max-w-md rounded-3xl border border-slate-200 bg-white p-7 text-center shadow-xl"><ShieldCheck className="mx-auto mb-4 h-10 w-10 text-emerald-700" /><h1 className="text-xl font-bold">{title}</h1><p className="mt-2 text-sm leading-6 text-slate-600">{message}</p></section></main>;
}
