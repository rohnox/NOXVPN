package com.v2ray.ang.core

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.extension.isComplexType
import com.v2ray.ang.extension.toast
import com.v2ray.ang.extension.toastError
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.SettingsManager
import com.v2ray.ang.helper.MessageHelper
import com.v2ray.ang.root.RootManager
import com.v2ray.ang.service.CoreProxyOnlyService
import com.v2ray.ang.service.CoreRootService
import com.v2ray.ang.service.CoreVpnService
import com.v2ray.ang.util.LogUtil
import com.v2ray.ang.util.Utils

object LauncherManager {

    /** Embedded protocol entry points; the upstream activities retain VPN consent and service ownership. */
    fun launchEmbeddedOpenVpn(context: Context, guid: String, onFailure: (Throwable) -> Unit = {}) {
        // The engine invokes the host handover gate only after consent and password prompts succeed.
        if (context is android.app.Activity && (context.isFinishing || context.isDestroyed)) return
        try {
            context.startActivity(Intent(context, de.blinkt.openvpn.LaunchVPN::class.java)
                .setAction(Intent.ACTION_MAIN)
                .putExtra(de.blinkt.openvpn.LaunchVPN.EXTRA_KEY, guid)
                .putExtra(de.blinkt.openvpn.LaunchVPN.EXTRA_HIDELOG, true))
        } catch (error: Exception) {
            LogUtil.e(AppConfig.TAG, "OpenVPN mode=VPN operation=dispatch profile=$guid", IllegalStateException(error.javaClass.simpleName))
            onFailure(error)
        }
    }

    fun disconnectEmbeddedOpenVpn(context: Context) {
        context.startActivity(Intent(context, de.blinkt.openvpn.activities.DisconnectVPN::class.java))
    }

    fun launchEmbeddedCisco(context: Context) {
        // Opening profiles is navigation; handover happens only after an explicit Connect.
        context.startActivity(Intent(context, app.openconnect.MainActivity::class.java))
    }

    fun launchCiscoProfile(context: Context, guid: String) {
        context.startActivity(Intent(context, app.openconnect.api.GrantPermissionsActivity::class.java)
            .putExtra(context.packageName + app.openconnect.api.GrantPermissionsActivity.EXTRA_UUID, guid)
            .putExtra(context.packageName + app.openconnect.api.GrantPermissionsActivity.EXTRA_START_ACTIVITY, app.openconnect.MainActivity::class.java.name))
    }

    /** Called only after Android VPN consent. Backend status, not this dispatch, reports connection. */
    fun launchWireGuard(context: Context, guid: String, onFailure: (Throwable) -> Unit = {}) {
        MessageHelper.prepareVpnHandover(context, VpnEngine.WireGuard) { stopped ->
            if (context is android.app.Activity && (context.isFinishing || context.isDestroyed)) return@prepareVpnHandover
            if (!stopped) { context.toast(R.string.protocol_handover_failed); return@prepareVpnHandover }
            try {
                androidx.core.content.ContextCompat.startForegroundService(context,
                    Intent(context, com.v2ray.ang.service.WireGuardControllerService::class.java)
                        .putExtra(com.v2ray.ang.service.WireGuardControllerService.PROFILE, guid))
            } catch (error: Exception) {
                LogUtil.e(AppConfig.TAG, "WireGuard mode=VPN operation=dispatch profile=$guid", IllegalStateException(error.javaClass.simpleName))
                onFailure(error)
            }
        }
    }

    fun stopWireGuard(context: Context) {
        MessageHelper.stopVpnHandover(context, VpnEngine.WireGuard)
    }

    /** Invoked only by Cisco's confirmed Connect path, never profile browsing. */
    fun prepareEmbeddedCiscoConnection(context: Context, onReady: () -> Unit) {
        prepareEmbeddedConnection(context, VpnEngine.Cisco, onReady)
    }

    fun prepareEmbeddedOpenVpnConnection(context: Context, onReady: () -> Unit) {
        prepareEmbeddedConnection(context, VpnEngine.OpenVpn, onReady)
    }

    private fun prepareEmbeddedConnection(context: Context, target: VpnEngine, onReady: () -> Unit) {
        if (context is android.app.Activity && (context.isFinishing || context.isDestroyed)) return
        MessageHelper.prepareVpnHandover(context, target) { stopped ->
            if (context is android.app.Activity && (context.isFinishing || context.isDestroyed)) return@prepareVpnHandover
            if (!stopped) {
                context.toast(R.string.protocol_handover_failed)
                (context as? android.app.Activity)?.finish()
                return@prepareVpnHandover
            }
            onReady()
        }
    }

    fun startServiceFromToggle(context: Context): Boolean {
        if (MmkvManager.getSelectServer().isNullOrEmpty()) {
            context.toast(R.string.app_tile_first_use)
            return false
        }
        try {
            startContextService(context)
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "LauncherManager: ${e.message}", e)
            context.toast(e.message ?: e.javaClass.simpleName)
            return false
        }
        return true
    }

    fun startService(context: Context, guid: String? = null) {
        LogUtil.i(AppConfig.TAG, "LauncherManager: startService from ${context::class.java.simpleName}")

        if (guid != null) {
            MmkvManager.setSelectServer(guid)
        }

        try {
            startContextService(context)
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "LauncherManager: ${e.message}", e)
            context.toast(e.message ?: e.javaClass.simpleName)
        }
    }

    fun stopService(context: Context) {
        if (SettingsManager.isVpnMode() && !SettingsManager.isRootMode()) MessageHelper.stopVpnHandover(context, VpnEngine.Xray)
        else MessageHelper.sendMsg2Service(context, AppConfig.MSG_STATE_STOP, "")
    }

    /** Restarts the active daemon without starting a stopped service. */
    fun restartService(context: Context) {
        MessageHelper.sendMsg2Service(context, AppConfig.MSG_STATE_RESTART, "")
    }

    /** Restarts the active daemon, or delegates to the caller's permission-aware start flow. */
    fun restartServiceOrStart(context: Context, startIfStopped: () -> Unit) {
        MessageHelper.sendMsg2ServiceForResult(context, AppConfig.MSG_STATE_RESTART, "") { handled ->
            if (!handled) startIfStopped()
        }
    }

    @Throws(Exception::class)
    private fun startContextService(context: Context) {
        // Note: isRunning check is removed here to avoid loading Native libraries in the UI process.
        // The check is performed in CoreServiceManager when the service starts in the daemon process.

        val guid = MmkvManager.getSelectServer()
            ?: run {
                LogUtil.e(AppConfig.TAG, "LauncherManager: No server selected")
                error(context.getString(R.string.app_tile_first_use))
            }

        val config = MmkvManager.decodeServerConfig(guid)
            ?: run {
                LogUtil.e(AppConfig.TAG, "LauncherManager: Failed to decode server config")
                error(context.getString(R.string.toast_config_file_invalid))
            }

        if (!config.configType.isComplexType()
            && !Utils.isValidUrl(config.server)
            && !Utils.isPureIpAddress(config.server.orEmpty())
        ) {
            LogUtil.e(AppConfig.TAG, "LauncherManager: Invalid server configuration")
            error(context.getString(R.string.toast_config_file_invalid))
        }

        SettingsManager.refreshRuntimeSocksPort()

        if (config.insecure == true && config.pinnedCA256.isNullOrEmpty()) {
            context.toastError(R.string.toast_allow_insecure_deprecated)
            Utils.setClipboard(context, context.getString(R.string.toast_allow_insecure_deprecated))
        }

        if (MmkvManager.decodeSettingsBool(AppConfig.PREF_PROXY_SHARING)) {
            context.toast(R.string.toast_warning_pref_proxysharing_short)
        } else {
            context.toast(R.string.toast_services_start)
        }

        val isRootMode = SettingsManager.isRootMode()
        if (isRootMode && !RootManager.isRootAvailable()) {
            LogUtil.e(AppConfig.TAG, "LauncherManager: root mode requires root but none available")
            error(context.getString(R.string.toast_root_required))
        }

        val intent = if (isRootMode) {
            LogUtil.i(AppConfig.TAG, "LauncherManager: Starting Root service")
            Intent(context.applicationContext, CoreRootService::class.java)
        } else if (SettingsManager.isVpnMode()) {
            LogUtil.i(AppConfig.TAG, "LauncherManager: Starting VPN service")
            Intent(context.applicationContext, CoreVpnService::class.java)
        } else {
            LogUtil.i(AppConfig.TAG, "LauncherManager: Starting Proxy service")
            Intent(context.applicationContext, CoreProxyOnlyService::class.java)
        }

        if (!isRootMode && SettingsManager.isVpnMode()) {
            MessageHelper.prepareVpnHandover(context, VpnEngine.Xray) { stopped ->
                if (context is android.app.Activity && (context.isFinishing || context.isDestroyed)) return@prepareVpnHandover
                // An asynchronous handover must never start a deleted or subsequently selected server.
                if (!stopped) { context.toast(R.string.protocol_handover_failed); return@prepareVpnHandover }
                if (MmkvManager.getSelectServer() != guid || MmkvManager.decodeServerConfig(guid) == null) return@prepareVpnHandover
                try { dispatchService(context, intent) }
                catch (error: Exception) {
                    LogUtil.e(AppConfig.TAG, "Xray mode=VPN phase=handover operation=start profile=$guid", IllegalStateException(error.javaClass.simpleName))
                    context.toast(R.string.protocol_unavailable)
                }
            }
        } else dispatchService(context, intent)
    }

    private fun dispatchService(context: Context, intent: Intent) {
        try {
            ContextCompat.startForegroundService(context, intent)
        } catch (e: SecurityException) {
            LogUtil.e(AppConfig.TAG, "LauncherManager: Missing permission to start foreground service", e)
            throw IllegalStateException(e.message ?: e.javaClass.simpleName, e)
        } catch (e: RuntimeException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                e.javaClass.name == "android.app.ForegroundServiceStartNotAllowedException"
            ) {
                LogUtil.e(AppConfig.TAG, "LauncherManager: Foreground service start not allowed", e)
                throw IllegalStateException(e.message ?: e.javaClass.simpleName, e)
            }
            throw e
        }
    }
}
