import { useState } from 'react';
import type { OpeningStateDto, ProductDto } from '../../bridge/contract.generated';
import { paisaToRupeesInput, rupeesToPaisa, validateDecimalInput } from '../../bridge/nativeBridge';
import { Dialog, Notice, TextInput } from './HybridUi';
import { errorMessage, type OpeningAccountOption, type OpeningStateDraft } from './hybridModels';

export function OpeningStateForm({ busy, current, products, accounts, onCancel, onSave }: {
  busy: boolean;
  current: OpeningStateDto | null;
  products: ReadonlyArray<ProductDto>;
  accounts: ReadonlyArray<OpeningAccountOption>;
  onCancel?(): void;
  onSave(draft: OpeningStateDraft, passphrase: string, correction: boolean): Promise<void>;
}) {
  const [draft, setDraft] = useState<OpeningStateDraft>(() => initialDraft(current));
  const [confirmed, setConfirmed] = useState(false);
  const [passphrase, setPassphrase] = useState('');
  const [showPassphrase, setShowPassphrase] = useState(false);
  const [localError, setLocalError] = useState<string | null>(null);
  const correction = current !== null;
  const updateMap = (key: 'productQuantities' | 'accountBalancesRupees', id: string, value: string) =>
    setDraft((valueBefore) => ({ ...valueBefore, [key]: { ...valueBefore[key], [id]: value } }));

  const review = () => {
    setLocalError(null);
    try {
      if (!draft.openingDate) throw new Error('Choose the date these starting balances belong to.');
      Object.values(draft.productQuantities).filter((value) => value.trim()).forEach((value) => validateDecimalInput(value, 'each stock quantity'));
      Object.values(draft.accountBalancesRupees).filter((value) => value.trim()).forEach(rupeesToPaisa);
      if (!confirmed) throw new Error('Tick the confirmation after checking the figures.');
      setShowPassphrase(true);
    } catch (failure) {
      setLocalError(errorMessage(failure));
    }
  };

  return <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
    <h2 className="font-bold">{correction ? 'Correct opening balances' : 'Enter opening balances'}</h2>
    <p className="mb-5 mt-1 text-sm leading-6 text-slate-600">{correction
      ? 'Enter the complete corrected starting picture—not only what changed. Zero removes an earlier starting amount.'
      : 'Use only amounts that already existed when this installation began. This is optional; if everything was zero, leave it unrecorded.'}</p>

    <label className="mb-5 block text-sm font-semibold text-slate-700">Opening date
      <input id="opening-date" type="date" required value={draft.openingDate} onChange={(event) => setDraft({ ...draft, openingDate: event.target.value })} className="mt-1.5 w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5" />
    </label>

    <BalanceSection title="Stock already in the shop" hint="Enter physical quantity, such as liters or kilograms.">
      {products.length === 0 ? <Empty>No stock-tracked products are available.</Empty> : products.map((product) => <AmountRow key={product.productId} id={`opening-product-${product.productId}`} name={product.name} suffix={product.unit} value={draft.productQuantities[product.productId] ?? ''} onChange={(value) => updateMap('productQuantities', product.productId, value)} />)}
    </BalanceSection>

    {(['Cash or bank', 'Customer owes shop', 'Shop owes supplier'] as const).map((kind) => {
      const rows = accounts.filter((account) => account.kind === kind);
      return <BalanceSection key={kind} title={kind} hint={kind === 'Cash or bank' ? 'Money available in this account.' : kind === 'Customer owes shop' ? 'Unpaid amount the customer already owed the shop.' : 'Unpaid amount the shop already owed the supplier.'}>
        {rows.length === 0 ? <Empty>None available.</Empty> : rows.map((account) => <AmountRow key={account.accountId} id={`opening-account-${account.accountId}`} name={account.name} suffix="PKR" value={draft.accountBalancesRupees[account.accountId] ?? ''} onChange={(value) => updateMap('accountBalancesRupees', account.accountId, value)} />)}
      </BalanceSection>;
    })}

    <label className="mb-4 block text-sm font-semibold text-slate-700">Notes (optional)
      <textarea id="opening-notes" rows={3} value={draft.notes} onChange={(event) => setDraft({ ...draft, notes: event.target.value })} className="mt-1.5 w-full resize-none rounded-xl border border-slate-300 bg-white px-3 py-2.5" />
    </label>
    <label className="mb-4 flex items-start gap-3 rounded-xl bg-amber-50 p-3 text-sm leading-5 text-amber-950">
      <input id="confirm-opening-state" type="checkbox" checked={confirmed} onChange={(event) => setConfirmed(event.target.checked)} className="mt-1 h-4 w-4" />
      <span>I checked these as the complete {correction ? 'corrected ' : ''}starting balances. They are not today’s sales, purchases, or payments.</span>
    </label>
    {localError && <Notice message={localError} />}
    <div className="flex gap-3">
      {onCancel && <button type="button" disabled={busy} onClick={onCancel} className="flex-1 rounded-xl border border-slate-300 px-4 py-3 font-semibold text-slate-700">Cancel</button>}
      <button id="review-opening-state-button" type="button" disabled={busy} onClick={review} className="flex-1 rounded-xl bg-emerald-700 px-4 py-3 font-semibold text-white disabled:opacity-50">Review & confirm</button>
    </div>

    {showPassphrase && <Dialog title={correction ? 'Confirm correction' : 'Confirm opening balances'} onClose={() => { if (!busy) { setShowPassphrase(false); setPassphrase(''); } }}>
      <p className="mb-4 text-sm leading-6 text-slate-600">Enter your passphrase. This protects a sensitive change to the shop’s starting figures.</p>
      <TextInput id="opening-passphrase" label="Your passphrase" type="password" autoComplete="current-password" value={passphrase} onChange={setPassphrase} />
      {localError && <Notice message={localError} />}
      <button id="save-opening-state-button" disabled={busy || !passphrase} onClick={() => { void onSave(draft, passphrase, correction).then(() => { setShowPassphrase(false); setPassphrase(''); onCancel?.(); }).catch((failure) => setLocalError(errorMessage(failure))); }} className="w-full rounded-xl bg-emerald-700 px-4 py-3 font-semibold text-white disabled:opacity-50">{busy ? 'Saving…' : correction ? 'Save correction' : 'Record opening balances'}</button>
    </Dialog>}
  </section>;
}

function initialDraft(current: OpeningStateDto | null): OpeningStateDraft {
  return {
    openingDate: current?.openingDate ?? localDate(),
    productQuantities: Object.fromEntries(current?.inventoryBalances.map((item) => [item.productId, item.quantity]) ?? []),
    accountBalancesRupees: Object.fromEntries(current?.accountBalances.map((item) => [item.accountId, paisaToRupeesInput(item.balancePaisa)]) ?? []),
    notes: current?.notes ?? '',
  };
}

function localDate() {
  const now = new Date();
  const offset = now.getTimezoneOffset() * 60_000;
  return new Date(now.getTime() - offset).toISOString().slice(0, 10);
}

function BalanceSection({ title, hint, children }: { title: string; hint: string; children: React.ReactNode }) {
  return <div className="mb-5 rounded-xl border border-slate-200 p-4"><h3 className="font-semibold">{title}</h3><p className="mb-3 mt-1 text-xs leading-5 text-slate-500">{hint}</p><div className="space-y-3">{children}</div></div>;
}

function AmountRow({ id, name, suffix, value, onChange }: { id: string; name: string; suffix: string; value: string; onChange(value: string): void }) {
  return <label className="grid grid-cols-[1fr_minmax(8rem,11rem)] items-center gap-3 text-sm"><span>{name}</span><span className="relative"><input id={id} inputMode="decimal" placeholder="0" value={value} onChange={(event) => onChange(event.target.value)} className="w-full rounded-lg border border-slate-300 py-2 pl-3 pr-12 text-right" /><span className="pointer-events-none absolute right-2 top-2.5 text-xs text-slate-400">{suffix}</span></span></label>;
}

function Empty({ children }: { children: React.ReactNode }) {
  return <p className="text-sm text-slate-400">{children}</p>;
}
