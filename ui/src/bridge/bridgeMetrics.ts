import type { BridgeAction } from './contract.generated';

type ActionMetric = {
  calls: number;
  failures: number;
  requestChars: number;
  responseChars: number;
  totalDurationMs: number;
};

export type BridgeMetricsSnapshot = {
  totalCalls: number;
  totalQueryCalls: number;
  totalRequestChars: number;
  totalResponseChars: number;
  actions: Partial<Record<BridgeAction, ActionMetric>>;
};

declare global {
  interface Window {
    __saafMilkBridgeMetrics?: {
      reset(): void;
      snapshot(): BridgeMetricsSnapshot;
    };
  }
}

const actionMetrics = new Map<BridgeAction, ActionMetric>();

export function recordBridgeRequest(action: BridgeAction, requestChars: number): number {
  const metric = metricFor(action);
  metric.calls += 1;
  metric.requestChars += requestChars;
  return performance.now();
}

export function recordBridgeResponse(action: BridgeAction, responseChars: number, startedAt: number, failed: boolean): void {
  const metric = metricFor(action);
  metric.responseChars += responseChars;
  metric.totalDurationMs += Math.max(0, performance.now() - startedAt);
  if (failed) metric.failures += 1;
}

function metricFor(action: BridgeAction): ActionMetric {
  const existing = actionMetrics.get(action);
  if (existing) return existing;
  const created: ActionMetric = { calls: 0, failures: 0, requestChars: 0, responseChars: 0, totalDurationMs: 0 };
  actionMetrics.set(action, created);
  return created;
}

function snapshot(): BridgeMetricsSnapshot {
  const actions = Object.fromEntries(
    [...actionMetrics].map(([action, metric]) => [action, { ...metric, totalDurationMs: Math.round(metric.totalDurationMs) }]),
  ) as BridgeMetricsSnapshot['actions'];
  const values = [...actionMetrics.values()];
  return {
    totalCalls: values.reduce((sum, metric) => sum + metric.calls, 0),
    totalQueryCalls: [...actionMetrics].reduce((sum, [action, metric]) => sum + (action.startsWith('query.') ? metric.calls : 0), 0),
    totalRequestChars: values.reduce((sum, metric) => sum + metric.requestChars, 0),
    totalResponseChars: values.reduce((sum, metric) => sum + metric.responseChars, 0),
    actions,
  };
}

if (typeof window !== 'undefined') {
  window.__saafMilkBridgeMetrics = Object.freeze({
    reset: () => actionMetrics.clear(),
    snapshot,
  });
}
