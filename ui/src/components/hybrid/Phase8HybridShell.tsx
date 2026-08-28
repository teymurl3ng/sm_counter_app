import { FormEvent, ReactNode, useCallback, useEffect, useMemo, useState } from 'react';
import {
  Archive,
  CheckCircle2,
  Edit3,
  LogOut,
  PackagePlus,
  RefreshCw,
  RotateCcw,
  ShieldCheck,
  X,
} from 'lucide-react';
import type {
  CurrentSessionDto,
  MasterDataDto,
  ProductDto,
  ProductUnitCode,
  SessionHandle,
} from '../../bridge/contract.generated';
import {
  callNative,
  formatPaisa,
  NativeBridgeError,
  paisaToRupeesInput,
  rupeesToPaisa,
  verifyNativeContract,
} from '../../bridge/nativeBridge';

type Stage = 'checking' | 'setup' | 'login' | 'recovery' | 'ready' | 'blocked';
type ProductDraft = {
  name: string;
  unitCode: ProductUnitCode;
  rateRupees: string;
  inventoryTracking: boolean;
  notes: string;
};

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : 'The app could not complete this request.';
}

export function Phase8HybridShell() {
  const [stage, setStage] = useState<Stage>('checking');
  const [session, setSession] = useState<SessionHandle | null>(null);
  const [currentUser, setCurrentUser] = useState<CurrentSessionDto | null>(null);
  const [masterData, setMasterData] = useState<MasterDataDto | null>(null);
  const [recoveryCode, setRecoveryCode] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
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
  }, [loadMasterData, session]);

  const clearSession = () => {
    setSession(null);
    setCurrentUser(null);
    setMasterData(null);
    setRecoveryCode(null);
    setNotice(null);
    setStage('login');
  };

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
      clearSession();
    } catch (failure) {
      if (failure instanceof NativeBridgeError && failure.code === 'session_expired') clearSession();
      else setError(errorMessage(failure));
    } finally {
      setBusy(false);
    }
  };

  const saveProduct = async (draft: ProductDraft, productId?: ProductDto['productId']) => {
    if (!session || !currentUser) return;
    setBusy(true);
    setError(null);
    setNotice(null);
    const payload = {
      name: draft.name,
      unitCode: draft.unitCode,
      defaultSaleRatePaisaPerUnit: draft.rateRupees.trim() ? rupeesToPaisa(draft.rateRupees) : null,
      inventoryTracking: draft.inventoryTracking,
      notes: draft.notes.trim() || null,
    };
    try {
      if (productId) await callNative('command.updateProduct', { productId, ...payload }, session);
      else await callNative('command.createProduct', payload, session);
      await loadMasterData(session, currentUser);
      setNotice(productId ? 'Product changes saved.' : 'Product added successfully.');
    } catch (failure) {
      setError(errorMessage(failure));
      throw failure;
    } finally {
      setBusy(false);
    }
  };

  const setProductArchived = async (product: ProductDto, archived: boolean) => {
    if (!session || !currentUser) return;
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      await callNative('command.setProductArchived', { productId: product.productId, archived }, session);
      await loadMasterData(session, currentUser);
      setNotice(archived ? `${product.name} moved to archived products.` : `${product.name} restored to active products.`);
    } catch (failure) {
      setError(errorMessage(failure));
      throw failure;
    } finally {
      setBusy(false);
    }
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
    <ProductWorkspace
      busy={busy}
      currentUser={currentUser}
      error={error}
      masterData={masterData}
      notice={notice}
      onLogout={() => void logout()}
      onSaveProduct={saveProduct}
      onSetArchived={setProductArchived}
    />
  );
}

function ProductWorkspace({
  busy,
  currentUser,
  error,
  masterData,
  notice,
  onLogout,
  onSaveProduct,
  onSetArchived,
}: {
  busy: boolean;
  currentUser: CurrentSessionDto | null;
  error: string | null;
  masterData: MasterDataDto | null;
  notice: string | null;
  onLogout(): void;
  onSaveProduct(draft: ProductDraft, productId?: ProductDto['productId']): Promise<void>;
  onSetArchived(product: ProductDto, archived: boolean): Promise<void>;
}) {
  const [showArchived, setShowArchived] = useState(false);
  const [editing, setEditing] = useState<ProductDto | 'new' | null>(null);
  const [archiveTarget, setArchiveTarget] = useState<ProductDto | null>(null);
  const canManage = currentUser?.effectivePermissionCodes.includes('manage_master_data') === true;
  const products = masterData?.products ?? [];
  const activeCount = products.filter((product) => !product.archived).length;
  const archivedCount = products.length - activeCount;
  const visibleProducts = useMemo(
    () => products.filter((product) => product.archived === showArchived),
    [products, showArchived],
  );

  return (
    <main className="min-h-screen bg-[#f7f6f0] pb-10 text-slate-900">
      <header className="bg-emerald-900 px-5 pb-7 pt-7 text-white shadow-lg">
        <div className="mx-auto flex max-w-4xl items-center justify-between gap-4">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-emerald-200">Saaf Milk</p>
            <h1 className="mt-1 text-2xl font-bold">Products</h1>
            <p id="signed-in-user" className="mt-1 text-sm text-emerald-100">Signed in as {currentUser?.displayName} · {plainRole(currentUser?.roleCode)}</p>
          </div>
          <button id="logout-button" disabled={busy} onClick={onLogout} className="rounded-xl border border-emerald-600 p-3 text-emerald-50 disabled:opacity-50" aria-label="Sign out"><LogOut className="h-5 w-5" /></button>
        </div>
      </header>

      <div className="mx-auto max-w-4xl px-4 py-6">
        {!canManage ? (
          <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <h2 className="font-bold">Product management is not available for this account</h2>
            <p className="mt-2 text-sm text-slate-600">Ask the owner to use an account with permission to manage products.</p>
          </section>
        ) : (
          <>
            <section className="mb-5 grid grid-cols-2 gap-3">
              <SummaryCard label="Active products" value={activeCount.toString()} />
              <SummaryCard label="Archived products" value={archivedCount.toString()} />
            </section>

            {(notice || error) && <Notice message={notice ?? error ?? ''} success={Boolean(notice)} />}

            <section className="mt-5 overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
              <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 p-4">
                <div className="flex rounded-xl bg-slate-100 p-1">
                  <FilterButton id="active-products-tab" active={!showArchived} onClick={() => setShowArchived(false)}>Active ({activeCount})</FilterButton>
                  <FilterButton id="archived-products-tab" active={showArchived} onClick={() => setShowArchived(true)}>Archived ({archivedCount})</FilterButton>
                </div>
                <button id="add-product-button" disabled={busy} onClick={() => setEditing('new')} className="flex items-center gap-2 rounded-xl bg-emerald-700 px-4 py-2.5 text-sm font-semibold text-white disabled:opacity-50">
                  <PackagePlus className="h-4 w-4" /> Add product
                </button>
              </div>

              <div id="native-product-list" className="divide-y divide-slate-100">
                {visibleProducts.map((product) => (
                  <article key={product.productId} data-product-id={product.productId} className="p-4">
                    <div className="flex items-start justify-between gap-3">
                      <div className="min-w-0">
                        <div className="flex flex-wrap items-center gap-2">
                          <h2 className="font-bold text-slate-900">{product.name}</h2>
                          <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs font-semibold text-slate-600">{product.unit}</span>
                        </div>
                        <p className="mt-1 text-sm text-slate-500">Current stock: {product.quantityOnHand} {product.unit}</p>
                        <p className="text-sm text-slate-500">Default selling rate: {formatPaisa(product.defaultSaleRatePaisaPerUnit)}</p>
                        <p className="text-sm text-slate-500">Stock tracking: {product.inventoryTracking ? 'On' : 'Off'}</p>
                        {product.notes && <p className="mt-2 text-sm text-slate-600">{product.notes}</p>}
                      </div>
                      <div className="flex shrink-0 gap-2">
                        <button id={`edit-product-${product.productId}`} disabled={busy} onClick={() => setEditing(product)} className="rounded-lg border border-slate-200 p-2 text-slate-600 disabled:opacity-50" aria-label={`Edit ${product.name}`}><Edit3 className="h-4 w-4" /></button>
                        {product.archived ? (
                          <button id={`restore-product-${product.productId}`} disabled={busy} onClick={() => void onSetArchived(product, false).catch(() => undefined)} className="rounded-lg border border-emerald-200 p-2 text-emerald-700 disabled:opacity-50" aria-label={`Restore ${product.name}`}><RotateCcw className="h-4 w-4" /></button>
                        ) : (
                          <button id={`archive-product-${product.productId}`} disabled={busy} onClick={() => setArchiveTarget(product)} className="rounded-lg border border-amber-200 p-2 text-amber-700 disabled:opacity-50" aria-label={`Archive ${product.name}`}><Archive className="h-4 w-4" /></button>
                        )}
                      </div>
                    </div>
                  </article>
                ))}
                {!masterData && <p className="p-5 text-sm text-slate-500"><RefreshCw className="mr-2 inline h-4 w-4 animate-spin" />Loading protected product records…</p>}
                {masterData && visibleProducts.length === 0 && <p className="p-5 text-sm text-slate-500">{showArchived ? 'No archived products.' : 'No active products yet.'}</p>}
              </div>
            </section>
          </>
        )}
      </div>

      {editing && (
        <ProductForm
          key={editing === 'new' ? 'new' : editing.productId}
          busy={busy}
          product={editing === 'new' ? null : editing}
          onClose={() => setEditing(null)}
          onSave={async (draft) => {
            await onSaveProduct(draft, editing === 'new' ? undefined : editing.productId);
            setEditing(null);
          }}
        />
      )}

      {archiveTarget && (
        <Dialog title="Archive product" onClose={() => setArchiveTarget(null)}>
          <div id="archive-confirmation">
            <p className="text-sm leading-6 text-slate-600">Archive <strong>{archiveTarget.name}</strong>? It will stop appearing in new transactions, but its old records will remain safe.</p>
            <div className="mt-5 flex gap-3">
              <button onClick={() => setArchiveTarget(null)} className="flex-1 rounded-xl border border-slate-300 px-4 py-3 text-sm font-semibold">Keep active</button>
              <button id="confirm-product-archive" disabled={busy} onClick={() => void onSetArchived(archiveTarget, true).then(() => setArchiveTarget(null)).catch(() => undefined)} className="flex-1 rounded-xl bg-amber-600 px-4 py-3 text-sm font-semibold text-white disabled:opacity-50">Archive</button>
            </div>
          </div>
        </Dialog>
      )}
    </main>
  );
}

function ProductForm({ busy, product, onClose, onSave }: { busy: boolean; product: ProductDto | null; onClose(): void; onSave(draft: ProductDraft): Promise<void> }) {
  const [name, setName] = useState(product?.name ?? '');
  const [unitCode, setUnitCode] = useState<ProductUnitCode>(product?.unit === 'Kg' ? 'Kg' : 'Liters');
  const [rateRupees, setRateRupees] = useState(paisaToRupeesInput(product?.defaultSaleRatePaisaPerUnit ?? null));
  const [inventoryTracking, setInventoryTracking] = useState(product?.inventoryTracking ?? true);
  const [notes, setNotes] = useState(product?.notes ?? '');
  const [localError, setLocalError] = useState<string | null>(null);
  return (
    <Dialog title={product ? 'Edit product' : 'Add product'} onClose={onClose}>
      <form onSubmit={(event) => {
        event.preventDefault();
        setLocalError(null);
        try {
          if (rateRupees.trim()) rupeesToPaisa(rateRupees);
        } catch (failure) {
          setLocalError(errorMessage(failure));
          return;
        }
        void onSave({ name, unitCode, rateRupees, inventoryTracking, notes }).catch(() => undefined);
      }}>
        <TextInput id="product-name" label="Product name" value={name} onChange={setName} />
        <label className="mb-4 block text-sm font-semibold text-slate-700">Unit
          <select id="product-unit" value={unitCode} onChange={(event) => setUnitCode(event.target.value as ProductUnitCode)} className="mt-1.5 w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5">
            <option value="Liters">Liters</option>
            <option value="Kg">Kg</option>
          </select>
        </label>
        <TextInput id="product-rate" label="Default selling rate in PKR (optional)" value={rateRupees} onChange={setRateRupees} inputMode="decimal" required={false} />
        <label className="mb-4 block text-sm font-semibold text-slate-700">Notes (optional)
          <textarea id="product-notes" value={notes} onChange={(event) => setNotes(event.target.value)} rows={3} className="mt-1.5 w-full resize-none rounded-xl border border-slate-300 bg-white px-3 py-2.5" />
        </label>
        <label className="mb-5 flex items-center gap-3 rounded-xl bg-slate-50 p-3 text-sm font-semibold text-slate-700">
          <input id="product-inventory-tracking" type="checkbox" checked={inventoryTracking} onChange={(event) => setInventoryTracking(event.target.checked)} className="h-5 w-5 accent-emerald-700" />
          Track this product’s stock
        </label>
        {localError && <Notice message={localError} />}
        <button id="save-product-button" disabled={busy} className="w-full rounded-xl bg-emerald-700 px-4 py-3 font-semibold text-white disabled:cursor-wait disabled:opacity-60">{busy ? 'Saving…' : product ? 'Save changes' : 'Add product'}</button>
      </form>
    </Dialog>
  );
}

function Dialog({ title, onClose, children }: { title: string; onClose(): void; children: ReactNode }) {
  return <div className="fixed inset-0 z-50 flex items-end bg-slate-950/40 p-0 sm:items-center sm:justify-center sm:p-4"><section role="dialog" aria-modal="true" className="max-h-[92vh] w-full overflow-y-auto rounded-t-3xl bg-white p-5 shadow-2xl sm:max-w-lg sm:rounded-3xl"><div className="mb-5 flex items-center justify-between"><h2 className="text-xl font-bold">{title}</h2><button type="button" onClick={onClose} className="rounded-lg p-2 text-slate-500" aria-label="Close"><X className="h-5 w-5" /></button></div>{children}</section></div>;
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
    <button id="create-owner-button" disabled={busy} className="w-full rounded-xl bg-emerald-700 px-4 py-3 font-semibold text-white disabled:opacity-60">{busy ? 'Please wait…' : 'Create owner account'}</button>
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
    <button id="login-button" disabled={busy} className="w-full rounded-xl bg-emerald-700 px-4 py-3 font-semibold text-white disabled:opacity-60">{busy ? 'Please wait…' : 'Sign in securely'}</button>
  </AuthCard>;
}

function AuthCard({ title, description, error, onSubmit, children }: { title: string; description: string; error: string | null; onSubmit(event: FormEvent): void; children: ReactNode }) {
  return <main className="min-h-screen bg-[#f7f6f0] px-5 py-10 text-slate-900"><form onSubmit={onSubmit} className="mx-auto max-w-md rounded-3xl border border-slate-200 bg-white p-6 shadow-xl shadow-emerald-950/5"><ShieldCheck className="mb-4 h-10 w-10 text-emerald-700" /><p className="text-xs font-bold uppercase tracking-[0.2em] text-emerald-700">Saaf Milk</p><h1 className="mt-1 text-2xl font-bold">{title}</h1><p className="mb-6 mt-2 text-sm text-slate-600">{description}</p>{children}{error && <Notice message={error} />}</form></main>;
}

function TextInput({ id, label, value, onChange, type = 'text', autoComplete, inputMode, required = true }: { id: string; label: string; value: string; onChange(value: string): void; type?: string; autoComplete?: string; inputMode?: 'decimal'; required?: boolean }) {
  return <label className="mb-4 block text-sm font-semibold text-slate-700">{label}<input id={id} required={required} value={value} onChange={(event) => onChange(event.target.value)} type={type} autoComplete={autoComplete} inputMode={inputMode} className="mt-1.5 w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5 outline-none focus:border-emerald-600 focus:ring-2 focus:ring-emerald-100" /></label>;
}

function FilterButton({ id, active, onClick, children }: { id: string; active: boolean; onClick(): void; children: ReactNode }) {
  return <button id={id} onClick={onClick} className={`rounded-lg px-3 py-2 text-xs font-bold ${active ? 'bg-white text-emerald-800 shadow-sm' : 'text-slate-500'}`}>{children}</button>;
}

function SummaryCard({ label, value }: { label: string; value: string }) {
  return <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm"><p className="text-xs font-semibold uppercase tracking-wide text-slate-500">{label}</p><p className="mt-1 text-2xl font-bold text-slate-900">{value}</p></div>;
}

function Notice({ message, success = false }: { message: string; success?: boolean }) {
  return <p role="status" className={`mb-4 flex items-start gap-2 rounded-xl px-3 py-2 text-sm ${success ? 'bg-emerald-50 text-emerald-800' : 'bg-rose-50 text-rose-800'}`}>{success && <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0" />}{message}</p>;
}

function CenteredMessage({ title, message }: { title: string; message: string }) {
  return <main className="flex min-h-screen items-center justify-center bg-[#f7f6f0] px-5 text-slate-900"><section className="max-w-md rounded-3xl border border-slate-200 bg-white p-7 text-center shadow-xl"><ShieldCheck className="mx-auto mb-4 h-10 w-10 text-emerald-700" /><h1 className="text-xl font-bold">{title}</h1><p className="mt-2 text-sm leading-6 text-slate-600">{message}</p></section></main>;
}

function plainRole(roleCode: string | undefined): string {
  if (roleCode === 'owner') return 'Owner';
  if (roleCode === 'manager') return 'Manager';
  if (roleCode === 'cashier') return 'Cashier';
  if (roleCode === 'delivery') return 'Delivery staff';
  return roleCode ?? 'User';
}
