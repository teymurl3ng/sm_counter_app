import { useState } from 'react';
import { Building2, RefreshCw } from 'lucide-react';
import type { BusinessSettingsDto } from '../../bridge/contract.generated';
import { paisaToRupeesInput, rupeesToPaisa, validateDecimalInput } from '../../bridge/nativeBridge';
import { Notice, TextInput } from './HybridUi';
import { errorMessage, type BusinessRatesDraft, type StoreIdentityDraft } from './hybridModels';

export function BusinessSettingsPanel({ busy, settings, error, notice, onSaveStore, onSaveRates }: {
  busy: boolean;
  settings: BusinessSettingsDto | null;
  error: string | null;
  notice: string | null;
  onSaveStore(draft: StoreIdentityDraft): Promise<void>;
  onSaveRates(draft: BusinessRatesDraft): Promise<void>;
}) {
  if (!settings) return <p className="rounded-2xl border border-slate-200 bg-white p-5 text-sm text-slate-500"><RefreshCw className="mr-2 inline h-4 w-4 animate-spin" />Loading protected shop settings…</p>;
  return <>
    {(notice || error) && <Notice message={notice ?? error ?? ''} success={Boolean(notice)} />}
    <StoreIdentityForm key={`store-${settings.store?.updatedAt ?? 'new'}`} busy={busy} settings={settings} onSave={onSaveStore} />
    <RatesForm busy={busy} settings={settings} onSave={onSaveRates} />
  </>;
}

function StoreIdentityForm({ busy, settings, onSave }: { busy: boolean; settings: BusinessSettingsDto; onSave(draft: StoreIdentityDraft): Promise<void> }) {
  const [storeName, setStoreName] = useState(settings.store?.storeName ?? '');
  const [phone, setPhone] = useState(settings.store?.phone ?? '');
  const [address, setAddress] = useState(settings.store?.address ?? '');
  return <section className="mb-5 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
    <div className="mb-4 flex items-start gap-3"><div className="rounded-xl bg-emerald-50 p-2 text-emerald-700"><Building2 className="h-5 w-5" /></div><div><h2 className="font-bold">Shop identity</h2><p className="text-sm text-slate-500">The name and contact details shown by Saaf Milk.</p></div></div>
    <form onSubmit={(event) => { event.preventDefault(); void onSave({ storeName, phone, address }).catch(() => undefined); }}>
      <TextInput id="store-name" label="Shop name" value={storeName} onChange={setStoreName} autoComplete="organization" />
      <TextInput id="store-phone" label="Phone (optional)" value={phone} onChange={setPhone} autoComplete="tel" required={false} />
      <label className="mb-4 block text-sm font-semibold text-slate-700">Address (optional)<textarea id="store-address" value={address} onChange={(event) => setAddress(event.target.value)} rows={3} className="mt-1.5 w-full resize-none rounded-xl border border-slate-300 bg-white px-3 py-2.5" /></label>
      <p className="mb-4 rounded-xl bg-slate-50 px-3 py-2 text-sm text-slate-600">Currency: <strong>PKR</strong> — fixed for this shop database.</p>
      <button id="save-store-identity-button" disabled={busy} className="w-full rounded-xl bg-emerald-700 px-4 py-3 font-semibold text-white disabled:opacity-50">{busy ? 'Saving…' : 'Save shop details'}</button>
    </form>
  </section>;
}

function RatesForm({ busy, settings, onSave }: { busy: boolean; settings: BusinessSettingsDto; onSave(draft: BusinessRatesDraft): Promise<void> }) {
  const [draft, setDraft] = useState<BusinessRatesDraft>({
    vendorMilkRateRupees: paisaToRupeesInput(settings.vendorMilkRatePaisaPerUnit),
    vendorDeliveryFeeRupees: paisaToRupeesInput(settings.vendorDeliveryFeePaisaPerUnit),
    tsStandardPercent: settings.tsStandardPercent,
    retailMilkRateRupees: paisaToRupeesInput(settings.retailMilkRatePaisaPerUnit),
    retailYogurtRateRupees: paisaToRupeesInput(settings.retailYogurtRatePaisaPerUnit),
    defaultDeliverySurchargeRupees: paisaToRupeesInput(settings.defaultDeliverySurchargePaisa),
  });
  const [localError, setLocalError] = useState<string | null>(null);
  const field = (key: keyof BusinessRatesDraft) => (value: string) => setDraft((current) => ({ ...current, [key]: value }));
  return <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
    <h2 className="font-bold">Shop-wide rates</h2>
    <p className="mb-4 mt-1 text-sm leading-6 text-slate-500">These defaults are used by the native purchase, sale, and delivery calculations. They apply to the whole shop.</p>
    <form onSubmit={(event) => { event.preventDefault(); setLocalError(null); try { Object.entries(draft).forEach(([key, value]) => { if (key === 'tsStandardPercent') validateDecimalInput(value, 'milk quality standard'); else rupeesToPaisa(value); }); } catch (failure) { setLocalError(errorMessage(failure)); return; } void onSave(draft).catch(() => undefined); }}>
      <div className="grid gap-x-4 sm:grid-cols-2">
        <TextInput id="vendor-milk-rate" label="Supplier milk buying rate — PKR per unit" value={draft.vendorMilkRateRupees} onChange={field('vendorMilkRateRupees')} inputMode="decimal" />
        <TextInput id="vendor-delivery-fee" label="Supplier delivery fee — PKR per unit" value={draft.vendorDeliveryFeeRupees} onChange={field('vendorDeliveryFeeRupees')} inputMode="decimal" />
        <TextInput id="retail-milk-rate" label="Retail milk selling rate — PKR per unit" value={draft.retailMilkRateRupees} onChange={field('retailMilkRateRupees')} inputMode="decimal" />
        <TextInput id="retail-yogurt-rate" label="Retail yogurt selling rate — PKR per unit" value={draft.retailYogurtRateRupees} onChange={field('retailYogurtRateRupees')} inputMode="decimal" />
        <TextInput id="ts-standard-percent" label="Milk quality standard — total solids %" value={draft.tsStandardPercent} onChange={field('tsStandardPercent')} inputMode="decimal" />
        <TextInput id="default-delivery-surcharge" label="Default delivery surcharge — PKR" value={draft.defaultDeliverySurchargeRupees} onChange={field('defaultDeliverySurchargeRupees')} inputMode="decimal" />
      </div>
      <p className="mb-4 rounded-xl bg-sky-50 px-3 py-2 text-sm leading-5 text-sky-900">The milk quality percentage is used by the existing native supplier-purchase calculation. This screen does not calculate purchase prices itself.</p>
      {localError && <Notice message={localError} />}
      <button id="save-business-rates-button" disabled={busy} className="w-full rounded-xl bg-emerald-700 px-4 py-3 font-semibold text-white disabled:opacity-50">{busy ? 'Saving…' : 'Save shop-wide rates'}</button>
    </form>
  </section>;
}
