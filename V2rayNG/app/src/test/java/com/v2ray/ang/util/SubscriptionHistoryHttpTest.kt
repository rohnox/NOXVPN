package com.v2ray.ang.util

import android.util.Log
import com.v2ray.ang.dto.UrlContentRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito
import java.net.ServerSocket
import java.net.InetAddress
import kotlin.concurrent.thread

class SubscriptionHistoryHttpTest {
    private lateinit var server: ServerSocket
    private lateinit var worker: Thread
    private lateinit var log: org.mockito.MockedStatic<Log>
    private lateinit var priority: java.lang.reflect.Field
    private var previousPriority = 0
    @Volatile private var respond: (Map<String, String>) -> ByteArray = { response("404 Not Found", "application/json", byteArrayOf()) }
    private val url get() = "http://127.0.0.1:${server.localPort}/history"
    @BeforeEach fun setup() {
        log = Mockito.mockStatic(Log::class.java)
        priority = LogUtil::class.java.getDeclaredField("cachedMinPriority").apply { isAccessible = true }
        previousPriority = priority.getInt(null)
        priority.setInt(null, Log.ERROR)
        server = ServerSocket(0, 16, InetAddress.getByName("127.0.0.1"))
        worker = thread(isDaemon = true, name = "subscription-test-http") {
            while (!server.isClosed) {
                val socket = try { server.accept() } catch (_: java.io.IOException) { break }
                try {
                    socket.use {
                        it.soTimeout = 2000
                        val input = it.getInputStream().bufferedReader(Charsets.US_ASCII)
                        input.readLine()
                        val headers = mutableMapOf<String, String>()
                        while (true) {
                            val line = input.readLine() ?: break
                            if (line.isEmpty()) break
                            val parts = line.split(':', limit = 2)
                            if (parts.size == 2) headers[parts[0].lowercase()] = parts[1].trim()
                        }
                        it.getOutputStream().write(respond(headers))
                    }
                } catch (_: java.io.IOException) { }
            }
        }
    }
    @AfterEach fun cleanup() { server.close(); worker.join(3000); priority.setInt(null, previousPriority); log.close() }
    private fun response(status: String, type: String, body: ByteArray, extra: String = "", chunked: Boolean = false): ByteArray {
        val length = if (chunked) "Transfer-Encoding: chunked" else "Content-Length: ${body.size}"
        val head = "HTTP/1.1 $status\r\nContent-Type: $type\r\n$length\r\nConnection: close\r\n$extra\r\n".toByteArray(Charsets.US_ASCII)
        return if (chunked) head + "${body.size.toString(16)}\r\n".toByteArray() + body + "\r\n0\r\n\r\n".toByteArray() else head + body
    }
    @Test fun fetchesJsonWithConfiguredHeaders() {
        respond = { headers ->
            val valid = headers["user-agent"] == "fixture-agent" && headers["x-test"] == "fixture" && headers["accept"] == "application/json"
            response("200 OK", "application/json", (if (valid) "{\"ok\":true}" else "{\"ok\":false}").toByteArray())
        }
        assertEquals("{\"ok\":true}", HttpUtil.getBoundedSubscriptionJson(UrlContentRequest(url, userAgent = "fixture-agent", requestHeaders = """{"X-Test":"fixture"}""")))
    }
    @Test fun redirectsAreNotFollowed() {
        val requests = java.util.concurrent.atomic.AtomicInteger()
        respond = { requests.incrementAndGet(); response("302 Found", "application/json", byteArrayOf(), "Location: /other\r\n") }
        assertNull(HttpUtil.getBoundedSubscriptionJson(UrlContentRequest(url)))
        assertEquals(1, requests.get())
    }
    @Test fun htmlIsNotTreatedAsProviderData() {
        respond = { response("200 OK", "text/html", "<html>login</html>".toByteArray()) }
        assertNull(HttpUtil.getBoundedSubscriptionJson(UrlContentRequest(url)))
    }
    @Test fun unknownLengthBodyIsStillLimited() {
        respond = { response("200 OK", "application/json", ByteArray(1_048_577) { 32 }, chunked = true) }
        assertNull(HttpUtil.getBoundedSubscriptionJson(UrlContentRequest(url)))
    }
}
