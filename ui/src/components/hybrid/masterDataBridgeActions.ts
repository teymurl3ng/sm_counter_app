import type { CustomerDto, ProductDto, SessionHandle, VendorDto } from '../../bridge/contract.generated';
import { callNative, paisaToRupeesInput, rupeesToPaisa, validateDecimalInput } from '../../bridge/nativeBridge';
import type { CustomerDraft, ProductDraft, SupplierDraft } from './hybridModels';
import type { RefreshPlan, RunAndRefresh } from './authorizedDataRefresh';

type RefreshAuthorizedData = (refresh: RefreshPlan) => Promise<void>;
const MASTER_RECORDS: RefreshPlan = ['masterData', 'transactionReferences'];

export function masterDataBridgeActions(
  session: SessionHandle | null,
  runAndRefresh: RunAndRefresh,
  refreshAuthorizedData: RefreshAuthorizedData,
) {
  const saveProduct = (draft: ProductDraft, productId?: ProductDto['productId']) => runAndRefresh(async () => {
    if (!session) return;
    const payload = {
      name: draft.name,
      unitCode: draft.unitCode,
      defaultSaleRatePaisaPerUnit: draft.rateRupees.trim() ? rupeesToPaisa(draft.rateRupees) : null,
      inventoryTracking: draft.inventoryTracking,
      notes: draft.notes.trim() || null,
    };
    if (productId) await callNative('command.updateProduct', { productId, ...payload }, session);
    else await callNative('command.createProduct', payload, session);
  }, productId ? 'Product changes saved.' : 'Product added successfully.', MASTER_RECORDS);

  const setProductArchived = (product: ProductDto, archived: boolean) => runAndRefresh(async () => {
    if (session) await callNative('command.setProductArchived', { productId: product.productId, archived }, session);
  }, archived ? `${product.name} moved to archived products.` : `${product.name} restored to active products.`, MASTER_RECORDS);

  const customerProfilePayload = (draft: CustomerDraft, accountId: CustomerDto['accountId']) => ({
    accountId,
    phone: draft.phone.trim() || null,
    areaRoute: draft.areaRoute.trim() || null,
    address: draft.address.trim() || null,
    notes: draft.notes.trim() || null,
    creditLimitPaisa: draft.creditLimitRupees.trim() ? rupeesToPaisa(draft.creditLimitRupees) : null,
    dailyQuota: draft.dailyQuota.trim() ? validateDecimalInput(draft.dailyQuota, 'the daily quantity') : null,
    active: draft.active,
  });

  const saveCustomer = async (draft: CustomerDraft, customer?: CustomerDto) => {
    try {
      await runAndRefresh(async () => {
        if (!session) return;
        let accountId = customer?.accountId;
        if (!accountId) accountId = (await callNative('command.createCustomer', { name: draft.name }, session)).entityId;
        else if (draft.name.trim() !== customer?.name) await callNative('command.renameCustomer', { accountId, name: draft.name }, session);
        await callNative('command.saveCustomerProfile', customerProfilePayload(draft, accountId), session);
      }, customer ? 'Customer changes saved.' : 'Customer added successfully.', MASTER_RECORDS);
    } catch (failure) {
      await refreshAuthorizedData(MASTER_RECORDS).catch(() => undefined);
      throw failure;
    }
  };

  const setCustomerActive = (customer: CustomerDto, active: boolean) => runAndRefresh(async () => {
    if (!session) return;
    await callNative('command.saveCustomerProfile', customerProfilePayload({
      name: customer.name,
      phone: customer.phone ?? '',
      areaRoute: customer.areaRoute ?? '',
      address: customer.address ?? '',
      notes: customer.notes ?? '',
      creditLimitRupees: paisaToRupeesInput(customer.creditLimitPaisa),
      dailyQuota: customer.dailyQuota ?? '',
      active,
    }, customer.accountId), session);
  }, active ? `${customer.name} restored to active customers.` : `${customer.name} moved to inactive customers.`, MASTER_RECORDS);

  const supplierProfilePayload = (draft: SupplierDraft, accountId: VendorDto['accountId']) => ({
    accountId,
    phone: draft.phone.trim() || null,
    location: draft.location.trim() || null,
    notes: draft.notes.trim() || null,
    active: draft.active,
  });

  const saveSupplier = async (draft: SupplierDraft, supplier?: VendorDto) => {
    try {
      await runAndRefresh(async () => {
        if (!session) return;
        let accountId = supplier?.accountId;
        if (!accountId) accountId = (await callNative('command.createVendor', { name: draft.name }, session)).entityId;
        else if (draft.name.trim() !== supplier?.name) await callNative('command.renameVendor', { accountId, name: draft.name }, session);
        await callNative('command.saveVendorProfile', supplierProfilePayload(draft, accountId), session);
      }, supplier ? 'Supplier changes saved.' : 'Supplier added successfully.', MASTER_RECORDS);
    } catch (failure) {
      await refreshAuthorizedData(MASTER_RECORDS).catch(() => undefined);
      throw failure;
    }
  };

  const setSupplierActive = (supplier: VendorDto, active: boolean) => runAndRefresh(async () => {
    if (!session) return;
    await callNative('command.saveVendorProfile', supplierProfilePayload({
      name: supplier.name,
      phone: supplier.phone ?? '',
      location: supplier.location ?? '',
      notes: supplier.notes ?? '',
      active,
    }, supplier.accountId), session);
  }, active ? `${supplier.name} restored to active suppliers.` : `${supplier.name} moved to inactive suppliers.`, MASTER_RECORDS);

  return { saveCustomer, saveProduct, saveSupplier, setCustomerActive, setProductArchived, setSupplierActive };
}
