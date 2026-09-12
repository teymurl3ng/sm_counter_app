package com.saaf.milkshop.application.bridge

import com.saaf.milkshop.application.readmodels.BootstrapStatusDto
import com.saaf.milkshop.application.readmodels.BusinessSettingsDto
import com.saaf.milkshop.application.readmodels.CashierSummaryDto
import com.saaf.milkshop.application.readmodels.AccountReferenceDto
import com.saaf.milkshop.application.readmodels.AccountLedgerDto
import com.saaf.milkshop.application.readmodels.AttendanceDto
import com.saaf.milkshop.application.readmodels.CurrentSessionDto
import com.saaf.milkshop.application.readmodels.DashboardDto
import com.saaf.milkshop.application.readmodels.DeliveryDto
import com.saaf.milkshop.application.readmodels.CustomerDto
import com.saaf.milkshop.application.readmodels.EmployeeDto
import com.saaf.milkshop.application.readmodels.EventDetailDto
import com.saaf.milkshop.application.readmodels.EventHistoryDto
import com.saaf.milkshop.application.readmodels.ExpenseCategoryDto
import com.saaf.milkshop.application.readmodels.FinancialOverviewDto
import com.saaf.milkshop.application.readmodels.MasterDataDto
import com.saaf.milkshop.application.readmodels.ProductDto
import com.saaf.milkshop.application.readmodels.ProfitabilityReportDto
import com.saaf.milkshop.application.readmodels.ProductReferenceDto
import com.saaf.milkshop.application.readmodels.ReconciliationArchiveDto
import com.saaf.milkshop.application.readmodels.TransactionReferencesDto
import com.saaf.milkshop.application.readmodels.VendorDto
import com.saaf.milkshop.application.readmodels.money
import com.saaf.milkshop.domain.DailyCloseRecord
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
    .put("expenseCategories", jsonArray(value.expenseCategories, ::expenseCategoryJson))
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

internal fun cashierSummaryJson(value: CashierSummaryDto): JSONObject = JSONObject()
    .put("paymentAccounts", jsonArray(value.paymentAccounts) { account ->
        JSONObject().put("accountId", account.accountId.toString()).put("name", account.name)
            .put("categoryCode", account.categoryCode)
            .put("canonicalBalancePaisa", account.canonicalBalancePaisa.toString())
    })
    .put("startDateInclusive", value.startDateInclusive)
    .put("endDateExclusive", value.endDateExclusive)
    .put("salesValuePaisa", value.salesValuePaisa.toString())
    .put("creditCollectionsPaisa", value.creditCollectionsPaisa.toString())

internal fun financialOverviewJson(value: FinancialOverviewDto): JSONObject = JSONObject()
    .put("customers", jsonArray(value.customers, ::accountPositionJson))
    .put("vendors", jsonArray(value.vendors, ::accountPositionJson))
    .put("totals", JSONObject()
        .put("startDateInclusive", value.totals.startDateInclusive)
        .put("endDateExclusive", value.totals.endDateExclusive)
        .put("eventCount", value.totals.eventCount.toString())
        .put("salesValuePaisa", value.totals.salesValuePaisa.toString())
        .put("saleQuantity", value.totals.saleQuantity)
        .put("purchaseAmountPaisa", value.totals.purchaseAmountPaisa.toString())
        .put("purchasedMilkLiters", value.totals.purchasedMilkLiters)
        .put("producedYogurtKg", value.totals.producedYogurtKg)
        .put("productionMilkUsedLiters", value.totals.productionMilkUsedLiters)
        .put("vendorPaymentsPaisa", value.totals.vendorPaymentsPaisa.toString())
        .put("creditCollectionsPaisa", value.totals.creditCollectionsPaisa.toString())
        .put("operatingExpensesPaisa", value.totals.operatingExpensesPaisa.toString())
        .put("salaryPaymentsPaisa", value.totals.salaryPaymentsPaisa.toString()))

internal fun profitabilityReportJson(value: ProfitabilityReportDto): JSONObject = JSONObject()
    .put("startInclusive", value.startInclusive)
    .put("endExclusive", value.endExclusive)
    .put("products", jsonArray(value.products) { product ->
        JSONObject()
            .put("productId", product.productId.toString())
            .put("productName", product.productName)
            .put("quantitySold", product.quantitySold)
            .put("salesRevenuePaisa", product.salesRevenuePaisa.toString())
            .putNullable("costOfProductsSoldPaisa", product.costOfProductsSoldPaisa?.toString())
            .putNullable("profitBeforeShopExpensesPaisa", product.profitBeforeShopExpensesPaisa?.toString())
            .putNullable("grossMarginPercent", product.grossMarginPercent)
            .putNullable("spoilageCostPaisa", product.spoilageCostPaisa?.toString())
            .putNullable("abnormalLossPaisa", product.abnormalLossPaisa?.toString())
            .put("coverageCode", product.coverageCode)
    })
    .put("deliverySurchargePaisa", value.deliverySurchargePaisa.toString())
    .put("recordedOperatingExpensesPaisa", value.recordedOperatingExpensesPaisa.toString())
    .put("recordedSalaryPaymentsPaisa", value.recordedSalaryPaymentsPaisa.toString())
    .putNullable("estimatedRecordedResultPaisa", value.estimatedRecordedResultPaisa?.toString())
    .put("coverageCode", value.coverageCode)
    .put("reportingBasisCode", value.reportingBasisCode)
    .put("valuationScopeCode", value.valuationScopeCode)

internal fun attendanceJson(values: List<AttendanceDto>): JSONArray = jsonArray(values, ::attendanceItemJson)

internal fun spoilageJson(value: com.saaf.milkshop.application.readmodels.SpoilageDto): JSONObject = JSONObject()
    .put("eventId", value.eventId.toString()).put("occurredAt", value.occurredAt)
    .put("productId", value.productId.toString()).put("productName", value.productName).put("unit", value.unit)
    .put("quantity", value.quantity).put("reasonCode", value.reasonCode)
    .putNullable("correctsEventId", value.correctsEventId?.toString()).put("isSuperseded", value.isSuperseded)
    .putNullable("notes", value.notes)

internal fun deliveriesJson(values: List<DeliveryDto>): JSONArray = jsonArray(values) { value ->
    JSONObject().put("deliveryId", value.deliveryId.toString())
        .put("customerAccountId", value.customerAccountId.toString()).put("customerName", value.customerName)
        .put("productId", value.productId.toString()).put("productName", value.productName).put("unit", value.unit)
        .put("quantity", value.quantity).putNullable("route", value.route).put("businessDate", value.businessDate)
        .putNullable("saleEventId", value.saleEventId?.toString()).putNullable("statusCode", value.statusCode)
        .putNullable("statusOccurredAt", value.statusOccurredAt).putNullable("notes", value.notes)
}

internal fun accountLedgerJson(value: AccountLedgerDto): JSONObject = JSONObject()
    .put("accountId", value.accountId.toString()).put("accountName", value.accountName)
    .put("displayName", value.displayName).put("categoryCode", value.categoryCode)
    .put("currentBalancePaisa", value.currentBalancePaisa.toString())
    .put("entries", jsonArray(value.entries) { entry ->
        JSONObject().put("logId", entry.logId.toString()).put("eventId", entry.eventId.toString())
            .put("occurredAt", entry.occurredAt).put("createdAt", entry.createdAt)
            .put("eventTypeCode", entry.eventTypeCode).putNullable("channelCode", entry.channelCode)
            .put("movementPaisa", entry.movementPaisa.toString()).put("balanceAfterPaisa", entry.balanceAfterPaisa.toString())
            .putNullable("correctsEventId", entry.correctsEventId?.toString()).put("isCorrection", entry.isCorrection)
            .put("isSuperseded", entry.isSuperseded).putNullable("notes", entry.notes)
    })

internal fun eventHistoryJson(value: EventHistoryDto): JSONObject = JSONObject()
    .put("eventId", value.eventId.toString()).put("createdAt", value.createdAt).put("occurredAt", value.occurredAt)
    .put("eventTypeCode", value.eventTypeCode).putNullable("channelCode", value.channelCode)
    .putNullable("correctsEventId", value.correctsEventId?.toString()).put("isCorrection", value.isCorrection)
    .put("isSuperseded", value.isSuperseded).putNullable("actor", value.actor?.let { actor ->
        JSONObject().putNullable("userId", actor.userId?.toString()).putNullable("displayName", actor.displayName)
            .putNullable("roleCode", actor.roleCode)
    }).putNullable("notes", value.notes)

internal fun eventDetailJson(value: EventDetailDto): JSONObject = JSONObject()
    .put("event", eventHistoryJson(value.event))
    .put("inventoryMovements", jsonArray(value.inventoryMovements) { movement ->
        JSONObject().put("productName", movement.productName).put("quantity", movement.quantity).put("unit", movement.unit)
    })
    .putNullable("financialMovements", value.financialMovements?.let { movements ->
        jsonArray(movements) { movement ->
            JSONObject().put("accountName", movement.accountName).put("amountPaisa", movement.amountPaisa.toString())
        }
    })
    .putNullable("originalValues", value.originalValues?.let { original ->
        JSONObject()
            .putNullable("rawLiters", original.rawLiters)
            .putNullable("fatPercent", original.fatPercent)
            .putNullable("lrReading", original.lrReading)
            .putNullable("milkLitersUsed", original.milkLitersUsed)
            .putNullable("yogurtKgProduced", original.yogurtKgProduced)
            .putNullable("unitRatePaisa", original.unitRatePaisa?.toString())
            .putNullable("quantity", original.quantity)
            .putNullable("valuePaisa", original.valuePaisa?.toString())
            .putNullable("surchargePaisa", original.surchargePaisa?.toString())
            .putNullable("channelCode", original.channelCode)
            .putNullable("amountPaisa", original.amountPaisa?.toString())
            .putNullable("reasonCode", original.reasonCode)
    })

internal fun reconciliationArchiveJson(value: ReconciliationArchiveDto): JSONObject = JSONObject()
    .put("cashCounts", jsonArray(value.cashCounts) { count ->
        JSONObject().put("countId", count.countId.toString()).put("accountId", count.accountId.toString())
            .put("businessDate", count.businessDate).put("countedCashPaisa", count.countedCashPaisa.toString())
            .put("expectedCashPaisa", count.expectedCashPaisa.toString()).put("variancePaisa", count.variancePaisa.toString())
            .put("countedAt", count.countedAt).putNullable("notes", count.notes)
    })
    .put("closeActions", jsonArray(value.closeActions) { action ->
        JSONObject().put("actionId", action.actionId.toString()).put("businessDate", action.businessDate)
            .put("actionCode", action.actionCode).putNullable("physicalCashCountId", action.physicalCashCountId?.toString())
            .put("actor", JSONObject().putNullable("userId", action.actor.userId?.toString())
                .putNullable("displayName", action.actor.displayName).putNullable("roleCode", action.actor.roleCode))
            .putNullable("reason", action.reason).put("createdAt", action.createdAt)
            .putNullable("supersedesActionId", action.supersedesActionId?.toString())
    })

internal fun dailyCloseResultJson(value: DailyCloseRecord): JSONObject = JSONObject()
    .put("actionId", value.action.actionId.toString()).put("statusCode", value.status)
    .putNullable("countedCashPaisa", value.countedCash?.let { money(it).toString() })
    .putNullable("expectedCashPaisa", value.expectedCash?.let { money(it).toString() })
    .putNullable("variancePaisa", value.variance?.let { money(it).toString() })

private fun productReferenceJson(value: ProductReferenceDto): JSONObject = JSONObject()
    .put("productId", value.productId.toString()).put("name", value.name).put("unit", value.unit)

private fun accountReferenceJson(value: AccountReferenceDto): JSONObject = JSONObject()
    .put("accountId", value.accountId.toString()).put("name", value.name)

private fun accountPositionJson(value: com.saaf.milkshop.application.readmodels.AccountPositionDto): JSONObject = JSONObject()
    .put("accountId", value.accountId.toString()).put("name", value.name)
    .put("categoryCode", value.categoryCode).put("canonicalBalancePaisa", value.canonicalBalancePaisa.toString())

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
