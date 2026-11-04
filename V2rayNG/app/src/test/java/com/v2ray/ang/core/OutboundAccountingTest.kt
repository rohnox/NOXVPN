package com.v2ray.ang.core

import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File

class OutboundAccountingTest {
    @Test fun missingStatsAndPolicyAreAddedWithoutNotificationPreference() {
        val json = JsonParser.parseString("{}").asJsonObject
        CoreConfigManager.enableOutboundAccounting(json)
        assertTrue(json.getAsJsonObject("stats").entrySet().isEmpty())
        assertTrue(json.getAsJsonObject("policy").getAsJsonObject("system").get("statsOutboundUplink").asBoolean)
        assertTrue(json.getAsJsonObject("policy").getAsJsonObject("system").get("statsOutboundDownlink").asBoolean)
    }
    @Test fun customPolicyLevelsRoutingAndSystemOptionsSurvive() {
        val json = JsonParser.parseString("""{"policy":{"levels":{"8":{"handshake":4}},"system":{"statsInboundUplink":true,"statsOutboundDownlink":false}},"routing":{"rules":[]},"stats":{}}""").asJsonObject
        val before = json.getAsJsonObject("routing").deepCopy()
        CoreConfigManager.enableOutboundAccounting(json)
        CoreConfigManager.enableOutboundAccounting(json)
        assertEquals(4, json.getAsJsonObject("policy").getAsJsonObject("levels").getAsJsonObject("8").get("handshake").asInt)
        assertTrue(json.getAsJsonObject("policy").getAsJsonObject("system").get("statsInboundUplink").asBoolean)
        assertTrue(json.getAsJsonObject("policy").getAsJsonObject("system").get("statsOutboundDownlink").asBoolean)
        assertEquals(before, json.getAsJsonObject("routing"))
    }
    @Test fun nullObjectsAreInitializedButMalformedObjectsAreRejected() {
        val json = JsonParser.parseString("""{"stats":null,"policy":{"system":null}}""").asJsonObject
        CoreConfigManager.enableOutboundAccounting(json)
        for (text in listOf("""{"policy":false}""", """{"stats":5}""", """{"policy":{"system":[]}}""")) {
            assertThrows(IllegalArgumentException::class.java) { CoreConfigManager.enableOutboundAccounting(JsonParser.parseString(text).asJsonObject) }
        }
    }
    @Test fun generatedTemplateRetainsOutboundCountersForEveryRunMode() {
        val json = JsonParser.parseString(File("src/main/assets/v2ray_config.json").readText()).asJsonObject
        val system = json.getAsJsonObject("policy").getAsJsonObject("system")
        assertTrue(json.get("stats").isJsonObject)
        assertTrue(system.get("statsOutboundUplink").asBoolean)
        assertTrue(system.get("statsOutboundDownlink").asBoolean)
    }
}
