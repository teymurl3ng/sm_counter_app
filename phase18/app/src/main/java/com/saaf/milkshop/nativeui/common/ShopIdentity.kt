package com.saaf.milkshop.nativeui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.nativeui.DataChangeBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext

val LocalShopName = compositionLocalOf { "Your shop" }

/** One observable merchant identity for welcome, access, and every role shell. */
@Composable
fun ShopIdentityProvider(database: MilkShopDatabase, content: @Composable () -> Unit) {
    val name by produceState("Your shop", database) {
        DataChangeBus.changes.onStart { emit(Unit) }.collect {
            value = runCatching { withContext(Dispatchers.IO) {
                database.storeProfileDao().get()?.storeName?.trim()?.takeIf(String::isNotEmpty)
                    ?: "Your shop"
            } }.getOrElse { error ->
                if (error is kotlinx.coroutines.CancellationException) throw error
                android.util.Log.w("NativeShopIdentity", "Merchant identity could not refresh", error)
                value
            }
        }
    }
    CompositionLocalProvider(LocalShopName provides name, content = content)
}
