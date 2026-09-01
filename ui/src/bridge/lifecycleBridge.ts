import type { SessionHandle } from './contract.generated';
import type { ManagementArea } from '../components/hybrid/HybridNavigation';

interface InjectedLifecycleBridge {
  postMessage(message: string): void;
}

export interface NativeRestorationState {
  sessionHandle: SessionHandle;
  activeArea: ManagementArea | null;
}

declare global {
  interface Window {
    saafMilkLifecycle?: InjectedLifecycleBridge;
    __saafMilkRestoration?: unknown;
  }
}

const managementAreas = new Set<ManagementArea>([
  'dashboard', 'operations', 'deliveries', 'accounts', 'history', 'cash-close',
  'products', 'customers', 'suppliers', 'employees', 'costing-planning',
  'settings', 'access', 'backup',
]);

export function publishActiveArea(activeArea: ManagementArea): void {
  window.saafMilkLifecycle?.postMessage(JSON.stringify({ activeArea }));
}

export function waitForNativeRestoration(timeoutMs = 200): Promise<NativeRestorationState | null> {
  const immediate = consumeNativeRestoration();
  if (immediate) return Promise.resolve(immediate);
  return new Promise((resolve) => {
    const finish = () => {
      window.clearTimeout(timeout);
      window.removeEventListener('saafMilkRestorationReady', finish);
      resolve(consumeNativeRestoration());
    };
    const timeout = window.setTimeout(finish, timeoutMs);
    window.addEventListener('saafMilkRestorationReady', finish, { once: true });
  });
}

function consumeNativeRestoration(): NativeRestorationState | null {
  const candidate = window.__saafMilkRestoration;
  delete window.__saafMilkRestoration;
  if (!candidate || typeof candidate !== 'object') return null;
  const raw = candidate as { sessionHandle?: unknown; activeArea?: unknown };
  if (typeof raw.sessionHandle !== 'string' || raw.sessionHandle.length === 0) return null;
  const activeArea = typeof raw.activeArea === 'string' && managementAreas.has(raw.activeArea as ManagementArea)
    ? raw.activeArea as ManagementArea
    : null;
  return { sessionHandle: raw.sessionHandle as SessionHandle, activeArea };
}
