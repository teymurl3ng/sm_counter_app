package com.saaf.milkshop.application.bridge

import com.saaf.milkshop.application.readmodels.BootstrapStatusDto
import com.saaf.milkshop.application.readmodels.BusinessSettingsDto
import com.saaf.milkshop.application.readmodels.AccountReferenceDto
import com.saaf.milkshop.application.readmodels.AttendanceDto
import com.saaf.milkshop.application.readmodels.CurrentSessionDto
import com.saaf.milkshop.application.readmodels.DashboardDto
import com.saaf.milkshop.application.readmodels.CustomerDto
import com.saaf.milkshop.application.readmodels.EmployeeDto
import com.saaf.milkshop.application.readmodels.ExpenseCategoryDto
import com.saaf.milkshop.application.readmodels.MasterDataDto
import com.saaf.milkshop.application.readmodels.ProductDto
import com.saaf.milkshop.application.readmodels.ProductReferenceDto
import com.saaf.milkshop.application.readmodels.TransactionReferencesDto
import com.saaf.milkshop.application.readmodels.VendorDto
import org.json.JSONArray
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

internal fun businessSettingsJson(value: BusinessSettingsDto): JSONObject = JSONObject()
    .putNullable("store", value.store?.let { store ->
        JSONObject().put("storeName", store.storeName).put("currencyCode", store.currencyCode)
            .putNullable("phone", store.phone).putNullable("address", store.address).put("updatedAt", store.updatedAt)
    })
    .put("vendorMilkRatePaisaPerUnit", value.vendorMilkRatePaisaPerUnit.toString())
    .put("vendorDeliveryFeePaisaPerUnit", value.vendorDeliveryFeePaisaPerUnit.toString())
    .put("tsStandardPercent", value.tsStandardPercent)
    .put("retailMilkRatePaisaPerUnit", value.retailMilkRatePaisaPerUnit.toString())
    .put("retailYogurtRatePaisaPerUnit", value.retailYogurtRatePaisaPerUnit.toString())
    .put("defaultDeliverySurchargePaisa", value.defaultDeliverySurchargePaisa.toString())
    .putNullable("openingState", value.openingState?.let { opening ->
        JSONObject().put("eventId", opening.eventId.toString()).put("openingDate", opening.openingDate)
            .put("inventoryBalances", jsonArray(opening.inventoryBalances) { balance ->
                JSONObject().put("productId", balance.productId.toString()).put("quantity", balance.quantity)
            })
            .put("accountBalances", jsonArray(opening.accountBalances) { balance ->
                JSONObject().put("accountId", balance.accountId.toString()).put("balancePaisa", balance.balancePaisa.toString())
            })
            .putNullable("notes", opening.notes)
    })
    .put("expenseCategories", jsonArray(value.expenseCategories, ::expenseCategoryJson))

internal fun transactionReferencesJson(value: TransactionReferencesDto): JSONObject = JSONObject()
    .put("products", jsonArray(value.products, ::productReferenceJson))
    .put("customers", jsonArray(value.customers, ::accountReferenceJson))
    .put("vendors", jsonArray(value.vendors, ::accountReferenceJson))
    .put("employees", jsonArray(value.employees, ::accountReferenceJson))
    .put("paymentAccounts", jsonArray(value.paymentAccounts, ::accountReferenceJson))
    .putNullable("vendorMilkRatePaisaPerUnit", value.vendorMilkRatePaisaPerUnit?.toString())
    .putNullable("vendorDeliveryFeePaisaPerUnit", value.vendorDeliveryFeePaisaPerUnit?.toString())
    .putNullable("tsStandardPercent", value.tsStandardPercent)
    .putNullable("retailMilkRatePaisaPerUnit", value.retailMilkRatePaisaPerUnit?.toString())
    .putNullable("retailYogurtRatePaisaPerUnit", value.retailYogurtRatePaisaPerUnit?.toString())
    .putNullable("defaultDeliverySurchargePaisa", value.defaultDeliverySurchargePaisa?.toString())

internal fun dashboardJson(value: DashboardDto): JSONObject = JSONObject()
    .put("stock", jsonArray(value.stock) { stock ->
        JSONObject().put("productId", stock.productId.toString()).put("name", stock.name)
            .put("unit", stock.unit).put("quantityOnHand", stock.quantityOnHand)
    })
    .put("totals", JSONObject()
        .put("startDateInclusive", value.totals.startDateInclusive)
        .put("endDateExclusive", value.totals.endDateExclusive)
        .put("eventCount", value.totals.eventCount.toString())
        .put("saleQuantity", value.totals.saleQuantity)
        .put("purchasedMilkLiters", value.totals.purchasedMilkLiters)
        .put("producedYogurtKg", value.totals.producedYogurtKg)
        .put("productionMilkUsedLiters", value.totals.productionMilkUsedLiters))

internal fun attendanceJson(values: List<AttendanceDto>): JSONArray = jsonArray(values, ::attendanceItemJson)

private fun productReferenceJson(value: ProductReferenceDto): JSONObject = JSONObject()
    .put("productId", value.productId.toString()).put("name", value.name).put("unit", value.unit)

private fun accountReferenceJson(value: AccountReferenceDto): JSONObject = JSONObject()
    .put("accountId", value.accountId.toString()).put("name", value.name)

private fun attendanceItemJson(value: AttendanceDto): JSONObject = JSONObject()
    .put("attendanceId", value.attendanceId.toString()).put("employeeAccountId", value.employeeAccountId.toString())
    .put("employeeName", value.employeeName).put("businessDate", value.businessDate).put("statusCode", value.statusCode)
    .putNullable("notes", value.notes).put("createdAt", value.createdAt)

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
