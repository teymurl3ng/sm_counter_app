import type { QuickUnlockMethod, SessionHandle } from '../../bridge/contract.generated';
import { callNative, NativeBridgeError } from '../../bridge/nativeBridge';

export type ReauthenticationProof =
  | { method: 'passphrase'; secret: string }
  | { method: QuickUnlockMethod; secret: string };

export const emptyReauthenticationProof = (): ReauthenticationProof => ({ method: 'passphrase', secret: '' });

export async function reauthenticateWith(
  session: SessionHandle,
  proof: ReauthenticationProof,
): Promise<void> {
  if (proof.method === 'passphrase') {
    try {
      await callNative('command.auth.reauthenticate', { passphrase: proof.secret }, session);
    } catch (failure) {
      if (failure instanceof NativeBridgeError && failure.code === 'authentication_failed') {
        throw new Error('Your Saaf owner passphrase is incorrect.');
      }
      throw failure;
    }
    return;
  }
  await callNative('command.auth.quickReauthenticate', {
    method: proof.method,
    pin: proof.method === 'pin' ? proof.secret : null,
  }, session);
}

export function quickUnlockBridgeActions(session: SessionHandle | null) {
  const requireSession = (): SessionHandle => {
    if (!session) throw new Error('Sign in to continue.');
    return session;
  };
  return {
    status: () => callNative('system.quickUnlockStatus', {}),
    quickLogin: (loginName: string, method: QuickUnlockMethod, pin: string | null) =>
      callNative('command.auth.quickLogin', { loginName, method, pin }),
    quickReauthenticate: (method: QuickUnlockMethod, pin: string | null) =>
      callNative('command.auth.quickReauthenticate', { method, pin }, requireSession()),
    enroll: (currentPassphrase: string, enableDeviceAuthentication: boolean, pin: string | null) =>
      callNative('command.auth.enrollQuickUnlock', { currentPassphrase, enableDeviceAuthentication, pin }, requireSession()),
    remove: (currentPassphrase: string) =>
      callNative('command.auth.removeQuickUnlock', { currentPassphrase }, requireSession()),
  };
}

export type QuickUnlockActions = ReturnType<typeof quickUnlockBridgeActions>;
