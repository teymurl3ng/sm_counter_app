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
  };
}

export type BackupActions = ReturnType<typeof backupBridgeActions>;
