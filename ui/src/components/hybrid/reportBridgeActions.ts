import type {
  CashierSummaryDto,
  DashboardDto,
  FinancialOverviewDto,
  IsoDateString,
  ProfitabilityReportDto,
  SessionHandle,
} from '../../bridge/contract.generated';
import { callNative } from '../../bridge/nativeBridge';

export type ReportBundle = {
  dashboard: DashboardDto;
  cashierSummary: CashierSummaryDto | null;
  financialOverview: FinancialOverviewDto | null;
  profitability: ProfitabilityReportDto | null;
};

export function reportBridgeActions(session: SessionHandle | null) {
  const loadReports = async (
    startDate: string,
    throughDate: string,
    canViewCashier: boolean,
    canViewFinancial: boolean,
  ): Promise<ReportBundle> => {
    if (!session) throw new Error('Sign in to view shop reports.');
    if (startDate > throughDate) throw new Error('The From date cannot be after the Through date.');
    const start = asIsoDate(startDate);
    const endExclusive = nextDate(throughDate);
    const range = { startDateInclusive: start, endDateExclusive: endExclusive };
    const [dashboard, cashierSummary, financialOverview, profitability] = await Promise.all([
      callNative('query.dashboard', range, session),
      canViewCashier ? callNative('query.cashierSummary', range, session) : Promise.resolve(null),
      canViewFinancial ? callNative('query.financialOverview', range, session) : Promise.resolve(null),
      canViewFinancial
        ? callNative('query.profitabilityReport', { startInclusive: start, endExclusive }, session)
        : Promise.resolve(null),
    ]);
    return { dashboard, cashierSummary, financialOverview, profitability };
  };
  return { loadReports };
}

function asIsoDate(value: string): IsoDateString {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) throw new Error('Choose a valid report date.');
  const parsed = new Date(`${value}T00:00:00Z`);
  if (Number.isNaN(parsed.getTime()) || parsed.toISOString().slice(0, 10) !== value) {
    throw new Error('Choose a valid report date.');
  }
  return value as IsoDateString;
}

function nextDate(value: string): IsoDateString {
  const parsed = new Date(`${asIsoDate(value)}T00:00:00Z`);
  parsed.setUTCDate(parsed.getUTCDate() + 1);
  const next = parsed.toISOString().slice(0, 10);
  if (!/^\d{4}-\d{2}-\d{2}$/.test(next)) throw new Error('Choose a Through date before 9999-12-31.');
  return next as IsoDateString;
}
