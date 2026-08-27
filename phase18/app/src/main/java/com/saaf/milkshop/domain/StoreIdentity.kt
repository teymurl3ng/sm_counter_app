package com.saaf.milkshop.domain

import androidx.room3.withWriteTransaction
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.data.StoreProfileEntity
import java.time.Instant

data class StoreIdentityInput(
    val storeName: String,
    val phone: String? = null,
    val address: String? = null,
)

class StoreIdentityService(private val database: MilkShopDatabase) {
    suspend fun get(): StoreProfileEntity? = database.storeProfileDao().get()

    suspend fun save(input: StoreIdentityInput): StoreProfileEntity = database.withWriteTransaction {
        val name = input.storeName.trim()
        require(name.isNotEmpty()) { "Enter a store name" }
        require(!name.contains('\n')) { "Store name cannot contain a new line" }
        val profile = StoreProfileEntity(
            storeName = name,
            currencyCode = CURRENCY_PKR,
            phone = input.phone.cleanOptional(),
            address = input.address.cleanOptional(),
            updatedAt = Instant.now().toString(),
        )
        database.storeProfileDao().save(profile)
        profile
    }

    companion object {
        const val CURRENCY_PKR = "PKR"
    }
}
