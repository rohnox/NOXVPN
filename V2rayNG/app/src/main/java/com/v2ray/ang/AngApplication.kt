package com.v2ray.ang

import android.app.Application
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.work.Configuration
import androidx.work.WorkManager
import com.v2ray.ang.AppConfig.ANG_PACKAGE
import com.v2ray.ang.handler.AppLocaleManager
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.SettingsManager
import com.v2ray.ang.ui.compose.ThemeManager

class AngApplication : Application() {
    companion object {
        lateinit var application: AngApplication
    }

    /**
     * Attaches the base context to the application.
     * @param base The base context.
     */
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base?.let(ContextCompat::getContextForLanguage))
        application = this
    }

    private val workManagerConfiguration: Configuration = Configuration.Builder()
        .setDefaultProcessName("${ANG_PACKAGE}:bg")
        .build()

    /**
     * Initializes the application.
     */
    override fun onCreate() {
        super.onCreate()
        de.blinkt.openvpn.core.EmbeddedRuntime.setOwnerRegistration { service, endpoint ->
            com.v2ray.ang.helper.MessageHelper.registerVpnOwner(service, com.v2ray.ang.core.VpnEngine.OpenVpn, endpoint)
        }
        app.openconnect.core.EmbeddedRuntime.setOwnerRegistration { service, endpoint ->
            com.v2ray.ang.helper.MessageHelper.registerVpnOwner(service, com.v2ray.ang.core.VpnEngine.Cisco, endpoint)
        }
        app.openconnect.core.EmbeddedRuntime.setConnectionGate { context, connect ->
            com.v2ray.ang.core.LauncherManager.prepareEmbeddedCiscoConnection(context) {
                connect.run()
            }
        }
        de.blinkt.openvpn.core.EmbeddedRuntime.setConnectionGate { context, connect ->
            com.v2ray.ang.core.LauncherManager.prepareEmbeddedOpenVpnConnection(context) { connect.run() }
        }
        de.blinkt.openvpn.core.EmbeddedRuntime.setFailureListener { operation, profileId, error ->
            com.v2ray.ang.util.LogUtil.e(AppConfig.TAG, "OpenVPN mode=VPN phase=handover operation=$operation profile=$profileId", IllegalStateException(error.javaClass.simpleName))
        }
        app.openconnect.core.EmbeddedRuntime.setFailureListener { operation, error ->
            com.v2ray.ang.util.LogUtil.e(AppConfig.TAG, "Cisco mode=VPN phase=handover operation=$operation", IllegalStateException(error.javaClass.simpleName))
        }

        MmkvManager.initialize(this)

        AppLocaleManager.initialize(this)

        // Initialize WorkManager with the custom configuration
        WorkManager.initialize(this, workManagerConfiguration)

        // Ensure critical preference defaults are present in MMKV early
        SettingsManager.initApp(this)

        // Initialize theme state from MMKV
        ThemeManager.refresh()
    }
}
