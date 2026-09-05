import type { AccountPositionDto, FinancialOverviewDto, ProfitabilityReportDto } from '../../bridge/contract.generated';
import { formatPaisa } from '../../bridge/nativeBridge';
import { SummaryCard } from './HybridUi';
import { balanceMeaning, type LedgerKind } from './ledgerPresentation';
import { coveragePresentation } from './reportPresentation';

export function FinancialReportSection({ overview, profitability }: {
  overview: FinancialOverviewDto;
  profitability: ProfitabilityReportDto;
}) {
  const coverage = coveragePresentation(profitability.coverageCode);
  return <div className="space-y-5">
    <section className={`rounded-2xl border p-4 ${coverage.complete ? 'border-emerald-200 bg-emerald-50' : 'border-amber-300 bg-amber-50'}`}>
      <p className={`font-bold ${coverage.complete ? 'text-emerald-900' : 'text-amber-950'}`}>{coverage.label}</p>
      <p className="mt-1 text-sm leading-6 text-slate-700">{coverage.explanation}</p>
    </section>
    <section>
      <h2 className="text-lg font-bold">Money recorded in this period</h2>
      <div className="mt-3 grid grid-cols-2 gap-3 sm:grid-cols-4">
        <SummaryCard label="Sales" value={formatPaisa(overview.totals.salesValuePaisa)} />
        <SummaryCard label="Milk purchases" value={formatPaisa(overview.totals.purchaseAmountPaisa)} />
        <SummaryCard label="Shop expenses" value={formatPaisa(overview.totals.operatingExpensesPaisa)} />
        <SummaryCard label="Salary payments" value={formatPaisa(overview.totals.salaryPaymentsPaisa)} />
        <SummaryCard label="Paid to suppliers" value={formatPaisa(overview.totals.vendorPaymentsPaisa)} />
        <SummaryCard label="Collected from customers" value={formatPaisa(overview.totals.creditCollectionsPaisa)} />
        <SummaryCard label="Delivery charges" value={formatPaisa(profitability.deliverySurchargePaisa)} />
        <SummaryCard label="Estimated result from recorded data" value={profitability.estimatedRecordedResultPaisa === null ? 'Not available yet' : formatPaisa(profitability.estimatedRecordedResultPaisa)} />
      </div>
      <p className="mt-3 text-sm leading-6 text-slate-600">The estimated result uses saved product costs. It is not your bank balance, drawer cash, or personal take-home amount.</p>
    </section>
    <section>
      <h2 className="text-lg font-bold">Result by product</h2>
      {profitability.products.length === 0 ? <Empty message="No product sales were recorded in this period." /> : <div className="mt-3 grid gap-3 sm:grid-cols-2">
        {profitability.products.map((product) => {
          const productCoverage = coveragePresentation(product.coverageCode);
          return <article key={product.productId} className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
            <div className="flex items-start justify-between gap-3"><h3 className="font-bold">{product.productName}</h3><span className={`rounded-full px-2 py-1 text-xs font-semibold ${productCoverage.complete ? 'bg-emerald-100 text-emerald-800' : 'bg-amber-100 text-amber-900'}`}>{productCoverage.label}</span></div>
            <dl className="mt-3 space-y-2 text-sm">
              <Row label="Quantity sold" value={product.quantitySold} />
              <Row label="Sales" value={formatPaisa(product.salesRevenuePaisa)} />
              <Row label="Cost of products sold" value={product.costOfProductsSoldPaisa === null ? 'Not available' : formatPaisa(product.costOfProductsSoldPaisa)} />
              <Row label="Sales minus product cost" value={product.profitBeforeShopExpensesPaisa === null ? 'Not available' : formatPaisa(product.profitBeforeShopExpensesPaisa)} />
              <Row label="Gross margin" value={product.grossMarginPercent === null ? 'Not available' : `${product.grossMarginPercent}%`} />
              <Row label="Spoilage cost" value={product.spoilageCostPaisa === null ? 'Not available' : formatPaisa(product.spoilageCostPaisa)} />
              <Row label="Abnormal production loss" value={product.abnormalLossPaisa === null ? 'Not available' : formatPaisa(product.abnormalLossPaisa)} />
            </dl>
          </article>;
        })}
      </div>}
      <p className="mt-3 text-sm leading-5 text-slate-600">Basis: management estimate using saved product costs. Scope: recorded cost only; no lower-selling-price adjustment.</p>
    </section>
    <div className="grid gap-5 lg:grid-cols-2">
      <AccountList title="Customer balances now" empty="No customer balances are recorded." kind="customer" accounts={overview.customers} />
      <AccountList title="Supplier balances now" empty="No supplier balances are recorded." kind="supplier" accounts={overview.vendors} />
    </div>
  </div>;
}

function AccountList({ title, empty, kind, accounts }: { title: string; empty: string; kind: LedgerKind; accounts: ReadonlyArray<AccountPositionDto> }) {
  return <section><h2 className="text-lg font-bold">{title}</h2>{accounts.length === 0 ? <Empty message={empty} /> : <div className="mt-3 overflow-hidden rounded-2xl border border-slate-200 bg-white">{accounts.map((account) => { const meaning = balanceMeaning(kind, account.canonicalBalancePaisa); return <div key={account.accountId} className="flex items-center justify-between gap-4 border-b border-slate-100 px-4 py-3 last:border-b-0"><div><p className="font-semibold">{account.name}</p><p className={`text-xs ${meaning.tone}`}>{meaning.label}</p></div><span className="whitespace-nowrap text-sm font-bold tabular-nums">{meaning.amount}</span></div>; })}</div>}</section>;
}

function Row({ label, value }: { label: string; value: string }) {
  return <div className="grid grid-cols-[minmax(0,1fr)_auto] items-start gap-4"><dt className="text-slate-600">{label}</dt><dd className="whitespace-nowrap text-right font-semibold text-slate-900 tabular-nums">{value}</dd></div>;
}

function Empty({ message }: { message: string }) {
  return <p className="mt-3 rounded-2xl border border-dashed border-slate-300 bg-white p-4 text-sm text-slate-500">{message}</p>;
}
