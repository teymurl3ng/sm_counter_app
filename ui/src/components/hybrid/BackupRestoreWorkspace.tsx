import { useState, type FormEvent } from 'react';
import { DatabaseBackup, FileCheck2, ShieldAlert } from 'lucide-react';
import type { BackupInspectionDto, BackupRestoreResultDto } from '../../bridge/contract.generated';
import { NativeBridgeError } from '../../bridge/nativeBridge';
import type { BackupActions } from './backupBridgeActions';
import { Dialog, FilterButton, Notice, TextInput } from './HybridUi';
import { errorMessage } from './hybridModels';

type BackupArea = 'create' | 'restore';

export function BackupRestoreWorkspace({ actions, canRestore, onRestored }: {
  actions: BackupActions;
  canRestore: boolean;
  onRestored(ticket: BackupInspectionDto['restoreTicket']): Promise<BackupRestoreResultDto>;
}) {
  const [area, setArea] = useState<BackupArea>('create');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [inspection, setInspection] = useState<BackupInspectionDto | null>(null);
  const run = async <T,>(operation: () => Promise<T>): Promise<T | null> => {
    setBusy(true); setError(null); setNotice(null);
    try { return await operation(); }
    catch (failure) {
      if (failure instanceof NativeBridgeError && failure.code === 'user_cancelled') setNotice('No file was changed.');
      else setError(errorMessage(failure));
      return null;
    } finally { setBusy(false); }
  };
  return <div className="space-y-4">
    <section className="rounded-2xl border border-sky-200 bg-sky-50 p-4 text-sm leading-6 text-sky-950">
      <strong>Backup is a protected snapshot, not live synchronization.</strong> It can protect this shop or move a complete copy to another installation. It does not keep two working phones continuously updated.
    </section>
    {canRestore && <div className="flex rounded-xl bg-slate-100 p-1">
      <FilterButton id="backup-create-tab" active={area === 'create'} onClick={() => setArea('create')}>Create backup</FilterButton>
      <FilterButton id="backup-restore-tab" active={area === 'restore'} onClick={() => setArea('restore')}>Restore backup</FilterButton>
    </div>}
    {notice && <Notice message={notice} success />}{error && <Notice message={error} />}
    {area === 'create' ? <CreateBackupPanel actions={actions} busy={busy} run={run} onSuccess={(name) => setNotice(`Encrypted backup created: ${name}. Keep its backup passphrase somewhere safe.`)} />
      : <RestoreBackupPanel actions={actions} busy={busy} inspection={inspection} setInspection={setInspection} run={run} onRestored={onRestored} />}
  </div>;
}

function CreateBackupPanel({ actions, busy, run, onSuccess }: {
  actions: BackupActions; busy: boolean; run<T>(operation: () => Promise<T>): Promise<T | null>; onSuccess(name: string): void;
}) {
  const [accountPassphrase, setAccountPassphrase] = useState('');
  const [backupPassphrase, setBackupPassphrase] = useState('');
  const [confirm, setConfirm] = useState('');
  const [localError, setLocalError] = useState<string | null>(null);
  const submit = (event: FormEvent) => {
    event.preventDefault();
    if (backupPassphrase !== confirm) return setLocalError('The two backup passphrases do not match.');
    setLocalError(null);
    void run(() => actions.exportBackup(accountPassphrase, backupPassphrase)).then((result) => { if (result) onSuccess(result.displayName); })
      .finally(() => { setAccountPassphrase(''); setBackupPassphrase(''); setConfirm(''); });
  };
  return <form id="encrypted-backup-form" onSubmit={submit} className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
    <div className="mb-4 flex gap-3"><DatabaseBackup className="h-7 w-7 shrink-0 text-emerald-700" /><div><h2 className="font-bold">Create an encrypted backup file</h2><p className="mt-1 text-sm leading-6 text-slate-600">It includes shop records, users, and security history. Android will ask where to save it.</p></div></div>
    {localError && <Notice message={localError} />}
    <TextInput id="backup-account-passphrase" label="Your current account passphrase" value={accountPassphrase} onChange={setAccountPassphrase} type="password" autoComplete="current-password" />
    <TextInput id="backup-file-passphrase" label="New backup passphrase (at least 15 characters)" value={backupPassphrase} onChange={setBackupPassphrase} type="password" autoComplete="new-password" />
    <TextInput id="backup-file-passphrase-confirm" label="Repeat backup passphrase" value={confirm} onChange={setConfirm} type="password" autoComplete="new-password" />
    <p className="mb-4 rounded-xl bg-amber-50 p-3 text-sm leading-6 text-amber-950">The backup cannot be opened without this separate passphrase. The app does not save it for you.</p>
    <button id="create-encrypted-backup-button" disabled={busy} className="w-full rounded-xl bg-emerald-700 px-4 py-3 font-semibold text-white disabled:opacity-60">{busy ? 'Creating protected backup…' : 'Choose location and create backup'}</button>
  </form>;
}

function RestoreBackupPanel({ actions, busy, inspection, setInspection, run, onRestored }: {
  actions: BackupActions; busy: boolean; inspection: BackupInspectionDto | null; setInspection(value: BackupInspectionDto | null): void;
  run<T>(operation: () => Promise<T>): Promise<T | null>; onRestored(ticket: BackupInspectionDto['restoreTicket']): Promise<BackupRestoreResultDto>;
}) {
  const [accountPassphrase, setAccountPassphrase] = useState('');
  const [backupPassphrase, setBackupPassphrase] = useState('');
  const [confirming, setConfirming] = useState(false);
  const inspect = (event: FormEvent) => {
    event.preventDefault();
    void run(() => actions.inspectBackup(accountPassphrase, backupPassphrase)).then((result) => { if (result) setInspection(result); })
      .finally(() => { setAccountPassphrase(''); setBackupPassphrase(''); });
  };
  return <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
    <div className="mb-4 flex gap-3"><FileCheck2 className="h-7 w-7 shrink-0 text-emerald-700" /><div><h2 className="font-bold">Inspect before restoring</h2><p className="mt-1 text-sm leading-6 text-slate-600">The file is checked and decrypted by Android first. Nothing is replaced during inspection.</p></div></div>
    {!inspection ? <form id="inspect-backup-form" onSubmit={inspect}>
      <TextInput id="restore-account-passphrase" label="Your current owner passphrase" value={accountPassphrase} onChange={setAccountPassphrase} type="password" autoComplete="current-password" />
      <TextInput id="restore-file-passphrase" label="Backup file passphrase" value={backupPassphrase} onChange={setBackupPassphrase} type="password" autoComplete="off" />
      <button id="inspect-encrypted-backup-button" disabled={busy} className="w-full rounded-xl bg-emerald-700 px-4 py-3 font-semibold text-white disabled:opacity-60">{busy ? 'Checking backup…' : 'Choose and inspect backup'}</button>
    </form> : <div id="backup-inspection-result" className="space-y-3">
      <div className="rounded-xl bg-emerald-50 p-4"><p className="font-bold text-emerald-900">Backup passed its security checks</p><p className="mt-1 text-sm text-emerald-800">Created {formatDate(inspection.createdAt)} · Format {inspection.formatVersion} · {inspection.containsSecurityState ? 'Includes app users' : 'No app users'}</p></div>
      <div className="rounded-xl bg-rose-50 p-4 text-sm leading-6 text-rose-950"><strong>Restore fully replaces the current app data.</strong> Android creates a device-local safety snapshot first, then signs everyone out. Records entered after this backup was created will no longer be in the active copy.</div>
      <button id="open-restore-confirmation" disabled={busy || inspection.requiresEmptyInstallation} onClick={() => setConfirming(true)} className="w-full rounded-xl bg-rose-700 px-4 py-3 font-semibold text-white disabled:opacity-60">Review final restore warning</button>
      <button id="choose-different-backup" disabled={busy} onClick={() => setInspection(null)} className="w-full rounded-xl px-4 py-2 text-sm font-semibold text-slate-600">Choose a different backup</button>
    </div>}
    {confirming && inspection && <RestoreConfirmation busy={busy} onCancel={() => setConfirming(false)} onConfirm={async () => { const result = await run(() => onRestored(inspection.restoreTicket)); if (!result) setConfirming(false); }} />}
  </section>;
}

function RestoreConfirmation({ busy, onCancel, onConfirm }: { busy: boolean; onCancel(): void; onConfirm(): Promise<void> }) {
  const [checked, setChecked] = useState(false);
  return <Dialog title="Replace this installation?" onClose={onCancel}><ShieldAlert className="mb-3 h-10 w-10 text-rose-700" /><p className="text-sm leading-6 text-slate-700">This is not a merge. The inspected backup becomes the complete active copy and every signed-in user is logged out.</p><label className="my-5 flex items-start gap-3 rounded-xl bg-slate-50 p-3 text-sm"><input id="confirm-restore-checkbox" type="checkbox" checked={checked} onChange={(event) => setChecked(event.target.checked)} className="mt-1 h-4 w-4" /><span>I understand that current app data will be replaced by the inspected backup.</span></label><button id="restore-inspected-backup-button" disabled={busy || !checked} onClick={() => void onConfirm()} className="w-full rounded-xl bg-rose-700 px-4 py-3 font-semibold text-white disabled:opacity-50">{busy ? 'Restoring safely…' : 'Replace data and restore'}</button></Dialog>;
}

function formatDate(value: string): string {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
}
