import type { DeliveryDto, DeliveryStatusCode, EntityMutationResultDto, IsoDateString, Int64String, SessionHandle } from '../../bridge/contract.generated';
import { callNative, validateDecimalInput } from '../../bridge/nativeBridge';
import type { DeliveryDraft } from './hybridModels';
import type { RunAndRefresh } from './authorizedDataRefresh';

const DELIVERY_RECORDS = ['deliveries'] as const;

export function deliveryBridgeActions(session: SessionHandle | null, runAndRefresh: RunAndRefresh) {
  const createDelivery = (draft: DeliveryDraft): Promise<EntityMutationResultDto> => runAndRefresh(async () => {
    if (!session) throw new Error('Sign in to create a delivery.');
    return callNative('command.createDelivery', {
      customerAccountId: draft.customerAccountId,
      productId: draft.productId,
      quantity: validateDecimalInput(draft.quantity, 'the delivery quantity'),
      businessDate: draft.businessDate as IsoDateString,
      saleEventId: draft.saleEventId,
      ...(draft.route.trim() ? { route: draft.route.trim() } : {}),
      ...(draft.notes.trim() ? { notes: draft.notes.trim() } : {}),
    }, session);
  }, 'Delivery added to the pending list.', DELIVERY_RECORDS);

  const transitionDelivery = async (delivery: DeliveryDto, statusCode: DeliveryStatusCode, notes: string) => {
    await runAndRefresh(async () => {
      if (!session) throw new Error('Sign in to update a delivery.');
      await callNative('command.transitionDelivery', {
        deliveryId: delivery.deliveryId,
        statusCode,
        ...(notes.trim() ? { notes: notes.trim() } : {}),
      }, session);
    }, statusCode === 'delivered' ? 'Delivery marked as delivered.' : 'Delivery cancelled.', DELIVERY_RECORDS);
  };

  const linkDeliverySale = async (deliveryId: DeliveryDto['deliveryId'], saleEventId: Int64String) => {
    await runAndRefresh(async () => {
      if (!session) throw new Error('Sign in to link a sale.');
      await callNative('command.linkDeliverySale', { deliveryId, saleEventId }, session);
    }, 'Sale record linked to the delivery.', DELIVERY_RECORDS);
  };

  const archiveDelivery = async (delivery: DeliveryDto) => {
    await runAndRefresh(async () => {
      if (!session) throw new Error('Sign in to archive a delivery.');
      await callNative('command.archiveDelivery', { deliveryId: delivery.deliveryId }, session);
    }, 'Delivery removed from the active list.', DELIVERY_RECORDS);
  };

  return { archiveDelivery, createDelivery, linkDeliverySale, transitionDelivery };
}
