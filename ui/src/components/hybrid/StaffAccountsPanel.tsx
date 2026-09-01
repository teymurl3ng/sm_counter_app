import { useMemo, useState, type FormEvent } from 'react';
import { UserPlus } from 'lucide-react';
import type { AppRoleCode, CurrentSessionDto, EmployeeDto, Int64String, UserAdministrationDto } from '../../bridge/contract.generated';
import { Notice, TextInput } from './HybridUi';
import { roleExplanation, roleLabel } from './securityPresentation';
import type { NewUserDraft } from './securityModels';
import { StaffUserCard } from './StaffUserCard';

const allRoles: ReadonlyArray<AppRoleCode> = ['owner', 'admin', 'manager', 'cashier', 'operator'];
const ordinaryRoles: ReadonlyArray<AppRoleCode> = ['manager', 'cashier', 'operator'];

export function StaffAccountsPanel({ currentUser, users, employees, busy, error, onCreate, onResume, onRole, onEnabled, onReset }: {
  currentUser: CurrentSessionDto;
  users: ReadonlyArray<UserAdministrationDto>;
  employees: ReadonlyArray<EmployeeDto>;
  busy: boolean;
  error: string | null;
  onCreate(draft: NewUserDraft, ownerPassphrase: string): Promise<void>;
  onResume(user: UserAdministrationDto, ownerPassphrase: string): Promise<void>;
  onRole(user: UserAdministrationDto, role: AppRoleCode, ownerPassphrase: string): Promise<void>;
  onEnabled(user: UserAdministrationDto, enabled: boolean, ownerPassphrase: string): Promise<void>;
  onReset(user: UserAdministrationDto, ownerPassphrase: string): Promise<void>;
}) {
  const owner = currentUser.roleCode === 'owner';
  const roles = owner ? allRoles : ordinaryRoles;
  const [loginName, setLoginName] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [roleCode, setRoleCode] = useState<AppRoleCode>(roles[0]);
  const [employeeId, setEmployeeId] = useState('');
  const [passphrase, setPassphrase] = useState('');
  const linked = useMemo(() => new Set(users.map((user) => user.employeeAccountId).filter(Boolean)), [users]);
  const availableEmployees = employees.filter((employee) => employee.active && (!linked.has(employee.accountId) || employee.accountId === employeeId));
  const privileged = roleCode === 'owner' || roleCode === 'admin';
  const submit = (event: FormEvent) => {
    event.preventDefault();
    const draft: NewUserDraft = { loginName, displayName, roleCode, employeeAccountId: employeeId ? employeeId as Int64String : null };
    void onCreate(draft, passphrase).then(() => { setLoginName(''); setDisplayName(''); setRoleCode(roles[0]); setEmployeeId(''); }).catch(() => undefined).finally(() => setPassphrase(''));
  };
  return <div className="space-y-4">
    <form id="new-user-form" onSubmit={submit} className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <div className="mb-4 flex items-center gap-3"><UserPlus className="h-6 w-6 text-emerald-700" /><div><h2 className="font-bold">Create an app account</h2><p className="text-sm text-slate-500">Create the identity, then hand over the device for private passphrase setup.</p></div></div>
      {error && <Notice message={error} />}
      <div className="grid gap-x-4 sm:grid-cols-2">
        <TextInput id="new-user-display-name" label="Staff member's name" value={displayName} onChange={setDisplayName} autoComplete="off" />
        <TextInput id="new-user-login-name" label="Login name" value={loginName} onChange={setLoginName} autoComplete="off" />
        <label className="mb-4 block text-sm font-semibold text-slate-700">App role<select id="new-user-role" value={roleCode} onChange={(event) => setRoleCode(event.target.value as AppRoleCode)} className="mt-1.5 w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5">{roles.map((role) => <option key={role} value={role}>{roleLabel(role)}</option>)}</select></label>
        <label className="mb-4 block text-sm font-semibold text-slate-700">Employee profile (optional)<select id="new-user-employee" value={employeeId} onChange={(event) => setEmployeeId(event.target.value)} className="mt-1.5 w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5"><option value="">No employee link</option>{availableEmployees.map((employee) => <option key={employee.accountId} value={employee.accountId}>{employee.name}</option>)}</select></label>
      </div>
      <p className="mb-4 rounded-xl bg-slate-50 p-3 text-sm leading-6 text-slate-600"><strong>{roleLabel(roleCode)}:</strong> {roleExplanation(roleCode)}</p>
      {privileged && <TextInput id="new-user-owner-passphrase" label="Your current passphrase (required for this powerful role)" value={passphrase} onChange={setPassphrase} type="password" autoComplete="current-password" />}
      <button id="begin-user-enrollment-button" disabled={busy} className="w-full rounded-xl bg-emerald-700 px-4 py-3 font-semibold text-white disabled:opacity-60">{busy ? 'Creating…' : 'Create account and begin private setup'}</button>
    </form>
    <section id="staff-account-list" className="space-y-3">
      <div><h2 className="text-lg font-bold">App accounts</h2><p className="text-sm text-slate-500">Employee records and app login accounts are separate. Linking them is optional.</p></div>
      {users.map((user) => <StaffUserCard key={user.userId} currentUser={currentUser} user={user} employees={employees} roles={roles} busy={busy} onResume={onResume} onRole={onRole} onEnabled={onEnabled} onReset={onReset} />)}
    </section>
  </div>;
}
