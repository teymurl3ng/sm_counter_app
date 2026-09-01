import { useState, type FormEvent } from 'react';
import { EyeOff, KeyRound } from 'lucide-react';
import { Dialog, Notice, TextInput } from './HybridUi';
import type { PrivateSetup } from './securityModels';

export function PrivateCredentialSetup({ setup, busy, error, onCancel, onComplete }: {
  setup: PrivateSetup;
  busy: boolean;
  error: string | null;
  onCancel(): void;
  onComplete(passphrase: string): Promise<void>;
}) {
  const [handedOver, setHandedOver] = useState(false);
  const [passphrase, setPassphrase] = useState('');
  const [confirm, setConfirm] = useState('');
  const [localError, setLocalError] = useState<string | null>(null);
  const submit = (event: FormEvent) => {
    event.preventDefault();
    if (passphrase !== confirm) return setLocalError('The two passphrases do not match.');
    setLocalError(null);
    void onComplete(passphrase).catch(() => undefined).finally(() => { setPassphrase(''); setConfirm(''); });
  };
  return <Dialog title={`Private setup for ${setup.displayName}`} onClose={onCancel}>
    {!handedOver ? <div className="text-center">
      <EyeOff className="mx-auto h-12 w-12 text-emerald-700" />
      <h3 className="mt-4 text-lg font-bold">Hand the device to this staff member</h3>
      <p className="mt-2 text-sm leading-6 text-slate-600">Their passphrase is private. The owner or administrator should look away and must not ask them to share it.</p>
      <button id="private-setup-ready" onClick={() => setHandedOver(true)} className="mt-6 w-full rounded-xl bg-emerald-700 px-4 py-3 font-semibold text-white">The staff member has the device</button>
      <button id="cancel-private-setup" onClick={onCancel} className="mt-2 w-full rounded-xl px-4 py-2 text-sm font-semibold text-slate-600">Finish later</button>
    </div> : <form onSubmit={submit}>
      <div className="mb-5 flex gap-3 rounded-xl bg-emerald-50 p-4"><KeyRound className="h-6 w-6 shrink-0 text-emerald-700" /><p className="text-sm leading-6 text-emerald-900">Choose a private passphrase of at least 15 characters. Do not tell it to anyone.</p></div>
      {(localError ?? error) && <Notice message={localError ?? error ?? ''} />}
      <TextInput id="private-passphrase" label="Your new passphrase" value={passphrase} onChange={setPassphrase} type="password" autoComplete="new-password" />
      <TextInput id="private-confirm-passphrase" label="Repeat your new passphrase" value={confirm} onChange={setConfirm} type="password" autoComplete="new-password" />
      <button id="complete-enrollment-button" disabled={busy} className="w-full rounded-xl bg-emerald-700 px-4 py-3 font-semibold text-white disabled:opacity-60">{busy ? 'Saving privately…' : 'Save my private passphrase'}</button>
    </form>}
  </Dialog>;
}
