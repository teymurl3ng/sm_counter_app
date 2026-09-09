import { useEffect, useState } from 'react';
import type { AppRoleCode, CurrentSessionDto, EmployeeDto, QuickUnlockStatusDto, UserAdministrationDto } from '../../bridge/contract.generated';
import { FilterButton, Notice } from './HybridUi';
import { MyAccountPanel } from './MyAccountPanel';
import { PrivateCredentialSetup } from './PrivateCredentialSetup';
import { SecurityAuditPanel } from './SecurityAuditPanel';
import type { SecurityActions } from './securityBridgeActions';
import type { NewUserDraft, PrivateSetup, SecurityArea, SecurityBundle } from './securityModels';
import { StaffAccountsPanel } from './StaffAccountsPanel';
import { errorMessage } from './hybridModels';

export function SecurityWorkspace({ currentUser, employees, actions, quickUnlockStatus, canManageUsers, canViewAudit, onChangeOwnPassphrase, onEnrollQuickUnlock, onRemoveQuickUnlock }: {
  currentUser: CurrentSessionDto;
  employees: ReadonlyArray<EmployeeDto>;
  actions: SecurityActions;
  quickUnlockStatus: QuickUnlockStatusDto | null;
  canManageUsers: boolean;
  canViewAudit: boolean;
  onChangeOwnPassphrase(currentPassphrase: string, newPassphrase: string): Promise<void>;
  onEnrollQuickUnlock(currentPassphrase: string, enableDeviceAuthentication: boolean, pin: string | null): Promise<void>;
  onRemoveQuickUnlock(currentPassphrase: string): Promise<void>;
}) {
  const [area, setArea] = useState<SecurityArea>(canManageUsers ? 'staff' : 'account');
  const [bundle, setBundle] = useState<SecurityBundle | null>(null);
  const [setup, setSetup] = useState<PrivateSetup | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  useEffect(() => {
    if (!canManageUsers && !canViewAudit) return;
    let cancelled = false;
    setBusy(true);
    void actions.load().then((loaded) => { if (!cancelled) setBundle(loaded); }).catch((failure) => { if (!cancelled) setError(errorMessage(failure)); }).finally(() => { if (!cancelled) setBusy(false); });
    return () => { cancelled = true; };
  }, [actions, canManageUsers, canViewAudit]);

  const run = async <T,>(operation: () => Promise<T>, success: string): Promise<T> => {
    setBusy(true); setError(null); setNotice(null);
    try { const result = await operation(); setNotice(success); return result; }
    catch (failure) { setError(errorMessage(failure)); throw failure; }
    finally { setBusy(false); }
  };
  const begin = async (draft: NewUserDraft, passphrase: string) => {
    const result = await run(() => actions.beginEnrollment(draft, passphrase), 'Account created. Complete the passphrase privately now, or finish later.');
    setBundle(result.bundle); setSetup({ challenge: result.challenge, displayName: draft.displayName, purpose: 'new' });
  };
  const userSetup = (user: UserAdministrationDto, challenge: PrivateSetup['challenge'], purpose: PrivateSetup['purpose']) => setSetup({ challenge, displayName: user.displayName, purpose });
  const resume = async (user: UserAdministrationDto, passphrase: string) => userSetup(user, await run(() => actions.resumeEnrollment(user.userId, passphrase), 'Private setup is ready.'), 'resume');
  const reset = async (user: UserAdministrationDto, passphrase: string) => {
    const result = await run(() => actions.beginCredentialReset(user.userId, passphrase), `${user.displayName} must choose a new private passphrase.`);
    setBundle(result.bundle); userSetup(user, result.challenge, 'reset');
  };
  const complete = async (passphrase: string) => {
    if (!setup) return;
    const loaded = await run(() => actions.completeEnrollment(setup.challenge.enrollmentToken, passphrase), 'Private passphrase saved. This account can now sign in.');
    setBundle(loaded); setSetup(null);
  };
  const updateRole = async (user: UserAdministrationDto, role: AppRoleCode, passphrase: string) => setBundle(await run(() => actions.changeRole(user.userId, role, passphrase), `${user.displayName}'s role was changed.`));
  const updateEnabled = async (user: UserAdministrationDto, enabled: boolean, passphrase: string) => setBundle(await run(() => actions.setEnabled(user.userId, enabled, passphrase), `${user.displayName}'s account was ${enabled ? 'enabled' : 'disabled'}.`));
  const enrollQuickUnlock = (passphrase: string, enableDeviceAuthentication: boolean, pin: string | null) => run(
    () => onEnrollQuickUnlock(passphrase, enableDeviceAuthentication, pin),
    'Quick unlock is ready on this phone.',
  );
  const removeQuickUnlock = (passphrase: string) => run(
    () => onRemoveQuickUnlock(passphrase),
    'Quick unlock was removed from this phone.',
  );

  return <div>
    <div className="mb-5 grid grid-flow-col auto-cols-fr rounded-xl bg-slate-100 p-1">
      <FilterButton id="security-account-tab" active={area === 'account'} onClick={() => setArea('account')}>My account</FilterButton>
      {canManageUsers && <FilterButton id="security-staff-tab" active={area === 'staff'} onClick={() => setArea('staff')}>Staff accounts</FilterButton>}
      {canViewAudit && <FilterButton id="security-audit-tab" active={area === 'audit'} onClick={() => setArea('audit')}>Security history</FilterButton>}
    </div>
    {notice && <Notice message={notice} success />}
    {area === 'account' ? <MyAccountPanel currentUser={currentUser} quickUnlockStatus={quickUnlockStatus} busy={busy} error={error} onChange={onChangeOwnPassphrase} onEnrollQuickUnlock={enrollQuickUnlock} onRemoveQuickUnlock={removeQuickUnlock} />
      : !bundle ? <p className="rounded-2xl border border-slate-200 bg-white p-5 text-sm text-slate-500">{busy ? 'Loading protected account records…' : error ?? 'Account records are unavailable.'}</p>
      : area === 'staff' && canManageUsers ? <StaffAccountsPanel currentUser={currentUser} users={bundle.users} employees={employees} busy={busy} error={error} onCreate={begin} onResume={resume} onRole={updateRole} onEnabled={updateEnabled} onReset={reset} />
      : <SecurityAuditPanel entries={bundle.audit} users={bundle.users} />}
    {setup && <PrivateCredentialSetup setup={setup} busy={busy} error={error} onCancel={() => setSetup(null)} onComplete={complete} />}
  </div>;
}
