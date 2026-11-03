package com.v2ray.ang.ui.protocols

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.ui.base.BaseComponentActivity
import com.v2ray.ang.util.LogUtil
import de.blinkt.openvpn.core.IOpenVPNServiceInternal
import de.blinkt.openvpn.core.OpenVPNService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun openVpnChallengeMessage(raw: String?): String? {
    if (raw == null || raw.length > 4096) return null
    val parts = raw.split(':', limit = 2)
    if (parts.size != 2 || "R" !in parts[0].split(',')) return null
    return parts[1]
}
internal data class ChallengeState(val available: Boolean = false, val busy: Boolean = false,
    val submitted: Boolean = false, val failed: Boolean = false)

class OpenVpnChallengeViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow(ChallengeState())
    internal val state = _state.asStateFlow()
    private var service: IOpenVPNServiceInternal? = null
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = IOpenVPNServiceInternal.Stub.asInterface(binder)
            _state.update { it.copy(available = service != null) }
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            _state.update { it.copy(available = false) }
        }
    }
    // Bind only to the already-running engine. A delayed challenge must not create a VPN service.
    private val bound = app.bindService(Intent(app, OpenVPNService::class.java).setAction(OpenVPNService.START_SERVICE), connection, 0)
    internal fun respond(value: String) {
        if (_state.value.busy || value.isEmpty() || value.length > 1024) return
        val target = service ?: return
        _state.update { it.copy(busy = true, failed = false) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { target.challengeResponse(value) }
                _state.update { it.copy(submitted = true) }
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "OpenVPN challenge IPC failed", IllegalStateException(e.javaClass.simpleName).apply { stackTrace = e.stackTrace })
                _state.update { it.copy(failed = true) }
            } finally { _state.update { it.copy(busy = false) } }
        }
    }
    override fun onCleared() {
        if (bound) getApplication<Application>().unbindService(connection)
        super.onCleared()
    }
}

class OpenVpnChallengeActivity : BaseComponentActivity() {
    private val model: OpenVpnChallengeViewModel by viewModels()
    @Composable override fun ScreenContent() {
        val state by model.state.collectAsStateWithLifecycle()
        // Responses are ephemeral secrets and deliberately do not enter saved instance state.
        var response by remember { mutableStateOf("") }
        val message = openVpnChallengeMessage(intent.getStringExtra(OpenVPNService.EXTRA_CHALLENGE_TXT))
        LaunchedEffect(state.submitted) { if (state.submitted) finish() }
        Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).imePadding().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.protocol_challenge_title), style = MaterialTheme.typography.titleLarge)
            Text(message ?: stringResource(R.string.protocol_unavailable))
            OutlinedTextField(value = response, onValueChange = { response = it.take(1024) },
                label = { Text(stringResource(R.string.protocol_challenge_response)) }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
            if (state.failed) Text(stringResource(R.string.protocol_unavailable))
            Button(onClick = { model.respond(response) }, enabled = message != null && state.available && !state.busy && response.isNotEmpty()) {
                Text(stringResource(R.string.protocol_challenge_send))
            }
            TextButton(onClick = { finish() }) { Text(stringResource(R.string.protocol_back)) }
        }
    }
}
