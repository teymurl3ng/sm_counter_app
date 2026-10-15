package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.data.EventActorLinkEntity
import com.saaf.milkshop.data.EventEntity
import com.saaf.milkshop.data.FinancialLogEntity
import com.saaf.milkshop.data.InventoryLogEntity
import com.saaf.milkshop.data.AccountEntity
import com.saaf.milkshop.data.AppUserEntity
import com.saaf.milkshop.data.CustomerProfileEntity
import com.saaf.milkshop.data.DeliveryEntity
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.data.SyncOutboxEntity
import com.saaf.milkshop.sync.AccountAuthClient
import com.saaf.milkshop.sync.AccountAuthResult
import com.saaf.milkshop.sync.SyncEnvelopeFactory
import com.saaf.milkshop.sync.SyncHttp
import com.saaf.milkshop.sync.SyncHttpRequest
import com.saaf.milkshop.sync.SyncHttpResponse
import com.saaf.milkshop.sync.SyncPushResult
import com.saaf.milkshop.sync.SyncServerClient
import com.saaf.milkshop.sync.SyncServerConfig
import com.saaf.milkshop.sync.SyncServerConfigStore
import com.saaf.milkshop.sync.SyncTokenStore
import com.saaf.milkshop.sync.TrustedTime
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException

/**
 * Phase 4: the transport and auth client, exercised against a scripted
 * HTTP fake that replays the shapes the DEV probe contract froze. Nothing
 * here touches the network; nothing here can affect entry paths (D9).
 */
@RunWith(AndroidJUnit4::class)
class SyncTransportTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val config = SyncServerConfig(
        environment = SyncServerConfig.ENVIRONMENT_DEV,
        baseUrl = "https://saafmilk-dev.example",
        publishableKey = "sb_publishable_test",
    )

    @Before
    fun setUp() {
        TrustedTime.resetForTest(context)
        TrustedTime.warmUp(context)
        SyncTokenStore(context).clear()
    }

    @After
    fun tearDown() {
        TrustedTime.resetForTest(context)
        SyncTokenStore(context).clear()
    }

    private class FakeSyncHttp : SyncHttp {
        val requests = mutableListOf<SyncHttpRequest>()
        var handler: (SyncHttpRequest) -> SyncHttpResponse = { SyncHttpResponse(500, null) }

        override suspend fun execute(request: SyncHttpRequest): SyncHttpResponse {
            requests += request
            return handler(request)
        }

        fun route(tokenStatus: Int = 200, tokenBody: String? = null, profileStatus: Int = 200, profileBody: String? = null) {
            handler = { request ->
                when {
                    request.url.contains("/auth/v1/token") -> SyncHttpResponse(tokenStatus, tokenBody?.toByteArray())
                    request.url.contains("/rest/v1/rpc/my_profile") -> SyncHttpResponse(profileStatus, profileBody?.toByteArray())
                    else -> SyncHttpResponse(500, null)
                }
            }
        }
    }

    private fun tokenBody() =
        """{"access_token":"jwt-token-1","refresh_token":"refresh-1","expires_in":3600,"server_time_epoch_ms":1800000000000}"""

    @Test
    fun malformedPushAcknowledgementsKeepQueuedWork() = runBlocking {
        val db = MilkShopDatabase.createInMemory()
        try {
            SyncTokenStore(context).save(SyncTokenStore.Session("test", null, null))
            val envelope = JSONObject().put("table", "products").put("uid", "test").put("payload", JSONObject())
            for (body in listOf("", "garbage", "{}", "{\"ok\":\"true\"}")) {
                val http = FakeSyncHttp().apply { handler = { SyncHttpResponse(200, body.toByteArray()) } }
                assertEquals(SyncPushResult.Transient("malformed_response"),
                    SyncServerClient(http, config, db, SyncTokenStore(context)).push(listOf(envelope), emptyList()))
            }
        } finally { db.close() }
    }

    private fun profileBody(active: Boolean = true) = """
        {"staff_id":"11111111-1111-4111-8111-111111111111","username":"owner","display_name":"Owner",
         "role":"owner","active":$active,"terminated_at":null,"business_id":"22222222-2222-4222-8222-222222222222",
         "shops":[{"shop_id":"33333333-3333-4333-8333-333333333333","name":"Main shop","timezone":"Asia/Karachi"}],
         "server_time_epoch_ms":1800000000000}
    """.trimIndent()

    @Test
    fun tokenStore_roundTripsUnderKeystoreEncryption() {
        val store = SyncTokenStore(context)
        store.clear()
        store.save(SyncTokenStore.Session("access-secret-123", "refresh-secret-456", 1800003600000))

        val loaded = store.load()!!
        assertEquals("access-secret-123", loaded.accessToken)
        assertEquals("refresh-secret-456", loaded.refreshToken)
        assertEquals(1800003600000, loaded.accessTokenExpiresAtEpochMs)

        val raw = File(File(context.noBackupFilesDir, "sync"), "session.bin").readBytes()
        assertFalse(String(raw, Charsets.ISO_8859_1).contains("access-secret-123"))
        assertFalse(String(raw, Charsets.ISO_8859_1).contains("refresh-secret-456"))

        store.clear()
        assertNull(store.load())
    }

    @Test
    fun serverConfig_storeRoundTrips() {
        SyncServerConfigStore.clear(context)
        assertNull(SyncServerConfigStore.load(context))
        SyncServerConfigStore.save(context, config)
        val loaded = SyncServerConfigStore.load(context)!!
        assertEquals(config.baseUrl, loaded.baseUrl)
        assertEquals(config.publishableKey, loaded.publishableKey)
        assertEquals(config.environment, loaded.environment)
        assertEquals("https://saafmilk-dev.example/rest/v1/rpc/ping", loaded.url("/rest/v1/rpc/ping"))
        SyncServerConfigStore.clear(context)
        assertNull(SyncServerConfigStore.load(context))
    }

    @Test
    fun signIn_allowed_carriesIdentityAndAnchorsTime() = runBlocking {
        val http = FakeSyncHttp()
        http.route(tokenBody = tokenBody(), profileBody = profileBody())
        val result = AccountAuthClient(http, config).signIn("Owner", "123456789012345".toCharArray())

        assertTrue(result.toString(), result is AccountAuthResult.Allowed)
        result as AccountAuthResult.Allowed
        assertEquals("11111111-1111-4111-8111-111111111111", result.staffId)
        assertEquals("owner", result.role)
        assertEquals("Asia/Karachi", result.shopTimezone)
        assertEquals(1, result.shops.size)
        assertEquals("Main shop", result.shops.single().shopName)
        assertEquals(1800003600000L, result.accessTokenExpiresAtEpochMs)
        // Profile verification anchored trusted time from the server body.
        assertTrue(TrustedTime.isAnchored())
        assertTrue(Math.abs((TrustedTime.trustedNowEpochMs() ?: 0) - 1800000000000) < 5_000)
        // Header boundary per spec §5: apikey everywhere, publishable bearer for sign-in.
        val signIn = http.requests.first { it.url.contains("/auth/v1/token") }
        assertEquals("sb_publishable_test", signIn.headers["apikey"])
        assertEquals("Bearer sb_publishable_test", signIn.headers["Authorization"])
        assertEquals("owner@saafmilk.local", JSONObject(String(signIn.body!!)).getString("email"))
        val profile = http.requests.first { it.url.contains("my_profile") }
        assertEquals("Bearer jwt-token-1", profile.headers["Authorization"])
    }

    @Test
    fun refresh_nullTimezoneFallsBackToShopZone() = runBlocking {
        val profile = JSONObject(profileBody()).put("shop_timezone", JSONObject.NULL)
        profile.getJSONArray("shops").getJSONObject(0).put("shop_timezone", JSONObject.NULL)
        val http = FakeSyncHttp()
        http.route(tokenBody = tokenBody(), profileBody = profile.toString())
        val result = AccountAuthClient(http, config).refresh("fixture-refresh") as AccountAuthResult.Allowed
        assertEquals("Asia/Karachi", result.shops.single().timezone)
        assertEquals("Asia/Karachi", result.shopTimezone)
    }

    @Test
    fun refresh_absentTimezonesKeepExistingBusinessZone() = runBlocking {
        val original = com.saaf.milkshop.sync.BusinessClock.currentZone()
        val profile = JSONObject(profileBody()).put("shop_timezone", JSONObject.NULL)
        profile.getJSONArray("shops").getJSONObject(0)
            .put("shop_timezone", JSONObject.NULL).put("timezone", JSONObject.NULL)
        val http = FakeSyncHttp()
        http.route(tokenBody = tokenBody(), profileBody = profile.toString())
        val result = AccountAuthClient(http, config).refresh("fixture-refresh") as AccountAuthResult.Allowed
        assertNull(result.shops.single().timezone)
        assertNull(result.shopTimezone)
        result.shopTimezone?.let(com.saaf.milkshop.sync.BusinessClock::setZone)
        assertEquals(original, com.saaf.milkshop.sync.BusinessClock.currentZone())
    }

    @Test
    fun refresh_explicitTimezoneStillTakesPrecedence() = runBlocking {
        val profile = JSONObject(profileBody()).put("shop_timezone", "Europe/London")
        val http = FakeSyncHttp()
        http.route(tokenBody = tokenBody(), profileBody = profile.toString())
        val result = AccountAuthClient(http, config).refresh("fixture-refresh") as AccountAuthResult.Allowed
        assertEquals("Europe/London", result.shopTimezone)
        assertEquals("Asia/Karachi", result.shops.single().timezone)
    }

    @Test
    fun signIn_invalidCredentials_isTheOnlyCredentialVerdict() = runBlocking {
        val http = FakeSyncHttp()
        http.route(tokenStatus = 400, tokenBody = """{"error_code":"invalid_credentials"}""")
        val result = AccountAuthClient(http, config).signIn("owner", "wrong".toCharArray())
        assertEquals(AccountAuthResult.InvalidCredentials, result)
    }

    @Test
    fun signIn_disabledStaff_andNoStaffProfile_classifyDefinitively() = runBlocking {
        val disabled = FakeSyncHttp()
        disabled.route(tokenBody = tokenBody(), profileBody = profileBody(active = false))
        assertEquals(AccountAuthResult.Disabled, AccountAuthClient(disabled, config).signIn("owner", "x".toCharArray()))

        val noStaff = FakeSyncHttp()
        noStaff.route(tokenBody = tokenBody(), profileBody = "null")
        assertEquals(AccountAuthResult.NotAuthorized, AccountAuthClient(noStaff, config).signIn("owner", "x".toCharArray()))
    }

    @Test
    fun signIn_unknownOutcomes_neverBecomeIdentityDecisions() = runBlocking {
        val io = FakeSyncHttp()
        io.handler = { throw IOException("offline") }
        assertTrue(AccountAuthClient(io, config).signIn("owner", "x".toCharArray()) is AccountAuthResult.Unknown)

        val server = FakeSyncHttp()
        server.route(tokenStatus = 503)
        assertTrue(AccountAuthClient(server, config).signIn("owner", "x".toCharArray()) is AccountAuthResult.Unknown)

        val malformed = FakeSyncHttp()
        malformed.route(tokenStatus = 400, tokenBody = """{"error_code":"validation_failed"}""")
        assertTrue(AccountAuthClient(malformed, config).signIn("owner", "x".toCharArray()) is AccountAuthResult.Unknown)
    }

    @Test
    fun envelopeFactory_embedsChildrenAndProfiles() = runBlocking {
        val database = MilkShopDatabase.createInMemory()
        try {
            // Opening stock first: the app's own negative-stock guard rejects
            // a sale movement from an empty drawer (as it should).
            val purchase = EventEntity(
                createdAt = "2026-09-28T06:00:00Z",
                occurredAt = "2026-09-28",
                eventType = "PURCHASE_RECEIPT",
                rawLiters = 80.0,
                fatPercentage = 4.0,
                lrReading = 28.0,
                snfPercentage = 5.0,
                tsPercentage = 9.0,
                adjustedLiters = 80.0,
                amountOwed = 14520.0,
                milkRateUsed = 170.0,
                deliveryFeeUsed = 11.0,
                tsStandardUsed = 13.0,
            )
            val purchaseId = database.eventDao().insert(purchase)
            database.inventoryLogDao().insert(
                InventoryLogEntity(eventId = purchaseId, productId = 1L, quantity = 80.0),
            )
            // The actor link is FK-bound to a real app user.
            val actorUserId = database.appUserDao().insert(
                AppUserEntity(
                    loginName = "envelope.owner",
                    displayName = "Envelope Owner",
                    role = "owner",
                    createdAt = "2026-09-28T09:00:00Z",
                    updatedAt = "2026-09-28T09:00:00Z",
                ),
            )
            val event = EventEntity(
                createdAt = "2026-09-28T10:00:00Z",
                occurredAt = "2026-09-28",
                eventType = "SALE",
                saleQuantity = 2.0,
                saleValue = 380.0,
                unitRate = 190.0,
            )
            val eventId = database.eventDao().insert(event)
            database.inventoryLogDao().insert(InventoryLogEntity(eventId = eventId, productId = 1L, quantity = -2.0))
            database.financialLogDao().insert(FinancialLogEntity(eventId = eventId, accountId = 1L, amount = 380.0))
            database.eventActorLinkDao().insert(
                EventActorLinkEntity(eventId, actorUserId, "Owner", "owner", "2026-09-28T10:00:01Z"),
            )
            val accountId = database.accountDao().insert(
                AccountEntity(name = "Customer: Envelope", type = "Asset", category = "Customer"),
            )
            database.customerProfileDao().save(
                CustomerProfileEntity(accountId = accountId, phone = "0300", createdAt = "2026-09-28T09:00:00Z"),
            )
            val deliveryId = database.deliveryDao().insert(
                DeliveryEntity(
                    customerAccountId = accountId,
                    productId = 1L,
                    quantity = 2.0,
                    businessDate = "2026-09-28",
                    saleEventId = eventId,
                    createdAt = "2026-09-28T10:00:02Z",
                ),
            )
            val correction = database.eventDao().insert(
                EventEntity(
                    createdAt = "2026-09-28T12:00:00Z",
                    occurredAt = "2026-09-28",
                    eventType = "SALE",
                    saleQuantity = 3.0,
                    saleValue = 570.0,
                    unitRate = 190.0,
                    correctsEventId = eventId,
                ),
            )

            val outbox = database.syncOutboxDao().nextBatch(1000)
            val build = SyncEnvelopeFactory(database).build(outbox)

            val eventEnvelope = build.envelopes.first { it.getString("uid") == event.eventUid }
            assertEquals("events", eventEnvelope.getString("table"))
            val payload = eventEnvelope.getJSONObject("payload")
            assertEquals(2.0, payload.getDouble("sale_quantity"), 1e-9)
            assertEquals(1, payload.getJSONArray("inventory_logs").length())
            assertEquals(1, payload.getJSONArray("financial_logs").length())
            assertEquals("Owner", payload.getJSONObject("actor_link").getString("actor_name_snapshot"))

            // Cross-device references travel by uid, never local ids (D3).
            val correctionEnvelope = build.envelopes.first {
                it.getJSONObject("payload").optLong("event_id") == correction
            }
            assertEquals(event.eventUid, correctionEnvelope.getJSONObject("payload").getString("corrects_event_uid"))
            val deliveryUid = database.backupDataDao().deliveries().single { it.deliveryId == deliveryId }.uid
            val deliveryEnvelope = build.envelopes.first { it.getString("uid") == deliveryUid }
            assertEquals(event.eventUid, deliveryEnvelope.getJSONObject("payload").getString("sale_event_uid"))

            val accountUid = database.backupDataDao().accounts().single { it.accountId == accountId }.uid
            val accountEnvelope = build.envelopes.first { it.getString("uid") == accountUid }
            assertEquals("0300", accountEnvelope.getJSONObject("payload").getJSONObject("profile").getString("phone"))

            val settingsEnvelope = build.envelopes.first { it.getString("uid") == "setting:milk_rate_vdr" }
            assertEquals("milk_rate_vdr", settingsEnvelope.getJSONObject("payload").getString("setting_key"))

            // A queued row whose record vanished is reported, not invented.
            val orphan = SyncEnvelopeFactory(database)
                .build(listOf(SyncOutboxEntity(tableName = "events", rowUid = "u1_missing", enqueuedAt = "x")))
            assertEquals(1, orphan.skippedRows.size)
            assertTrue(orphan.envelopes.isEmpty())
        } finally {
            database.close()
        }
    }

    @Test
    fun serverClient_classifiesOutcomes_andAnchorsTime() = runBlocking {
        val database = MilkShopDatabase.createInMemory()
        val store = SyncTokenStore(context)
        val http = FakeSyncHttp()
        val client = SyncServerClient(http, config, database, store) {
            "249b9c78-475d-483d-b252-314dc5784ff9"
        }
        try {
            val event = EventEntity(
                createdAt = "2026-09-28T11:00:00Z",
                occurredAt = "2026-09-28",
                eventType = "SALE",
                saleQuantity = 1.0,
                saleValue = 190.0,
                unitRate = 190.0,
            )
            database.eventDao().insert(event)
            val rows = database.syncOutboxDao().nextBatch(1000).filter { it.tableName == "events" }
            val envelopes = SyncEnvelopeFactory(database).build(rows).envelopes

            // No session yet: transient, nothing sent.
            assertEquals(SyncPushResult.Transient("no_session"), client.push(envelopes, rows))
            assertEquals(0, http.requests.size)

            store.save(SyncTokenStore.Session("jwt-token-1", null, null))

            http.handler = { SyncHttpResponse(200, """{"ok":true,"server_time_epoch_ms":1800000000000}""".toByteArray()) }
            val success = client.push(envelopes, rows)
            assertTrue(success.toString(), success is SyncPushResult.Success)
            assertTrue(Math.abs((TrustedTime.trustedNowEpochMs() ?: 0) - 1800000000000) < 10_000)
            val pushRequest = http.requests.single()
            assertTrue(pushRequest.url.endsWith("/rest/v1/rpc/sync_push"))
            assertEquals("Bearer jwt-token-1", pushRequest.headers["Authorization"])
            val body = JSONObject(String(pushRequest.body!!))
            assertTrue(body.has("device_id"))
            assertEquals("249b9c78-475d-483d-b252-314dc5784ff9", body.getString("shop_id"))
            assertTrue(body.getJSONArray("envelopes").length() >= 1)

            // The RPC's real refusal shape: HTTP 200 with ok:false and a
            // stable reason code (e.g. a cashier pushing a correction).
            http.handler = {
                SyncHttpResponse(
                    200,
                    """{"ok":false,"error":{"code":"correction_privilege_required","message":"Correcting recorded history requires an owner or admin account"}}""".toByteArray(),
                )
            }
            val refused = client.push(envelopes, rows)
            assertTrue(refused.toString(), refused is SyncPushResult.Permanent)
            assertEquals("correction_privilege_required", (refused as SyncPushResult.Permanent).reasonCode)

            http.handler = { SyncHttpResponse(403, """{"error":{"code":"policy_rejected"}}""".toByteArray()) }
            val permanent = client.push(envelopes, rows)
            assertTrue(permanent is SyncPushResult.Permanent)
            assertEquals("policy_rejected", (permanent as SyncPushResult.Permanent).reasonCode)

            http.handler = { SyncHttpResponse(401, null) }
            assertTrue(client.push(envelopes, rows) is SyncPushResult.Transient)

            http.handler = { SyncHttpResponse(503, null) }
            assertTrue(client.push(envelopes, rows) is SyncPushResult.Transient)

            http.handler = { throw IOException("offline") }
            assertTrue(client.push(envelopes, rows) is SyncPushResult.Transient)
        } finally {
            database.close()
        }
    }
}
