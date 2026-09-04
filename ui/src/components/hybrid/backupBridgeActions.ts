import type { OpaqueNativeTicket, SessionHandle } from '../../bridge/contract.generated';
import { callNative } from '../../bridge/nativeBridge';

export function backupBridgeActions(session: SessionHandle | null) {
  const requireSession = (): SessionHandle => {
    if (!session) throw new Error('Sign in to continue.');
    return session;
  };
  const reauthenticate = (passphrase: string) => callNative('command.auth.reauthenticate', { passphrase }, requireSession());
  return {
    exportBackup: async (accountPassphrase: string, backupPassphrase: string) => {
      await reauthenticate(accountPassphrase);
      return callNative('system.exportEncryptedBackup', { passphrase: backupPassphrase }, requireSession());
    },
    inspectBackup: async (accountPassphrase: string, backupPassphrase: string) => {
      await reauthenticate(accountPassphrase);
      return callNative('system.inspectEncryptedBackup', { passphrase: backupPassphrase }, requireSession());
    },
    restoreBackup: (restoreTicket: OpaqueNativeTicket) => callNative('system.restoreInspectedBackup', { restoreTicket, confirmRestore: true }, requireSession()),
    cloudStatus: () => callNative('system.cloudBackupStatus', {}, requireSession()),
    connectCloudBackup: async (accountPassphrase: string, allowMetered: boolean) => {
      await reauthenticate(accountPassphrase);
      return callNative('system.connectCloudBackup', { allowMetered }, requireSession());
    },
    configureCloudBackup: async (accountPassphrase: string, enabled: boolean, allowMetered: boolean) => {
      await reauthenticate(accountPassphrase);
      return callNative('system.configureCloudBackup', { enabled, allowMetered }, requireSession());
    },
    runCloudBackupNow: () => callNative('system.runCloudBackupNow', {}, requireSession()),
    refreshCloudBackups: () => callNative('system.refreshCloudBackups', {}, requireSession()),
    inspectCloudBackup: async (accountPassphrase: string, remoteFileId: string) => {
      await reauthenticate(accountPassphrase);
      return callNative('system.inspectCloudBackup', { remoteFileId }, requireSession());
    },
    restoreCloudBackup: (restoreTicket: OpaqueNativeTicket) => callNative('system.restoreInspectedCloudBackup', { restoreTicket, confirmRestore: true }, requireSession()),
    disconnectCloudBackup: async (accountPassphrase: string) => {
      await reauthenticate(accountPassphrase);
      return callNative('system.disconnectCloudBackup', { confirmRevoke: true }, requireSession());
    },
  };
}

export type BackupActions = ReturnType<typeof backupBridgeActions>;
