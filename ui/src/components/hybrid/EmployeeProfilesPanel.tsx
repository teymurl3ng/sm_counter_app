import { useMemo, useState } from 'react';
import { Archive, Edit3, RefreshCw, RotateCcw, Search, UserPlus } from 'lucide-react';
import type { EmployeeDto } from '../../bridge/contract.generated';
import { formatPaisa, paisaToRupeesInput, rupeesToPaisa } from '../../bridge/nativeBridge';
import { Dialog, FilterButton, Notice, SummaryCard, TextInput } from './HybridUi';
import { errorMessage, type EmployeeDraft } from './hybridModels';

export function EmployeeProfilesPanel({ busy, employees, error, notice, onSave, onSetActive }: {
  busy: boolean;
  employees: ReadonlyArray<EmployeeDto> | null;
  error: string | null;
  notice: string | null;
  onSave(draft: EmployeeDraft, employee?: EmployeeDto): Promise<void>;
  onSetActive(employee: EmployeeDto, active: boolean): Promise<void>;
}) {
  const [showInactive, setShowInactive] = useState(false);
  const [search, setSearch] = useState('');
  const [editing, setEditing] = useState<EmployeeDto | 'new' | null>(null);
  const [deactivateTarget, setDeactivateTarget] = useState<EmployeeDto | null>(null);
  const allEmployees = employees ?? [];
  const activeCount = allEmployees.filter((employee) => employee.active).length;
  const visibleEmployees = useMemo(() => {
    const query = search.trim().toLocaleLowerCase();
    return allEmployees.filter((employee) => employee.active !== showInactive && (!query ||
      employee.name.toLocaleLowerCase().includes(query) ||
      (employee.phone ?? '').toLocaleLowerCase().includes(query) ||
      (employee.roleLabel ?? '').toLocaleLowerCase().includes(query)));
  }, [allEmployees, search, showInactive]);

  return <>
    <section className="mb-5 grid grid-cols-2 gap-3">
      <SummaryCard label="Active employees" value={activeCount.toString()} />
      <SummaryCard label="Inactive employees" value={(allEmployees.length - activeCount).toString()} />
    </section>
    {(notice || error) && <Notice message={notice ?? error ?? ''} success={Boolean(notice)} />}
    <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
      <div className="border-b border-slate-100 p-4">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex rounded-xl bg-slate-100 p-1">
            <FilterButton id="active-employees-tab" active={!showInactive} onClick={() => setShowInactive(false)}>Active ({activeCount})</FilterButton>
            <FilterButton id="inactive-employees-tab" active={showInactive} onClick={() => setShowInactive(true)}>Inactive ({allEmployees.length - activeCount})</FilterButton>
          </div>
          <button id="add-employee-button" disabled={busy} onClick={() => setEditing('new')} className="flex items-center gap-2 rounded-xl bg-emerald-700 px-4 py-2.5 text-sm font-semibold text-white disabled:opacity-50"><UserPlus className="h-4 w-4" />Add employee</button>
        </div>
        <label className="relative mt-3 block"><Search className="absolute left-3 top-3 h-4 w-4 text-slate-400" /><input id="employee-search" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search name, phone, or job" className="w-full rounded-xl border border-slate-200 py-2.5 pl-9 pr-3 text-sm outline-none focus:border-emerald-600" /></label>
      </div>
      <div id="native-employee-list" className="divide-y divide-slate-100">
        {visibleEmployees.map((employee) => <EmployeeRow key={employee.accountId} busy={busy} employee={employee} onEdit={() => setEditing(employee)} onDeactivate={() => setDeactivateTarget(employee)} onRestore={() => onSetActive(employee, true)} />)}
        {employees === null && <p className="p-5 text-sm text-slate-500"><RefreshCw className="mr-2 inline h-4 w-4 animate-spin" />Loading protected employee records…</p>}
        {employees !== null && visibleEmployees.length === 0 && <p className="p-5 text-sm text-slate-500">{search.trim() ? 'No employees match this search.' : showInactive ? 'No inactive employees.' : 'No active employees yet.'}</p>}
      </div>
    </section>
    {editing && <EmployeeForm key={editing === 'new' ? 'new-employee' : editing.accountId} busy={busy} employee={editing === 'new' ? null : editing} onClose={() => setEditing(null)} onSave={async (draft) => { await onSave(draft, editing === 'new' ? undefined : editing); setEditing(null); }} />}
    {deactivateTarget && <Dialog title="Deactivate employee" onClose={() => setDeactivateTarget(null)}><div id="employee-deactivate-confirmation"><p className="text-sm leading-6 text-slate-600">Deactivate <strong>{deactivateTarget.name}</strong>? They will be removed from new attendance and salary selections, but their balance and complete history will remain safe.</p><div className="mt-5 flex gap-3"><button onClick={() => setDeactivateTarget(null)} className="flex-1 rounded-xl border border-slate-300 px-4 py-3 text-sm font-semibold">Keep active</button><button id="confirm-employee-deactivate" disabled={busy} onClick={() => void onSetActive(deactivateTarget, false).then(() => setDeactivateTarget(null)).catch(() => undefined)} className="flex-1 rounded-xl bg-amber-600 px-4 py-3 text-sm font-semibold text-white disabled:opacity-50">Deactivate</button></div></div></Dialog>}
  </>;
}

function EmployeeRow({ busy, employee, onEdit, onDeactivate, onRestore }: { busy: boolean; employee: EmployeeDto; onEdit(): void; onDeactivate(): void; onRestore(): Promise<void> }) {
  return <article data-employee-id={employee.accountId} className="p-4"><div className="flex items-start justify-between gap-3"><div className="min-w-0">
    <h2 className="font-bold text-slate-900">{employee.name}</h2>
    {employee.roleLabel && <p className="text-sm text-slate-600">Job: {employee.roleLabel}</p>}
    <p className="mt-1 text-sm font-semibold text-slate-700">Salary balance: {formatPaisa(employee.canonicalBalancePaisa)}</p>
    <p className="text-xs text-slate-400">Calculated from saved salary transactions; it cannot be edited here.</p>
    {employee.phone && <p className="mt-2 text-sm text-slate-600">Phone: {employee.phone}</p>}
    {employee.referenceSalaryPaisa !== null && <p className="text-sm text-slate-600">Reference salary: {formatPaisa(employee.referenceSalaryPaisa)}{employee.salaryPeriodCode ? ` / ${employee.salaryPeriodCode.replace('_', ' ')}` : ''}</p>}
    {employee.joinDate && <p className="text-sm text-slate-600">Joined: {employee.joinDate}</p>}
  </div><div className="flex shrink-0 gap-2"><button id={`edit-employee-${employee.accountId}`} disabled={busy} onClick={onEdit} className="rounded-lg border border-slate-200 p-2 text-slate-600 disabled:opacity-50" aria-label={`Edit ${employee.name}`}><Edit3 className="h-4 w-4" /></button>{employee.active ? <button id={`deactivate-employee-${employee.accountId}`} disabled={busy} onClick={onDeactivate} className="rounded-lg border border-amber-200 p-2 text-amber-700 disabled:opacity-50" aria-label={`Deactivate ${employee.name}`}><Archive className="h-4 w-4" /></button> : <button id={`restore-employee-${employee.accountId}`} disabled={busy} onClick={() => void onRestore().catch(() => undefined)} className="rounded-lg border border-emerald-200 p-2 text-emerald-700 disabled:opacity-50" aria-label={`Restore ${employee.name}`}><RotateCcw className="h-4 w-4" /></button>}</div></div></article>;
}

function EmployeeForm({ busy, employee, onClose, onSave }: { busy: boolean; employee: EmployeeDto | null; onClose(): void; onSave(draft: EmployeeDraft): Promise<void> }) {
  const [name, setName] = useState(employee?.name ?? '');
  const [roleLabel, setRoleLabel] = useState(employee?.roleLabel ?? '');
  const [phone, setPhone] = useState(employee?.phone ?? '');
  const [referenceSalaryRupees, setReferenceSalaryRupees] = useState(paisaToRupeesInput(employee?.referenceSalaryPaisa ?? null));
  const [salaryPeriodCode, setSalaryPeriodCode] = useState(employee?.salaryPeriodCode ?? 'monthly');
  const [joinDate, setJoinDate] = useState(employee?.joinDate ?? '');
  const [localError, setLocalError] = useState<string | null>(null);
  return <Dialog title={employee ? 'Edit employee' : 'Add employee'} onClose={onClose}><form onSubmit={(event) => { event.preventDefault(); setLocalError(null); try { if (referenceSalaryRupees.trim()) rupeesToPaisa(referenceSalaryRupees); } catch (failure) { setLocalError(errorMessage(failure)); return; } void onSave({ name, roleLabel, phone, referenceSalaryRupees, salaryPeriodCode, joinDate, active: employee?.active ?? true }).catch(() => undefined); }}>
    <TextInput id="employee-name" label="Employee name" value={name} onChange={setName} autoComplete="name" />
    <TextInput id="employee-role" label="Job or role (optional)" value={roleLabel} onChange={setRoleLabel} required={false} />
    <TextInput id="employee-phone" label="Phone (optional)" value={phone} onChange={setPhone} autoComplete="tel" required={false} />
    <TextInput id="employee-reference-salary" label="Reference salary in PKR (optional)" value={referenceSalaryRupees} onChange={setReferenceSalaryRupees} inputMode="decimal" required={false} />
    <label className="mb-4 block text-sm font-semibold text-slate-700">Salary period (optional)<select id="employee-salary-period" value={salaryPeriodCode} onChange={(event) => setSalaryPeriodCode(event.target.value)} className="mt-1.5 w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5"><option value="">Not set</option><option value="monthly">Monthly</option><option value="weekly">Weekly</option><option value="daily">Daily</option></select></label>
    <TextInput id="employee-join-date" label="Join date (optional)" value={joinDate} onChange={setJoinDate} type="date" required={false} />
    <p className="mb-4 rounded-xl bg-sky-50 px-3 py-2 text-xs leading-5 text-sky-800">This creates a staff record only. It does not create an app login. The owner controls app accounts and permissions separately.</p>
    {localError && <Notice message={localError} />}
    <button id="save-employee-button" disabled={busy} className="w-full rounded-xl bg-emerald-700 px-4 py-3 font-semibold text-white disabled:cursor-wait disabled:opacity-60">{busy ? 'Saving…' : employee ? 'Save employee' : 'Add employee'}</button>
  </form></Dialog>;
}
