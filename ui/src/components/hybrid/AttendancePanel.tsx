import { useEffect, useState } from 'react';
import { CalendarCheck2, RefreshCw } from 'lucide-react';
import type { AccountReferenceDto, AttendanceDto, AttendanceStatusCode } from '../../bridge/contract.generated';
import { Notice, SummaryCard } from './HybridUi';
import { errorMessage } from './hybridModels';

type AttendanceDraft = { statusCode: AttendanceStatusCode; notes: string };

export function AttendancePanel({ busy, employees, error, notice, onLoad, onRecord }: {
  busy: boolean;
  employees: ReadonlyArray<AccountReferenceDto>;
  error: string | null;
  notice: string | null;
  onLoad(businessDate: string): Promise<ReadonlyArray<AttendanceDto>>;
  onRecord(employeeAccountId: AccountReferenceDto['accountId'], businessDate: string, statusCode: AttendanceStatusCode, notes: string): Promise<void>;
}) {
  const [businessDate, setBusinessDate] = useState(todayLocal());
  const [records, setRecords] = useState<ReadonlyArray<AttendanceDto>>([]);
  const [drafts, setDrafts] = useState<Record<string, AttendanceDraft>>({});
  const [loading, setLoading] = useState(true);
  const [localError, setLocalError] = useState<string | null>(null);

  const load = async () => {
    setLoading(true);
    setLocalError(null);
    try {
      const loaded = await onLoad(businessDate);
      setRecords(loaded);
      const next: Record<string, AttendanceDraft> = {};
      for (const employee of employees) {
        const existing = loaded.find((item) => item.employeeAccountId === employee.accountId);
        next[employee.accountId] = {
          statusCode: isAttendanceStatus(existing?.statusCode) ? existing.statusCode : 'present',
          notes: existing?.notes ?? '',
        };
      }
      setDrafts(next);
    } catch (failure) {
      setLocalError(errorMessage(failure));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { void load(); }, [businessDate, employees]);

  const present = records.filter((item) => item.statusCode === 'present').length;
  const absent = records.filter((item) => item.statusCode === 'absent').length;
  const updateDraft = (accountId: string, change: Partial<AttendanceDraft>) => setDrafts((current) => ({
    ...current,
    [accountId]: { ...(current[accountId] ?? { statusCode: 'present', notes: '' }), ...change },
  }));

  return <>
    <section className="mb-5 grid grid-cols-3 gap-3">
      <SummaryCard label="Recorded" value={records.length.toString()} />
      <SummaryCard label="Present" value={present.toString()} />
      <SummaryCard label="Absent" value={absent.toString()} />
    </section>
    {(notice || error || localError) && <Notice message={notice ?? error ?? localError ?? ''} success={Boolean(notice)} />}
    <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
      <div className="border-b border-slate-100 p-4">
        <label className="block text-sm font-semibold text-slate-700">Attendance date<input id="attendance-date" type="date" value={businessDate} onChange={(event) => setBusinessDate(event.target.value)} className="mt-1.5 w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5 outline-none focus:border-emerald-600" /></label>
        <p className="mt-2 text-sm leading-5 text-slate-600">Saving again for the same employee and date revises the earlier mark while preserving its history.</p>
      </div>
      <div id="native-attendance-list" className="divide-y divide-slate-100">
        {loading && <p className="p-5 text-sm text-slate-500"><RefreshCw className="mr-2 inline h-4 w-4 animate-spin" />Loading attendance…</p>}
        {!loading && employees.map((employee) => {
          const draft = drafts[employee.accountId] ?? { statusCode: 'present', notes: '' };
          const existing = records.find((item) => item.employeeAccountId === employee.accountId);
          return <article key={employee.accountId} data-attendance-employee-id={employee.accountId} className="p-4">
            <div className="mb-3 flex items-center justify-between gap-3"><div><h2 className="font-bold text-slate-900">{employee.name}</h2><p className="text-xs text-slate-500">{existing ? 'Already recorded — save to revise' : 'Not recorded yet'}</p></div><CalendarCheck2 className="h-5 w-5 text-emerald-700" /></div>
            <div className="grid gap-3 sm:grid-cols-[1fr_2fr_auto] sm:items-end">
              <label className="text-sm font-semibold text-slate-700">Status<select id={`attendance-status-${employee.accountId}`} value={draft.statusCode} onChange={(event) => updateDraft(employee.accountId, { statusCode: event.target.value as AttendanceStatusCode })} className="mt-1.5 w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5"><option value="present">Present</option><option value="absent">Absent</option><option value="half_day">Half day</option></select></label>
              <label className="text-sm font-semibold text-slate-700">Note (optional)<input id={`attendance-notes-${employee.accountId}`} value={draft.notes} onChange={(event) => updateDraft(employee.accountId, { notes: event.target.value })} className="mt-1.5 w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5" /></label>
              <button id={`save-attendance-${employee.accountId}`} disabled={busy} onClick={() => void onRecord(employee.accountId, businessDate, draft.statusCode, draft.notes).then(load).catch(() => undefined)} className="rounded-xl bg-emerald-700 px-4 py-2.5 text-sm font-semibold text-white disabled:opacity-50">{existing ? 'Revise' : 'Save'}</button>
            </div>
          </article>;
        })}
        {!loading && employees.length === 0 && <p className="p-5 text-sm text-slate-500">No active employees are available. Ask a manager to add or restore an employee first.</p>}
      </div>
    </section>
  </>;
}

function todayLocal(): string {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function isAttendanceStatus(value: string | undefined): value is AttendanceStatusCode {
  return value === 'present' || value === 'absent' || value === 'half_day';
}
