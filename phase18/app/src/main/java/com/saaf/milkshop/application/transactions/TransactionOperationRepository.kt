package com.saaf.milkshop.application.transactions

import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.CreditCollectionInput
import com.saaf.milkshop.domain.CreditCollectionResult
import com.saaf.milkshop.domain.CreditCollectionService
import com.saaf.milkshop.domain.ExpenseInput
import com.saaf.milkshop.domain.ExpenseResult
import com.saaf.milkshop.domain.ExpenseService
import com.saaf.milkshop.domain.SalaryPaymentInput
import com.saaf.milkshop.domain.SalaryPaymentResult
import com.saaf.milkshop.domain.SalaryPaymentService
import com.saaf.milkshop.domain.SaleInput
import com.saaf.milkshop.domain.SaleResult
import com.saaf.milkshop.domain.SaleService
import com.saaf.milkshop.domain.VendorPaymentInput
import com.saaf.milkshop.domain.VendorPaymentResult
import com.saaf.milkshop.domain.VendorPaymentService
import com.saaf.milkshop.domain.VendorPurchaseInput
import com.saaf.milkshop.domain.VendorPurchaseResult
import com.saaf.milkshop.domain.VendorPurchaseService
import com.saaf.milkshop.domain.YogurtProductionInput
import com.saaf.milkshop.domain.YogurtProductionResult
import com.saaf.milkshop.domain.YogurtProductionService
import com.saaf.milkshop.sync.BusinessDateGuard

/**
 * Application-facing transaction operation contract.
 *
 * Presentation actions depend on this contract instead of the Room database
 * or concrete domain service constructors. The existing domain services remain
 * authoritative for validation, accounting, calculations, and persistence.
 */
interface TransactionOperationRepository {
    suspend fun recordPurchase(input: VendorPurchaseInput): VendorPurchaseResult
    suspend fun recordSale(input: SaleInput): SaleResult
    suspend fun recordProduction(input: YogurtProductionInput): YogurtProductionResult
    suspend fun recordVendorPayment(input: VendorPaymentInput): VendorPaymentResult
    suspend fun recordCreditCollection(input: CreditCollectionInput): CreditCollectionResult
    suspend fun recordExpense(input: ExpenseInput): ExpenseResult
    suspend fun recordSalaryPayment(input: SalaryPaymentInput): SalaryPaymentResult
}

/**
 * Production implementation that composes the existing domain services.
 * H4: this legacy path bypasses the bridge command layer, so the trusted-date
 * horizon guard is applied here — a wrong device clock can no longer post an
 * arbitrarily future date while a trusted anchor exists (backdating to open
 * days stays allowed, mirroring the bridge layer).
 */
class DomainTransactionOperationRepository(
    private val database: MilkShopDatabase,
) : TransactionOperationRepository {
    override suspend fun recordPurchase(input: VendorPurchaseInput): VendorPurchaseResult =
        BusinessDateGuard.requireHappenedWithinHorizon(input.occurredAt).let {
            VendorPurchaseService(database).record(input)
        }

    override suspend fun recordSale(input: SaleInput): SaleResult =
        BusinessDateGuard.requireHappenedWithinHorizon(input.occurredAt).let {
            SaleService(database).record(input)
        }

    override suspend fun recordProduction(input: YogurtProductionInput): YogurtProductionResult =
        BusinessDateGuard.requireHappenedWithinHorizon(input.occurredAt).let {
            YogurtProductionService(database).record(input)
        }

    override suspend fun recordVendorPayment(input: VendorPaymentInput): VendorPaymentResult =
        BusinessDateGuard.requireHappenedWithinHorizon(input.occurredAt).let {
            VendorPaymentService(database).record(input)
        }

    override suspend fun recordCreditCollection(input: CreditCollectionInput): CreditCollectionResult =
        BusinessDateGuard.requireHappenedWithinHorizon(input.occurredAt).let {
            CreditCollectionService(database).record(input)
        }

    override suspend fun recordExpense(input: ExpenseInput): ExpenseResult =
        BusinessDateGuard.requireHappenedWithinHorizon(input.occurredAt).let {
            ExpenseService(database).record(input)
        }

    override suspend fun recordSalaryPayment(input: SalaryPaymentInput): SalaryPaymentResult =
        BusinessDateGuard.requireHappenedWithinHorizon(input.occurredAt).let {
            SalaryPaymentService(database).record(input)
        }
}
