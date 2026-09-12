import { useEffect, useId, useRef, useState, type KeyboardEvent, type ReactNode } from 'react';
import { AlertCircle, CheckCircle2, ChevronDown, ShieldCheck, X } from 'lucide-react';

export function Dialog({ title, onClose, children }: { title: string; onClose(): void; children: ReactNode }) {
  const titleId = useId();
  const panelRef = useRef<HTMLElement>(null);
  const contentRef = useRef<HTMLDivElement>(null);
  const closeRef = useRef<HTMLButtonElement>(null);
  const [hasMoreContent, setHasMoreContent] = useState(false);

  useEffect(() => {
    const panel = panelRef.current;
    const content = contentRef.current;
    const previousFocus = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    if (!panel || !content) return;
    const updateOverflow = () => setHasMoreContent(panel.scrollTop + panel.clientHeight < panel.scrollHeight - 8);
    const observer = new ResizeObserver(updateOverflow);
    observer.observe(panel);
    observer.observe(content);
    panel.addEventListener('scroll', updateOverflow, { passive: true });
    const frame = window.requestAnimationFrame(() => {
      updateOverflow();
      closeRef.current?.focus({ preventScroll: true });
    });
    return () => {
      window.cancelAnimationFrame(frame);
      observer.disconnect();
      panel.removeEventListener('scroll', updateOverflow);
      previousFocus?.focus({ preventScroll: true });
    };
  }, []);

  const handleKeyDown = (event: KeyboardEvent<HTMLElement>) => {
    if (event.key === 'Escape') {
      event.preventDefault();
      onClose();
      return;
    }
    if (event.key !== 'Tab') return;
    const focusable = Array.from(panelRef.current?.querySelectorAll<HTMLElement>('button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), a[href], [tabindex]:not([tabindex="-1"])') ?? [])
      .filter((item) => item.getClientRects().length > 0);
    if (focusable.length === 0) return;
    const first = focusable[0];
    const last = focusable[focusable.length - 1];
    if (event.shiftKey && document.activeElement === first) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault();
      first.focus();
    }
  };

  return (
    <div className="hybrid-overlay fixed inset-0 z-50 flex items-end bg-slate-950/40 p-0 sm:items-center sm:justify-center sm:p-4">
      <section ref={panelRef} role="dialog" aria-modal="true" aria-labelledby={titleId} onKeyDown={handleKeyDown} className="hybrid-dialog-panel w-full overflow-y-auto rounded-t-3xl bg-white shadow-2xl sm:max-w-lg sm:rounded-3xl">
        <div className="hybrid-dialog-header sticky top-0 z-10 flex items-center justify-between gap-4 border-b border-slate-100 bg-white px-5 py-4">
          <h2 id={titleId} className="text-xl font-bold text-slate-900">{title}</h2>
          <button ref={closeRef} type="button" data-native-back-close onClick={onClose} className="flex min-h-12 items-center justify-center gap-1.5 rounded-xl px-3 font-semibold text-slate-700" aria-label={`Close ${title}`}><X className="h-5 w-5 shrink-0" aria-hidden="true" /><span>Close</span></button>
        </div>
        <div ref={contentRef} className="px-5 pb-5 pt-4">{children}</div>
        {hasMoreContent && <button type="button" onClick={() => panelRef.current?.scrollTo({ top: panelRef.current.scrollHeight, behavior: 'auto' })} className="hybrid-dialog-continue sticky bottom-3 z-10 mx-5 mb-3 flex min-h-12 w-[calc(100%-2.5rem)] items-center justify-center gap-2 rounded-xl border border-slate-300 bg-white px-4 py-2 text-sm font-bold text-emerald-800 shadow-lg">Continue to form actions<ChevronDown className="h-4 w-4" /></button>}
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
  inputMode?: 'decimal' | 'numeric';
  required?: boolean;
}) {
  return (
    <label className="mb-3 block text-sm font-semibold leading-5 text-slate-700">{label}
      <input id={id} required={required} value={value} onChange={(event) => onChange(event.target.value)} type={type} autoComplete={autoComplete} inputMode={inputMode} className="mt-1.5 min-h-12 w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5 text-base outline-none focus:border-emerald-600 focus:ring-2 focus:ring-emerald-100" />
    </label>
  );
}

export function FilterButton({ id, active, onClick, children }: { id: string; active: boolean; onClick(): void; children: ReactNode }) {
  return <button type="button" id={id} aria-pressed={active} onClick={onClick} className={`min-h-12 min-w-0 flex-1 rounded-lg px-3 py-2 text-sm font-bold leading-5 ${active ? 'bg-white text-emerald-800 shadow-sm' : 'text-slate-600'}`}>{children}</button>;
}

export function SummaryCard({ label, value }: { label: string; value: string }) {
  const isCurrency = value.startsWith('PKR ');
  return <div className="min-w-0 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm"><p className="text-xs font-semibold uppercase leading-4 tracking-wide text-slate-600">{label}</p><p className={`hybrid-summary-value mt-1 font-bold leading-7 text-slate-900 ${isCurrency ? 'whitespace-nowrap text-[clamp(1rem,4.4vw,1.5rem)]' : 'break-words text-xl sm:text-2xl'}`}>{isCurrency ? <>PKR&nbsp;{value.slice(4)}</> : value}</p></div>;
}

export function Notice({ message, success = false }: { message: string; success?: boolean }) {
  const Icon = success ? CheckCircle2 : AlertCircle;
  return <p role={success ? 'status' : 'alert'} aria-live={success ? 'polite' : 'assertive'} className={`mb-4 flex items-start gap-2 rounded-xl border px-3 py-2.5 text-sm leading-5 ${success ? 'border-emerald-200 bg-emerald-50 text-emerald-900' : 'border-rose-200 bg-rose-50 text-rose-900'}`}><Icon className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />{message}</p>;
}

export function CenteredMessage({ title, message }: { title: string; message: string }) {
  return <main className="hybrid-screen flex items-center justify-center bg-[#f7f6f0] px-5 text-slate-900"><section className="max-w-md rounded-3xl border border-slate-200 bg-white p-7 text-center shadow-xl"><ShieldCheck className="mx-auto mb-4 h-10 w-10 text-emerald-700" /><h1 className="text-xl font-bold">{title}</h1><p className="mt-2 text-base leading-6 text-slate-600">{message}</p></section></main>;
}
