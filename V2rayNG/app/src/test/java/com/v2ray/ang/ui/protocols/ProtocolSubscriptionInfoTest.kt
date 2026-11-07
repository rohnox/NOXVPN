package com.v2ray.ang.ui.protocols

import com.google.gson.Gson
import com.v2ray.ang.ui.main.subscriptionDaysRemaining
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ProtocolSubscriptionInfoTest {
    private fun parse(fields: String) = parseProtocolSubscriptionInfo("""{"user":{"username":"demo",$fields}}""", "demo")
    @Test fun bytesAndFractionalQuotaUseProviderUnitsAndClampRemaining() {
        val info = parse(""""upload_bytes":1048576,"download_bytes":2097152,"data_limit":2.5,"data_limit_unit":"GB","expiry_date_iso":"2030-01-02"""")
        assertEquals(3145728L, info.usedBytes)
        assertEquals(2684354560L, info.limitBytes)
        assertEquals(2681208832L, info.remainingBytes)
        assertEquals(2, subscriptionDaysRemaining(info.expireSeconds, java.time.Instant.parse("2030-01-01T00:00:00Z").epochSecond - 1))
        assertEquals(0L, parse(""""upload_bytes":2097152,"download_bytes":1048576,"data_limit":2,"data_limit_unit":"MB"""").remainingBytes)
        assertFalse(info.toString().contains("demo"))
    }
    @Test fun explicitUnlimitedDiffersFromMissingOrInvalidFields() {
        val unlimited = parse(""""upload_bytes":0,"download_bytes":0,"data_limit":null,"expiry_date_iso":null""")
        assertTrue(unlimited.unlimitedTraffic); assertTrue(unlimited.unlimitedTime); assertEquals(0L, unlimited.usedBytes)
        val missing = parse(""""is_active":true""")
        assertNull(missing.usedBytes); assertNull(missing.limitBytes); assertFalse(missing.unlimitedTraffic); assertFalse(missing.unlimitedTime)
        val bad = parse(""""upload_bytes":-1,"download_bytes":5,"data_limit":10,"data_limit_unit":"unknown","expiry_date_iso":"not-a-date"""")
        assertNull(bad.usedBytes); assertNull(bad.limitBytes); assertNull(bad.expireSeconds); assertFalse(bad.unlimitedTime)
    }
    @Test fun rejectsOverflowAndWrongAccountInsteadOfShowingFabricatedQuota() {
        assertNull(parse(""""upload_bytes":9223372036854775807,"download_bytes":1,"data_limit":99999999999999999999,"data_limit_unit":"GB"""").usedBytes)
        assertNull(parse(""""data_limit":99999999999999999999,"data_limit_unit":"GB"""").limitBytes)
        assertThrows(IllegalArgumentException::class.java) { parseProtocolSubscriptionInfo("""{"user":{"username":"other"}}""", "demo") }
        assertThrows(Exception::class.java) { parseProtocolSubscriptionInfo("{}", "demo") }
    }
    @Test fun waitingActivationDoesNotPretendToHaveStartedOrToBeUnlimited() {
        val pending = parse(""""activation_type":"flexible_days","first_connection_at":null,"pending_activation_days":30,"expiry_date_iso":null""")
        assertEquals(30, pending.pendingDays); assertFalse(pending.unlimitedTime); assertNull(pending.expireSeconds)
        val active = parse(""""activation_type":"flexible_days","first_connection_at":"2030-01-01","pending_activation_days":30,"expiry_date_iso":"2030-02-01T00:00:00Z"""")
        assertNull(active.pendingDays); assertEquals(0, subscriptionDaysRemaining(active.expireSeconds, active.expireSeconds!!))
    }
    @Test fun readsPreviousReleasedCatalogAndRoundTripsNewAccountMetadata() {
        val gson = Gson()
        val legacy = gson.fromJson("""{"id":"s","title":"Old","url":"https://example.invalid/sub/t/demo","updatedAt":10,"profiles":[]}""", ProtocolSubscription::class.java)
        assertNull(legacy.info); assertEquals("Old", legacy.title)
        val info = parse(""""upload_bytes":2,"download_bytes":3,"data_limit":0,"expiry_date_iso":null""")
        assertEquals(legacy.copy(info = info), gson.fromJson(gson.toJson(legacy.copy(info = info)), ProtocolSubscription::class.java))
    }
    @Test fun privateProviderAccountFixtureParsesWithoutPublishingItsValues() {
        val path = System.getenv("SPECIAL_ACCOUNT_FIXTURE")
        org.junit.jupiter.api.Assumptions.assumeTrue(!path.isNullOrBlank())
        val json = java.io.File(path!!).readText()
        val username = com.google.gson.JsonParser.parseString(json).asJsonObject.getAsJsonObject("user").get("username").asString
        val info = parseProtocolSubscriptionInfo(json, username)
        assertEquals(username, info.username); assertNotNull(info.usedBytes)
        assertTrue(info.limitBytes != null || info.unlimitedTraffic)
        assertTrue(info.expireSeconds != null || info.unlimitedTime || info.pendingDays != null)
    }
}
