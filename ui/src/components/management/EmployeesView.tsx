import React, { useState } from 'react';
import { MilkShopViewModel } from '../../viewmodels/useMilkShopViewModel';
import { Plus, User, Phone, Edit, Trash2 } from 'lucide-react';
import { Modal } from '../common/Modal';
import { ConfirmModal } from '../common/ConfirmModal';

export const EmployeesView: React.FC<{ vm: MilkShopViewModel }> = ({ vm }) => {
  const [isAddOpen, setIsAddOpen] = useState(false);
  const [isAttendanceOpen, setIsAttendanceOpen] = useState(false);
  const [employeeToDeleteId, setEmployeeToDeleteId] = useState<string | null>(null);
  
  // New/Edit Employee State
  const [editingEmployeeId, setEditingEmployeeId] = useState<string | null>(null);
  const [name, setName] = useState('');
  const [role, setRole] = useState('');
  const [phone, setPhone] = useState('');
  const [salary, setSalary] = useState<number | ''>('');
  const [salaryType, setSalaryType] = useState<'monthly' | 'weekly' | 'daily'>('monthly');

  // Attendance state
  const [selectedEmployeeId, setSelectedEmployeeId] = useState('');
  const [attendanceStatus, setAttendanceStatus] = useState<'present' | 'absent' | 'half_day'>('present');
  const [attendanceNotes, setAttendanceNotes] = useState('');

  const handleAddSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;

    if (editingEmployeeId) {
      vm.updateEmployee(editingEmployeeId, {
        name: name.trim(),
        role: role.trim() || 'Staff',
        phone: phone.trim(),
        salary: Number(salary) || 0,
        salaryType
      });
    } else {
      vm.addEmployee({
        name: name.trim(),
        role: role.trim() || 'Staff',
        phone: phone.trim(),
        salary: Number(salary) || 0,
        salaryType
      });
    }
    setIsAddOpen(false);
    resetForm();
  };

  const resetForm = () => {
    setEditingEmployeeId(null);
    setName('');
    setRole('');
    setPhone('');
    setSalary('');
    setSalaryType('monthly');
  };

  const handleEditEmployee = (emp: any) => {
    setEditingEmployeeId(emp.id);
    setName(emp.name);
    setRole(emp.role);
    setPhone(emp.phone);
    setSalary(emp.salary);
    setSalaryType(emp.salaryType);
    setIsAddOpen(true);
  };

  const handleDeleteEmployee = (id: string) => {
    setEmployeeToDeleteId(id);
  };

  const openAttendance = (empId: string) => {
    const existing = vm.attendances.find(a => a.employeeId === empId);
    setSelectedEmployeeId(empId);
    setAttendanceStatus(existing?.status || 'present');
    setAttendanceNotes(existing?.notes || '');
    setIsAttendanceOpen(true);
  };

  const handleAttendanceSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    vm.recordAttendance(selectedEmployeeId, attendanceStatus, attendanceNotes);
    setIsAttendanceOpen(false);
  };

  return (
    <div className="space-y-4 pb-16">
      <div className="flex items-center justify-between gap-3">
        <div>
          <h2 className="text-xl sm:text-2xl font-extrabold text-slate-900 dark:text-slate-100 tracking-tight">
            Employees
          </h2>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
            Manage staff, salaries, and daily attendance.
          </p>
        </div>

        <button
          onClick={() => setIsAddOpen(true)}
          className="px-3.5 py-2 rounded-xl bg-[#135d46] dark:bg-emerald-600 hover:bg-[#0e4837] dark:hover:bg-emerald-700 text-white font-bold text-xs flex items-center gap-1.5 cursor-pointer shadow-xs shrink-0 transition-colors"
        >
          <Plus className="w-4 h-4" />
          <span className="hidden sm:inline">Add Employee</span>
          <span className="sm:hidden">Add</span>
        </button>
      </div>

      <div className="grid gap-3 sm:grid-cols-2">
        {vm.employees.map(emp => {
          const attendance = vm.attendances.find(a => a.employeeId === emp.id);
          
          return (
            <div key={emp.id} className="bg-white dark:bg-slate-900 rounded-2xl p-4 shadow-sm border border-slate-200/80 dark:border-slate-800">
              <div className="flex justify-between items-start mb-3">
                <div className="flex items-center gap-2.5">
                  <div className="w-10 h-10 rounded-full bg-emerald-100 dark:bg-emerald-900/30 text-emerald-700 dark:text-emerald-400 flex items-center justify-center font-black">
                    {emp.name.charAt(0).toUpperCase()}
                  </div>
                  <div>
                    <h3 className="text-sm font-bold text-slate-900 dark:text-slate-100">{emp.name}</h3>
                    <p className="text-[10px] text-slate-500 dark:text-slate-400 flex items-center gap-1">
                      <User className="w-3 h-3" /> {emp.role}
                      {emp.phone && <><Phone className="w-3 h-3 ml-2" /> {emp.phone}</>}
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-1">
                  <button
                    onClick={() => handleEditEmployee(emp)}
                    className="p-1.5 text-slate-400 dark:text-slate-500 hover:text-emerald-600 dark:hover:text-emerald-400 hover:bg-emerald-50 dark:hover:bg-emerald-900/20 rounded-lg transition-colors"
                  >
                    <Edit className="w-4 h-4" />
                  </button>
                  <button
                    onClick={() => handleDeleteEmployee(emp.id)}
                    className="p-1.5 text-slate-400 dark:text-slate-500 hover:text-rose-600 dark:hover:text-rose-400 hover:bg-rose-50 dark:hover:bg-rose-900/20 rounded-lg transition-colors"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>

              <div className="flex items-center justify-between mt-2 pt-3 border-t border-slate-100 dark:border-slate-800">
                <div className="text-xs">
                  <span className="text-slate-500 dark:text-slate-400 block">Salary ({emp.salaryType})</span>
                  <span className="font-bold text-slate-800 dark:text-slate-200">Rs. {emp.salary}</span>
                </div>
                
                <div className="text-right">
                  <span className="text-slate-500 dark:text-slate-400 text-[10px] block mb-1">Today's Attendance</span>
                  {attendance ? (
                    <button 
                      onClick={() => openAttendance(emp.id)}
                      className={`text-[10px] px-2 py-1 rounded-md font-bold uppercase ${
                        attendance.status === 'present' ? 'bg-emerald-100 dark:bg-emerald-900/40 text-emerald-800 dark:text-emerald-400' :
                        attendance.status === 'absent' ? 'bg-rose-100 dark:bg-rose-900/40 text-rose-800 dark:text-rose-400' :
                        'bg-amber-100 dark:bg-amber-900/40 text-amber-800 dark:text-amber-400'
                      }`}
                    >
                      {attendance.status.replace('_', ' ')}
                    </button>
                  ) : (
                    <button
                      onClick={() => openAttendance(emp.id)}
                      className="text-[10px] px-2 py-1 rounded-md bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400 font-bold hover:bg-slate-200 dark:hover:bg-slate-700 transition-colors"
                    >
                      Mark Attendance
                    </button>
                  )}
                </div>
              </div>
            </div>
          );
        })}

        {vm.employees.length === 0 && (
          <div className="col-span-full p-8 text-center bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 text-slate-400 dark:text-slate-500 text-xs">
            No employees added yet. Click <strong>+ Add Employee</strong> to register staff.
          </div>
        )}
      </div>

      <Modal
        isOpen={isAddOpen}
        onClose={() => {
          setIsAddOpen(false);
          resetForm();
        }}
        title={editingEmployeeId ? "Edit Employee" : "Add Employee"}
        subtitle={editingEmployeeId ? "Update staff member details" : "Register a new staff member"}
      >
        <form onSubmit={handleAddSubmit} className="space-y-4 text-xs">
          <div>
            <label className="block text-slate-700 dark:text-slate-300 font-bold mb-1">Full Name</label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl px-3.5 py-2.5 text-slate-900 dark:text-slate-100 focus:outline-none focus:border-[#135d46] dark:focus:border-emerald-500"
              required
            />
          </div>
          
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-slate-700 dark:text-slate-300 font-bold mb-1">Role</label>
              <input
                type="text"
                value={role}
                onChange={(e) => setRole(e.target.value)}
                className="w-full bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl px-3.5 py-2.5 text-slate-900 dark:text-slate-100 focus:outline-none focus:border-[#135d46] dark:focus:border-emerald-500"
                placeholder="e.g. Sales, Delivery"
              />
            </div>
            <div>
              <label className="block text-slate-700 dark:text-slate-300 font-bold mb-1">Phone</label>
              <input
                type="text"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                className="w-full bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl px-3.5 py-2.5 text-slate-900 dark:text-slate-100 focus:outline-none focus:border-[#135d46] dark:focus:border-emerald-500"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-slate-700 dark:text-slate-300 font-bold mb-1">Salary Amount (Rs.)</label>
              <input
                type="number"
                min="0"
                value={salary}
                onChange={(e) => setSalary(e.target.value === '' ? '' : Number(e.target.value))}
                className="w-full bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl px-3.5 py-2.5 text-slate-900 dark:text-slate-100 font-mono font-bold focus:outline-none focus:border-[#135d46] dark:focus:border-emerald-500"
                required
              />
            </div>
            <div>
              <label className="block text-slate-700 dark:text-slate-300 font-bold mb-1">Salary Type</label>
              <select
                value={salaryType}
                onChange={(e) => setSalaryType(e.target.value as any)}
                className="w-full bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl px-3.5 py-2.5 text-slate-900 dark:text-slate-100 font-bold cursor-pointer focus:outline-none focus:border-[#135d46] dark:focus:border-emerald-500"
              >
                <option value="monthly">Monthly</option>
                <option value="weekly">Weekly</option>
                <option value="daily">Daily</option>
              </select>
            </div>
          </div>

          <div className="flex justify-end gap-2 pt-2">
            <button
              type="button"
              onClick={() => setIsAddOpen(false)}
              className="px-4 py-2 rounded-xl bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 font-bold hover:bg-slate-200 dark:hover:bg-slate-700"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-5 py-2 rounded-xl bg-[#135d46] dark:bg-emerald-600 hover:bg-[#0e4837] dark:hover:bg-emerald-700 text-white font-bold shadow-xs"
            >
              {editingEmployeeId ? 'Save Changes' : 'Save Employee'}
            </button>
          </div>
        </form>
      </Modal>

      <Modal
        isOpen={isAttendanceOpen}
        onClose={() => setIsAttendanceOpen(false)}
        title="Mark Attendance"
        subtitle="Record daily presence"
      >
        <form onSubmit={handleAttendanceSubmit} className="space-y-4 text-xs">
          <div className="grid grid-cols-3 gap-2">
            <label className={`border rounded-xl p-3 text-center cursor-pointer transition-all ${attendanceStatus === 'present' ? 'bg-emerald-50 dark:bg-emerald-900/20 border-emerald-500 dark:border-emerald-500 text-emerald-800 dark:text-emerald-400 shadow-xs' : 'border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-400 hover:bg-slate-50 dark:hover:bg-slate-800'}`}>
              <input type="radio" name="att_status" className="hidden" checked={attendanceStatus === 'present'} onChange={() => setAttendanceStatus('present')} />
              <span className="font-bold block">Present</span>
            </label>
            <label className={`border rounded-xl p-3 text-center cursor-pointer transition-all ${attendanceStatus === 'half_day' ? 'bg-amber-50 dark:bg-amber-900/20 border-amber-500 dark:border-amber-500 text-amber-800 dark:text-amber-400 shadow-xs' : 'border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-400 hover:bg-slate-50 dark:hover:bg-slate-800'}`}>
              <input type="radio" name="att_status" className="hidden" checked={attendanceStatus === 'half_day'} onChange={() => setAttendanceStatus('half_day')} />
              <span className="font-bold block">Half Day</span>
            </label>
            <label className={`border rounded-xl p-3 text-center cursor-pointer transition-all ${attendanceStatus === 'absent' ? 'bg-rose-50 dark:bg-rose-900/20 border-rose-500 dark:border-rose-500 text-rose-800 dark:text-rose-400 shadow-xs' : 'border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-400 hover:bg-slate-50 dark:hover:bg-slate-800'}`}>
              <input type="radio" name="att_status" className="hidden" checked={attendanceStatus === 'absent'} onChange={() => setAttendanceStatus('absent')} />
              <span className="font-bold block">Absent</span>
            </label>
          </div>

          <div>
            <label className="block text-slate-700 dark:text-slate-300 font-bold mb-1">Notes (Optional)</label>
            <input
              type="text"
              value={attendanceNotes}
              onChange={(e) => setAttendanceNotes(e.target.value)}
              className="w-full bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl px-3.5 py-2.5 text-slate-900 dark:text-slate-100 focus:outline-none focus:border-[#135d46] dark:focus:border-emerald-500"
              placeholder="e.g. Arrived late, Sick leave..."
            />
          </div>

          <div className="flex justify-end gap-2 pt-2">
            <button
              type="button"
              onClick={() => setIsAttendanceOpen(false)}
              className="px-4 py-2 rounded-xl bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 font-bold hover:bg-slate-200 dark:hover:bg-slate-700"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-5 py-2 rounded-xl bg-[#135d46] dark:bg-emerald-600 hover:bg-[#0e4837] dark:hover:bg-emerald-700 text-white font-bold shadow-xs"
            >
              Save Attendance
            </button>
          </div>
        </form>
      </Modal>

      <ConfirmModal
        isOpen={Boolean(employeeToDeleteId)}
        onClose={() => setEmployeeToDeleteId(null)}
        onConfirm={() => {
          if (employeeToDeleteId) {
            vm.deleteEmployee(employeeToDeleteId);
            setEmployeeToDeleteId(null);
          }
        }}
        title="Delete Employee"
        message="Are you sure you want to delete this employee? This will remove their record from active staff."
      />
    </div>
  );
};
