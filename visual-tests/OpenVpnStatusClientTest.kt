package com.v2ray.ang.ui.visual

import android.app.Application
import android.content.*
import android.os.IBinder
import android.os.ParcelFileDescriptor
import com.v2ray.ang.ui.protocols.OpenVpnStatusClient
import de.blinkt.openvpn.core.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class OpenVpnStatusClientTest {
    @Test fun directDaemonSnapshotAndSubsequentDisconnectReachHostWithoutUiCache() {
        val app = mock<Context>()
        whenever(app.applicationContext).thenReturn(app)
        whenever(app.packageName).thenReturn("com.specialvpn.app")
        var link: ServiceConnection? = null
        doAnswer { link = it.getArgument(1); true }.whenever(app).bindService(any<Intent>(), any<ServiceConnection>(), eq(Context.BIND_AUTO_CREATE))
        val remote = mock<IServiceStatus>()
        val binder = mock<IBinder>()
        whenever(binder.queryLocalInterface(any())).thenReturn(remote)
        var callback: IStatusCallbacks? = null
        val initial = CountDownLatch(1)
        val states = java.util.Collections.synchronizedList(mutableListOf<ConnectionStatus?>())
        whenever(remote.registerStatusCallback(any())).thenAnswer {
            callback = it.getArgument(0)
            callback!!.updateStateString("CONNECTED", "", 0, ConnectionStatus.LEVEL_CONNECTED, null)
            val pipe = ParcelFileDescriptor.createPipe()
            ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]).use { stream -> stream.write(byteArrayOf(0x7f, 0xff.toByte())) }
            initial.countDown()
            pipe[0]
        }
        val client = OpenVpnStatusClient(app, { states.add(it) }, { _, _ -> })
        client.start()
        link!!.onServiceConnected(ComponentName("test", "status"), binder)
        assertTrue(initial.await(3, TimeUnit.SECONDS))
        assertEquals(ConnectionStatus.LEVEL_CONNECTED, states.last())
        callback!!.updateStateString("DISCONNECTED", "", 0, ConnectionStatus.LEVEL_NOTCONNECTED, null)
        assertEquals(ConnectionStatus.LEVEL_NOTCONNECTED, states.last())
        client.close()
        val before = states.size
        callback!!.updateStateString("CONNECTED", "", 0, ConnectionStatus.LEVEL_CONNECTED, null)
        assertEquals(before, states.size)
        verify(app).unbindService(any())
    }
}
