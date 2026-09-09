import { useState, type FormEvent, type ReactNode } from 'react';
import { Fingerprint, KeyRound, ShieldCheck } from 'lucide-react';
import type { QuickUnlockMethod, QuickUnlockStatusDto } from '../../bridge/contract.generated';
import { Notice, TextInput } from './HybridUi';

export function OwnerSetup({ busy, error, onSubmit }: { busy: boolean; error: string | null; onSubmit(login: string, display: string, passphrase: string): Promise<void> }) {
  const [login, setLogin] = useState('');
  const [display, setDisplay] = useState('');
  const [passphrase, setPassphrase] = useState('');
  const [confirm, setConfirm] = useState('');
  const [localError, setLocalError] = useState<string | null>(null);
  return <AuthCard title="Create the first owner" description="The owner can create staff accounts and control their permissions later." error={localError ?? error} onSubmit={(event) => {
    event.preventDefault();
    if (passphrase !== confirm) return setLocalError('The two passphrases do not match.');
    setLocalError(null);
    void onSubmit(login, display, passphrase).finally(() => { setPassphrase(''); setConfirm(''); });
  }}>
    <TextInput id="owner-display-name" label="Your name" value={display} onChange={setDisplay} autoComplete="name" />
    <TextInput id="owner-login-name" label="Login name" value={login} onChange={setLogin} autoComplete="username" />
    <TextInput id="owner-passphrase" label="Passphrase (at least 15 characters)" value={passphrase} onChange={setPassphrase} type="password" autoComplete="new-password" />
    <TextInput id="owner-passphrase-confirm" label="Repeat passphrase" value={confirm} onChange={setConfirm} type="password" autoComplete="new-password" />
    <button id="create-owner-button" disabled={busy} className="w-full rounded-xl bg-emerald-700 px-4 py-3 font-semibold text-white disabled:opacity-60">{busy ? 'Please wait…' : 'Create owner account'}</button>
  </AuthCard>;
}

export function Login({ busy, error, notice, quickUnlockStatus, onSubmit, onQuickSubmit, onRecover }: {
  busy: boolean;
  error: string | null;
  notice: string | null;
  quickUnlockStatus: QuickUnlockStatusDto | null;
  onSubmit(login: string, passphrase: string): Promise<void>;
  onQuickSubmit(login: string, method: QuickUnlockMethod, pin: string | null): Promise<void>;
  onRecover(login: string, recoveryCode: string, newPassphrase: string): Promise<void>;
}) {
  const [login, setLogin] = useState('');
  const [passphrase, setPassphrase] = useState('');
  const [selectedQuickLogin, setSelectedQuickLogin] = useState('');
  const [quickPin, setQuickPin] = useState('');
  const [recovering, setRecovering] = useState(false);
  const [recoveryCode, setRecoveryCode] = useState('');
  const [newPassphrase, setNewPassphrase] = useState('');
  const [confirm, setConfirm] = useState('');
  const [localError, setLocalError] = useState<string | null>(null);
  const quickAccounts = quickUnlockStatus?.accounts ?? [];
  const quickAccount = quickAccounts.find((account) => account.loginName === selectedQuickLogin) ?? quickAccounts[0] ?? null;
  const quickSubmit = (method: QuickUnlockMethod) => {
    if (!quickAccount) return;
    if (method === 'pin' && !/^\d{4,12}$/.test(quickPin)) return setLocalError('Enter your 4–12 digit Quick PIN.');
    setLocalError(null);
    void onQuickSubmit(quickAccount.loginName, method, method === 'pin' ? quickPin : null).finally(() => setQuickPin(''));
  };
  if (recovering) return <AuthCard title="Recover the owner account" description="Use the latest recovery code you saved. A successful recovery replaces that code with a new one." error={localError ?? error} notice={notice} onSubmit={(event) => {
    event.preventDefault();
    if (newPassphrase !== confirm) return setLocalError('The two passphrases do not match.');
    setLocalError(null);
    void onRecover(login, recoveryCode, newPassphrase).finally(() => { setRecoveryCode(''); setNewPassphrase(''); setConfirm(''); });
  }}>
    <TextInput id="recovery-login-name" label="Owner login name" value={login} onChange={setLogin} autoComplete="username" />
    <TextInput id="recovery-code" label="Saved recovery code" value={recoveryCode} onChange={setRecoveryCode} autoComplete="off" />
    <TextInput id="recovery-new-passphrase" label="New passphrase (at least 15 characters)" value={newPassphrase} onChange={setNewPassphrase} type="password" autoComplete="new-password" />
    <TextInput id="recovery-confirm-passphrase" label="Repeat new passphrase" value={confirm} onChange={setConfirm} type="password" autoComplete="new-password" />
    <button id="recover-owner-button" disabled={busy} className="w-full rounded-xl bg-emerald-700 px-4 py-3 font-semibold text-white disabled:opacity-60">{busy ? 'Please wait…' : 'Recover owner account'}</button>
    <button id="cancel-recovery" type="button" disabled={busy} onClick={() => { setRecovering(false); setLocalError(null); }} className="mt-3 w-full rounded-xl px-4 py-2 text-sm font-semibold text-slate-600">Back to sign in</button>
  </AuthCard>;
  return <AuthCard title="Sign in" description="Choose your Saaf account, then use the quickest safe method available." error={localError ?? error} notice={notice} onSubmit={(event) => {
    event.preventDefault();
    void onSubmit(login, passphrase).finally(() => setPassphrase(''));
  }}>
    {quickAccount && <section aria-label="Quick unlock" className="mb-5 border-b border-slate-200 pb-5">
      {quickAccounts.length > 1 && <div className="mb-3 grid gap-2 sm:grid-cols-2">{quickAccounts.map((account) => <button key={account.loginName} type="button" aria-pressed={account.loginName === quickAccount.loginName} onClick={() => { setSelectedQuickLogin(account.loginName); setLogin(account.loginName); setQuickPin(''); }} className={`min-h-12 rounded-xl border px-3 py-2 text-left text-sm ${account.loginName === quickAccount.loginName ? 'border-emerald-600 bg-emerald-50 text-emerald-950' : 'border-slate-200 text-slate-700'}`}><strong>{account.displayName}</strong><br /><span className="text-xs opacity-75">{account.loginName}</span></button>)}</div>}
      <p className="mb-3 text-sm text-slate-600">Unlocking <strong className="text-slate-900">{quickAccount.displayName}</strong> on this phone.</p>
      {quickAccount.deviceAuthenticationEnabled && <button id="quick-device-login-button" type="button" disabled={busy || !quickUnlockStatus?.deviceAuthenticationAvailable} onClick={() => quickSubmit('device')} className="mb-3 flex min-h-12 w-full items-center justify-center gap-2 rounded-xl bg-emerald-700 px-4 py-3 font-semibold text-white disabled:opacity-50"><Fingerprint className="h-5 w-5" aria-hidden="true" />{quickUnlockStatus?.deviceAuthenticationLabel ?? 'Use phone security'}</button>}
      {quickAccount.pinEnabled && <div><label className="block text-sm font-semibold leading-5 text-slate-700">Quick PIN<input id="quick-login-pin" value={quickPin} onChange={(event) => setQuickPin(event.target.value.replace(/\D/g, '').slice(0, 12))} onKeyDown={(event) => { if (event.key === 'Enter') { event.preventDefault(); quickSubmit('pin'); } }} type="password" inputMode="numeric" autoComplete="off" className="mt-1.5 min-h-12 w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5 text-base outline-none focus:border-emerald-600 focus:ring-2 focus:ring-emerald-100" /></label><button id="quick-pin-login-button" type="button" disabled={busy || quickPin.length < 4} onClick={() => quickSubmit('pin')} className="mt-3 flex min-h-12 w-full items-center justify-center gap-2 rounded-xl border border-emerald-300 px-4 py-3 font-semibold text-emerald-800 disabled:opacity-50"><KeyRound className="h-5 w-5" aria-hidden="true" />Unlock with Quick PIN</button></div>}
      <p className="mt-4 text-center text-xs font-semibold uppercase tracking-wide text-slate-500">or use your full passphrase</p>
    </section>}
    <TextInput id="login-name" label="Login name" value={login} onChange={setLogin} autoComplete="username" />
    <TextInput id="login-passphrase" label="Passphrase" value={passphrase} onChange={setPassphrase} type="password" autoComplete="current-password" />
    <button id="login-button" disabled={busy} className="w-full rounded-xl bg-emerald-700 px-4 py-3 font-semibold text-white disabled:opacity-60">{busy ? 'Please wait…' : 'Sign in securely'}</button>
    <button id="recover-owner-link" type="button" disabled={busy} onClick={() => setRecovering(true)} className="mt-3 w-full rounded-xl px-4 py-2 text-sm font-semibold text-emerald-800">Owner forgot the passphrase?</button>
  </AuthCard>;
}

export function RecoveryCodeScreen({ recoveryCode, onSaved }: { recoveryCode: string; onSaved(): void }) {
  return (
    <main className="hybrid-screen bg-[#f7f6f0] px-5 py-10 text-slate-900">
      <section className="mx-auto max-w-lg rounded-3xl border border-amber-200 bg-white p-6 shadow-xl shadow-amber-950/5">
        <ShieldCheck className="mb-4 h-10 w-10 text-emerald-700" />
        <h1 className="text-2xl font-bold">Save the owner recovery code</h1>
        <p className="mt-2 text-sm leading-6 text-slate-600">Write this code on paper and keep it somewhere private. It is shown only once and can recover the owner account.</p>
        <div id="owner-recovery-code" className="my-6 break-all rounded-2xl bg-amber-50 p-5 font-mono text-lg font-bold tracking-wide text-amber-950">{recoveryCode}</div>
        <button id="recovery-saved" className="w-full rounded-xl bg-emerald-700 px-4 py-3 font-semibold text-white" onClick={onSaved}>I have saved it safely</button>
      </section>
    </main>
  );
}

function AuthCard({ title, description, error, notice = null, onSubmit, children }: { title: string; description: string; error: string | null; notice?: string | null; onSubmit(event: FormEvent): void; children: ReactNode }) {
  return <main className="hybrid-screen bg-[#f7f6f0] px-5 py-10 text-slate-900"><form onSubmit={onSubmit} className="mx-auto max-w-md rounded-3xl border border-slate-200 bg-white p-6 shadow-xl shadow-emerald-950/5"><ShieldCheck className="mb-4 h-10 w-10 text-emerald-700" /><p className="text-xs font-bold uppercase tracking-[0.2em] text-emerald-700">Saaf Milk</p><h1 className="mt-1 text-2xl font-bold">{title}</h1><p className="mb-6 mt-2 text-sm text-slate-600">{description}</p>{notice && <Notice message={notice} success />}{error && <Notice message={error} />}{children}</form></main>;
}
