import type {
  AttendanceDto,
  AttendanceStatusCode,
  EmployeeDto,
  IsoDateString,
  SessionHandle,
} from '../../bridge/contract.generated';
import { callNative, paisaToRupeesInput, rupeesToPaisa } from '../../bridge/nativeBridge';
import type { EmployeeDraft } from './hybridModels';
import type { RefreshPlan, RunAndRefresh } from './authorizedDataRefresh';

const EMPLOYEE_RECORDS: RefreshPlan = ['masterData', 'transactionReferences'];

export function employeeBridgeActions(session: SessionHandle | null, runAndRefresh: RunAndRefresh) {
  const profilePayload = (draft: EmployeeDraft, accountId: EmployeeDto['accountId']) => ({
    accountId,
    roleLabel: draft.roleLabel.trim() || null,
    phone: draft.phone.trim() || null,
    referenceSalaryPaisa: draft.referenceSalaryRupees.trim() ? rupeesToPaisa(draft.referenceSalaryRupees) : null,
    salaryPeriodCode: draft.salaryPeriodCode || null,
    joinDate: draft.joinDate ? draft.joinDate as IsoDateString : null,
    active: draft.active,
  });

  const saveEmployee = async (draft: EmployeeDraft, employee?: EmployeeDto) => {
    await runAndRefresh(async () => {
      if (!session) return;
      let accountId: EmployeeDto['accountId'];
      if (!employee) accountId = (await callNative('command.createEmployee', { name: draft.name }, session)).entityId;
      else {
        accountId = employee.accountId;
        if (draft.name.trim() !== employee.name) await callNative('command.renameEmployee', { accountId, name: draft.name }, session);
      }
      await callNative('command.saveEmployeeProfile', profilePayload(draft, accountId), session);
    }, employee ? 'Employee changes saved.' : 'Employee added successfully.', EMPLOYEE_RECORDS);
  };

  const setEmployeeActive = (employee: EmployeeDto, active: boolean) => runAndRefresh(async () => {
    if (!session) return;
    await callNative('command.saveEmployeeProfile', profilePayload({
      name: employee.name,
      roleLabel: employee.roleLabel ?? '',
      phone: employee.phone ?? '',
      referenceSalaryRupees: paisaToRupeesInput(employee.referenceSalaryPaisa),
      salaryPeriodCode: employee.salaryPeriodCode ?? '',
      joinDate: employee.joinDate ?? '',
      active,
    }, employee.accountId), session);
  }, active ? `${employee.name} restored to active employees.` : `${employee.name} moved to inactive employees.`, EMPLOYEE_RECORDS);

  const loadAttendance = (businessDate: string): Promise<ReadonlyArray<AttendanceDto>> => {
    if (!session) return Promise.resolve([]);
    return callNative('query.attendance', { businessDate: businessDate as IsoDateString }, session);
  };

  const recordAttendance = (
    employeeAccountId: EmployeeDto['accountId'],
    businessDate: string,
    statusCode: AttendanceStatusCode,
    notes: string,
  ) => runAndRefresh(async () => {
    if (!session) return;
    await callNative('command.recordAttendance', {
      employeeAccountId,
      businessDate: businessDate as IsoDateString,
      statusCode,
      ...(notes.trim() ? { notes: notes.trim() } : {}),
    }, session);
  }, 'Attendance saved.', []);

  return { loadAttendance, recordAttendance, saveEmployee, setEmployeeActive };
}
