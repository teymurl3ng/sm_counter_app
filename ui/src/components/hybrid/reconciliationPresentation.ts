import type { CashCountDto, DailyCloseActionDto, IsoDateString, PaisaString, ReconciliationArchiveDto } from '../../bridge/contract.generated';
import { formatPaisa } from '../../bridge/nativeBridge';

export function latestCountForDate(archive: ReconciliationArchiveDto, date: string): CashCountDto | null {
  return archive.cashCounts.find((count) => count.businessDate === date) ?? null;
}

export function currentActionForDate(archive: ReconciliationArchiveDto, date: string): DailyCloseActionDto | null {
  const actions = archive.closeActions.filter((action) => action.businessDate === date);
  return actions[actions.length - 1] ?? null;
}

export function archiveDates(archive: ReconciliationArchiveDto): ReadonlyArray<IsoDateString> {
  return [...new Set([...archive.cashCounts.map((count) => count.businessDate), ...archive.closeActions.map((action) => action.businessDate)])].sort().reverse();
}

export function varianceText(variancePaisa: PaisaString): string {
  const amount = BigInt(variancePaisa);
  if (amount === 0n) return 'Exact match';
  const absolute = (amount < 0n ? -amount : amount).toString() as PaisaString;
  return amount < 0n ? `${formatPaisa(absolute)} short` : `${formatPaisa(absolute)} extra`;
}
