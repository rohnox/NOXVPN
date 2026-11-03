package com.v2ray.ang.ui.protocols

import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.v2ray.ang.R
import com.v2ray.ang.ui.base.BaseComponentActivity

/** Retained for notification/back-stack entry points; the main app embeds the same screen. */
class ProtocolsActivity : BaseComponentActivity() {
    private val model: ProtocolsViewModel by viewModels()
    private val host = ProtocolsHost(this) { model }

    @Composable override fun ScreenContent() {
        Scaffold(topBar = {
            Surface {
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp)) {
                    TextButton(onClick = { finish() }) { Text(stringResource(R.string.protocol_back)) }
                    Text(stringResource(R.string.protocol_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(12.dp))
                }
            }
        }) { padding ->
            ProtocolsRoute(model, Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding), host::onAction)
        }
    }
}
