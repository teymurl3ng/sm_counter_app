import { useState } from 'react';
import type { BusinessSettingsDto, ExpenseCategoryDto, MasterDataDto, TransactionReferencesDto } from '../../bridge/contract.generated';
import { BusinessSettingsPanel } from './BusinessSettingsPanel';
import { ExpenseCategoriesPanel } from './ExpenseCategoriesPanel';
import { FilterButton } from './HybridUi';
import { OpeningStatePanel } from './OpeningStatePanel';
import type { BusinessRatesDraft, OpeningStateDraft, StoreIdentityDraft } from './hybridModels';

type SettingsArea = 'business' | 'opening' | 'expenses';

export function SettingsWorkspaceScreen({ busy, canManageSettings, categories, masterData, references, settings, error, notice, onSaveStore, onSaveRates, onSaveOpening, onCreateCategory, onSetCategoryArchived }: {
  busy: boolean;
  canManageSettings: boolean;
  categories: ReadonlyArray<ExpenseCategoryDto> | null;
  masterData: MasterDataDto | null;
  references: TransactionReferencesDto | null;
  settings: BusinessSettingsDto | null;
  error: string | null;
  notice: string | null;
  onSaveStore(draft: StoreIdentityDraft): Promise<void>;
  onSaveRates(draft: BusinessRatesDraft): Promise<void>;
  onSaveOpening(draft: OpeningStateDraft, passphrase: string, correction: boolean): Promise<void>;
  onCreateCategory(displayName: string): Promise<void>;
  onSetCategoryArchived(category: ExpenseCategoryDto, archived: boolean): Promise<void>;
}) {
  const [area, setArea] = useState<SettingsArea>(canManageSettings ? 'business' : 'expenses');
  return <>
    <div className="mb-5 flex rounded-xl bg-slate-200/70 p-1">
      {canManageSettings && <FilterButton id="business-settings-tab" active={area === 'business'} onClick={() => setArea('business')}>Shop details & rates</FilterButton>}
      {canManageSettings && <FilterButton id="opening-state-tab" active={area === 'opening'} onClick={() => setArea('opening')}>Opening balances</FilterButton>}
      <FilterButton id="expense-categories-tab" active={area === 'expenses'} onClick={() => setArea('expenses')}>Expense categories</FilterButton>
    </div>
    {area === 'business' && canManageSettings
      ? <BusinessSettingsPanel busy={busy} settings={settings} error={error} notice={notice} onSaveStore={onSaveStore} onSaveRates={onSaveRates} />
      : area === 'opening' && canManageSettings
        ? <OpeningStatePanel busy={busy} settings={settings} masterData={masterData} references={references} error={error} notice={notice} onSave={onSaveOpening} />
        : <ExpenseCategoriesPanel busy={busy} categories={categories} error={error} notice={notice} onCreate={onCreateCategory} onSetArchived={onSetCategoryArchived} />}
  </>;
}
