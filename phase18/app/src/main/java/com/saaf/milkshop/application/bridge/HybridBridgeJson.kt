package com.saaf.milkshop.application.bridge

import com.saaf.milkshop.application.readmodels.BootstrapStatusDto
import com.saaf.milkshop.application.readmodels.CurrentSessionDto
import com.saaf.milkshop.application.readmodels.CustomerDto
import com.saaf.milkshop.application.readmodels.EmployeeDto
import com.saaf.milkshop.application.readmodels.ExpenseCategoryDto
import com.saaf.milkshop.application.readmodels.MasterDataDto
import com.saaf.milkshop.application.readmodels.ProductDto
import com.saaf.milkshop.application.readmodels.VendorDto
import org.json.JSONObject

internal fun bootstrapStatusJson(value: BootstrapStatusDto): JSONObject = JSONObject().put("needsOwnerBootstrap", value.needsOwnerBootstrap)

internal fun currentSessionJson(value: CurrentSessionDto): JSONObject = JSONObject()
    .put("authenticationStatusCode", value.authenticationStatusCode)
    .put("userId", value.userId.toString())
    .put("loginName", value.loginName)
    .put("displayName", value.displayName)
    .put("roleCode", value.roleCode)
    .putNullable("employeeAccountId", value.employeeAccountId?.toString())
    .put("effectivePermissionCodes", jsonArray(value.effectivePermissionCodes))

internal fun masterDataJson(value: MasterDataDto): JSONObject = JSONObject()
    .put("products", jsonArray(value.products, ::productJson))
    .put("customers", jsonArray(value.customers, ::customerJson))
    .put("vendors", jsonArray(value.vendors, ::vendorJson))
    .put("employees", jsonArray(value.employees, ::employeeJson))
    .put("expenseCategories", jsonArray(value.expenseCategories, ::expenseCategoryJson))

private fun productJson(value: ProductDto): JSONObject = JSONObject()
    .put("productId", value.productId.toString()).put("name", value.name).put("unit", value.unit)
    .put("quantityOnHand", value.quantityOnHand).putNullable("defaultSaleRatePaisaPerUnit", value.defaultSaleRatePaisaPerUnit?.toString())
    .put("inventoryTracking", value.inventoryTracking).put("archived", value.archived).putNullable("notes", value.notes)

private fun customerJson(value: CustomerDto): JSONObject = JSONObject()
    .put("accountId", value.accountId.toString()).put("name", value.name).put("canonicalBalancePaisa", value.canonicalBalancePaisa.toString())
    .putNullable("phone", value.phone).putNullable("areaRoute", value.areaRoute).putNullable("address", value.address)
    .putNullable("notes", value.notes).putNullable("creditLimitPaisa", value.creditLimitPaisa?.toString())
    .putNullable("dailyQuota", value.dailyQuota).put("active", value.active)

private fun vendorJson(value: VendorDto): JSONObject = JSONObject()
    .put("accountId", value.accountId.toString()).put("name", value.name).put("canonicalBalancePaisa", value.canonicalBalancePaisa.toString())
    .putNullable("phone", value.phone).putNullable("location", value.location).putNullable("notes", value.notes).put("active", value.active)

private fun employeeJson(value: EmployeeDto): JSONObject = JSONObject()
    .put("accountId", value.accountId.toString()).put("name", value.name).put("canonicalBalancePaisa", value.canonicalBalancePaisa.toString())
    .putNullable("roleLabel", value.roleLabel).putNullable("phone", value.phone).putNullable("referenceSalaryPaisa", value.referenceSalaryPaisa?.toString())
    .putNullable("salaryPeriodCode", value.salaryPeriodCode).putNullable("joinDate", value.joinDate).put("active", value.active)

private fun expenseCategoryJson(value: ExpenseCategoryDto): JSONObject = JSONObject()
    .put("accountId", value.accountId.toString()).put("displayName", value.displayName).put("active", value.active)
