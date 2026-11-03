package com.v2ray.ang.ui.protocols

import de.blinkt.openvpn.core.StatusLogReader
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.IOException
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class StatusLogReaderTest {
    @Test fun `service open failure is reported off the caller without packet delivery`() {
        StatusLogReader().use { reader ->
            val done = CountDownLatch(1)
            val error = AtomicReference<Exception>()
            val callbackThread = AtomicReference<Thread>()
            reader.start({ throw IOException("Service unavailable") }, {
                throw AssertionError("No pipe was opened")
            }, {
                error.set(it); callbackThread.set(Thread.currentThread()); done.countDown()
            })
            assertTrue(done.await(2, TimeUnit.SECONDS))
            assertTrue(error.get() is IOException)
            assertNotSame(Thread.currentThread(), callbackThread.get())
        }
    }
    private fun frames(vararg packets: ByteArray): ByteArray = ByteArrayOutputStream().also { bytes ->
        DataOutputStream(bytes).use { out ->
            packets.forEach { out.writeShort(it.size); out.write(it) }
            out.writeShort(0x7fff)
        }
    }.toByteArray()

    @Test fun `waiting for service does not block caller and reconnect cancels old wait`() {
        StatusLogReader().use { reader ->
            val entered = CountDownLatch(1)
            val delivered = CountDownLatch(1)
            val worker = AtomicReference<Thread>()
            val error = AtomicReference<Exception>()
            assertTimeoutPreemptively(Duration.ofSeconds(2)) {
                reader.start({
                    worker.set(Thread.currentThread()); entered.countDown()
                    CountDownLatch(1).await()
                    ByteArrayInputStream(frames())
                }, { throw AssertionError("Cancelled pipe must not deliver") }, error::set)
            }
            assertTrue(entered.await(2, TimeUnit.SECONDS))
            assertNotSame(Thread.currentThread(), worker.get())
            reader.start({ ByteArrayInputStream(frames(byteArrayOf(7))) }, {
                assertArrayEquals(byteArrayOf(7), it); delivered.countDown()
            }, error::set)
            assertTrue(delivered.await(2, TimeUnit.SECONDS))
            assertNull(error.get())
        }
    }

    @Test fun `large unsigned frames and terminator are read and stream is closed`() {
        StatusLogReader().use { reader ->
            val large = ByteArray(40000) { (it % 127).toByte() }
            val closed = CountDownLatch(1)
            val result = AtomicReference<ByteArray>()
            val error = AtomicReference<Exception>()
            val source = object : ByteArrayInputStream(frames(large)) {
                override fun close() { super.close(); closed.countDown() }
            }
            reader.start({ source }, result::set, error::set)
            assertTrue(closed.await(2, TimeUnit.SECONDS))
            assertArrayEquals(large, result.get()); assertNull(error.get())
        }
    }

    @Test fun `truncated and empty frames fail instead of delivering corrupt log packets`() {
        for (bytes in listOf(byteArrayOf(0, 4, 1), byteArrayOf(0, 0), byteArrayOf())) {
            StatusLogReader().use { reader ->
                val failed = CountDownLatch(1)
                val error = AtomicReference<Exception>()
                reader.start({ ByteArrayInputStream(bytes) }, { throw AssertionError("Corrupt packet delivered") }, {
                    error.set(it); failed.countDown()
                })
                assertTrue(failed.await(2, TimeUnit.SECONDS))
                assertTrue(error.get() is IOException)
                if (bytes.isEmpty()) assertTrue(error.get() is EOFException)
            }
        }
    }
}
