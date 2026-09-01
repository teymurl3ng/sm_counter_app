import type { EventMutationResultDto, IsoDateString, PurchaseRecordedResultDto, SaleRecordedResultDto, SessionHandle } from '../../bridge/contract.generated';
import { callNative, rupeesToPaisa, validateDecimalInput } from '../../bridge/nativeBridge';
import type { ExpenseDraft, PaymentDraft, ProductionDraft, PurchaseDraft, SalaryPaymentDraft, SaleDraft, SpoilageDraft } from './hybridModels';
import type { RefreshPlan, RunAndRefresh } from './authorizedDataRefresh';

const PURCHASE_REFRESH: RefreshPlan = ['masterData', 'dashboard', 'financialOverview', 'eventHistory'];
const SALE_REFRESH: RefreshPlan = ['masterData', 'dashboard', 'cashierSummary', 'financialOverview', 'eventHistory', 'reconciliationArchive'];
const PRODUCTION_REFRESH: RefreshPlan = ['masterData', 'dashboard', 'financialOverview', 'eventHistory'];
const MONEY_REFRESH: RefreshPlan = ['masterData', 'dashboard', 'cashierSummary', 'financialOverview', 'eventHistory', 'reconciliationArchive'];
const SPOILAGE_REFRESH: RefreshPlan = ['masterData', 'dashboard', 'financialOverview', 'spoilageHistory', 'eventHistory'];

export function dailyOperationsBridgeActions(
  session: SessionHandle | null,
  runAndRefresh: RunAndRefresh,
) {
  const recordPurchase = (draft: PurchaseDraft): Promise<PurchaseRecordedResultDto> => runAndRefresh(async () => {
    if (!session) throw new Error('Sign in to record a purchase.');
    return callNative('command.recordPurchase', {
      vendorAccountId: draft.vendorAccountId,
      occurredAt: draft.occurredAt as IsoDateString,
      rawLiters: validateDecimalInput(draft.rawLiters, 'the milk received'),
      fatPercent: validateDecimalInput(draft.fatPercent, 'the fat percentage'),
      lrReading: validateDecimalInput(draft.lrReading, 'the LR reading'),
      ...(draft.notes.trim() ? { notes: draft.notes.trim() } : {}),
    }, session);
  }, 'Supplier milk purchase recorded.', PURCHASE_REFRESH);

  const recordSale = (draft: SaleDraft): Promise<SaleRecordedResultDto> => runAndRefresh(async () => {
    if (!session) throw new Error('Sign in to record a sale.');
    const channelCode = draft.paymentKind === 'credit' ? `credit-${draft.channelKind}` : draft.channelKind;
    return callNative('command.recordSale', {
      productId: draft.productId,
      occurredAt: draft.occurredAt as IsoDateString,
      channelCode,
      unitRatePaisa: rupeesToPaisa(draft.unitRateRupees),
      quantity: draft.entryMode === 'quantity' ? validateDecimalInput(draft.quantity, 'the quantity sold') : null,
      valuePaisa: draft.entryMode === 'value' ? rupeesToPaisa(draft.valueRupees) : null,
      paymentKindCode: draft.paymentKind,
      paymentAccountId: draft.paymentKind === 'account' ? draft.paymentAccountId : null,
      customerAccountId: draft.paymentKind === 'credit' ? draft.customerAccountId : null,
      surchargeEnabled: draft.surchargeEnabled,
      surchargePaisa: draft.surchargeEnabled && draft.surchargeRupees.trim() ? rupeesToPaisa(draft.surchargeRupees) : null,
      ...(draft.notes.trim() ? { notes: draft.notes.trim() } : {}),
    }, session);
  }, 'Sale recorded successfully.', SALE_REFRESH);

  const recordProduction = (draft: ProductionDraft): Promise<EventMutationResultDto> => runAndRefresh(async () => {
    if (!session) throw new Error('Sign in to record production.');
    return callNative('command.recordProduction', {
      occurredAt: draft.occurredAt as IsoDateString,
      milkLitersUsed: validateDecimalInput(draft.milkLitersUsed, 'the milk used'),
      yogurtKgProduced: validateDecimalInput(draft.yogurtKgProduced, 'the yogurt produced'),
      ...(draft.notes.trim() ? { notes: draft.notes.trim() } : {}),
    }, session);
  }, 'Production batch recorded.', PRODUCTION_REFRESH);

  const recordExpense = (draft: ExpenseDraft): Promise<EventMutationResultDto> => runAndRefresh(async () => {
    if (!session) throw new Error('Sign in to record an expense.');
    return callNative('command.recordExpense', {
      expenseCategoryAccountId: draft.expenseCategoryAccountId,
      paymentAccountId: draft.paymentAccountId,
      amountPaisa: rupeesToPaisa(draft.amountRupees),
      occurredAt: draft.occurredAt as IsoDateString,
      ...(draft.notes.trim() ? { notes: draft.notes.trim() } : {}),
    }, session);
  }, 'Expense recorded successfully.', MONEY_REFRESH);

  const recordPayment = (draft: PaymentDraft): Promise<EventMutationResultDto> => runAndRefresh(async () => {
    if (!session) throw new Error('Sign in to record a payment.');
    const common = {
      paymentAccountId: draft.paymentAccountId,
      amountPaisa: rupeesToPaisa(draft.amountRupees),
      occurredAt: draft.occurredAt as IsoDateString,
      ...(draft.notes.trim() ? { notes: draft.notes.trim() } : {}),
    };
    return draft.kind === 'supplier'
      ? callNative('command.recordVendorPayment', { vendorAccountId: draft.partyAccountId, ...common }, session)
      : callNative('command.recordCreditCollection', { customerAccountId: draft.partyAccountId, ...common }, session);
  }, draft.kind === 'supplier' ? 'Supplier payment recorded.' : 'Customer collection recorded.', MONEY_REFRESH);

  const recordSalaryPayment = (draft: SalaryPaymentDraft): Promise<EventMutationResultDto> => runAndRefresh(async () => {
    if (!session) throw new Error('Sign in to record a salary payment.');
    return callNative('command.recordSalaryPayment', {
      employeeAccountId: draft.employeeAccountId,
      paymentAccountId: draft.paymentAccountId,
      amountPaisa: rupeesToPaisa(draft.amountRupees),
      occurredAt: draft.occurredAt as IsoDateString,
      ...(draft.notes.trim() ? { notes: draft.notes.trim() } : {}),
    }, session);
  }, 'Salary payment recorded.', MONEY_REFRESH);

  const recordSpoilage = (draft: SpoilageDraft): Promise<EventMutationResultDto> => runAndRefresh(async () => {
    if (!session) throw new Error('Sign in to record spoiled stock.');
    return callNative('command.recordSpoilage', {
      productId: draft.productId,
      quantity: validateDecimalInput(draft.quantity, 'the quantity lost'),
      occurredAt: draft.occurredAt as IsoDateString,
      reasonCode: draft.reasonCode,
      ...(draft.notes.trim() ? { notes: draft.notes.trim() } : {}),
    }, session);
  }, 'Spoiled stock recorded.', SPOILAGE_REFRESH);

  return { recordExpense, recordPayment, recordProduction, recordPurchase, recordSalaryPayment, recordSale, recordSpoilage };
}
