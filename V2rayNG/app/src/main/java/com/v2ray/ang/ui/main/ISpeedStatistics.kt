package com.v2ray.ang.ui.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.v2ray.ang.R
import com.v2ray.ang.extension.toTrafficString
import kotlin.math.max

@Composable
internal fun ISpeedStatistics(state: MainUiState, modifier: Modifier = Modifier) {
    val history = state.trafficHistory
    val rates = trafficRates(history)
    val upload = state.sessionTraffic?.takeIf { it.available }?.uploaded
    val download = state.sessionTraffic?.takeIf { it.available }?.downloaded
    val duration = rememberSessionSeconds(state.sessionTraffic, state.isRunning)
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val latestRate = rates.lastOrNull()
    val uploadRate = latestRate?.up?.let { formatHistoryTraffic(it.toLong(), locale) + "/s" } ?: stringResource(R.string.ispeed_unavailable)
    val downloadRate = latestRate?.down?.let { formatHistoryTraffic(it.toLong(), locale) + "/s" } ?: stringResource(R.string.ispeed_unavailable)

    Column(modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(stringResource(R.string.top_traffic), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.top_session), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatisticMetric(stringResource(R.string.top_download), download?.toTrafficString() ?: stringResource(R.string.ispeed_unavailable), Modifier.weight(1f))
            StatisticMetric(stringResource(R.string.top_upload), upload?.toTrafficString() ?: stringResource(R.string.ispeed_unavailable), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatisticMetric(stringResource(R.string.top_upload) + " · " + stringResource(R.string.nox_live_rate), uploadRate, Modifier.weight(1f))
            StatisticMetric(stringResource(R.string.top_download) + " · " + stringResource(R.string.nox_live_rate), downloadRate, Modifier.weight(1f))
        }
        StatisticMetric(stringResource(R.string.top_session), duration?.let { formatSessionDuration(it) } ?: stringResource(R.string.ispeed_unavailable), Modifier.fillMaxWidth())
        DashboardPanel {
            Text(stringResource(R.string.top_traffic), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(if (rates.isEmpty()) R.string.top_remote_history_empty else R.string.nox_live_rates),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (rates.size < 2) {
                Text(stringResource(R.string.ispeed_unavailable), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val maxRate = rates.maxOf { max(it.up, it.down) }.coerceAtLeast(1.0)
                val upColor = MaterialTheme.colorScheme.tertiary
                val downColor = MaterialTheme.colorScheme.primary
                val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .35f)
                Canvas(Modifier.fillMaxWidth().height(180.dp)) {
                    val upPath = Path()
                    val downPath = Path()
                    rates.forEachIndexed { index, point ->
                        val x = index.toFloat() / (rates.lastIndex).coerceAtLeast(1) * size.width
                        val upY = size.height - (point.up / maxRate).toFloat() * size.height
                        val downY = size.height - (point.down / maxRate).toFloat() * size.height
                        if (index == 0) { upPath.moveTo(x, upY); downPath.moveTo(x, downY) }
                        else { upPath.lineTo(x, upY); downPath.lineTo(x, downY) }
                    }
                    repeat(4) { index ->
                        val y = size.height * index / 3f
                        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                    }
                    drawPath(upPath, upColor, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
                    drawPath(downPath, downColor, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Text(stringResource(R.string.top_upload), color = upColor, style = MaterialTheme.typography.labelMedium)
                    Text(stringResource(R.string.top_download), color = downColor, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        DashboardPanel {
            Text(stringResource(R.string.top_session), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(if (state.isRunning) stringResource(R.string.top_online) else stringResource(R.string.top_offline),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(state.selectedServerName.ifBlank { stringResource(R.string.ispeed_no_server) },
                style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(if (history.isEmpty()) stringResource(R.string.top_remote_history_empty) else stringResource(R.string.nox_samples, history.size),
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatisticMetric(label: String, value: String, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}
