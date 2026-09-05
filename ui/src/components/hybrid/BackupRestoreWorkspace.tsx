import { useEffect, useState, type FormEvent } from 'react';
import { Cloud, DatabaseBackup, FileCheck2, RefreshCw, ShieldAlert } from 'lucide-react';
import type { BackupInspectionDto, BackupRestoreResultDto, CloudBackupStatusDto } from '../../bridge/contract.generated';
import { NativeBridgeError } from '../../bridge/nativeBridge';
import type { BackupActions } from './backupBridgeActions';
import { Dialog, FilterButton, Notice, TextInput } from './HybridUi';
import { errorMessage } from './hybridModels';

type BackupArea = 'create' | 'cloud' | 'restore';
type Runner = <T,>(operation: () => Promise<T>) => Promise<T | null>;

export function BackupRestoreWorkspace({ actions, canRestore, onRestored, onCloudRestored }: {
  actions: BackupActions;
  canRestore: boolean;
  onRestored(ticket: BackupInspectionDto['restoreTicket']): Promise<BackupRestoreResultDto>;
  onCloudRestored(ticket: BackupInspectionDto['restoreTicket']): Promise<BackupRestoreResultDto>;
}) {
  const [area, setArea] = useState<BackupArea>('create');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [inspection, setInspection] = useState<BackupInspectionDto | null>(null);
  const [cloudStatus, setCloudStatus] = useState<CloudBackupStatusDto | null>(null);
  const run: Runner = async <T,>(operation: () => Promise<T>): Promise<T | null> => {
    setBusy(true); setError(null); setNotice(null);
    try { return await operation(); }
    catch (failure) {
      if (failure instanceof NativeBridgeError && failure.code === 'user_cancelled') setNotice('No file was changed.');
      else setError(errorMessage(failure));
      return null;
    } finally { setBusy(false); }
  };

  useEffect(() => {
    if (!canRestore) return;
    void actions.cloudStatus().then(setCloudStatus).catch((failure) => setError(errorMessage(failure)));
  }, [actions, canRestore]);

  return <div className="space-y-4">
    <section className="rounded-2xl border border-sky-200 bg-sky-50 p-4 text-sm leading-6 text-sky-950">
      <strong>Backup is a protected snapshot, not live synchronization.</strong> It can protect this shop or move a complete copy to another installation. It does not keep two working phones continuously updated.
    </section>
    {canRestore && <div className="grid grid-cols-3 rounded-xl bg-slate-100 p-1">
      <FilterButton id="backup-create-tab" active={area === 'create'} onClick={() => setArea('create')}>Manual</FilterButton>
      <FilterButton id="backup-cloud-tab" active={area === 'cloud'} onClick={() => setArea('cloud')}>Cloud</FilterButton>
      <FilterButton id="backup-restore-tab" active={area === 'restore'} onClick={() => setArea('restore')}>Restore file</FilterButton>
    </div>}
    {notice && <Notice message={notice} success />}{error && <Notice message={error} />}
    {area === 'create' ? <CreateBackupPanel actions={actions} busy={busy} run={run} onSuccess={(name) => setNotice(`Encrypted backup created: ${name}. Keep its backup passphrase somewhere safe.`)} />
      : area === 'cloud' ? <CloudBackupPanel actions={actions} busy={busy} run={run} status={cloudStatus} setStatus={setCloudStatus} setNotice={setNotice} onCloudRestored={onCloudRestored} />
        : <RestoreBackupPanel actions={actions} busy={busy} inspection={inspection} setInspection={setInspection} run={run} onRestored={onRestored} />}
  </div>;
}

function CloudBackupPanel({ actions, busy, run, status, setStatus, setNotice, onCloudRestored }: {
  actions: BackupActions;
  busy: boolean;
  run: Runner;
  status: CloudBackupStatusDto | null;
  setStatus(value: CloudBackupStatusDto | null): void;
  setNotice(value: string | null): void;
  onCloudRestored(ticket: BackupInspectionDto['restoreTicket']): Promise<BackupRestoreResultDto>;
}) {
  const [accountPassphrase, setAccountPassphrase] = useState('');
  const [allowMetered, setAllowMetered] = useState(false);
  const [localError, setLocalError] = useState<string | null>(null);
  const [selectedRemoteId, setSelectedRemoteId] = useState('');
  const [cloudInspection, setCloudInspection] = useState<BackupInspectionDto | null>(null);
  const [confirmingRestore, setConfirmingRestore] = useState(false);
  const [disconnectPassphrase, setDisconnectPassphrase] = useState('');

  useEffect(() => setAllowMetered(status?.allowMetered ?? false), [status?.allowMetered]);

  const connect = (event: FormEvent) => {
    event.preventDefault();
    setLocalError(null);
    void run(() => actions.connectCloudBackup(accountPassphrase, allowMetered)).then((value) => {
      if (value) { setStatus(value); setNotice('Google Drive connected. The first encrypted backup has been queued.'); }
    }).finally(() => setAccountPassphrase(''));
  };

  if (!status) return <section className="rounded-2xl border border-slate-200 bg-white p-5 text-sm text-slate-600">Loading cloud-backup status…</section>;

  if (!status.configured) return <form id="connect-cloud-backup-form" onSubmit={connect} className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
    <div className="mb-4 flex gap-3"><Cloud className="h-7 w-7 shrink-0 text-emerald-700" /><div><h2 className="font-bold">Automatic encrypted Google Drive backup</h2><p className="mt-1 text-sm leading-6 text-slate-600">Choose an owner-controlled Google account. Backups use its hidden app-data space and run approximately once a day.</p></div></div>
    {localError && <Notice message={localError} />}
    <TextInput id="cloud-connect-account-passphrase" label="Your current owner passphrase" value={accountPassphrase} onChange={setAccountPassphrase} type="password" autoComplete="current-password" />
    <p className="mb-4 rounded-xl bg-amber-50 p-3 text-sm leading-6 text-amber-950"><strong>Android will ask for a separate cloud recovery passphrase.</strong> Keep it outside the phone: a lost phone and forgotten passphrase make the cloud copies permanently unreadable.</p>
    <label className="mb-4 flex items-start gap-3 rounded-xl bg-slate-50 p-3 text-sm"><input id="cloud-allow-metered" type="checkbox" checked={allowMetered} onChange={(event) => setAllowMetered(event.target.checked)} className="mt-1 h-4 w-4" /><span>Allow automatic backup on mobile or metered data. Leave off for Wi-Fi only.</span></label>
    <button id="connect-cloud-backup-button" disabled={busy} className="w-full rounded-xl bg-emerald-700 px-4 py-3 font-semibold text-white disabled:opacity-60">{busy ? 'Connecting securely…' : 'Choose Google account and connect'}</button>
  </form>;

  const refresh = () => void run(actions.refreshCloudBackups).then((value) => { if (value) setStatus(value); });
  const updatePolicy = (event: FormEvent) => {
    event.preventDefault();
    void run(() => actions.configureCloudBackup(accountPassphrase, status.enabled, allowMetered)).then((value) => {
      if (value) { setStatus(value); setNotice('Cloud-backup settings updated.'); }
    }).finally(() => setAccountPassphrase(''));
  };
  const inspectCloud = (event: FormEvent) => {
    event.preventDefault();
    if (!selectedRemoteId) return setLocalError('Choose a cloud recovery point first.');
    setLocalError(null);
    void run(() => actions.inspectCloudBackup(accountPassphrase, selectedRemoteId)).then((value) => {
      if (value) setCloudInspection(value);
    }).finally(() => setAccountPassphrase(''));
  };
  const disconnect = () => void run(() => actions.disconnectCloudBackup(disconnectPassphrase)).then((value) => {
    if (value) { setStatus(null); void actions.cloudStatus().then(setStatus); setNotice('Google Drive disconnected. Existing encrypted cloud recovery points were retained.'); }
  }).finally(() => setDisconnectPassphrase(''));

  return <div className="space-y-4">
    <section className={`rounded-2xl border p-5 shadow-sm ${status.stale || status.lastErrorMessage ? 'border-amber-300 bg-amber-50' : 'border-emerald-200 bg-white'}`}>
      <div className="flex items-start justify-between gap-3"><div><h2 className="font-bold">Google Drive {status.enabled ? 'enabled' : 'paused'}</h2><p className="mt-1 text-sm text-slate-600">{status.accountDisplay} · {status.scheduleDescription} · {status.allowMetered ? 'Any network' : 'Wi-Fi only'}</p></div><button id="refresh-cloud-backups-button" disabled={busy} onClick={refresh} className="rounded-lg border border-slate-300 bg-white p-2" aria-label="Refresh cloud recovery points"><RefreshCw className="h-4 w-4" /></button></div>
      <div className="mt-4 grid gap-2 text-sm sm:grid-cols-2"><p><span className="text-slate-500">Last verified</span><br /><strong>{status.lastSuccessAt ? formatDate(status.lastSuccessAt) : 'Not yet completed'}</strong></p><p><span className="text-slate-500">Recovery points</span><br /><strong>{status.snapshots.length}</strong></p></div>
      {status.pending && <p className="mt-3 rounded-xl bg-sky-50 p-3 text-sm text-sky-900">An encrypted snapshot is safely waiting to upload or verify.</p>}
      {status.lastErrorMessage && <p className="mt-3 rounded-xl bg-amber-100 p-3 text-sm text-amber-950">{status.lastErrorMessage}</p>}
      <button id="run-cloud-backup-now-button" disabled={busy || !status.enabled} onClick={() => void run(actions.runCloudBackupNow).then((value) => { if (value) setNotice('Encrypted cloud backup queued.'); })} className="mt-4 w-full rounded-xl bg-emerald-700 px-4 py-3 font-semibold text-white disabled:opacity-50">Back up now</button>
    </section>

    <form id="cloud-backup-policy-form" onSubmit={updatePolicy} className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <h3 className="font-bold">Network policy</h3>
      <label className="my-4 flex items-start gap-3 text-sm"><input type="checkbox" checked={allowMetered} onChange={(event) => setAllowMetered(event.target.checked)} className="mt-1 h-4 w-4" /><span>Allow mobile or metered data</span></label>
      <TextInput id="cloud-policy-account-passphrase" label="Owner passphrase to save changes" value={accountPassphrase} onChange={setAccountPassphrase} type="password" autoComplete="current-password" />
      <div className="grid gap-2 sm:grid-cols-2"><button id="save-cloud-policy-button" disabled={busy} className="rounded-xl bg-slate-900 px-4 py-3 font-semibold text-white disabled:opacity-50">Save network policy</button><button id="toggle-cloud-backup-button" type="button" disabled={busy} onClick={() => void run(() => actions.configureCloudBackup(accountPassphrase, !status.enabled, allowMetered)).then((value) => { if (value) setStatus(value); }).finally(() => setAccountPassphrase(''))} className="rounded-xl border border-slate-300 px-4 py-3 font-semibold">{status.enabled ? 'Pause automatic backup' : 'Resume automatic backup'}</button></div>
    </form>

    <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <h3 className="font-bold">Cloud recovery points</h3>
      {status.snapshots.length === 0 ? <p className="mt-2 text-sm text-slate-600">No verified cloud backup is listed yet.</p> : <form id="inspect-cloud-backup-form" onSubmit={inspectCloud} className="mt-3">
        <div className="mb-4 max-h-64 space-y-2 overflow-auto">{status.snapshots.map((snapshot) => <label key={snapshot.remoteFileId} className="flex items-center gap-3 rounded-xl border border-slate-200 p-3 text-sm"><input type="radio" name="cloud-snapshot" checked={selectedRemoteId === snapshot.remoteFileId} onChange={() => { setSelectedRemoteId(snapshot.remoteFileId); setCloudInspection(null); }} /><span><strong>{formatDate(snapshot.createdAt)}</strong><br /><span className="text-slate-500">{formatBytes(snapshot.sizeBytes)}</span></span></label>)}</div>
        {!cloudInspection ? <><TextInput id="cloud-restore-account-passphrase" label="Your current owner passphrase" value={accountPassphrase} onChange={setAccountPassphrase} type="password" autoComplete="current-password" /><p className="mb-4 rounded-xl bg-slate-50 p-3 text-sm text-slate-700">Android will securely ask for the separate cloud recovery passphrase.</p><button id="inspect-cloud-backup-button" disabled={busy || !selectedRemoteId} className="w-full rounded-xl bg-emerald-700 px-4 py-3 font-semibold text-white disabled:opacity-50">Inspect selected recovery point</button></> : <div className="space-y-3"><div className="rounded-xl bg-emerald-50 p-4 text-sm text-emerald-900"><strong>Cloud backup passed its security checks.</strong><br />Created {formatDate(cloudInspection.createdAt)}</div><div className="rounded-xl bg-rose-50 p-4 text-sm leading-6 text-rose-950"><strong>Restore fully replaces this installation</strong> after creating a local safety snapshot, then signs everyone out and pauses cloud backup.</div><button type="button" id="open-cloud-restore-confirmation" onClick={() => setConfirmingRestore(true)} className="w-full rounded-xl bg-rose-700 px-4 py-3 font-semibold text-white">Review final restore warning</button></div>}
      </form>}
    </section>

    <section className="rounded-2xl border border-rose-200 bg-white p-5 shadow-sm"><h3 className="font-bold text-rose-900">Disconnect Google Drive</h3><p className="mt-2 text-sm leading-6 text-slate-600">This revokes all Google scopes granted to Saaf Milk and removes local scheduling and key state. Existing encrypted recovery points remain in Drive.</p><div className="mt-4"><TextInput id="cloud-disconnect-account-passphrase" label="Owner passphrase" value={disconnectPassphrase} onChange={setDisconnectPassphrase} type="password" autoComplete="current-password" /></div><button id="disconnect-cloud-backup-button" disabled={busy || !disconnectPassphrase} onClick={disconnect} className="w-full rounded-xl border border-rose-300 px-4 py-3 font-semibold text-rose-800 disabled:opacity-50">Revoke access and disconnect</button></section>
    {localError && <Notice message={localError} />}
    {confirmingRestore && cloudInspection && <RestoreConfirmation busy={busy} onCancel={() => setConfirmingRestore(false)} onConfirm={async () => { const result = await run(() => onCloudRestored(cloudInspection.restoreTicket)); if (!result) setConfirmingRestore(false); }} />}
  </div>;
}

function CreateBackupPanel({ actions, busy, run, onSuccess }: {
  actions: BackupActions; busy: boolean; run: Runner; onSuccess(name: string): void;
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
  run: Runner; onRestored(ticket: BackupInspectionDto['restoreTicket']): Promise<BackupRestoreResultDto>;
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

function formatBytes(value: string): string {
  const bytes = Number(value);
  if (!Number.isFinite(bytes)) return value;
  if (bytes < 1024 * 1024) return `${Math.max(1, Math.round(bytes / 1024))} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}
