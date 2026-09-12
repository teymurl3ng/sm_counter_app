import { KeyRound, ShieldCheck, Smartphone } from 'lucide-react';
import type { QuickUnlockAccountDto, QuickUnlockStatusDto } from '../../bridge/contract.generated';
import { TextInput } from './HybridUi';
import type { ReauthenticationProof } from './quickUnlockBridgeActions';

export function ReauthenticationFields({ idPrefix, label, proof, onChange, account, status, allowPin = true }: {
  idPrefix: string;
  label: string;
  proof: ReauthenticationProof;
  onChange(value: ReauthenticationProof): void;
  account: QuickUnlockAccountDto | null;
  status: QuickUnlockStatusDto | null;
  allowPin?: boolean;
}) {
  const deviceAvailable = account?.deviceAuthenticationEnabled === true && status?.deviceAuthenticationAvailable === true;
  const pinAvailable = allowPin && account?.pinEnabled === true;
  const hasQuickChoice = deviceAvailable || pinAvailable;
  return <div className="mb-4">
    <p className="mb-2 text-sm font-semibold text-slate-700">{label}</p>
    {hasQuickChoice && <div role="group" aria-label={label} className="mb-3 grid gap-2 sm:grid-cols-3">
      <MethodButton id={`${idPrefix}-passphrase-method`} active={proof.method === 'passphrase'} onClick={() => onChange({ method: 'passphrase', secret: '' })} icon={<ShieldCheck className="h-4 w-4" aria-hidden="true" />}>Passphrase</MethodButton>
      {deviceAvailable && <MethodButton id={`${idPrefix}-device-method`} active={proof.method === 'device'} onClick={() => onChange({ method: 'device', secret: '' })} icon={<Smartphone className="h-4 w-4" aria-hidden="true" />}>Phone security</MethodButton>}
      {pinAvailable && <MethodButton id={`${idPrefix}-pin-method`} active={proof.method === 'pin'} onClick={() => onChange({ method: 'pin', secret: '' })} icon={<KeyRound className="h-4 w-4" aria-hidden="true" />}>Quick PIN</MethodButton>}
    </div>}
    {proof.method === 'passphrase' ? <TextInput id={`${idPrefix}-passphrase`} label="Current account passphrase" value={proof.secret} onChange={(secret) => onChange({ method: 'passphrase', secret })} type="password" autoComplete="current-password" />
      : proof.method === 'pin' ? <TextInput id={`${idPrefix}-pin`} label="Quick PIN" value={proof.secret} onChange={(secret) => onChange({ method: 'pin', secret: secret.replace(/\D/g, '').slice(0, 12) })} type="password" inputMode="numeric" autoComplete="off" />
        : <p className="rounded-xl bg-emerald-50 p-3 text-sm leading-6 text-emerald-950">Android will ask for {status?.deviceAuthenticationLabel ?? 'phone security'} after you continue.</p>}
  </div>;
}

function MethodButton({ id, active, onClick, icon, children }: { id: string; active: boolean; onClick(): void; icon: React.ReactNode; children: React.ReactNode }) {
  return <button id={id} type="button" aria-pressed={active} onClick={onClick} className={`flex min-h-12 items-center justify-center gap-2 rounded-xl border px-3 py-2 text-sm font-semibold ${active ? 'border-emerald-600 bg-emerald-50 text-emerald-900' : 'border-slate-200 text-slate-600'}`}>{icon}{children}</button>;
}

export function reauthenticationReady(proof: ReauthenticationProof): boolean {
  return proof.method === 'device' || proof.secret.length > 0;
}
