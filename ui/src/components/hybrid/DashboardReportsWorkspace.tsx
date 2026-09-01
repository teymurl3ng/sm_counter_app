import { useCallback, useEffect, useRef, useState } from 'react';
import { RefreshCw } from 'lucide-react';
import { Notice } from './HybridUi';
import { errorMessage, todayInput } from './hybridModels';
import { FinancialReportSection } from './FinancialReportSection';
import { OperationalReportSection } from './OperationalReportSection';
import type { ReportBundle } from './reportBridgeActions';
import { reportPeriodLabel } from './reportPresentation';

export function DashboardReportsWorkspace({ canViewCashier, canViewFinancial, onLoad }: {
  canViewCashier: boolean;
  canViewFinancial: boolean;
  onLoad(startDate: string, throughDate: string, canViewCashier: boolean, canViewFinancial: boolean): Promise<ReportBundle>;
}) {
  const today = todayInput();
  const [startDate, setStartDate] = useState(today);
  const [throughDate, setThroughDate] = useState(today);
  const [bundle, setBundle] = useState<ReportBundle | null>(null);
  const [shownPeriod, setShownPeriod] = useState({ start: today, through: today });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const loadedInitialReport = useRef(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const loaded = await onLoad(startDate, throughDate, canViewCashier, canViewFinancial);
      setBundle(loaded);
      setShownPeriod({ start: startDate, through: throughDate });
    } catch (failure) {
      setError(errorMessage(failure));
    } finally {
      setLoading(false);
    }
  }, [canViewCashier, canViewFinancial, onLoad, startDate, throughDate]);

  useEffect(() => {
    if (loadedInitialReport.current) return;
    loadedInitialReport.current = true;
    void load();
  }, [load]);

  return <div className="space-y-5">
    <section className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
      <div className="grid gap-3 sm:grid-cols-[1fr_1fr_auto] sm:items-end">
        <DateInput id="report-from-date" label="From" value={startDate} onChange={setStartDate} />
        <DateInput id="report-through-date" label="Through" value={throughDate} onChange={setThroughDate} />
        <button id="run-report-button" type="button" disabled={loading} onClick={() => void load()} className="flex items-center justify-center gap-2 rounded-xl bg-emerald-800 px-4 py-2.5 font-bold text-white disabled:opacity-50"><RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />{loading ? 'Loading…' : 'Run report'}</button>
      </div>
      {bundle && !loading && <p id="report-period-label" className="mt-3 text-sm text-slate-500">Showing {reportPeriodLabel(shownPeriod.start, shownPeriod.through)}</p>}
    </section>
    {error && <Notice message={error} />}
    {loading && !bundle ? <p className="rounded-2xl border border-slate-200 bg-white p-5 text-sm text-slate-500">Loading the shop report…</p> : bundle && <>
      <OperationalReportSection dashboard={bundle.dashboard} cashierSummary={bundle.cashierSummary} />
      {bundle.financialOverview && bundle.profitability && <FinancialReportSection overview={bundle.financialOverview} profitability={bundle.profitability} />}
    </>}
  </div>;
}

function DateInput({ id, label, value, onChange }: { id: string; label: string; value: string; onChange(value: string): void }) {
  return <label className="text-sm font-semibold text-slate-700">{label}<input id={id} type="date" required value={value} onChange={(event) => onChange(event.target.value)} className="mt-1.5 w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5 outline-none focus:border-emerald-600 focus:ring-2 focus:ring-emerald-100" /></label>;
}
