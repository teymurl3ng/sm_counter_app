import { useState, type FormEvent } from 'react';
import type { AppRoleCode, CurrentSessionDto, EmployeeDto, UserAdministrationDto } from '../../bridge/contract.generated';
import { Dialog, TextInput } from './HybridUi';
import { roleLabel } from './securityPresentation';

type Action = 'resume' | 'role' | 'enable' | 'disable' | 'reset';

export function StaffUserCard({ currentUser, user, employees, roles, busy, onResume, onRole, onEnabled, onReset }: {
  currentUser: CurrentSessionDto;
  user: UserAdministrationDto;
  employees: ReadonlyArray<EmployeeDto>;
  roles: ReadonlyArray<AppRoleCode>;
  busy: boolean;
  onResume(user: UserAdministrationDto, passphrase: string): Promise<void>;
  onRole(user: UserAdministrationDto, role: AppRoleCode, passphrase: string): Promise<void>;
  onEnabled(user: UserAdministrationDto, enabled: boolean, passphrase: string): Promise<void>;
  onReset(user: UserAdministrationDto, passphrase: string): Promise<void>;
}) {
  const [action, setAction] = useState<Action | null>(null);
  const [passphrase, setPassphrase] = useState('');
  const [nextRole, setNextRole] = useState<AppRoleCode>((roles.includes(user.roleCode as AppRoleCode) ? user.roleCode : roles[0]) as AppRoleCode);
  const targetPrivileged = user.roleCode === 'owner' || user.roleCode === 'admin';
  const canControl = currentUser.roleCode === 'owner' || !targetPrivileged;
  const active = user.statusCode === 'active';
  const employee = employees.find((item) => item.accountId === user.employeeAccountId);
  const status = !active ? 'Disabled' : user.setupRequired ? 'Waiting for private setup' : 'Ready';
  const submit = (event: FormEvent) => {
    event.preventDefault();
    const operation = action === 'resume' ? onResume(user, passphrase)
      : action === 'role' ? onRole(user, nextRole, passphrase)
      : action === 'enable' ? onEnabled(user, true, passphrase)
      : action === 'disable' ? onEnabled(user, false, passphrase)
      : onReset(user, passphrase);
    void operation.then(() => setAction(null)).catch(() => undefined).finally(() => setPassphrase(''));
  };
  return <article className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
    <div className="flex flex-wrap items-start justify-between gap-3"><div><h3 className="font-bold">{user.displayName}</h3><p className="font-mono text-xs text-slate-500">{user.loginName}</p></div><span className={`rounded-full px-2.5 py-1 text-xs font-bold ${active && !user.setupRequired ? 'bg-emerald-50 text-emerald-800' : user.setupRequired ? 'bg-amber-50 text-amber-800' : 'bg-slate-100 text-slate-600'}`}>{status}</span></div>
    <p className="mt-3 text-sm text-slate-600">{roleLabel(user.roleCode)}{employee ? ` · Linked to ${employee.name}` : ' · No employee link'}</p>
    {!canControl && <p className="mt-3 rounded-xl bg-slate-50 p-3 text-xs leading-5 text-slate-600">Only an owner can change this powerful account.</p>}
    {canControl && <div className="mt-4 flex flex-wrap gap-2">
      {active && user.setupRequired && <ActionButton id={`resume-user-${user.userId}`} onClick={() => setAction('resume')}>Finish private setup</ActionButton>}
      {active && !user.setupRequired && <ActionButton id={`reset-user-${user.userId}`} onClick={() => setAction('reset')}>Reset passphrase</ActionButton>}
      <ActionButton id={`role-user-${user.userId}`} onClick={() => setAction('role')}>Change role</ActionButton>
      {user.userId !== currentUser.userId && <ActionButton id={`${active ? 'disable' : 'enable'}-user-${user.userId}`} onClick={() => setAction(active ? 'disable' : 'enable')}>{active ? 'Disable' : 'Enable'}</ActionButton>}
    </div>}
    {action && <Dialog title={actionTitle(action, user.displayName)} onClose={() => setAction(null)}><form onSubmit={submit}>
      {action === 'role' && <label className="mb-4 block text-sm font-semibold text-slate-700">New app role<select id="change-user-role" value={nextRole} onChange={(event) => setNextRole(event.target.value as AppRoleCode)} className="mt-1.5 w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5">{roles.map((role) => <option key={role} value={role}>{roleLabel(role)}</option>)}</select></label>}
      <p className="mb-4 text-sm leading-6 text-slate-600">Confirm this sensitive action with your own passphrase.</p>
      <TextInput id="staff-action-passphrase" label="Your current passphrase" value={passphrase} onChange={setPassphrase} type="password" autoComplete="current-password" />
      <button id="confirm-staff-action" disabled={busy} className="w-full rounded-xl bg-emerald-700 px-4 py-3 font-semibold text-white disabled:opacity-60">{busy ? 'Please wait…' : 'Confirm'}</button>
    </form></Dialog>}
  </article>;
}

function ActionButton({ id, onClick, children }: { id: string; onClick(): void; children: string }) {
  return <button id={id} onClick={onClick} className="rounded-lg border border-slate-300 px-3 py-2 text-xs font-bold text-slate-700">{children}</button>;
}

function actionTitle(action: Action, name: string): string {
  if (action === 'resume') return `Private setup for ${name}`;
  if (action === 'role') return `Change ${name}'s role`;
  if (action === 'reset') return `Reset ${name}'s passphrase`;
  return `${action === 'enable' ? 'Enable' : 'Disable'} ${name}`;
}
