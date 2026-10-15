package com.saaf.milkshop.domain

/** The exact amount arithmetic shared by authoritative posting and draft/credit previews. */
data class SaleAmounts(val quantity: Double, val value: Double, val surcharge: Double) {
    val total: Double get() = value + surcharge
    companion object {
        fun calculate(unitRate: Double, quantity: Double?, value: Double?, surcharge: Double): SaleAmounts {
            require(unitRate.isFinite() && unitRate > 0 && (quantity != null).xor(value != null))
            require(surcharge.isFinite() && surcharge >= 0)
            val entered = quantity ?: requireNotNull(value)
            require(entered.isFinite() && entered > 0)
            val result = SaleAmounts(quantity ?: entered / unitRate, value ?: entered * unitRate, surcharge)
            require(result.quantity.isFinite() && result.value.isFinite() && result.total.isFinite())
            return result
        }
    }
}
