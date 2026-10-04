package com.saaf.milkshop.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM tests for the sync-review hold titles. The hold row carries the device
 * payload snapshot; the label must name the row in human terms and never
 * throw on a thin or malformed payload.
 */
class SyncHoldLabelsTest {

    @Test
    fun productLabel_usesName() {
        assertEquals(
            "Product — Milk",
            SyncHoldLabels.holdLabel(
                "products",
                """{"uid":"u1_seed_products_Milk","name":"Milk","unit":"litre"}""",
                "u1_seed_products_Milk",
            ),
        )
    }

    @Test
    fun accountLabel_nameAndType() {
        assertEquals(
            "Account — Ahmed Khan (customer)",
            SyncHoldLabels.holdLabel(
                "accounts",
                """{"name":"Ahmed Khan","type":"CUSTOMER","category":"retail"}""",
                "u1_79416751bdf44b92abe926f75feeb8f9",
            ),
        )
    }

    @Test
    fun eventLabel_typeAndAmountTrimmed() {
        assertEquals(
            "Event — Sale · 1200",
            SyncHoldLabels.holdLabel(
                "events",
                """{"event_type":"sale","amount":1200.0,"occurred_at":"2026-10-04T09:00:00Z"}""",
                "u1_aacd6a97ddd74beb8719486aece73bc4",
            ),
        )
    }

    @Test
    fun eventLabel_quantityWhenAmountAbsent() {
        // Real DEV seed shape: a SALE event carries sale_quantity, not amount.
        assertEquals(
            "Event — Sale · qty 1.5",
            SyncHoldLabels.holdLabel(
                "events",
                """{"event_type":"SALE","sale_quantity":1.5,"occurred_at":"2026-09-28"}""",
                "u1_e5ce23f6ae4543b89672f03aa64c5cbc",
            ),
        )
    }

    @Test
    fun eventLabel_typeOnlyWhenNothingNumeric() {
        assertEquals(
            "Event — Production",
            SyncHoldLabels.holdLabel(
                "events",
                """{"event_type":"PRODUCTION","occurred_at":"2026-09-28"}""",
                "u1_aacd6a97ddd74beb8719486aece73bc4",
            ),
        )
    }

    @Test
    fun settingsLabel_usesKey() {
        assertEquals(
            "Setting — milk_rate_vdr",
            SyncHoldLabels.holdLabel(
                "settings",
                """{"setting_key":"milk_rate_vdr","setting_value":"120.5"}""",
                "setting:milk_rate_vdr",
            ),
        )
    }

    @Test
    fun deliveryLabel_humanFieldsOnly() {
        val label = SyncHoldLabels.holdLabel(
            "deliveries",
            """{"customer_account_id":3,"quantity":25.0,"route":"North","business_date":"2026-08-12"}""",
            "u1_del_1",
        )
        assertEquals("Delivery — route North · qty 25 · 2026-08-12", label)
    }

    @Test
    fun malformedPayload_fallsBackToShortUid() {
        val label = SyncHoldLabels.holdLabel("products", "{not json", "u1_9063eba67c3d41789cbf39243d115719")
        assertTrue(label.startsWith("Product · "))
        assertTrue(label.contains("u1_9063"))
    }

    @Test
    fun nullAndBlankPayload_fallBack() {
        assertTrue(SyncHoldLabels.holdLabel("accounts", null, "u1_f5a755f752c749c185023d112a724eaf").startsWith("Account · "))
        assertTrue(SyncHoldLabels.holdLabel("accounts", "", "u1_f5a755f752c749c185023d112a724eaf").startsWith("Account · "))
    }

    @Test
    fun missingField_fallsBackWithinKnownFields() {
        // products has no name in this payload; the generic chain must not
        // invent a title, and the table+uid fallback keeps it decipherable.
        val label = SyncHoldLabels.holdLabel("products", """{"unit":"litre"}""", "u1_seed_products_Milk")
        assertTrue(label.startsWith("Product · "))
    }

    @Test
    fun unknownTable_humanizesTableName() {
        val label = SyncHoldLabels.holdLabel("some_future_table", """{"name":"X"}""", "u1_x")
        assertEquals("Some_future_table — X", label)
    }

    @Test
    fun jsonNullValue_isTreatedAsAbsent() {
        val label = SyncHoldLabels.holdLabel("store_profile", """{"store_name":null}""", "u1_singleton_store_profile")
        assertTrue(label.startsWith("Shop profile · "))
    }
}
