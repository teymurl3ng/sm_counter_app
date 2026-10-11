package com.saaf.milkshop

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.StoreIdentityInput
import com.saaf.milkshop.domain.StoreIdentityService
import com.saaf.milkshop.nativeui.DataChangeBus
import com.saaf.milkshop.nativeui.common.LocalShopName
import com.saaf.milkshop.nativeui.common.ShopIdentityProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeShopIdentityRegressionTest {
    @get:Rule val compose = createComposeRule()
    @Test fun changedMerchantNameRefreshesWithoutRestartOrChangingActor() {
        val database = MilkShopDatabase.createInMemory()
        try {
            runBlocking { StoreIdentityService(database).save(StoreIdentityInput("Test grocer")) }
            assertEquals("Test grocer", runBlocking { database.storeProfileDao().get()?.storeName })
            compose.setContent {
                ShopIdentityProvider(database) {
                    androidx.compose.foundation.layout.Column {
                        Text(LocalShopName.current)
                        Text("Signed in: Closing cashier")
                    }
                }
            }
            try {
                compose.waitUntil(5_000) { runCatching { compose.onNodeWithText("Test grocer").fetchSemanticsNode() }.isSuccess }
            } catch (failure: Throwable) {
                println("Identity failure: " + compose.onRoot(useUnmergedTree = true).printToString())
                throw failure
            }
            val renamed = "نور جنرل اسٹور — Neighbourhood groceries"
            runBlocking { StoreIdentityService(database).save(StoreIdentityInput(renamed)) }
            DataChangeBus.notifyDataChanged()
            compose.waitUntil(5_000) { runCatching { compose.onNodeWithText(renamed).fetchSemanticsNode() }.isSuccess }
            compose.onNodeWithText(renamed).assertIsDisplayed()
            compose.onNodeWithText("Signed in: Closing cashier").assertIsDisplayed()
        } finally { database.close() }
    }
}
