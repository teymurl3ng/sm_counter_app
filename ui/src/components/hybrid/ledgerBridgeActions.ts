import type { AccountLedgerDto, Int64String, SessionHandle } from '../../bridge/contract.generated';
import { callNative } from '../../bridge/nativeBridge';

export function ledgerBridgeActions(session: SessionHandle | null) {
  const loadAccountLedger = (accountId: Int64String): Promise<AccountLedgerDto> => {
    if (!session) throw new Error('Sign in to view account history.');
    return callNative('query.accountLedger', { accountId }, session);
  };

  return { loadAccountLedger };
}
