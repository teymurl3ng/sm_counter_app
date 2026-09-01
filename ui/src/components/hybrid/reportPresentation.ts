export type CoveragePresentation = {
  label: string;
  explanation: string;
  complete: boolean;
};

export function coveragePresentation(code: string): CoveragePresentation {
  if (code === 'complete') {
    return {
      label: 'Cost history complete',
      explanation: 'The app has saved cost information for the stock sold in this period.',
      complete: true,
    };
  }
  if (code === 'missing_basis') {
    return {
      label: 'Starting cost needed',
      explanation: 'Some sold stock came from opening stock without a saved starting cost. Add that cost before relying on the result.',
      complete: false,
    };
  }
  if (code === 'stale_conversion_policy') {
    return {
      label: 'Yogurt cost policy needs review',
      explanation: 'The saved yogurt-making cost is missing or out of date for part of this period.',
      complete: false,
    };
  }
  if (code === 'unsupported_flow') {
    return {
      label: 'Some stock cost is not supported yet',
      explanation: 'At least one recorded stock movement cannot yet be assigned a dependable cost by this app version.',
      complete: false,
    };
  }
  return {
    label: 'Cost information incomplete',
    explanation: 'The app has a mixture of complete and incomplete cost information for this period.',
    complete: false,
  };
}

export function reportPeriodLabel(start: string, through: string): string {
  return start === through ? start : `${start} through ${through}`;
}
