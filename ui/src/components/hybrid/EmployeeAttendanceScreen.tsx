import { useState } from 'react';
import type { AccountReferenceDto, AttendanceDto, AttendanceStatusCode, EmployeeDto } from '../../bridge/contract.generated';
import { AttendancePanel } from './AttendancePanel';
import { EmployeeProfilesPanel } from './EmployeeProfilesPanel';
import { FilterButton } from './HybridUi';
import type { EmployeeDraft } from './hybridModels';

type EmployeeArea = 'profiles' | 'attendance';

export function EmployeeAttendanceScreen({ busy, canManage, employees, attendanceEmployees, error, notice, onSaveEmployee, onSetEmployeeActive, onLoadAttendance, onRecordAttendance }: {
  busy: boolean;
  canManage: boolean;
  employees: ReadonlyArray<EmployeeDto> | null;
  attendanceEmployees: ReadonlyArray<AccountReferenceDto>;
  error: string | null;
  notice: string | null;
  onSaveEmployee(draft: EmployeeDraft, employee?: EmployeeDto): Promise<void>;
  onSetEmployeeActive(employee: EmployeeDto, active: boolean): Promise<void>;
  onLoadAttendance(businessDate: string): Promise<ReadonlyArray<AttendanceDto>>;
  onRecordAttendance(employeeAccountId: AccountReferenceDto['accountId'], businessDate: string, statusCode: AttendanceStatusCode, notes: string): Promise<void>;
}) {
  const [area, setArea] = useState<EmployeeArea>(canManage ? 'profiles' : 'attendance');
  return <>
    <div className="mb-5 grid grid-cols-2 rounded-xl bg-slate-200/70 p-1">
      {canManage && <FilterButton id="employee-profiles-tab" active={area === 'profiles'} onClick={() => setArea('profiles')}>Employee profiles</FilterButton>}
      <FilterButton id="attendance-tab" active={area === 'attendance'} onClick={() => setArea('attendance')}>Daily attendance</FilterButton>
    </div>
    {area === 'profiles' && canManage ? <EmployeeProfilesPanel busy={busy} employees={employees} error={error} notice={notice} onSave={onSaveEmployee} onSetActive={onSetEmployeeActive} /> : <AttendancePanel busy={busy} employees={attendanceEmployees} error={error} notice={notice} onLoad={onLoadAttendance} onRecord={onRecordAttendance} />}
  </>;
}
