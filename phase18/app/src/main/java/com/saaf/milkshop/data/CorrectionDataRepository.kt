package com.saaf.milkshop.data

import com.saaf.milkshop.domain.CorrectionInput
import com.saaf.milkshop.domain.CorrectionResult
import com.saaf.milkshop.domain.CorrectionService
import com.saaf.milkshop.sync.BusinessDateGuard

/**
 * Application-facing boundary for correction execution.
 * CorrectionService remains authoritative for correction semantics, ledger
 * deltas, validation, and persistence.
 */
class CorrectionDataRepository(
    database: MilkShopDatabase,
) {
    private val service = CorrectionService(database)

    suspend fun correct(input: CorrectionInput): CorrectionResult {
        // H4: corrections backdate to the corrected event's date by design —
        // the horizon guard only blocks future-beyond-horizon dates.
        BusinessDateGuard.requireHappenedWithinHorizon(input.occurredAt)
        return service.correct(input)
    }
}
