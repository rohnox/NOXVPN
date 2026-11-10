package com.v2ray.ang.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
internal fun DashboardHeader(
    onRefresh: (() -> Unit)?,
    onLanguageClick: () -> Unit,
    onTelegramClick: () -> Unit
) {
    Box(Modifier.fillMaxWidth()) {
        GoldWave(Modifier.matchParentSize())
        Row(
            Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Image(painterResource(R.drawable.nox_logo_mark), "NOX", Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)))
            Text(
                "NOX",
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            if (onRefresh != null) {
                IconButton(onClick = onRefresh) {
                    Icon(painterResource(R.drawable.ic_refresh_24dp), "Refresh subscription", tint = MaterialTheme.colorScheme.primary)
                }
            }
            IconButton(onClick = onLanguageClick) {
                Icon(painterResource(R.drawable.ic_translate_24dp), "Language settings", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onTelegramClick) {
                Icon(painterResource(R.drawable.ic_telegram_24dp), "NOX Telegram channel", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
internal fun DashboardNavigation(tab: DashboardTab, onSelect: (DashboardTab) -> Unit) {
    Surface(Modifier.navigationBarsPadding().padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .35f))) {
        Column(Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
            NavigationBar(Modifier.height(64.dp), containerColor = Color.Transparent, tonalElevation = 0.dp,
                windowInsets = WindowInsets(0, 0, 0, 0)) {
                DashboardTab.entries.filter { it != DashboardTab.Plans }.forEach { destination ->
                    if (destination == DashboardTab.Protocols) Spacer(Modifier.weight(0.85f))
                    NavigationBarItem(selected = destination == tab, onClick = { onSelect(destination) },
                        icon = { Icon(painterResource(destination.icon), null, Modifier.size(21.dp)) },
                        label = { Text(stringResource(destination.label), maxLines = 1, style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = .14f),
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary))
                }
            }
        }
    }
}
