import type { ExpenseCategoryDto, SessionHandle } from '../../bridge/contract.generated';
import { callNative, rupeesToPaisa, validateDecimalInput } from '../../bridge/nativeBridge';
import type { BusinessRatesDraft, StoreIdentityDraft } from './hybridModels';

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

  return { createExpenseCategory, saveStoreIdentity, setExpenseCategoryArchived, updateBusinessSettings };
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
