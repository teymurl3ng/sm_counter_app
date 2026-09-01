import type { CostComponentCode } from '../../bridge/contract.generated';

export function componentLabel(code: CostComponentCode): string {
  const labels: Record<CostComponentCode, string> = {
    culture: 'Yogurt culture',
    packaging: 'Production packaging',
    variable_energy: 'Energy used to make yogurt',
    direct_labor: 'Direct yogurt-making work',
    fixed_production_overhead: 'Fixed production overhead',
  };
  return labels[code];
}

export function reviewReasonLabel(code: string): string {
  const labels: Record<string, string> = {
    monthly: 'Regular monthly review',
    component_change: 'A component cost changed materially',
    capacity_change: 'Normal production capacity changed materially',
    process_change: 'The yogurt-making process changed',
    quarterly_variance: 'Quarterly actual-versus-standard review',
  };
  return labels[code] ?? code;
}

export function basisKindLabel(code: string): string {
  return code === 'documented_historical' ? 'Documented historical cost' : 'Owner estimate';
}

export function yieldStatus(code: string): { label: string; explanation: string; tone: string } {
  if (code === 'within_expected_range') return { label: 'Within expected range', explanation: 'This batch is inside the approved shop-specific range.', tone: 'text-emerald-700' };
  if (code === 'review_required') return { label: 'Review this batch', explanation: 'The measured output is outside the approved range. This is a warning, not an automatic loss.', tone: 'text-amber-800' };
  return { label: 'Calibration not ready', explanation: 'The shop does not yet have an approved yield baseline for this batch.', tone: 'text-slate-600' };
}

export function nextMondayInput(): string {
  const value = localDateValue(new Date());
  const date = new Date(`${value}T00:00:00Z`);
  const days = ((8 - date.getUTCDay()) % 7) || 7;
  date.setUTCDate(date.getUTCDate() + days);
  return date.toISOString().slice(0, 10);
}

export function nextMonthInput(): string {
  const now = new Date();
  const next = new Date(Date.UTC(now.getFullYear(), now.getMonth() + 1, 1));
  return next.toISOString().slice(0, 7);
}

export function periodEnd(start: string, category: string): string {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(start)) return '';
  const date = new Date(`${start}T00:00:00Z`);
  if (category === 'Milk purchases') date.setUTCDate(date.getUTCDate() + 7);
  else date.setUTCMonth(date.getUTCMonth() + 1);
  return date.toISOString().slice(0, 10);
}

function localDateValue(date: Date): string {
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}

export function decimalPaisaPerKg(value: string): string {
  const negative = value.startsWith('-');
  const unsigned = negative ? value.slice(1) : value;
  const [whole = '0', fraction = ''] = unsigned.split('.');
  const digits = `${whole}${fraction}`.replace(/^0+(?=\d)/, '').padStart(fraction.length + 1, '0');
  const padded = digits.padStart(fraction.length + 3, '0');
  const actualSplit = padded.length - fraction.length - 2;
  const rupeesWhole = padded.slice(0, actualSplit) || '0';
  const rupeesFraction = padded.slice(actualSplit).replace(/0+$/, '');
  return `${negative ? '-' : ''}PKR ${rupeesWhole}${rupeesFraction ? `.${rupeesFraction}` : ''} per kg`;
}

export function lossReasonLabel(code: string): string {
  const labels: Record<string, string> = {
    spill: 'Spill',
    contamination: 'Contamination',
    power_failure: 'Power failure',
    equipment_failure: 'Equipment failure',
    measurement_correction: 'Measurement correction',
    other: 'Other',
  };
  return labels[code] ?? code;
}
