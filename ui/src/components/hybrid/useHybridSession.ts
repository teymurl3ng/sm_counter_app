import { useCallback, useEffect, useState } from 'react';
import type { CurrentSessionDto, QuickUnlockMethod, QuickUnlockStatusDto, SessionHandle } from '../../bridge/contract.generated';
import { callNative, NativeBridgeError, verifyNativeContract } from '../../bridge/nativeBridge';
import { waitForNativeRestoration } from '../../bridge/lifecycleBridge';
import { errorMessage, type HybridStage } from './hybridModels';
import type { ManagementArea } from './HybridNavigation';

type LoadAuthorizedData = (session: SessionHandle, user: CurrentSessionDto) => Promise<void>;

export function useHybridSession(loadAuthorizedData: LoadAuthorizedData, clearAuthorizedData: () => void) {
  const [stage, setStage] = useState<HybridStage>('checking');
  const [session, setSession] = useState<SessionHandle | null>(null);
  const [currentUser, setCurrentUser] = useState<CurrentSessionDto | null>(null);
  const [restoredArea, setRestoredArea] = useState<ManagementArea | null>(null);
  const [recoveryCode, setRecoveryCode] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [quickUnlockStatus, setQuickUnlockStatus] = useState<QuickUnlockStatusDto | null>(null);

  const refreshQuickUnlockStatus = useCallback(async () => {
    const status = await callNative('system.quickUnlockStatus', {});
    setQuickUnlockStatus(status);
    return status;
  }, []);

  const clearSession = useCallback(() => {
    setSession(null);
    setCurrentUser(null);
    setRestoredArea(null);
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
        try {
          await refreshQuickUnlockStatus();
        } catch {
          if (!cancelled) setQuickUnlockStatus(null);
        }
        const restoration = await waitForNativeRestoration();
        if (restoration) {
          try {
            const user = await callNative('query.currentSession', {}, restoration.sessionHandle);
            if (!cancelled) {
              setSession(restoration.sessionHandle);
              setCurrentUser(user);
              setRestoredArea(restoration.activeArea);
              await loadAuthorizedData(restoration.sessionHandle, user);
              setStage('ready');
            }
            return;
          } catch (failure) {
            if (!cancelled) {
              setError(failure instanceof NativeBridgeError && failure.code === 'session_expired'
                ? 'Your previous session expired. Sign in again.'
                : errorMessage(failure));
            }
          }
        }
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
  }, [loadAuthorizedData, refreshQuickUnlockStatus]);

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

  const quickLogin = async (loginName: string, method: QuickUnlockMethod, pin: string | null) => {
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const result = await callNative('command.auth.quickLogin', { loginName, method, pin });
      setSession(result.sessionHandle);
      setCurrentUser(result.currentSession);
      await loadAuthorizedData(result.sessionHandle, result.currentSession);
      setStage('ready');
    } catch (failure) {
      if (failure instanceof NativeBridgeError && failure.code === 'user_cancelled') return;
      if (failure instanceof NativeBridgeError && failure.code === 'quick_unlock_invalidated') {
        await refreshQuickUnlockStatus().catch(() => undefined);
      }
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
      await refreshQuickUnlockStatus().catch(() => undefined);
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
      .then(async () => {
        await refreshQuickUnlockStatus().catch(() => undefined);
        clearSession();
      })
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
      await refreshQuickUnlockStatus().catch(() => undefined);
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
    setQuickUnlockStatus((current) => current ? { ...current, accounts: [] } : null);
    setNotice('Backup restored safely. Sign in using an owner account from that backup.');
  };

  const enrollQuickUnlock = async (currentPassphrase: string, enableDeviceAuthentication: boolean, pin: string | null) => {
    if (!session) throw new Error('Sign in to continue.');
    const status = await callNative('command.auth.enrollQuickUnlock', { currentPassphrase, enableDeviceAuthentication, pin }, session);
    setQuickUnlockStatus(status);
  };

  const removeQuickUnlock = async (currentPassphrase: string) => {
    if (!session) throw new Error('Sign in to continue.');
    const status = await callNative('command.auth.removeQuickUnlock', { currentPassphrase }, session);
    setQuickUnlockStatus(status);
  };

  return {
    stage,
    session,
    currentUser,
    restoredArea,
    recoveryCode,
    error,
    notice,
    busy,
    quickUnlockStatus,
    setError,
    setNotice,
    setBusy,
    bootstrapOwner,
    login,
    quickLogin,
    recoverOwner,
    logout,
    changeOwnPassphrase,
    enrollQuickUnlock,
    removeQuickUnlock,
    acknowledgeRecoveryCode,
    finishRestore,
  };
}
