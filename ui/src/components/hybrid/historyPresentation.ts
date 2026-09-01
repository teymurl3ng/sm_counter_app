import type { CorrectionTypeCode, EventHistoryDto } from '../../bridge/contract.generated';

export function historyEventLabel(code: string): string {
  const labels: Record<string, string> = {
    PURCHASE_RECEIPT: 'Milk purchase', PRODUCTION: 'Yogurt production', SALE: 'Sale',
    VENDOR_PAYMENT: 'Supplier payment', CREDIT_COLLECTION: 'Customer payment received',
    EXPENSE: 'Shop expense', SALARY_PAYMENT: 'Salary payment', SPOILAGE: 'Spoiled or wasted stock',
    OPENING_STATE: 'Opening balances', CASH_COUNT: 'Cash count', DAILY_CLOSE: 'Daily close',
  };
  return labels[code] ?? code.toLowerCase().split('_').map((word) => word.charAt(0).toUpperCase() + word.slice(1)).join(' ');
}

export function correctionType(event: EventHistoryDto): CorrectionTypeCode | null {
  if (event.eventTypeCode === 'PURCHASE_RECEIPT') return 'purchase';
  if (event.eventTypeCode === 'PRODUCTION') return 'production';
  if (event.eventTypeCode === 'SALE') return 'sale';
  if (['VENDOR_PAYMENT', 'CREDIT_COLLECTION', 'EXPENSE', 'SALARY_PAYMENT'].includes(event.eventTypeCode)) return 'monetary';
  if (event.eventTypeCode === 'SPOILAGE') return 'spoilage';
  return null;
}

export function signedQuantity(value: string, unit: string): string {
  const prefix = value.startsWith('-') ? 'Removed' : 'Added';
  return `${prefix} ${value.replace(/^-/, '')} ${unit}`.trim();
}
