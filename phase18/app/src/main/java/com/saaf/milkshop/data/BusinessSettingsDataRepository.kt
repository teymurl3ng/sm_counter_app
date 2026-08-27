package com.saaf.milkshop.data

import com.saaf.milkshop.domain.BusinessSettings
import com.saaf.milkshop.domain.BusinessSettingsService

/**
 * Application-facing boundary for business-settings persistence.
 * BusinessSettingsService remains authoritative for validation and persistence.
 */
class BusinessSettingsDataRepository(
    database: MilkShopDatabase,
) {
    private val service = BusinessSettingsService(database)

    suspend fun get(): BusinessSettings = service.get()

    suspend fun update(settings: BusinessSettings) = service.update(settings)
}
