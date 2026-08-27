package com.saaf.milkshop.data

import com.saaf.milkshop.domain.CorrectionInput
import com.saaf.milkshop.domain.CorrectionResult
import com.saaf.milkshop.domain.CorrectionService

/**
 * Application-facing boundary for correction execution.
 * CorrectionService remains authoritative for correction semantics, ledger
 * deltas, validation, and persistence.
 */
class CorrectionDataRepository(
    database: MilkShopDatabase,
) {
    private val service = CorrectionService(database)

    suspend fun correct(input: CorrectionInput): CorrectionResult = service.correct(input)
}
