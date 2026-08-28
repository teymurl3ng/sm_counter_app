import { FormEvent, useCallback, useEffect, useState } from 'react';
import { LogOut, PackagePlus, RefreshCw, ShieldCheck } from 'lucide-react';
import type {
  CurrentSessionDto,
  MasterDataDto,
  ProductUnitCode,
  SessionHandle,
} from '../../bridge/contract.generated';
import {
  callNative,
  formatPaisa,
  NativeBridgeError,
  rupeesToPaisa,
  verifyNativeContract,
} from '../../bridge/nativeBridge';

type Stage = 'checking' | 'setup' | 'login' | 'recovery' | 'ready' | 'blocked';

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : 'The app could not complete this request.';
}

export function Phase7HybridShell() {
  const [stage, setStage] = useState<Stage>('checking');
  const [session, setSession] = useState<SessionHandle | null>(null);
  const [currentUser, setCurrentUser] = useState<CurrentSessionDto | null>(null);
  const [masterData, setMasterData] = useState<MasterDataDto | null>(null);
  const [recoveryCode, setRecoveryCode] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const loadMasterData = useCallback(async (activeSession: SessionHandle, user: CurrentSessionDto) => {
    if (!user.effectivePermissionCodes.includes('manage_master_data')) {
      setMasterData(null);
      return;
    }
    setMasterData(await callNative('query.masterData', {}, activeSession));
  }, []);

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
          await loadMasterData(session, user);
        } catch (failure) {
          if (failure instanceof NativeBridgeError && failure.code === 'session_expired') {
            setSession(null);
            setCurrentUser(null);
            setMasterData(null);
            setStage('login');
            setError('Your session expired. Sign in again.');
          } else {
            setError(errorMessage(failure));
          }
        }
      })();
    };
    document.addEventListener('visibilitychange', resume);
    return () => document.removeEventListener('visibilitychange', resume);
  }, [loadMasterData, session]);

  const finishAuthentication = async (activeSession: SessionHandle, user: CurrentSessionDto) => {
    setSession(activeSession);
    setCurrentUser(user);
    await loadMasterData(activeSession, user);
    setStage('ready');
  };

  const logout = async () => {
    if (!session) return;
    setBusy(true);
    setError(null);
    try {
      await callNative('command.auth.logout', {}, session);
    } catch (failure) {
      if (!(failure instanceof NativeBridgeError && failure.code === 'session_expired')) {
        setError(errorMessage(failure));
        setBusy(false);
        return;
      }
    }
    setSession(null);
    setCurrentUser(null);
    setMasterData(null);
    setRecoveryCode(null);
    setStage('login');
    setBusy(false);
  };

  if (stage === 'checking') return <CenteredMessage title="Opening Saaf Milk" message="Checking the secure app connection…" />;
  if (stage === 'blocked') return <CenteredMessage title="Saaf Milk cannot open" message={error ?? 'The secure app connection is unavailable.'} />;
  if (stage === 'setup') {
    return <OwnerSetup busy={busy} error={error} onSubmit={async (loginName, displayName, passphrase) => {
      setBusy(true);
      setError(null);
      try {
        const result = await callNative('command.auth.bootstrapOwner', { loginName, displayName, passphrase });
        setSession(result.sessionHandle);
        setCurrentUser(result.currentSession);
        await loadMasterData(result.sessionHandle, result.currentSession);
        setRecoveryCode(result.oneTimeRecoveryCode);
        setStage('recovery');
      } catch (failure) {
        setError(errorMessage(failure));
      } finally {
        setBusy(false);
      }
    }} />;
  }
  if (stage === 'login') {
    return <Login busy={busy} error={error} onSubmit={async (loginName, passphrase) => {
      setBusy(true);
      setError(null);
      try {
        const result = await callNative('command.auth.login', { loginName, passphrase });
        await finishAuthentication(result.sessionHandle, result.currentSession);
      } catch (failure) {
        setError(errorMessage(failure));
      } finally {
        setBusy(false);
      }
    }} />;
  }
  if (stage === 'recovery' && recoveryCode) {
    return (
      <main className="min-h-screen bg-[#f7f6f0] px-5 py-10 text-slate-900">
        <section className="mx-auto max-w-lg rounded-3xl border border-amber-200 bg-white p-6 shadow-xl shadow-amber-950/5">
          <ShieldCheck className="mb-4 h-10 w-10 text-emerald-700" />
          <h1 className="text-2xl font-bold">Save the owner recovery code</h1>
          <p className="mt-2 text-sm leading-6 text-slate-600">Write this code on paper and keep it somewhere private. It is shown only once and can recover the owner account.</p>
          <div id="owner-recovery-code" className="my-6 break-all rounded-2xl bg-amber-50 p-5 font-mono text-lg font-bold tracking-wide text-amber-950">{recoveryCode}</div>
          <button id="recovery-saved" className="w-full rounded-xl bg-emerald-700 px-4 py-3 font-semibold text-white" onClick={() => {
            setRecoveryCode(null);
            setStage('ready');
          }}>I have saved it safely</button>
        </section>
      </main>
    );
  }

  return (
    <main className="min-h-screen bg-[#f7f6f0] text-slate-900">
      <header className="bg-emerald-900 px-5 pb-6 pt-7 text-white shadow-lg">
        <div className="mx-auto flex max-w-3xl items-center justify-between gap-4">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-emerald-200">Saaf Milk</p>
            <h1 className="mt-1 text-2xl font-bold">Protected product records</h1>
            <p id="signed-in-user" className="mt-1 text-sm text-emerald-100">Signed in as {currentUser?.displayName} · {currentUser?.roleCode}</p>
          </div>
          <button id="logout-button" disabled={busy} onClick={() => void logout()} className="rounded-xl border border-emerald-600 p-3 text-emerald-50 disabled:opacity-50" aria-label="Sign out"><LogOut className="h-5 w-5" /></button>
        </div>
      </header>

      <div className="mx-auto grid max-w-3xl gap-5 px-4 py-6 md:grid-cols-[1fr_1.2fr]">
        {currentUser?.effectivePermissionCodes.includes('manage_master_data') && session ? (
          <AddProduct busy={busy} error={error} onSubmit={async (name, unitCode, rateRupees, notes) => {
            setBusy(true);
            setError(null);
            try {
              await callNative('command.createProduct', {
                name,
                unitCode,
                defaultSaleRatePaisaPerUnit: rateRupees.trim() ? rupeesToPaisa(rateRupees) : null,
                inventoryTracking: true,
                notes: notes.trim() || null,
              }, session);
              await loadMasterData(session, currentUser);
              setError('Product saved in the protected app records.');
            } catch (failure) {
              setError(errorMessage(failure));
              throw failure;
            } finally {
              setBusy(false);
            }
          }} />
        ) : (
          <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <h2 className="font-bold">Product management is not available for this account</h2>
            <p className="mt-2 text-sm text-slate-600">Your signed-in role does not have permission to add or view full product records.</p>
            {error && <Notice message={error} />}
          </section>
        )}

        <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <div className="mb-4 flex items-center justify-between">
            <div>
              <h2 className="text-lg font-bold">Products saved in Android</h2>
              <p className="text-sm text-slate-500">Read directly from the protected database.</p>
            </div>
            <RefreshCw className={`h-5 w-5 text-emerald-700 ${busy ? 'animate-spin' : ''}`} />
          </div>
          <div id="native-product-list" className="space-y-3">
            {masterData?.products.map((product) => (
              <article key={product.productId} className="rounded-xl border border-slate-100 bg-slate-50 p-4">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <h3 className="font-semibold">{product.name}</h3>
                    <p className="text-sm text-slate-500">{product.quantityOnHand} {product.unit}</p>
                  </div>
                  <span className="rounded-full bg-emerald-100 px-2.5 py-1 text-xs font-semibold text-emerald-800">{formatPaisa(product.defaultSaleRatePaisaPerUnit)}</span>
                </div>
              </article>
            ))}
            {masterData && masterData.products.length === 0 && <p className="text-sm text-slate-500">No products have been added yet.</p>}
            {!masterData && currentUser?.effectivePermissionCodes.includes('manage_master_data') && <p className="text-sm text-slate-500">Loading protected product records…</p>}
          </div>
        </section>
      </div>
    </main>
  );
}

function OwnerSetup({ busy, error, onSubmit }: { busy: boolean; error: string | null; onSubmit(login: string, display: string, passphrase: string): Promise<void> }) {
  const [login, setLogin] = useState('');
  const [display, setDisplay] = useState('');
  const [passphrase, setPassphrase] = useState('');
  const [confirm, setConfirm] = useState('');
  const [localError, setLocalError] = useState<string | null>(null);
  return <AuthCard title="Create the first owner" description="The owner can create staff accounts and control their permissions later." error={localError ?? error} onSubmit={(event) => {
    event.preventDefault();
    if (passphrase !== confirm) return setLocalError('The two passphrases do not match.');
    setLocalError(null);
    void onSubmit(login, display, passphrase).finally(() => { setPassphrase(''); setConfirm(''); });
  }}>
    <TextInput id="owner-display-name" label="Your name" value={display} onChange={setDisplay} autoComplete="name" />
    <TextInput id="owner-login-name" label="Login name" value={login} onChange={setLogin} autoComplete="username" />
    <TextInput id="owner-passphrase" label="Passphrase (at least 15 characters)" value={passphrase} onChange={setPassphrase} type="password" autoComplete="new-password" />
    <TextInput id="owner-passphrase-confirm" label="Repeat passphrase" value={confirm} onChange={setConfirm} type="password" autoComplete="new-password" />
    <SubmitButton id="create-owner-button" busy={busy} label="Create owner account" />
  </AuthCard>;
}

function Login({ busy, error, onSubmit }: { busy: boolean; error: string | null; onSubmit(login: string, passphrase: string): Promise<void> }) {
  const [login, setLogin] = useState('');
  const [passphrase, setPassphrase] = useState('');
  return <AuthCard title="Sign in" description="Use your private Saaf Milk account." error={error} onSubmit={(event) => {
    event.preventDefault();
    void onSubmit(login, passphrase).finally(() => setPassphrase(''));
  }}>
    <TextInput id="login-name" label="Login name" value={login} onChange={setLogin} autoComplete="username" />
    <TextInput id="login-passphrase" label="Passphrase" value={passphrase} onChange={setPassphrase} type="password" autoComplete="current-password" />
    <SubmitButton id="login-button" busy={busy} label="Sign in securely" />
  </AuthCard>;
}

function AddProduct({ busy, error, onSubmit }: { busy: boolean; error: string | null; onSubmit(name: string, unit: ProductUnitCode, rate: string, notes: string): Promise<void> }) {
  const [name, setName] = useState('');
  const [unit, setUnit] = useState<ProductUnitCode>('Liters');
  const [rate, setRate] = useState('');
  const [notes, setNotes] = useState('');
  const [saved, setSaved] = useState(false);
  return (
    <form className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm" onSubmit={(event) => {
      event.preventDefault();
      setSaved(false);
      void onSubmit(name, unit, rate, notes).then(() => {
        setName(''); setRate(''); setNotes(''); setSaved(true);
      }).catch(() => undefined);
    }}>
      <div className="mb-4 flex items-center gap-3"><PackagePlus className="h-6 w-6 text-emerald-700" /><div><h2 className="text-lg font-bold">Add one real product</h2><p className="text-sm text-slate-500">Saved only after Android approves it.</p></div></div>
      <TextInput id="product-name" label="Product name" value={name} onChange={setName} />
      <label className="mb-4 block text-sm font-semibold text-slate-700">Unit<select id="product-unit" value={unit} onChange={(event) => setUnit(event.target.value as ProductUnitCode)} className="mt-1.5 w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5"><option value="Liters">Liters</option><option value="Kg">Kg</option></select></label>
      <TextInput id="product-rate" label="Default sale rate in PKR (optional)" value={rate} onChange={setRate} inputMode="decimal" />
      <TextInput id="product-notes" label="Notes (optional)" value={notes} onChange={setNotes} />
      <SubmitButton id="save-product-button" busy={busy} label="Save product" />
      {(error || saved) && <Notice message={saved ? 'Product saved in the protected app records.' : error ?? ''} success={saved} />}
    </form>
  );
}

function AuthCard({ title, description, error, onSubmit, children }: { title: string; description: string; error: string | null; onSubmit(event: FormEvent): void; children: React.ReactNode }) {
  return <main className="min-h-screen bg-[#f7f6f0] px-5 py-10 text-slate-900"><form onSubmit={onSubmit} className="mx-auto max-w-md rounded-3xl border border-slate-200 bg-white p-6 shadow-xl shadow-emerald-950/5"><ShieldCheck className="mb-4 h-10 w-10 text-emerald-700" /><p className="text-xs font-bold uppercase tracking-[0.2em] text-emerald-700">Saaf Milk</p><h1 className="mt-1 text-2xl font-bold">{title}</h1><p className="mb-6 mt-2 text-sm text-slate-600">{description}</p>{children}{error && <Notice message={error} />}</form></main>;
}

function TextInput({ id, label, value, onChange, type = 'text', autoComplete, inputMode }: { id: string; label: string; value: string; onChange(value: string): void; type?: string; autoComplete?: string; inputMode?: 'decimal' }) {
  return <label className="mb-4 block text-sm font-semibold text-slate-700">{label}<input id={id} required={!label.includes('optional')} value={value} onChange={(event) => onChange(event.target.value)} type={type} autoComplete={autoComplete} inputMode={inputMode} className="mt-1.5 w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5 outline-none focus:border-emerald-600 focus:ring-2 focus:ring-emerald-100" /></label>;
}

function SubmitButton({ id, busy, label }: { id: string; busy: boolean; label: string }) {
  return <button id={id} disabled={busy} className="w-full rounded-xl bg-emerald-700 px-4 py-3 font-semibold text-white shadow-sm disabled:cursor-wait disabled:opacity-60">{busy ? 'Please wait…' : label}</button>;
}

function Notice({ message, success = false }: { message: string; success?: boolean }) {
  return <p role="status" className={`mt-4 rounded-xl px-3 py-2 text-sm ${success ? 'bg-emerald-50 text-emerald-800' : 'bg-rose-50 text-rose-800'}`}>{message}</p>;
}

function CenteredMessage({ title, message }: { title: string; message: string }) {
  return <main className="flex min-h-screen items-center justify-center bg-[#f7f6f0] px-5 text-slate-900"><section className="max-w-md rounded-3xl border border-slate-200 bg-white p-7 text-center shadow-xl"><ShieldCheck className="mx-auto mb-4 h-10 w-10 text-emerald-700" /><h1 className="text-xl font-bold">{title}</h1><p className="mt-2 text-sm leading-6 text-slate-600">{message}</p></section></main>;
}
