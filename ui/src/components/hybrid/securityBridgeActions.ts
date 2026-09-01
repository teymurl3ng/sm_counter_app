import type { AppRoleCode, Int64String, SessionHandle } from '../../bridge/contract.generated';
import { callNative } from '../../bridge/nativeBridge';
import type { NewUserDraft, SecurityBundle } from './securityModels';

export function securityBridgeActions(session: SessionHandle | null) {
  const requireSession = (): SessionHandle => {
    if (!session) throw new Error('Sign in to continue.');
    return session;
  };
  const load = async (): Promise<SecurityBundle> => {
    const active = requireSession();
    const [users, audit] = await Promise.all([callNative('query.users', {}, active), callNative('query.securityAudit', {}, active)]);
    return { users, audit };
  };
  const reauthenticate = (passphrase: string) => callNative('command.auth.reauthenticate', { passphrase }, requireSession());
  const reloadAfter = async (operation: () => Promise<unknown>): Promise<SecurityBundle> => { await operation(); return load(); };

  return {
    load,
    recoverOwner: (loginName: string, recoveryCode: string, newPassphrase: string) => callNative('command.auth.recoverOwner', { loginName, recoveryCode, newPassphrase }),
    changeOwnPassphrase: (currentPassphrase: string, newPassphrase: string) => callNative('command.auth.changeOwnPassphrase', { currentPassphrase, newPassphrase }, requireSession()),
    beginEnrollment: async (draft: NewUserDraft, passphrase: string) => {
      if (draft.roleCode === 'owner' || draft.roleCode === 'admin') await reauthenticate(passphrase);
      const challenge = await callNative('command.auth.beginUserEnrollment', draft, requireSession());
      return { challenge, bundle: await load() };
    },
    resumeEnrollment: async (targetUserId: Int64String, passphrase: string) => {
      await reauthenticate(passphrase);
      return callNative('command.auth.resumeUserEnrollment', { targetUserId }, requireSession());
    },
    completeEnrollment: async (enrollmentToken: string, passphrase: string) => {
      await callNative('command.auth.completeEnrollment', { enrollmentToken, passphrase });
      return load();
    },
    changeRole: (targetUserId: Int64String, roleCode: AppRoleCode, passphrase: string) => reloadAfter(async () => {
      await reauthenticate(passphrase);
      await callNative('command.auth.changeUserRole', { targetUserId, roleCode }, requireSession());
    }),
    setEnabled: (targetUserId: Int64String, enabled: boolean, passphrase: string) => reloadAfter(async () => {
      await reauthenticate(passphrase);
      await callNative('command.auth.setUserEnabled', { targetUserId, enabled }, requireSession());
    }),
    beginCredentialReset: async (targetUserId: Int64String, passphrase: string) => {
      await reauthenticate(passphrase);
      const challenge = await callNative('command.auth.beginCredentialReset', { targetUserId }, requireSession());
      return { challenge, bundle: await load() };
    },
  };
}

export type SecurityActions = ReturnType<typeof securityBridgeActions>;
