import { useState, type FormEvent, type ReactNode } from 'react';
import { ShieldCheck } from 'lucide-react';
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

export function Login({ busy, error, notice, onSubmit, onRecover }: {
  busy: boolean;
  error: string | null;
  notice: string | null;
  onSubmit(login: string, passphrase: string): Promise<void>;
  onRecover(login: string, recoveryCode: string, newPassphrase: string): Promise<void>;
}) {
  const [login, setLogin] = useState('');
  const [passphrase, setPassphrase] = useState('');
  const [recovering, setRecovering] = useState(false);
  const [recoveryCode, setRecoveryCode] = useState('');
  const [newPassphrase, setNewPassphrase] = useState('');
  const [confirm, setConfirm] = useState('');
  const [localError, setLocalError] = useState<string | null>(null);
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
  return <AuthCard title="Sign in" description="Use your private Saaf Milk account." error={error} notice={notice} onSubmit={(event) => {
    event.preventDefault();
    void onSubmit(login, passphrase).finally(() => setPassphrase(''));
  }}>
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
