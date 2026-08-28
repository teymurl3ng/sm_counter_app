import type { CommandRecordOpeningStateRequest, ExpenseCategoryDto, Int64String, IsoDateString, SessionHandle } from '../../bridge/contract.generated';
import { callNative, rupeesToPaisa, validateDecimalInput } from '../../bridge/nativeBridge';
import type { BusinessRatesDraft, OpeningStateDraft, StoreIdentityDraft } from './hybridModels';

type RunAndRefresh = (operation: () => Promise<void>, success: string) => Promise<void>;

export function settingsBridgeActions(session: SessionHandle | null, runAndRefresh: RunAndRefresh) {
  const saveStoreIdentity = (draft: StoreIdentityDraft) => runAndRefresh(async () => {
    if (!session) return;
    await callNative('command.saveStoreIdentity', {
      storeName: draft.storeName,
      phone: draft.phone.trim() || null,
      address: draft.address.trim() || null,
    }, session);
  }, 'Shop details saved.');

  const updateBusinessSettings = (draft: BusinessRatesDraft) => runAndRefresh(async () => {
    if (!session) return;
    await callNative('command.updateBusinessSettings', {
      vendorMilkRatePaisaPerUnit: positivePaisa(draft.vendorMilkRateRupees, 'supplier milk buying rate'),
      vendorDeliveryFeePaisaPerUnit: positivePaisa(draft.vendorDeliveryFeeRupees, 'supplier delivery fee'),
      tsStandardPercent: positiveDecimal(draft.tsStandardPercent, 'milk quality standard'),
      retailMilkRatePaisaPerUnit: positivePaisa(draft.retailMilkRateRupees, 'retail milk selling rate'),
      retailYogurtRatePaisaPerUnit: positivePaisa(draft.retailYogurtRateRupees, 'retail yogurt selling rate'),
      defaultDeliverySurchargePaisa: positivePaisa(draft.defaultDeliverySurchargeRupees, 'default delivery surcharge'),
    }, session);
  }, 'Shop-wide rates saved.');

  const createExpenseCategory = (displayName: string) => runAndRefresh(async () => {
    if (session) await callNative('command.createExpenseCategory', { displayName }, session);
  }, 'Expense category added.');

  const setExpenseCategoryArchived = (category: ExpenseCategoryDto, archived: boolean) => runAndRefresh(async () => {
    if (session) await callNative('command.setExpenseCategoryArchived', { accountId: category.accountId, archived }, session);
  }, archived ? `${category.displayName} moved to inactive categories.` : `${category.displayName} restored to active categories.`);

  const saveOpeningState = (draft: OpeningStateDraft, passphrase: string, correction: boolean) => runAndRefresh(async () => {
    if (!session) return;
    const payload: CommandRecordOpeningStateRequest = {
      openingDate: draft.openingDate as IsoDateString,
      inventoryBalances: (Object.keys(draft.productQuantities) as Int64String[])
        .filter((productId) => draft.productQuantities[productId].trim() !== '' && !isZero(draft.productQuantities[productId]))
        .map((productId) => ({ productId, quantity: validateDecimalInput(draft.productQuantities[productId], 'each stock quantity') })),
      accountBalances: (Object.keys(draft.accountBalancesRupees) as Int64String[])
        .filter((accountId) => draft.accountBalancesRupees[accountId].trim() !== '' && rupeesToPaisa(draft.accountBalancesRupees[accountId]) !== '0')
        .map((accountId) => ({ accountId, balancePaisa: rupeesToPaisa(draft.accountBalancesRupees[accountId]) })),
      ...(draft.notes.trim() ? { notes: draft.notes.trim() } : {}),
    };
    if (!correction && payload.inventoryBalances.length === 0 && payload.accountBalances.length === 0) {
      throw new Error('There is nothing to record. If every starting amount is zero, skip this setup.');
    }
    await callNative('command.auth.reauthenticate', { passphrase }, session);
    if (correction) await callNative('command.correctOpeningState', payload, session);
    else await callNative('command.recordOpeningState', payload, session);
  }, correction ? 'Opening balances corrected. The earlier record remains in history.' : 'Opening balances recorded.');

  return { createExpenseCategory, saveOpeningState, saveStoreIdentity, setExpenseCategoryArchived, updateBusinessSettings };
}

function isZero(value: string) {
  return /^0(?:\.0+)?$/.test(validateDecimalInput(value, 'each stock quantity'));
}

function positivePaisa(value: string, label: string) {
  const paisa = rupeesToPaisa(value);
  if (paisa === '0') throw new Error(`Enter a ${label} greater than zero.`);
  return paisa;
}

function positiveDecimal(value: string, label: string) {
  const decimal = validateDecimalInput(value, label);
  if (/^0(?:\.0+)?$/.test(decimal)) throw new Error(`Enter a ${label} greater than zero.`);
  return decimal;
}
