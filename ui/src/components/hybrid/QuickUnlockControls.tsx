import { useState, type FormEvent } from 'react';
import { Fingerprint, LockKeyhole } from 'lucide-react';
import type { CurrentSessionDto, QuickUnlockStatusDto } from '../../bridge/contract.generated';
import { Notice, TextInput } from './HybridUi';

export function QuickUnlockControls({ currentUser, status, busy, error, onEnroll, onRemove }: {
  currentUser: CurrentSessionDto;
  status: QuickUnlockStatusDto | null;
  busy: boolean;
  error: string | null;
  onEnroll(currentPassphrase: string, enableDeviceAuthentication: boolean, pin: string | null): Promise<void>;
  onRemove(currentPassphrase: string): Promise<void>;
}) {
  const enrolled = status?.accounts.find((account) => account.loginName === currentUser.loginName) ?? null;
  const [useDevice, setUseDevice] = useState(status?.deviceAuthenticationAvailable ?? false);
  const [usePin, setUsePin] = useState(false);
  const [pin, setPin] = useState('');
  const [confirmPin, setConfirmPin] = useState('');
  const [passphrase, setPassphrase] = useState('');
  const [localError, setLocalError] = useState<string | null>(null);

  const clearSecrets = () => { setPin(''); setConfirmPin(''); setPassphrase(''); };
  const enroll = (event: FormEvent) => {
    event.preventDefault();
    if (!useDevice && !usePin) return setLocalError('Choose phone security, a Quick PIN, or both.');
    if (usePin && pin !== confirmPin) return setLocalError('The two Quick PINs do not match.');
    if (usePin && !/^\d{4,12}$/.test(pin)) return setLocalError('Quick PIN must contain 4 to 12 digits. Six digits are recommended.');
    setLocalError(null);
    void onEnroll(passphrase, useDevice, usePin ? pin : null).catch(() => undefined).finally(clearSecrets);
  };
  const remove = (event: FormEvent) => {
    event.preventDefault();
    setLocalError(null);
    void onRemove(passphrase).catch(() => undefined).finally(clearSecrets);
  };

  return <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm md:col-span-2">
    <div className="mb-4 flex items-start gap-3"><Fingerprint className="mt-0.5 h-6 w-6 shrink-0 text-emerald-700" aria-hidden="true" /><div><h2 className="font-bold">Quick unlock on this phone</h2><p className="mt-1 text-sm leading-6 text-slate-600">Open your Saaf account with phone security or a short PIN. This setup stays on this phone and cannot open a backup on another phone.</p></div></div>
    {(localError ?? error) && <Notice message={localError ?? error ?? ''} />}
    {!status ? <p className="text-sm text-slate-600">Checking phone security…</p> : enrolled ? <form id="remove-quick-unlock-form" onSubmit={remove}>
      <div className="mb-4 rounded-xl bg-emerald-50 p-4 text-sm leading-6 text-emerald-950"><strong>Quick unlock is on for {enrolled.displayName}.</strong><br />{[enrolled.deviceAuthenticationEnabled ? status.deviceAuthenticationLabel : '', enrolled.pinEnabled ? 'Quick PIN' : ''].filter(Boolean).join(' and ')}</div>
      <p className="mb-4 text-sm leading-6 text-slate-600">To change the enabled methods, remove this setup and create it again. Your full passphrase always remains available.</p>
      <TextInput id="remove-quick-unlock-passphrase" label="Current full passphrase" value={passphrase} onChange={setPassphrase} type="password" autoComplete="current-password" />
      <button id="remove-quick-unlock-button" disabled={busy || !passphrase} className="w-full rounded-xl border border-rose-300 px-4 py-3 font-semibold text-rose-800 disabled:opacity-50">{busy ? 'Removing…' : 'Remove Quick unlock from this phone'}</button>
    </form> : <form id="enroll-quick-unlock-form" onSubmit={enroll}>
      <label className="mb-3 flex min-h-12 items-start gap-3 rounded-xl bg-slate-50 p-3 text-sm"><input id="quick-unlock-use-device" type="checkbox" checked={useDevice} disabled={!status.deviceAuthenticationAvailable} onChange={(event) => setUseDevice(event.target.checked)} className="mt-1 h-4 w-4" /><span><strong>{status.deviceAuthenticationLabel}</strong><br /><span className="text-slate-600">{status.deviceAuthenticationAvailable ? 'Uses this phone’s protected security screen.' : 'Set up a secure phone lock to make this available.'}</span></span></label>
      <label className="mb-4 flex min-h-12 items-start gap-3 rounded-xl bg-slate-50 p-3 text-sm"><input id="quick-unlock-use-pin" type="checkbox" checked={usePin} onChange={(event) => setUsePin(event.target.checked)} className="mt-1 h-4 w-4" /><span><strong>Quick PIN</strong><br /><span className="text-slate-600">Choose 4–12 digits. Six digits are recommended.</span></span></label>
      {usePin && <div className="grid gap-0 sm:grid-cols-2 sm:gap-3"><TextInput id="quick-unlock-pin" label="New Quick PIN" value={pin} onChange={(value) => setPin(value.replace(/\D/g, '').slice(0, 12))} type="password" inputMode="numeric" autoComplete="new-password" /><TextInput id="quick-unlock-pin-confirm" label="Repeat Quick PIN" value={confirmPin} onChange={(value) => setConfirmPin(value.replace(/\D/g, '').slice(0, 12))} type="password" inputMode="numeric" autoComplete="new-password" /></div>}
      <TextInput id="enroll-quick-unlock-passphrase" label="Current full passphrase" value={passphrase} onChange={setPassphrase} type="password" autoComplete="current-password" />
      <p className="mb-4 flex items-start gap-2 text-sm leading-6 text-slate-600"><LockKeyhole className="mt-1 h-4 w-4 shrink-0" aria-hidden="true" />Your passphrase confirms setup. It is not saved in Quick unlock.</p>
      <button id="enroll-quick-unlock-button" disabled={busy || !passphrase || (!useDevice && !usePin)} className="w-full rounded-xl bg-emerald-700 px-4 py-3 font-semibold text-white disabled:opacity-50">{busy ? 'Setting up…' : 'Set up Quick unlock'}</button>
    </form>}
  </section>;
}
