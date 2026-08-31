import type { AccountLedgerDto, PaisaString } from '../../bridge/contract.generated';
import { formatPaisa } from '../../bridge/nativeBridge';

export type LedgerKind = 'customer' | 'supplier';

export function balanceMeaning(kind: LedgerKind, value: PaisaString): { label: string; amount: string; tone: string } {
  const negative = value.startsWith('-');
  const zero = /^-?0+$/.test(value);
  if (zero) return { label: 'Settled', amount: formatPaisa(value), tone: 'text-emerald-700' };
  if (kind === 'customer') {
    return negative
      ? { label: 'Shop holds customer advance', amount: formatPaisa(absolutePaisa(value)), tone: 'text-sky-700' }
      : { label: 'Customer owes shop', amount: formatPaisa(value), tone: 'text-amber-700' };
  }
  return negative
    ? { label: 'Shop paid supplier in advance', amount: formatPaisa(absolutePaisa(value)), tone: 'text-sky-700' }
    : { label: 'Shop owes supplier', amount: formatPaisa(value), tone: 'text-rose-700' };
}

export function movementMeaning(kind: LedgerKind, value: PaisaString): string {
  if (/^-?0+$/.test(value)) return 'Balance unchanged';
  const increased = !value.startsWith('-');
  if (kind === 'customer') return increased ? 'Customer amount owed increased' : 'Customer amount owed decreased';
  return increased ? 'Shop amount owed increased' : 'Shop amount owed decreased';
}

export function eventLabel(code: string): string {
  const labels: Record<string, string> = {
    SALE: 'Credit sale',
    CREDIT_COLLECTION: 'Customer payment received',
    PURCHASE: 'Milk purchase',
    VENDOR_PAYMENT: 'Supplier payment made',
    OPENING: 'Opening balance',
  };
  return labels[code] ?? code.toLowerCase().split('_').map((word) => word.charAt(0).toUpperCase() + word.slice(1)).join(' ');
}

export function ledgerKind(ledger: AccountLedgerDto): LedgerKind {
  return ledger.categoryCode === 'Vendor' ? 'supplier' : 'customer';
}

function absolutePaisa(value: PaisaString): PaisaString {
  return (value.startsWith('-') ? value.slice(1) : value) as PaisaString;
}
