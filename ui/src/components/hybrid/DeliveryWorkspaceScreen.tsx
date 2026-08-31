import type { DeliveryDto, DeliveryStatusCode, EntityMutationResultDto, Int64String, TransactionReferencesDto } from '../../bridge/contract.generated';
import { Notice } from './HybridUi';
import { DeliveryCreateForm } from './DeliveryCreateForm';
import { DeliveryList } from './DeliveryList';
import type { DeliveryDraft } from './hybridModels';

export function DeliveryWorkspaceScreen({ busy, deliveries, references, error, notice, onCreate, onTransition, onLinkSale, onArchive }: {
  busy: boolean;
  deliveries: ReadonlyArray<DeliveryDto> | null;
  references: TransactionReferencesDto | null;
  error: string | null;
  notice: string | null;
  onCreate(draft: DeliveryDraft): Promise<EntityMutationResultDto>;
  onTransition(delivery: DeliveryDto, status: DeliveryStatusCode, notes: string): Promise<void>;
  onLinkSale(deliveryId: DeliveryDto['deliveryId'], saleEventId: Int64String): Promise<void>;
  onArchive(delivery: DeliveryDto): Promise<void>;
}) {
  if (!references || !deliveries) return <p className="rounded-2xl border border-slate-200 bg-white p-5 text-sm text-slate-500">Loading deliveries…</p>;
  return <div className="space-y-5">
    {(notice || error) && <Notice message={notice ?? error ?? ''} success={Boolean(notice)} />}
    {(references.customers.length === 0 || references.products.length === 0) && <Notice message="Add an active customer and product before creating a delivery." />}
    <DeliveryCreateForm busy={busy} references={references} onCreate={onCreate} />
    <DeliveryList busy={busy} deliveries={deliveries} onTransition={onTransition} onLinkSale={onLinkSale} onArchive={onArchive} />
  </div>;
}
