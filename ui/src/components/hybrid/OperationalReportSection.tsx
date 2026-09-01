import type { CashierSummaryDto, DashboardDto } from '../../bridge/contract.generated';
import { formatPaisa } from '../../bridge/nativeBridge';
import { SummaryCard } from './HybridUi';

export function OperationalReportSection({ dashboard, cashierSummary }: {
  dashboard: DashboardDto;
  cashierSummary: CashierSummaryDto | null;
}) {
  return <div className="space-y-5">
    <section>
      <h2 className="text-lg font-bold">Current stock</h2>
      <p className="mt-1 text-sm text-slate-600">Stock is the current position, even when you choose an earlier report period.</p>
      <div className="mt-3 grid grid-cols-2 gap-3 sm:grid-cols-3">
        {dashboard.stock.map((item) => <SummaryCard key={item.productId} label={item.name} value={`${item.quantityOnHand} ${item.unit}`} />)}
      </div>
    </section>
    <section>
      <h2 className="text-lg font-bold">Work recorded in this period</h2>
      <div className="mt-3 grid grid-cols-2 gap-3 sm:grid-cols-4">
        <SummaryCard label="Milk bought" value={`${dashboard.totals.purchasedMilkLiters} L`} />
        <SummaryCard label="Quantity sold" value={dashboard.totals.saleQuantity} />
        <SummaryCard label="Yogurt made" value={`${dashboard.totals.producedYogurtKg} kg`} />
        <SummaryCard label="Milk used for yogurt" value={`${dashboard.totals.productionMilkUsedLiters} L`} />
      </div>
    </section>
    {cashierSummary && <section>
      <h2 className="text-lg font-bold">Sales and payment accounts</h2>
      <div className="mt-3 grid grid-cols-2 gap-3 sm:grid-cols-3">
        <SummaryCard label="Sales before delivery charges" value={formatPaisa(cashierSummary.salesValuePaisa)} />
        <SummaryCard label="Customer payments collected" value={formatPaisa(cashierSummary.creditCollectionsPaisa)} />
        {cashierSummary.paymentAccounts.map((account) => <SummaryCard key={account.accountId} label={`${account.name} now`} value={formatPaisa(account.canonicalBalancePaisa)} />)}
      </div>
    </section>}
  </div>;
}
