package com.saaf.milkshop.presentation.management

import android.app.Activity
import android.app.AlertDialog
import android.content.res.ColorStateList
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.saaf.milkshop.R
import com.saaf.milkshop.domain.CustomerManagementService
import com.saaf.milkshop.domain.EmployeeManagementService
import com.saaf.milkshop.domain.VendorManagementService
import kotlin.math.roundToInt

/**
 * Presentation-only owner for the People & Accounts management UI.
 *
 * This class deliberately does not perform database work or business decisions.
 * Create/rename operations are delegated back to MainActivity through callbacks
 * so the existing service calls and refresh behavior remain unchanged.
 */
class PeopleAccountsUi(
    private val activity: Activity,
    private val formatMoney: (Double) -> String,
    private val onVendorCreated: (String) -> Unit,
    private val onVendorRenamed: (VendorManagementService.VendorInfo, String) -> Unit,
    private val onCustomerCreated: (String) -> Unit,
    private val onCustomerRenamed: (CustomerManagementService.CustomerInfo, String) -> Unit,
    private val onEmployeeCreated: (String) -> Unit,
    private val onEmployeeRenamed: (EmployeeManagementService.EmployeeInfo, String) -> Unit,
) {
    private val vendorList: LinearLayout = activity.findViewById(R.id.vendorManagementList)
    private val vendorError: TextView = activity.findViewById(R.id.vendorManagementErrorText)
    private val customerList: LinearLayout = activity.findViewById(R.id.customerManagementList)
    private val customerError: TextView = activity.findViewById(R.id.customerManagementErrorText)
    private val employeeList: LinearLayout = activity.findViewById(R.id.employeeManagementList)
    private val employeeError: TextView = activity.findViewById(R.id.employeeManagementErrorText)

    fun renderVendors(vendors: List<VendorManagementService.VendorInfo>) {
        vendorList.removeAllViews()
        if (vendors.isEmpty()) {
            vendorList.addView(emptyState(R.string.no_vendor_management_accounts))
        } else {
            vendors.forEach { vendor -> vendorList.addView(buildVendorRow(vendor)) }
        }
    }

    fun renderCustomers(customers: List<CustomerManagementService.CustomerInfo>) {
        customerList.removeAllViews()
        if (customers.isEmpty()) {
            customerList.addView(emptyState(R.string.no_customer_management_accounts))
        } else {
            customers.forEach { customer -> customerList.addView(buildCustomerRow(customer)) }
        }
    }

    fun renderEmployees(employees: List<EmployeeManagementService.EmployeeInfo>) {
        employeeList.removeAllViews()
        if (employees.isEmpty()) {
            employeeList.addView(emptyState(R.string.no_employee_management_accounts))
        } else {
            employees.forEach { employee -> employeeList.addView(buildEmployeeRow(employee)) }
        }
    }

    fun showAddVendorDialog() {
        val input = EditText(activity).apply {
            hint = activity.getString(R.string.vendor_name_hint)
        }
        AlertDialog.Builder(activity)
            .setTitle(R.string.add_vendor_management_title)
            .setView(input)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.add_vendor_button) { _, _ ->
                onVendorCreated(input.text.toString())
            }
            .show()
    }

    fun showAddCustomerDialog() {
        val input = EditText(activity).apply {
            hint = activity.getString(R.string.customer_name_hint)
        }
        AlertDialog.Builder(activity)
            .setTitle(R.string.add_customer_management_title)
            .setView(input)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.add_customer_button) { _, _ ->
                onCustomerCreated(input.text.toString())
            }
            .show()
    }

    fun showAddEmployeeDialog() {
        val input = EditText(activity).apply {
            hint = activity.getString(R.string.employee_name_hint)
        }
        AlertDialog.Builder(activity)
            .setTitle(R.string.add_employee_management_title)
            .setView(input)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.add_employee_button) { _, _ ->
                onEmployeeCreated(input.text.toString())
            }
            .show()
    }

    fun showVendorError(message: String) {
        vendorError.text = message
        vendorError.visibility = View.VISIBLE
    }

    fun clearVendorError() {
        vendorError.visibility = View.GONE
    }

    fun showCustomerError(message: String) {
        customerError.text = message
        customerError.visibility = View.VISIBLE
    }

    fun clearCustomerError() {
        customerError.visibility = View.GONE
    }

    fun showEmployeeError(message: String) {
        employeeError.text = message
        employeeError.visibility = View.VISIBLE
    }

    fun clearEmployeeError() {
        employeeError.visibility = View.GONE
    }

    private fun buildVendorRow(vendor: VendorManagementService.VendorInfo): View {
        val row = baseRow()
        val label = managementLabel(
            activity.getString(R.string.vendor_management_row, vendor.name, formatMoney(vendor.balance))
        )
        val edit = managementEditButton(R.string.edit_vendor_button) {
            showRenameVendorDialog(vendor)
        }
        row.addView(label)
        row.addView(edit)
        return row
    }

    private fun buildCustomerRow(customer: CustomerManagementService.CustomerInfo): View {
        val row = baseRow()
        val label = managementLabel(
            activity.getString(R.string.customer_management_row, customer.name, formatMoney(customer.balance))
        )
        val edit = managementEditButton(R.string.edit_customer_button) {
            showRenameCustomerDialog(customer)
        }
        row.addView(label)
        row.addView(edit)
        return row
    }

    private fun buildEmployeeRow(employee: EmployeeManagementService.EmployeeInfo): View {
        val row = baseRow()
        val label = managementLabel(
            activity.getString(R.string.employee_management_row, employee.name, formatMoney(employee.balance))
        )
        val edit = managementEditButton(R.string.edit_employee_button) {
            showRenameEmployeeDialog(employee)
        }
        row.addView(label)
        row.addView(edit)
        return row
    }

    private fun showRenameVendorDialog(vendor: VendorManagementService.VendorInfo) {
        val input = renameInput(vendor.name, R.string.vendor_name_hint)
        AlertDialog.Builder(activity)
            .setTitle(R.string.edit_vendor_title)
            .setView(input)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.save_vendor_button) { _, _ ->
                onVendorRenamed(vendor, input.text.toString())
            }
            .show()
    }

    private fun showRenameCustomerDialog(customer: CustomerManagementService.CustomerInfo) {
        val input = renameInput(customer.name, R.string.customer_name_hint)
        AlertDialog.Builder(activity)
            .setTitle(R.string.edit_customer_title)
            .setView(input)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.save_customer_button) { _, _ ->
                onCustomerRenamed(customer, input.text.toString())
            }
            .show()
    }

    private fun showRenameEmployeeDialog(employee: EmployeeManagementService.EmployeeInfo) {
        val input = renameInput(employee.name, R.string.employee_name_hint)
        AlertDialog.Builder(activity)
            .setTitle(R.string.edit_employee_title)
            .setView(input)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.save_employee_button) { _, _ ->
                onEmployeeRenamed(employee, input.text.toString())
            }
            .show()
    }

    private fun renameInput(value: String, hintRes: Int): EditText {
        return EditText(activity).apply {
            hint = activity.getString(hintRes)
            setText(value)
            setSelection(text.length)
        }
    }

    private fun baseRow(): LinearLayout = LinearLayout(activity).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        )
        setPadding(0, dp(8), 0, dp(8))
    }

    private fun managementLabel(textValue: String): TextView = TextView(activity).apply {
        text = textValue
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        setTextAppearance(android.R.style.TextAppearance_Material_Body1)
        setTextColor(activity.getColor(R.color.milk_shop_color_on_surface))
        textSize = 16f
        setPadding(dp(0), dp(4), dp(8), dp(4))
    }

    private fun managementEditButton(textRes: Int, onClick: () -> Unit): Button = Button(activity).apply {
        text = activity.getString(textRes)
        setOnClickListener { onClick() }
        setTextColor(activity.getColor(R.color.milk_shop_secondary_button_text))
        backgroundTintList = ColorStateList.valueOf(
            activity.getColor(R.color.milk_shop_color_primary_container)
        )
    }

    private fun emptyState(textRes: Int): TextView = TextView(activity).apply {
        text = activity.getString(textRes)
    }

    private fun dp(value: Int): Int = (value * activity.resources.displayMetrics.density).roundToInt()
}
