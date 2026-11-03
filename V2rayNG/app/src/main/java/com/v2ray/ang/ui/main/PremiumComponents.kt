package com.v2ray.ang.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.v2ray.ang.R

/** Shared visual treatment; connection state and actions remain owned by each feature. */
@Composable
internal fun PremiumButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) {
    val colors = if (enabled) listOf(Color(0xFFB38A37), Color(0xFFFFE6A0), Color(0xFFD6AE54))
        else listOf(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.surfaceContainerHigh)
    Button(onClick, modifier.heightIn(min = 46.dp).background(Brush.linearGradient(colors), RoundedCornerShape(18.dp)), enabled,
        shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color(0xFF251D0C), disabledContainerColor = Color.Transparent),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp), content = content)
}

@Composable
internal fun GoldWave(modifier: Modifier = Modifier) {
    val gold = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        val wave = Path().apply {
            moveTo(-size.width * .05f, size.height * .18f)
            cubicTo(size.width * .30f, size.height * .95f, size.width * .55f, size.height * .5f, size.width * 1.05f, size.height * .02f)
        }
        drawPath(wave, Brush.horizontalGradient(listOf(gold.copy(alpha = .04f), gold.copy(alpha = .32f), gold.copy(alpha = .05f))), style = Stroke(1.dp.toPx()))
        val glow = Path().apply {
            addPath(wave)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(glow, Brush.verticalGradient(listOf(gold.copy(alpha = .06f), Color.Transparent)))
    }
}

@Composable
internal fun DashboardHeader(onRefresh: (() -> Unit)?, onImport: (() -> Unit)?, more: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        GoldWave(Modifier.matchParentSize())
        Row(Modifier.fillMaxWidth().heightIn(min = 62.dp).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Image(painterResource(R.drawable.ispeed_logo), null, Modifier.size(32.dp))
            Text(stringResource(R.string.top_brand), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold, maxLines = 1)
            if (onRefresh != null) IconButton(onClick = onRefresh, modifier = Modifier.size(40.dp)) {
                Icon(painterResource(R.drawable.ic_refresh_24dp), stringResource(R.string.acc_update_subscriptions), Modifier.size(23.dp), tint = MaterialTheme.colorScheme.primary)
            }
            if (onImport != null) IconButton(onClick = onImport, modifier = Modifier.size(40.dp)) {
                Icon(painterResource(R.drawable.ic_add_24dp), stringResource(R.string.top_add), Modifier.size(23.dp))
            }
            more()
        }
    }
}

@Composable
internal fun DashboardNavigation(tab: DashboardTab, onSelect: (DashboardTab) -> Unit) {
    Surface(Modifier.navigationBarsPadding().padding(start = 12.dp, end = 12.dp, bottom = 8.dp), shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f))) {
        NavigationBar(Modifier.height(70.dp), containerColor = Color.Transparent, tonalElevation = 0.dp, windowInsets = WindowInsets(0, 0, 0, 0)) {
            DashboardTab.entries.forEach { destination ->
                NavigationBarItem(selected = destination == tab, onClick = { onSelect(destination) },
                    icon = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(painterResource(destination.icon), null, Modifier.size(23.dp))
                            Box(Modifier.width(16.dp).height(2.dp).clip(CircleShape).background(if (destination == tab) MaterialTheme.colorScheme.primary else Color.Transparent))
                        }
                    }, label = { Text(stringResource(destination.label), maxLines = 1, style = MaterialTheme.typography.labelSmall) },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent, selectedIconColor = MaterialTheme.colorScheme.primary, selectedTextColor = MaterialTheme.colorScheme.primary))
            }
        }
    }
}
