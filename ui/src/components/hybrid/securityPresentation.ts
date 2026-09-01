import type { AppRoleCode } from '../../bridge/contract.generated';

export function roleLabel(role: string): string {
  const labels: Record<string, string> = { owner: 'Owner', admin: 'Administrator', manager: 'Manager', cashier: 'Cashier', operator: 'Shop operator' };
  return labels[role] ?? role;
}

export function roleExplanation(role: AppRoleCode): string {
  const explanations: Record<AppRoleCode, string> = {
    owner: 'Full control, including recovery, restores, and other owners or administrators.',
    admin: 'Runs the app and manages ordinary staff, but cannot restore backups or control owners/admins.',
    manager: 'Manages shop work, records, reports, staff profiles, and daily closing—not app accounts or shop-wide security.',
    cashier: 'Handles sales, customer collections, expenses, deliveries, cash counting, and ordinary daily close.',
    operator: 'Handles milk intake, yogurt production, spoilage, deliveries, and attendance.',
  };
  return explanations[role];
}

export function auditActionLabel(code: string): string {
  const labels: Record<string, string> = {
    bootstrap_owner: 'First owner created',
    login: 'Sign-in attempt',
    logout: 'Signed out',
    reauthenticate: 'Sensitive action confirmed',
    create_user: 'App account created',
    complete_enrollment: 'Private passphrase setup completed',
    change_role: 'App role changed',
    enable_user: 'App account enabled',
    disable_user: 'App account disabled',
    reset_credential: 'Passphrase reset started',
    change_credential: 'Own passphrase changed',
    recover_owner: 'Owner account recovered',
    permission_denied: 'Permission denied',
  };
  return labels[code] ?? plainCode(code);
}

export function plainCode(code: string): string {
  return code.toLowerCase().split('_').filter(Boolean).map((word) => word.charAt(0).toUpperCase() + word.slice(1)).join(' ');
}
