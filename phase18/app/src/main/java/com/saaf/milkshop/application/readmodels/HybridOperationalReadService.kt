package com.saaf.milkshop.application.readmodels

import com.saaf.milkshop.data.EventEntity
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AnalyticsService
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.RolePermissions
import com.saaf.milkshop.domain.UserSecurityService

internal class HybridOperationalReadService(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
) {
    suspend fun dashboard(sessionHandle: String, startDateInclusive: String, endDateExclusive: String): DashboardDto {
        security.requirePermission(sessionHandle, AppPermission.VIEW_OPERATIONAL)
        val position = AnalyticsService(database).currentPosition()
        val totals = AnalyticsService(database).periodSummary(startDateInclusive, endDateExclusive)
        return DashboardDto(
            stock = position.stock.map { StockPositionDto(it.productId, it.name, it.unit, decimal(it.quantityOnHand)) },
            totals = totals.toOperationalTotalsDto(),
        )
    }

    suspend fun cashierSummary(sessionHandle: String, startDateInclusive: String, endDateExclusive: String): CashierSummaryDto {
        security.requirePermission(sessionHandle, AppPermission.SALE_AND_COLLECTION)
        val position = AnalyticsService(database).currentPosition()
        val totals = AnalyticsService(database).periodSummary(startDateInclusive, endDateExclusive)
        return CashierSummaryDto(
            paymentAccounts = position.accountBalances.filter { it.category == "Cash/Bank" }.map {
                AccountPositionDto(it.accountId, it.name, it.category, money(it.balance))
            },
            startDateInclusive = totals.startDateInclusive,
            endDateExclusive = totals.endDateExclusive,
            salesValuePaisa = money(totals.salesValue),
            creditCollectionsPaisa = money(totals.creditCollections),
        )
    }

    suspend fun transactionReferences(sessionHandle: String): TransactionReferencesDto {
        val actor = security.requirePermission(sessionHandle, AppPermission.VIEW_OPERATIONAL)
        val role = AppRole.parse(actor.role)
        fun allows(permission: AppPermission) = RolePermissions.allows(role, permission)
        val canSell = allows(AppPermission.SALE_AND_COLLECTION)
        val canPurchase = allows(AppPermission.PURCHASE)
        val canPaySupplier = allows(AppPermission.SUPPLIER_PAYMENT)
        val canPaySalary = allows(AppPermission.SALARY_PAYMENT)
        val canProduce = allows(AppPermission.PRODUCTION_AND_SPOILAGE)
        val canDeliver = allows(AppPermission.DELIVERY)
        val canAttend = allows(AppPermission.ATTENDANCE)
        val needsPaymentAccounts = canSell || canPaySupplier || canPaySalary || allows(AppPermission.EXPENSE)
        val settings = database.settingsDao().getAll().associate { it.key to it.value }
        val productProfiles = database.productProfileDao().all().associateBy { it.productId }
        val customerProfiles = database.customerProfileDao().all().associateBy { it.accountId }
        val vendorProfiles = database.vendorProfileDao().all().associateBy { it.accountId }
        val employeeProfiles = database.employeeProfileDao().all().associateBy { it.accountId }
        suspend fun refs(category: String) = database.accountDao().findByCategory(category).map {
            val name = when (category) {
                "Customer" -> it.name.removePrefix("Customer: ").trim()
                "Vendor" -> it.name.removePrefix("Vendor: ").trim()
                else -> it.name
            }
            AccountReferenceDto(it.accountId, name)
        }
        val settingKeys = com.saaf.milkshop.domain.SettingKeys
        return TransactionReferencesDto(
            products = database.productDao().all().filter { product ->
                (canSell || canPurchase || canProduce || canDeliver) && productProfiles[product.productId]?.archived != true
            }.map { ProductReferenceDto(it.productId, it.name, it.unit) },
            customers = if (canSell || canDeliver) refs("Customer").filter { customerProfiles[it.accountId]?.active != false } else emptyList(),
            vendors = if (canPurchase || canPaySupplier) refs("Vendor").filter { vendorProfiles[it.accountId]?.active != false } else emptyList(),
            employees = if (canPaySalary || canAttend) {
                database.accountDao().findByCategory("Salary")
                    .filter { employeeProfiles[it.accountId]?.active != false }
                    .map { AccountReferenceDto(it.accountId, it.name.removePrefix("Employee: ").trim()) }
            } else emptyList(),
            paymentAccounts = if (needsPaymentAccounts) refs("Cash/Bank") else emptyList(),
            vendorMilkRatePaisaPerUnit = settings[settingKeys.MILK_RATE_VENDOR]?.takeIf { canPurchase }?.let(::money),
            vendorDeliveryFeePaisaPerUnit = settings[settingKeys.DELIVERY_FEE_VENDOR]?.takeIf { canPurchase }?.let(::money),
            tsStandardPercent = settings[settingKeys.TS_STANDARD]?.takeIf { canPurchase }?.let(::decimal),
            retailMilkRatePaisaPerUnit = settings[settingKeys.RETAIL_RATE_MILK]?.takeIf { canSell }?.let(::money),
            retailYogurtRatePaisaPerUnit = settings[settingKeys.RETAIL_RATE_YOGURT]?.takeIf { canSell }?.let(::money),
            defaultDeliverySurchargePaisa = settings[settingKeys.DEFAULT_DELIVERY_SURCHARGE]?.takeIf { canSell }?.let(::money),
        )
    }

    suspend fun eventHistory(sessionHandle: String): List<EventHistoryDto> {
        security.requirePermission(sessionHandle, AppPermission.VIEW_OPERATIONAL)
        val events = database.eventDao().findAllDescending()
        val supersededIds = events.mapNotNull { it.correctsEventId }.toSet()
        return events.map { it.toHistoryDto(supersededIds) }
    }

    suspend fun eventDetail(sessionHandle: String, eventId: Long): EventDetailDto {
        val actor = security.requirePermission(sessionHandle, AppPermission.VIEW_OPERATIONAL)
        val event = database.eventDao().findById(eventId) ?: error("Event $eventId does not exist")
        val events = database.eventDao().findAllDescending()
        val inventory = database.inventoryLogDao().findByEventId(eventId).map { movement ->
            val product = database.productDao().findById(movement.productId)
            InventoryMovementDto(product?.name ?: "Product #${movement.productId}", decimal(movement.quantity), product?.unit ?: "")
        }
        val financial = if (RolePermissions.allows(AppRole.parse(actor.role), AppPermission.VIEW_FULL_FINANCIAL)) {
            database.financialLogDao().findByEventId(eventId).map { movement ->
                val account = database.accountDao().findById(movement.accountId)
                FinancialMovementDto(account?.name ?: "Account #${movement.accountId}", money(movement.amount))
            }
        } else null
        return EventDetailDto(event.toHistoryDto(events.mapNotNull { it.correctsEventId }.toSet()), inventory, financial)
    }

    suspend fun deliveries(sessionHandle: String): List<DeliveryDto> {
        security.requirePermission(sessionHandle, AppPermission.DELIVERY)
        return database.deliveryDao().activeDeliveries().map { delivery ->
            val customer = database.accountDao().findById(delivery.customerAccountId)
            val product = database.productDao().findById(delivery.productId)
            val status = database.deliveryDao().latestStatus(delivery.deliveryId)
            DeliveryDto(
                deliveryId = delivery.deliveryId,
                customerAccountId = delivery.customerAccountId,
                customerName = customer?.name ?: "Customer #${delivery.customerAccountId}",
                productId = delivery.productId,
                productName = product?.name ?: "Product #${delivery.productId}",
                unit = product?.unit ?: "",
                quantity = decimal(delivery.quantity),
                route = delivery.route,
                businessDate = delivery.businessDate,
                saleEventId = delivery.saleEventId,
                statusCode = status?.status,
                statusOccurredAt = status?.occurredAt,
                notes = delivery.notes,
            )
        }
    }

    suspend fun attendance(sessionHandle: String, businessDate: String): List<AttendanceDto> {
        security.requirePermission(sessionHandle, AppPermission.ATTENDANCE)
        return database.accountDao().findByCategory("Salary").mapNotNull { employee ->
            database.attendanceDao().current(employee.accountId, businessDate)?.let {
                AttendanceDto(it.attendanceId, employee.accountId, employee.name.removePrefix("Employee: ").trim(), it.businessDate, it.status, it.notes, it.createdAt)
            }
        }
    }

    suspend fun spoilageHistory(sessionHandle: String): List<SpoilageDto> {
        security.requirePermission(sessionHandle, AppPermission.PRODUCTION_AND_SPOILAGE)
        return database.spoilageDao().history().map {
            SpoilageDto(it.eventId, it.occurredAt, it.productId, it.productName, it.unit, decimal(it.quantity), it.reason, it.correctsEventId, it.superseded, it.notes)
        }
    }

    suspend fun reconciliationArchive(sessionHandle: String): ReconciliationArchiveDto {
        security.requirePermission(sessionHandle, AppPermission.CASH_COUNT_AND_CLOSE)
        val counts = database.physicalCashCountDao().all().map { count ->
            val expected = database.physicalCashCountDao().canonicalBalanceAt(count.accountId, count.countedAt)
            CashCountDto(count.countId, count.accountId, count.businessDate, money(count.countedAmount), money(expected), money(count.countedAmount - expected), count.countedAt, count.notes)
        }
        val actions = database.dailyCloseDao().all().map {
            DailyCloseActionDto(it.actionId, it.businessDate, it.action, it.physicalCashCountId, AuditActorDto(it.actorUserId, it.actorName, it.actorRole), it.reason, it.createdAt, it.supersedesActionId)
        }
        return ReconciliationArchiveDto(counts, actions)
    }

    private suspend fun EventEntity.toHistoryDto(supersededIds: Set<Long>): EventHistoryDto {
        val actor = database.eventActorLinkDao().find(eventId)?.let {
            AuditActorDto(it.actorUserId, it.actorNameSnapshot, it.actorRoleSnapshot)
        }
        return EventHistoryDto(
            eventId = eventId,
            createdAt = createdAt,
            occurredAt = occurredAt,
            eventTypeCode = eventType,
            channelCode = channel,
            correctsEventId = correctsEventId,
            isCorrection = correctsEventId != null,
            isSuperseded = eventId in supersededIds,
            actor = actor,
            notes = notes,
        )
    }
}
