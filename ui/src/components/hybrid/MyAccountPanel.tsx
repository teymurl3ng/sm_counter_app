import { useState, type FormEvent } from 'react';
import { KeyRound } from 'lucide-react';
import type { CurrentSessionDto } from '../../bridge/contract.generated';
import { Notice, TextInput } from './HybridUi';
import { roleExplanation, roleLabel } from './securityPresentation';

export function MyAccountPanel({ currentUser, busy, error, onChange }: {
  currentUser: CurrentSessionDto;
  busy: boolean;
  error: string | null;
  onChange(currentPassphrase: string, newPassphrase: string): Promise<void>;
}) {
  const [current, setCurrent] = useState('');
  const [next, setNext] = useState('');
  const [confirm, setConfirm] = useState('');
  const [localError, setLocalError] = useState<string | null>(null);
  const submit = (event: FormEvent) => {
    event.preventDefault();
    if (next !== confirm) return setLocalError('The two new passphrases do not match.');
    setLocalError(null);
    void onChange(current, next).catch(() => undefined).finally(() => { setCurrent(''); setNext(''); setConfirm(''); });
  };
  return <div className="grid gap-4 md:grid-cols-2">
    <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <p className="text-xs font-bold uppercase tracking-wide text-emerald-700">Your app account</p>
      <h2 className="mt-2 text-xl font-bold">{currentUser.displayName}</h2>
      <p className="mt-1 font-mono text-sm text-slate-500">{currentUser.loginName}</p>
      <div className="mt-4 rounded-xl bg-slate-50 p-4"><p className="font-bold">{roleLabel(currentUser.roleCode)}</p><p className="mt-1 text-sm leading-6 text-slate-600">{roleExplanation(currentUser.roleCode as Parameters<typeof roleExplanation>[0])}</p></div>
    </section>
    <form id="change-own-passphrase-form" onSubmit={submit} className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <div className="mb-4 flex items-center gap-3"><KeyRound className="h-6 w-6 text-emerald-700" /><div><h2 className="font-bold">Change your passphrase</h2><p className="text-sm text-slate-500">You will sign in again afterward.</p></div></div>
      {(localError ?? error) && <Notice message={localError ?? error ?? ''} />}
      <TextInput id="current-passphrase" label="Current passphrase" value={current} onChange={setCurrent} type="password" autoComplete="current-password" />
      <TextInput id="new-passphrase" label="New passphrase (at least 15 characters)" value={next} onChange={setNext} type="password" autoComplete="new-password" />
      <TextInput id="confirm-new-passphrase" label="Repeat new passphrase" value={confirm} onChange={setConfirm} type="password" autoComplete="new-password" />
      <button id="change-own-passphrase-button" disabled={busy} className="w-full rounded-xl bg-emerald-700 px-4 py-3 font-semibold text-white disabled:opacity-60">{busy ? 'Changing…' : 'Change passphrase'}</button>
    </form>
  </div>;
}
