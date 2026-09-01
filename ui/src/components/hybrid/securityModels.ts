import type { AppRoleCode, EnrollmentChallengeDto, Int64String, SecurityAuditDto, UserAdministrationDto } from '../../bridge/contract.generated';

export type SecurityBundle = {
  users: ReadonlyArray<UserAdministrationDto>;
  audit: ReadonlyArray<SecurityAuditDto>;
};

export type NewUserDraft = {
  loginName: string;
  displayName: string;
  roleCode: AppRoleCode;
  employeeAccountId: Int64String | null;
};

export type PrivateSetup = {
  challenge: EnrollmentChallengeDto;
  displayName: string;
  purpose: 'new' | 'reset' | 'resume';
};

export type SecurityArea = 'account' | 'staff' | 'audit';
