package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.operations.NativeDeliveriesFacade
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.CreateUserInput
import com.saaf.milkshop.domain.CustomerManagementService
import com.saaf.milkshop.domain.DeliveryInput
import com.saaf.milkshop.domain.DeliveryService
import com.saaf.milkshop.domain.UserSecurityService
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class NativeDeliveriesFacadeTest {
    private lateinit var database: MilkShopDatabase
    private lateinit var security: UserSecurityService
    private lateinit var facade: NativeDeliveriesFacade
    private lateinit var ownerSession: String
    private lateinit var operatorSession: String
    private var customerId: Long = 0L
    private var milkId: Long = 0L

    @Before
    fun setUp() {
        runBlocking {
            database = MilkShopDatabase.createInMemory()
            security = UserSecurityService(database)
            facade = NativeDeliveriesFacade(database, security)
            ownerSession = security.bootstrapOwner(
                "delivery.owner",
                "Delivery Owner",
                OWNER_PASSPHRASE.toCharArray(),
            ).session.sessionId
            val pending = security.beginUserEnrollment(
                ownerSession,
                CreateUserInput(
                    loginName = "delivery.operator",
                    displayName = "Delivery Operator",
                    role = AppRole.OPERATOR.wireValue,
                ),
            )
            security.completeUserEnrollment(pending.enrollmentToken, OPERATOR_PASSPHRASE.toCharArray())
            operatorSession = security.login("delivery.operator", OPERATOR_PASSPHRASE.toCharArray()).sessionId
            customerId = CustomerManagementService(database).create("Delivery Customer")
            milkId = database.productDao().findByName("Milk")!!.productId
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun loadExposesPendingPhysicalRowsWithoutMoneyFields() = runBlocking {
        DeliveryService(database).create(
            DeliveryInput(
                customerAccountId = customerId,
                productId = milkId,
                quantity = 4.5,
                route = "Route A",
                businessDate = DATE.toString(),
            ),
        )
        val context = facade.load(operatorSession, DATE)
        assertEquals(DATE.toString(), context.businessDate)
        assertTrue(context.postingOpen)
        assertEquals(1, context.today.size)
        assertTrue(context.earlier.isEmpty())
        val item = context.today.single()
        assertTrue(item.customerName.contains("Delivery Customer", ignoreCase = true))
        assertTrue(item.quantityLabel.contains("4.5"))
        assertEquals("Route A", item.route)
        val fields = context.javaClass.declaredFields.map { it.name.lowercase() }.toSet()
        assertFalse(fields.any { it.contains("rate") })
        assertFalse(fields.any { it.contains("amount") })
        assertFalse(fields.any { it.contains("paisa") })
        assertFalse(fields.any { it.contains("balance") })
        assertFalse(fields.any { it.contains("sale") })
    }

    @Test
    fun markDeliveredRemovesFromActiveList() = runBlocking {
        val deliveryId = DeliveryService(database).create(
            DeliveryInput(
                customerAccountId = customerId,
                productId = milkId,
                quantity = 2.0,
                businessDate = DATE.toString(),
            ),
        )
        val refreshed = facade.markDelivered(operatorSession, deliveryId, DATE)
        assertTrue(refreshed.isEmpty)
        assertEquals(
            listOf("pending", "delivered"),
            database.deliveryDao().statusHistory(deliveryId).map { it.status },
        )
    }

    companion object {
        private val DATE = LocalDate.of(2026, 9, 20)
        private const val OWNER_PASSPHRASE = "delivery owner passphrase 2026!!"
        private const val OPERATOR_PASSPHRASE = "delivery operator passphrase 2026!!"
    }
}
