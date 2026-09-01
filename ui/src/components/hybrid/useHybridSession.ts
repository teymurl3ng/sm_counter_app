import { useCallback, useEffect, useState } from 'react';
import type { CurrentSessionDto, SessionHandle } from '../../bridge/contract.generated';
import { callNative, NativeBridgeError, verifyNativeContract } from '../../bridge/nativeBridge';
import { errorMessage, type HybridStage } from './hybridModels';

type LoadAuthorizedData = (session: SessionHandle, user: CurrentSessionDto) => Promise<void>;

export function useHybridSession(loadAuthorizedData: LoadAuthorizedData, clearAuthorizedData: () => void) {
  const [stage, setStage] = useState<HybridStage>('checking');
  const [session, setSession] = useState<SessionHandle | null>(null);
  const [currentUser, setCurrentUser] = useState<CurrentSessionDto | null>(null);
  const [recoveryCode, setRecoveryCode] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const clearSession = useCallback(() => {
    setSession(null);
    setCurrentUser(null);
    clearAuthorizedData();
    setRecoveryCode(null);
    setNotice(null);
    setStage('login');
  }, [clearAuthorizedData]);

  useEffect(() => {
    let cancelled = false;
    void (async () => {
      try {
        await verifyNativeContract();
        const status = await callNative('query.bootstrapStatus', {});
        if (!cancelled) setStage(status.needsOwnerBootstrap ? 'setup' : 'login');
      } catch (failure) {
        if (!cancelled) {
          setError(errorMessage(failure));
          setStage('blocked');
        }
      }
    })();
    return () => { cancelled = true; };
  }, []);

  useEffect(() => {
    const resume = () => {
      if (document.visibilityState !== 'visible' || !session) return;
      void (async () => {
        try {
          const user = await callNative('query.currentSession', {}, session);
          setCurrentUser(user);
          await loadAuthorizedData(session, user);
        } catch (failure) {
          if (failure instanceof NativeBridgeError && failure.code === 'session_expired') {
            clearSession();
            setError('Your session expired. Sign in again.');
          } else {
            setError(errorMessage(failure));
          }
        }
      })();
    };
    document.addEventListener('visibilitychange', resume);
    return () => document.removeEventListener('visibilitychange', resume);
  }, [clearSession, loadAuthorizedData, session]);

  const bootstrapOwner = async (loginName: string, displayName: string, passphrase: string) => {
    setBusy(true);
    setError(null);
    try {
      const result = await callNative('command.auth.bootstrapOwner', { loginName, displayName, passphrase });
      setSession(result.sessionHandle);
      setCurrentUser(result.currentSession);
      await loadAuthorizedData(result.sessionHandle, result.currentSession);
      setRecoveryCode(result.oneTimeRecoveryCode);
      setStage('recovery');
    } catch (failure) {
      setError(errorMessage(failure));
    } finally {
      setBusy(false);
    }
  };

  const login = async (loginName: string, passphrase: string) => {
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const result = await callNative('command.auth.login', { loginName, passphrase });
      setSession(result.sessionHandle);
      setCurrentUser(result.currentSession);
      await loadAuthorizedData(result.sessionHandle, result.currentSession);
      setStage('ready');
    } catch (failure) {
      setError(errorMessage(failure));
    } finally {
      setBusy(false);
    }
  };

  const recoverOwner = async (loginName: string, savedRecoveryCode: string, newPassphrase: string) => {
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const result = await callNative('command.auth.recoverOwner', { loginName, recoveryCode: savedRecoveryCode, newPassphrase });
      setSession(result.sessionHandle);
      setCurrentUser(result.currentSession);
      await loadAuthorizedData(result.sessionHandle, result.currentSession);
      setRecoveryCode(result.replacementRecoveryCode);
      setStage('recovery');
    } catch (failure) {
      setError(errorMessage(failure));
    } finally {
      setBusy(false);
    }
  };

  const logout = () => {
    if (!session) return;
    setBusy(true);
    setError(null);
    void callNative('command.auth.logout', {}, session)
      .then(clearSession)
      .catch((failure) => {
        if (failure instanceof NativeBridgeError && failure.code === 'session_expired') clearSession();
        else setError(errorMessage(failure));
      })
      .finally(() => setBusy(false));
  };

  const changeOwnPassphrase = async (currentPassphrase: string, newPassphrase: string) => {
    if (!session) throw new Error('Sign in to continue.');
    setBusy(true);
    setError(null);
    try {
      await callNative('command.auth.changeOwnPassphrase', { currentPassphrase, newPassphrase }, session);
      clearSession();
      setNotice('Passphrase changed. Sign in again with the new one.');
    } catch (failure) {
      setError(errorMessage(failure));
      throw failure;
    } finally {
      setBusy(false);
    }
  };

  const acknowledgeRecoveryCode = () => {
    setRecoveryCode(null);
    setStage('ready');
  };

  const finishRestore = () => {
    clearSession();
    setNotice('Backup restored safely. Sign in using an owner account from that backup.');
  };

  return {
    stage,
    session,
    currentUser,
    recoveryCode,
    error,
    notice,
    busy,
    setError,
    setNotice,
    setBusy,
    bootstrapOwner,
    login,
    recoverOwner,
    logout,
    changeOwnPassphrase,
    acknowledgeRecoveryCode,
    finishRestore,
  };
}
