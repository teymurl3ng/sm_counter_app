package com.saaf.milkshop.domain

import androidx.room3.withWriteTransaction
import com.saaf.milkshop.data.MilkShopDatabase

data class BusinessSettings(
    val milkRateVendor: Double,
    val deliveryFeeVendor: Double,
    val tsStandard: Double,
    val retailRateMilk: Double,
    val retailRateYogurt: Double,
    val defaultDeliverySurcharge: Double,
)

class BusinessSettingsService(
    private val database: MilkShopDatabase,
) {
    suspend fun get(): BusinessSettings {
        val values = database.settingsDao().getAll().associate { it.key to it.value }
        return BusinessSettings(
            milkRateVendor = requireSetting(values, SettingKeys.MILK_RATE_VENDOR),
            deliveryFeeVendor = requireSetting(values, SettingKeys.DELIVERY_FEE_VENDOR),
            tsStandard = requireSetting(values, SettingKeys.TS_STANDARD),
            retailRateMilk = requireSetting(values, SettingKeys.RETAIL_RATE_MILK),
            retailRateYogurt = requireSetting(values, SettingKeys.RETAIL_RATE_YOGURT),
            defaultDeliverySurcharge = requireSetting(values, SettingKeys.DEFAULT_DELIVERY_SURCHARGE),
        )
    }

    suspend fun update(settings: BusinessSettings) {
        validatePositive(settings)
        database.withWriteTransaction {
            val dao = database.settingsDao()
            update(dao, SettingKeys.MILK_RATE_VENDOR, settings.milkRateVendor)
            update(dao, SettingKeys.DELIVERY_FEE_VENDOR, settings.deliveryFeeVendor)
            update(dao, SettingKeys.TS_STANDARD, settings.tsStandard)
            update(dao, SettingKeys.RETAIL_RATE_MILK, settings.retailRateMilk)
            update(dao, SettingKeys.RETAIL_RATE_YOGURT, settings.retailRateYogurt)
            update(dao, SettingKeys.DEFAULT_DELIVERY_SURCHARGE, settings.defaultDeliverySurcharge)
        }
    }

    private fun validatePositive(settings: BusinessSettings) {
        val values = listOf(
            settings.milkRateVendor,
            settings.deliveryFeeVendor,
            settings.tsStandard,
            settings.retailRateMilk,
            settings.retailRateYogurt,
            settings.defaultDeliverySurcharge,
        )
        require(values.all { it > 0.0 && it.isFinite() }) {
            "All business settings must be greater than zero."
        }
    }

    private suspend fun update(dao: com.saaf.milkshop.data.SettingsDao, key: String, value: Double) {
        check(dao.updateValue(key, value) == 1) { "Missing setting: $key" }
    }

    private fun requireSetting(values: Map<String, Double>, key: String): Double =
        values[key] ?: error("Missing setting: $key")
}
