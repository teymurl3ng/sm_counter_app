import type { OpaqueNativeTicket, SessionHandle } from '../../bridge/contract.generated';
import { callNative } from '../../bridge/nativeBridge';
import { reauthenticateWith, type ReauthenticationProof } from './quickUnlockBridgeActions';

export function backupBridgeActions(session: SessionHandle | null) {
  const requireSession = (): SessionHandle => {
    if (!session) throw new Error('Sign in to continue.');
    return session;
  };
  const reauthenticate = (proof: ReauthenticationProof) => reauthenticateWith(requireSession(), proof);
  return {
    exportBackup: async (proof: ReauthenticationProof, backupPassphrase: string) => {
      await reauthenticate(proof);
      return callNative('system.exportEncryptedBackup', { passphrase: backupPassphrase }, requireSession());
    },
    inspectBackup: async (proof: ReauthenticationProof, backupPassphrase: string) => {
      await reauthenticate(proof);
      return callNative('system.inspectEncryptedBackup', { passphrase: backupPassphrase }, requireSession());
    },
    restoreBackup: async (restoreTicket: OpaqueNativeTicket, proof: ReauthenticationProof) => {
      await reauthenticate(proof);
      return callNative('system.restoreInspectedBackup', { restoreTicket, confirmRestore: true }, requireSession());
    },
    cloudStatus: () => callNative('system.cloudBackupStatus', {}, requireSession()),
    connectCloudBackup: async (proof: ReauthenticationProof, allowMetered: boolean) => {
      await reauthenticate(proof);
      return callNative('system.connectCloudBackup', { allowMetered }, requireSession());
    },
    configureCloudBackup: async (proof: ReauthenticationProof, enabled: boolean, allowMetered: boolean) => {
      await reauthenticate(proof);
      return callNative('system.configureCloudBackup', { enabled, allowMetered }, requireSession());
    },
    runCloudBackupNow: () => callNative('system.runCloudBackupNow', {}, requireSession()),
    refreshCloudBackups: () => callNative('system.refreshCloudBackups', {}, requireSession()),
    inspectCloudBackup: async (proof: ReauthenticationProof, remoteFileId: string) => {
      await reauthenticate(proof);
      return callNative('system.inspectCloudBackup', { remoteFileId }, requireSession());
    },
    restoreCloudBackup: async (restoreTicket: OpaqueNativeTicket, proof: ReauthenticationProof) => {
      await reauthenticate(proof);
      return callNative('system.restoreInspectedCloudBackup', { restoreTicket, confirmRestore: true }, requireSession());
    },
    disconnectCloudBackup: async (proof: ReauthenticationProof) => {
      await reauthenticate(proof);
      return callNative('system.disconnectCloudBackup', { confirmRevoke: true }, requireSession());
    },
  };
}

export type BackupActions = ReturnType<typeof backupBridgeActions>;
