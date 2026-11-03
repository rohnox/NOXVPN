package com.v2ray.ang.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.v2ray.ang.R
import com.v2ray.ang.dto.SessionTraffic
import com.v2ray.ang.extension.toTrafficString
@Composable
fun MainBottomBar(
    displayText: String,
    isRunning: Boolean,

    traffic: SessionTraffic?,
    selectedServerName: String,
    onAction: (MainAction) -> Unit
) {
    Surface(tonalElevation = 3.dp) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                stringResource(if (isRunning) R.string.connection_connected else R.string.connection_not_connected),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            val server = if (selectedServerName.isBlank()) stringResource(R.string.ispeed_no_server) else selectedServerName
            Text(stringResource(R.string.ispeed_selected_server, server), maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.ispeed_duration, traffic?.elapsedSeconds?.let { formatSessionDuration(it) } ?: stringResource(R.string.ispeed_unavailable)),
                    style = MaterialTheme.typography.bodySmall)
                Text(stringResource(R.string.ispeed_data,
                    traffic?.takeIf { it.available }?.uploaded?.toTrafficString() ?: stringResource(R.string.ispeed_unavailable),
                    traffic?.takeIf { it.available }?.downloaded?.toTrafficString() ?: stringResource(R.string.ispeed_unavailable)),
                    style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = { onAction(MainAction.ToggleService) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
                colors = ButtonDefaults.buttonColors(containerColor =
                    if (isRunning) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary)
            ) {
                Text(stringResource(if (isRunning) R.string.acc_stop else R.string.acc_start),
                    style = MaterialTheme.typography.titleLarge)
            }
            TextButton(onClick = { onAction(MainAction.TestCurrentServer) }, modifier = Modifier.fillMaxWidth()) {
                Text(displayText, maxLines = 2, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

internal fun formatSessionDuration(seconds: Long): String {
    val safe = seconds.coerceAtLeast(0)
    return String.format(java.util.Locale.ROOT, "%02d:%02d:%02d", safe / 3600, safe / 60 % 60, safe % 60)
}
