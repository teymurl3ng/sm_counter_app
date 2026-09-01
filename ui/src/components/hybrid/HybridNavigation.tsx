import { Menu, X, type LucideIcon } from 'lucide-react';
import { useState } from 'react';

export type ManagementArea = 'dashboard' | 'operations' | 'deliveries' | 'accounts' | 'history' | 'cash-close' | 'products' | 'customers' | 'suppliers' | 'employees' | 'costing-planning' | 'settings' | 'access' | 'backup';
export type NavigationGroup = 'Everyday work' | 'Records & closing' | 'People & stock' | 'Shop controls';

export type NavigationItem = {
  area: ManagementArea;
  id: string;
  label: string;
  group: NavigationGroup;
  icon: LucideIcon;
};

const preferredPrimaryAreas: ReadonlyArray<ManagementArea> = ['dashboard', 'operations', 'deliveries', 'accounts'];
const groupOrder: ReadonlyArray<NavigationGroup> = ['Everyday work', 'Records & closing', 'People & stock', 'Shop controls'];

export function HybridNavigation({ items, activeArea, onSelect }: {
  items: ReadonlyArray<NavigationItem>;
  activeArea: ManagementArea;
  onSelect(area: ManagementArea): void;
}) {
  const [open, setOpen] = useState(false);
  const preferred = preferredPrimaryAreas.map((area) => items.find((item) => item.area === area)).filter((item): item is NavigationItem => Boolean(item));
  const primary = [...preferred, ...items.filter((item) => !preferred.some((candidate) => candidate.area === item.area))].slice(0, 4);
  const secondary = items.filter((item) => !primary.some((candidate) => candidate.area === item.area));
  const secondaryActive = secondary.some((item) => item.area === activeArea);
  const select = (area: ManagementArea) => { onSelect(area); setOpen(false); window.scrollTo({ top: 0, behavior: 'smooth' }); };
  const columnCount = primary.length + (secondary.length > 0 ? 1 : 0);

  return <>
    <nav id="app-navigation" className="fixed inset-x-0 bottom-0 z-40 border-t border-slate-200 bg-white/95 px-2 pt-1 shadow-[0_-8px_24px_rgba(15,23,42,0.08)] backdrop-blur md:sticky md:top-0 md:bottom-auto md:border-y md:px-4 md:py-2 md:shadow-sm" aria-label="Application areas" style={{ paddingBottom: 'max(0.35rem, env(safe-area-inset-bottom))' }}>
      <div id="primary-navigation" className="mx-auto grid max-w-4xl gap-1" style={{ gridTemplateColumns: `repeat(${columnCount}, minmax(0, 1fr))` }}>
        {primary.map((item) => <NavigationButton key={item.area} item={item} active={item.area === activeArea} onClick={() => select(item.area)} />)}
        {secondary.length > 0 && <button id="navigation-more-button" type="button" aria-expanded={open} aria-controls="navigation-more-panel" onClick={() => setOpen(true)} className={`flex min-h-14 min-w-0 flex-col items-center justify-center gap-1 rounded-xl px-1 py-2 text-[11px] font-bold md:min-h-0 md:flex-row md:gap-2 md:px-3 md:text-sm ${secondaryActive ? 'bg-emerald-50 text-emerald-800' : 'text-slate-500'}`}><Menu className="h-5 w-5 md:h-4 md:w-4" /><span className="truncate">More</span></button>}
      </div>
    </nav>
    {secondary.length > 0 && <div id="navigation-more-layer" className={`fixed inset-0 z-50 flex items-end bg-slate-950/45 transition-opacity md:items-center md:justify-center md:p-5 ${open ? 'visible opacity-100' : 'pointer-events-none invisible opacity-0'}`} aria-hidden={!open}>
      <button type="button" className="absolute inset-0" onClick={() => setOpen(false)} aria-label="Close navigation menu" tabIndex={open ? 0 : -1} />
      <section id="navigation-more-panel" role="dialog" aria-modal="true" aria-labelledby="navigation-more-title" className="relative max-h-[85vh] w-full overflow-y-auto rounded-t-3xl bg-[#f7f6f0] p-5 pb-8 shadow-2xl md:max-w-2xl md:rounded-3xl">
        <div className="mb-5 flex items-center justify-between"><div><p className="text-xs font-semibold uppercase tracking-[0.18em] text-emerald-700">Saaf Milk</p><h2 id="navigation-more-title" className="mt-1 text-xl font-bold text-slate-900">More areas</h2></div><button id="close-navigation-more" type="button" data-native-back-close onClick={() => setOpen(false)} className="rounded-xl bg-white p-2.5 text-slate-500 shadow-sm" aria-label="Close"><X className="h-5 w-5" /></button></div>
        <div className="grid gap-5 sm:grid-cols-2">{groupOrder.map((group) => {
          const groupItems = secondary.filter((item) => item.group === group);
          return groupItems.length > 0 && <div key={group}><h3 className="mb-2 text-xs font-bold uppercase tracking-wide text-slate-500">{group}</h3><div className="grid grid-cols-2 gap-2">{groupItems.map((item) => <NavigationButton key={item.area} item={item} active={item.area === activeArea} expanded onClick={() => select(item.area)} tabIndex={open ? 0 : -1} />)}</div></div>;
        })}</div>
      </section>
    </div>}
  </>;
}

function NavigationButton({ item, active, expanded = false, onClick, tabIndex }: {
  item: NavigationItem;
  active: boolean;
  expanded?: boolean;
  onClick(): void;
  tabIndex?: number;
}) {
  const Icon = item.icon;
  return <button id={item.id} type="button" data-app-area={item.area} data-app-area-active={active ? 'true' : 'false'} onClick={onClick} tabIndex={tabIndex} aria-current={active ? 'page' : undefined} className={`flex min-w-0 items-center justify-center rounded-xl font-bold transition-colors ${expanded ? 'min-h-20 flex-col gap-2 border px-2 py-3 text-xs' : 'min-h-14 flex-col gap-1 px-1 py-2 text-[11px] md:min-h-0 md:flex-row md:gap-2 md:px-3 md:text-sm'} ${active ? 'border-emerald-200 bg-emerald-50 text-emerald-800' : expanded ? 'border-slate-200 bg-white text-slate-600' : 'text-slate-500'}`}><Icon className={expanded ? 'h-5 w-5' : 'h-5 w-5 md:h-4 md:w-4'} /><span className="max-w-full truncate">{item.label}</span></button>;
}
