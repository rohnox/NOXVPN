package com.v2ray.ang.util

import java.net.InetSocketAddress
import java.net.Proxy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ExitIpTest {
    @Test fun countryMustMatchTheQueriedAddressAndKnownIsoCode() {
        assertEquals("NL", HttpUtil.parseExitCountryResponse("""{"ip":"203.0.113.7","country":"NL"}""", "203.0.113.7"))
        assertEquals("DE", HttpUtil.parseExitCountryResponse("""{"ip":"2001:db8:0:0:0:0:0:1","country":"DE"}""", "2001:db8::1"))
        listOf("{}", "[]", "broken", """{"ip":"203.0.113.8","country":"NL"}""",
            """{"ip":"203.0.113.7","country":"ZZ"}""", """{"ip":"example.com","country":"NL"}""", "x".repeat(257))
            .forEach { assertNull(HttpUtil.parseExitCountryResponse(it, "203.0.113.7")) }
    }
    @Test fun acceptsOnlyLiteralAddresses() {
        assertEquals("203.0.113.7", HttpUtil.parseExitIpResponse("""{"ip":"203.0.113.7"}"""))
        assertEquals("2001:db8::1", HttpUtil.parseExitIpResponse("""{"ip":"2001:db8::1"}"""))
        listOf("{}", "[]", "broken", """{"ip":1}""", """{"ip":"example.com"}""",
            """{"ip":"[::1]"}""", """{"ip":"fe80::1%eth0"}""", "x".repeat(257))
            .forEach { assertNull(HttpUtil.parseExitIpResponse(it)) }
    }

    @Test fun proxyIsExplicitAndCannotFallBackToDirectOrRedirects() {
        val client = HttpUtil.exitIpClient(10809, null, null)
        assertEquals(Proxy.Type.HTTP, client.proxy!!.type())
        assertEquals(10809, (client.proxy!!.address() as InetSocketAddress).port)
        assertTrue((client.proxy!!.address() as InetSocketAddress).address.isLoopbackAddress)
        assertFalse(client.followRedirects)
        assertFalse(client.followSslRedirects)
        assertFalse(client.retryOnConnectionFailure)
        assertEquals(6000, client.callTimeoutMillis)
        assertThrows(IllegalArgumentException::class.java) { HttpUtil.exitIpClient(0, null, null) }
    }

    @Test fun missingDaemonPortNeverCreatesARequest() = runTest {
        assertNull(HttpUtil.getExitIpViaProxy(0, null, null))
        assertNull(HttpUtil.getExitIpViaProxy(65536, null, null))
    }
}
