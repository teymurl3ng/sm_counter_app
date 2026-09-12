import {
  BRIDGE_CONTRACT_SHA256,
  BRIDGE_CONTRACT_VERSION,
  type BridgeAction,
  type BridgeError,
  type BridgeInfoDto,
  type BridgeRequest,
  type BridgeRequestPayloadByAction,
  type BridgeResponse,
  type BridgeResponseByAction,
  type DecimalString,
  type PaisaString,
  type SessionHandle,
} from './contract.generated';
import { recordBridgeRequest, recordBridgeResponse } from './bridgeMetrics';

interface InjectedNativeBridge {
  postMessage(message: string): void;
  onmessage: ((event: MessageEvent<string>) => void) | null;
}

declare global {
  interface Window {
    saafMilkNative?: InjectedNativeBridge;
  }
}

interface PendingRequest {
  action: BridgeAction;
  startedAt: number;
  resolve(value: unknown): void;
  reject(reason: unknown): void;
  timeout: number;
}

const pending = new Map<string, PendingRequest>();
let installedBridge: InjectedNativeBridge | null = null;

const LONG_INTERACTIVE_ACTIONS = new Set<BridgeAction>([
  'system.exportEncryptedBackup',
  'system.inspectEncryptedBackup',
  'system.restoreInspectedBackup',
  'system.connectCloudBackup',
  'system.inspectCloudBackup',
  'system.restoreInspectedCloudBackup',
  'system.disconnectCloudBackup',
]);

function responseTimeoutMs(action: BridgeAction): number {
  if (LONG_INTERACTIVE_ACTIONS.has(action)) return 300_000;
  if (action === 'command.auth.enrollQuickUnlock' || action === 'command.auth.quickLogin' || action === 'command.auth.quickReauthenticate') return 120_000;
  return 30_000;
}

export const RUNTIME_ACTIONS = [
  'system.bridgeInfo',
  'query.bootstrapStatus',
  'command.auth.bootstrapOwner',
  'command.auth.login',
  'command.auth.logout',
  'command.auth.reauthenticate',
  'system.quickUnlockStatus',
  'command.auth.enrollQuickUnlock',
  'command.auth.removeQuickUnlock',
  'command.auth.quickLogin',
  'command.auth.quickReauthenticate',
  'query.currentSession',
  'query.masterData',
  'command.createProduct',
  'command.updateProduct',
  'command.setProductArchived',
  'command.createCustomer',
  'command.renameCustomer',
  'command.saveCustomerProfile',
  'command.createVendor',
  'command.renameVendor',
  'command.saveVendorProfile',
  'query.transactionReferences',
  'query.dashboard',
  'query.cashierSummary',
  'query.financialOverview',
  'query.profitabilityReport',
  'query.costingManagement',
  'query.planning',
  'query.users',
  'query.securityAudit',
  'command.auth.changeOwnPassphrase',
  'command.auth.recoverOwner',
  'command.auth.beginUserEnrollment',
  'command.auth.resumeUserEnrollment',
  'command.auth.completeEnrollment',
  'command.auth.changeUserRole',
  'command.auth.setUserEnabled',
  'command.auth.beginCredentialReset',
  'command.recordPurchase',
  'command.recordSale',
  'command.recordProduction',
  'command.recordVendorPayment',
  'command.recordCreditCollection',
  'command.recordExpense',
  'command.recordSalaryPayment',
  'query.spoilageHistory',
  'command.recordSpoilage',
  'query.deliveries',
  'command.createDelivery',
  'command.transitionDelivery',
  'command.linkDeliverySale',
  'command.archiveDelivery',
  'query.accountLedger',
  'query.eventHistory',
  'query.eventDetail',
  'command.correctEvent',
  'query.reconciliationArchive',
  'command.recordPhysicalCashCount',
  'command.closeBusinessDate',
  'command.reopenBusinessDate',
  'query.attendance',
  'command.createEmployee',
  'command.renameEmployee',
  'command.saveEmployeeProfile',
  'command.recordAttendance',
  'query.businessSettings',
  'command.saveStoreIdentity',
  'command.updateBusinessSettings',
  'command.createExpenseCategory',
  'command.setExpenseCategoryArchived',
  'command.recordOpeningState',
  'command.correctOpeningState',
  'command.upsertBudget',
  'command.copyPreviousBudget',
  'command.upsertCollectionsTarget',
  'command.approveCostingPolicy',
  'command.recordOpeningCostBasis',
  'command.approveYieldPolicy',
  'command.recordProductionLossAssessment',
  'system.exportEncryptedBackup',
  'system.inspectEncryptedBackup',
  'system.restoreInspectedBackup',
  'system.cloudBackupStatus',
  'system.connectCloudBackup',
  'system.configureCloudBackup',
  'system.runCloudBackupNow',
  'system.refreshCloudBackups',
  'system.inspectCloudBackup',
  'system.restoreInspectedCloudBackup',
  'system.disconnectCloudBackup',
] as const satisfies ReadonlyArray<BridgeAction>;

export class NativeBridgeError extends Error {
  readonly code: BridgeError['code'];
  readonly field: string | null;
  readonly retryable: boolean;

  constructor(error: BridgeError) {
    super(error.message);
    this.name = 'NativeBridgeError';
    this.code = error.code;
    this.field = error.field;
    this.retryable = error.retryable;
  }
}

function bridge(): InjectedNativeBridge {
  const current = window.saafMilkNative;
  if (!current) {
    throw new Error('Open Saaf Milk from the installed Android app to use protected records.');
  }
  if (installedBridge !== current) {
    current.onmessage = receiveMessage;
    installedBridge = current;
  }
  return current;
}

function receiveMessage(event: MessageEvent<string>): void {
  let response: BridgeResponse<BridgeAction>;
  try {
    response = JSON.parse(event.data) as BridgeResponse<BridgeAction>;
  } catch {
    return;
  }
  const item = pending.get(response.requestId);
  if (!item) return;
  pending.delete(response.requestId);
  window.clearTimeout(item.timeout);
  recordBridgeResponse(item.action, event.data.length, item.startedAt, !response.ok);
  if (response.ok) item.resolve(response.data);
  else item.reject(new NativeBridgeError(response.error));
}

export function callNative<A extends BridgeAction>(
  action: A,
  payload: BridgeRequestPayloadByAction[A],
  sessionHandle?: SessionHandle,
): Promise<BridgeResponseByAction[A]> {
  const requestId = crypto.randomUUID();
  const request: BridgeRequest<A> = {
    contractVersion: BRIDGE_CONTRACT_VERSION,
    requestId,
    action,
    ...(sessionHandle ? { sessionHandle } : {}),
    payload,
  };
  const serializedRequest = JSON.stringify(request);
  const startedAt = recordBridgeRequest(action, serializedRequest.length);
  return new Promise<BridgeResponseByAction[A]>((resolve, reject) => {
    const timeout = window.setTimeout(() => {
      pending.delete(requestId);
      recordBridgeResponse(action, 0, startedAt, true);
      reject(new Error('The app took too long to respond. Please try again.'));
    }, responseTimeoutMs(action));
    pending.set(requestId, {
      action,
      startedAt,
      resolve: (value) => resolve(value as BridgeResponseByAction[A]),
      reject,
      timeout,
    });
    try {
      bridge().postMessage(serializedRequest);
    } catch (error) {
      pending.delete(requestId);
      window.clearTimeout(timeout);
      recordBridgeResponse(action, 0, startedAt, true);
      reject(error);
    }
  });
}

export async function verifyNativeContract(): Promise<BridgeInfoDto> {
  const info = await callNative('system.bridgeInfo', {});
  const missing = RUNTIME_ACTIONS.filter((action) => !info.supportedActions.includes(action));
  if (
    info.contractVersion !== BRIDGE_CONTRACT_VERSION ||
    info.contractSha256 !== BRIDGE_CONTRACT_SHA256 ||
    missing.length > 0
  ) {
    throw new Error('App components do not match. Reinstall a complete Saaf Milk build.');
  }
  return info;
}

export function rupeesToPaisa(value: string): PaisaString {
  const clean = value.trim();
  const match = /^(0|[1-9][0-9]*)(?:\.([0-9]{1,2}))?$/.exec(clean);
  if (!match) throw new Error('Enter a PKR amount with no more than two decimal places.');
  const whole = match[1];
  const fraction = (match[2] ?? '').padEnd(2, '0');
  const combined = `${whole}${fraction}`.replace(/^0+(?=[0-9])/, '');
  const maximum = '9223372036854775807';
  if (combined.length > maximum.length || (combined.length === maximum.length && combined > maximum)) {
    throw new Error('This amount is outside the supported range.');
  }
  return combined as PaisaString;
}

export function formatPaisa(value: PaisaString | null): string {
  if (value === null) return 'Not set';
  const negative = value.startsWith('-');
  const digits = negative ? value.slice(1) : value;
  const padded = digits.padStart(3, '0');
  const whole = padded.slice(0, -2).replace(/\B(?=(\d{3})+(?!\d))/g, ',');
  const fraction = padded.slice(-2);
  return `PKR ${negative ? '-' : ''}${whole}.${fraction}`;
}

export function paisaToRupeesInput(value: PaisaString | null): string {
  if (value === null) return '';
  const negative = value.startsWith('-');
  const digits = negative ? value.slice(1) : value;
  const padded = digits.padStart(3, '0');
  const whole = padded.slice(0, -2).replace(/^0+(?=[0-9])/, '');
  const fraction = padded.slice(-2);
  return `${negative ? '-' : ''}${whole}.${fraction}`;
}

export function validateDecimalInput(value: string, label: string): DecimalString {
  const clean = value.trim();
  if (!/^(0|[1-9][0-9]*)(?:\.[0-9]+)?$/.test(clean)) {
    throw new Error(`Enter ${label} as zero or a positive number.`);
  }
  return clean as DecimalString;
}
