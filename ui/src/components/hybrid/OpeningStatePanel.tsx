import { useMemo, useState } from 'react';
import { History, RefreshCw } from 'lucide-react';
import type { BusinessSettingsDto, MasterDataDto, PaisaString, TransactionReferencesDto } from '../../bridge/contract.generated';
import { formatPaisa } from '../../bridge/nativeBridge';
import { Notice } from './HybridUi';
import type { OpeningAccountOption, OpeningStateDraft } from './hybridModels';
import { OpeningStateForm } from './OpeningStateForm';

export function OpeningStatePanel({ busy, settings, masterData, references, error, notice, onSave }: {
  busy: boolean;
  settings: BusinessSettingsDto | null;
  masterData: MasterDataDto | null;
  references: TransactionReferencesDto | null;
  error: string | null;
  notice: string | null;
  onSave(draft: OpeningStateDraft, passphrase: string, correction: boolean): Promise<void>;
}) {
  const [correcting, setCorrecting] = useState(false);
  const current = settings?.openingState ?? null;
  const productIds = new Set(current?.inventoryBalances.map((item) => item.productId) ?? []);
  const accountIds = new Set(current?.accountBalances.map((item) => item.accountId) ?? []);
  const products = masterData?.products.filter((product) => product.inventoryTracking && (!product.archived || productIds.has(product.productId))) ?? [];
  const accounts = useMemo(() => accountOptions(masterData, references, accountIds), [masterData, references, current?.eventId]);

  if (!settings || !masterData || !references) return <p className="rounded-2xl border border-slate-200 bg-white p-5 text-sm text-slate-500"><RefreshCw className="mr-2 inline h-4 w-4 animate-spin" />Loading protected opening details…</p>;
  if (!settings.store) return <section className="rounded-2xl border border-amber-200 bg-amber-50 p-5"><h2 className="font-bold text-amber-950">Save shop details first</h2><p className="mt-2 text-sm leading-6 text-amber-900">Opening balances need a shop identity. Use the Shop details & rates tab, save the shop name, then return here.</p></section>;

  return <>
    {(notice || error) && <Notice message={notice ?? error ?? ''} success={Boolean(notice)} />}
    {current && !correcting ? <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <div className="flex items-start gap-3"><div className="rounded-xl bg-emerald-50 p-2 text-emerald-700"><History className="h-5 w-5" /></div><div><h2 className="font-bold">Opening balances recorded</h2><p className="mt-1 text-sm text-slate-500">Starting picture dated {current.openingDate}</p></div></div>
      <div className="my-5 grid grid-cols-2 gap-3"><Summary label="Stock items" value={`${current.inventoryBalances.length}`} /><Summary label="Money balances" value={`${current.accountBalances.length}`} /></div>
      {current.accountBalances.length > 0 && <div className="mb-5 rounded-xl bg-slate-50 p-3 text-sm text-slate-600">Recorded money total: <strong>{formatPaisa(current.accountBalances.reduce((sum, item) => sum + BigInt(item.balancePaisa), 0n).toString() as PaisaString)}</strong></div>}
      {current.notes && <p className="mb-5 rounded-xl bg-slate-50 p-3 text-sm text-slate-600">{current.notes}</p>}
      <p className="mb-4 text-sm leading-6 text-slate-600">A correction creates a new history record. It does not erase the original.</p>
      <button id="correct-opening-state-button" disabled={busy} onClick={() => setCorrecting(true)} className="w-full rounded-xl border border-emerald-700 px-4 py-3 font-semibold text-emerald-800">Correct opening balances</button>
    </section> : <OpeningStateForm key={current?.eventId ?? 'initial'} busy={busy} current={current} products={products} accounts={accounts} onCancel={current ? () => setCorrecting(false) : undefined} onSave={onSave} />}
  </>;
}

function accountOptions(masterData: MasterDataDto | null, references: TransactionReferencesDto | null, existing: Set<string>): OpeningAccountOption[] {
  if (!masterData || !references) return [];
  return [
    ...references.paymentAccounts.map((account) => ({ ...account, kind: 'Cash or bank' as const })),
    ...masterData.customers.filter((account) => account.active || existing.has(account.accountId)).map((account) => ({ accountId: account.accountId, name: account.name, kind: 'Customer owes shop' as const })),
    ...masterData.vendors.filter((account) => account.active || existing.has(account.accountId)).map((account) => ({ accountId: account.accountId, name: account.name, kind: 'Shop owes supplier' as const })),
  ];
}

function Summary({ label, value }: { label: string; value: string }) {
  return <div className="rounded-xl bg-slate-50 p-3"><p className="text-xs font-semibold uppercase tracking-wide text-slate-500">{label}</p><p className="mt-1 text-xl font-bold">{value}</p></div>;
}
