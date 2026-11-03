package com.v2ray.ang.ui.compose

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.v2ray.ang.AppConfig
import com.v2ray.ang.handler.MmkvManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private val LightColor = lightColorScheme(
    primary = Color(0xFF8A641D), onPrimary = Color.White,
    primaryContainer = Color(0xFFF5E4BA), onPrimaryContainer = Color(0xFF3B2B0D),
    secondary = Color(0xFF765D2D), onSecondary = Color.White,
    secondaryContainer = Color(0xFFF0E4CD), onSecondaryContainer = Color(0xFF302513),
    tertiary = Color(0xFF765D2D), onTertiary = Color.White,
    background = Color(0xFFF8F5EF), onBackground = Color(0xFF201E19),
    surface = Color.White, onSurface = Color(0xFF201E19),
    surfaceVariant = Color(0xFFECE7DD), onSurfaceVariant = Color(0xFF676054),
    outline = Color(0xFF8A8170), outlineVariant = Color(0xFFD6CCB8),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFF2EEE7), surfaceContainerHigh = Color(0xFFEAE4D9),
    surfaceContainerHighest = Color(0xFFDED5C5), scrim = Color.Black,
)

private val DarkColor = darkColorScheme(
    primary = Color(0xFFE4C274), onPrimary = Color(0xFF29200E),
    primaryContainer = Color(0xFF43351B), onPrimaryContainer = Color(0xFFF9E7BB),
    secondary = Color(0xFFC6AD78), onSecondary = Color(0xFF282216),
    secondaryContainer = Color(0xFF393021), onSecondaryContainer = Color(0xFFEEE0C1),
    tertiary = Color(0xFFC6AD78), onTertiary = Color(0xFF282216),
    background = Color(0xFF090A0B), onBackground = Color(0xFFEEECE7),
    surface = Color(0xFF111214), onSurface = Color(0xFFEEECE7),
    surfaceVariant = Color(0xFF292B2E), onSurfaceVariant = Color(0xFFBEBBB3),
    outline = Color(0xFF8E887B), outlineVariant = Color(0xFF46433C),
    surfaceContainerLowest = Color(0xFF050607), surfaceContainerLow = Color(0xFF17181A),
    surfaceContainer = Color(0xFF1D1F21), surfaceContainerHigh = Color(0xFF27292C),
    surfaceContainerHighest = Color(0xFF33363A), scrim = Color.Black,
)
// Semantic Colors
val colorPing = Color(0xFF009966) // Green
val colorPingRed = Color(0xFFFF0099) // Pink Red
val colorConfigType = Color(0xFF087FBB) // Orange
val colorFabActive = Color(0xFF087FBB) // Orange
val colorFabInactiveLight = Color(0xFF9C9C9C) // Gray
val colorFabInactiveDark = Color(0xFF646464) // Dark Gray
val dividerColorLight = Color(0xFFE0E0E0) // Light Gray
val dividerColorDark = Color(0xFF424242) // Dark Gray

// Toast Colors 85%
val toastNormalBgLight = Color(0xD9353A3E) // Dark Gray
val toastNormalBgDark = Color(0xD94A4F54) // Darker Gray
val toastSuccessBg = Color(0xD9388E3C) // Green
val toastErrorBg = Color(0xD9D50000) // Red
val toastInfoBg = Color(0xD93F51B5) // Indigo Blue
val toastIconCircleBg = Color(0x33FFFFFF) // Semi-transparent White
val toastTextColor = Color.White // White

object ThemeManager {
    private val _themeMode = MutableStateFlow(
        MmkvManager.decodeSettingsString(AppConfig.PREF_UI_MODE_NIGHT, "0") ?: "0"
    )
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _dynamicColorEnabled = MutableStateFlow(
        MmkvManager.decodeSettingsBool(AppConfig.PREF_DYNAMIC_COLOR, false)
    )
    val dynamicColorEnabled: StateFlow<Boolean> = _dynamicColorEnabled.asStateFlow()

    fun setThemeMode(mode: String) {
        MmkvManager.encodeSettings(AppConfig.PREF_UI_MODE_NIGHT, mode)
        _themeMode.value = mode
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        MmkvManager.encodeSettings(AppConfig.PREF_DYNAMIC_COLOR, enabled)
        _dynamicColorEnabled.value = enabled
    }

    fun refresh() {
        _themeMode.value =
            MmkvManager.decodeSettingsString(AppConfig.PREF_UI_MODE_NIGHT, "0") ?: "0"
        _dynamicColorEnabled.value =
            MmkvManager.decodeSettingsBool(AppConfig.PREF_DYNAMIC_COLOR, false)
    }
}

@Composable
fun resolveDarkTheme(): Boolean {
    val mode by ThemeManager.themeMode.collectAsState()
    return when (mode) {
        "1" -> false
        "2" -> true
        // Automatic mode uses metallic black; explicit light mode remains available.
        else -> true
    }
}

val LocalDarkTheme = compositionLocalOf { false }

/** Pure palette access for UI rendering checks, without initializing persistent settings. */
internal fun staticAppColorScheme(darkTheme: Boolean) = if (darkTheme) DarkColor else LightColor

@Composable
fun AppTheme(
    darkTheme: Boolean = resolveDarkTheme(),
    content: @Composable () -> Unit
) {
    val dynamicColor by ThemeManager.dynamicColorEnabled.collectAsState()
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        else -> staticAppColorScheme(darkTheme)
    }
    val snackbarController = rememberAppSnackbarController()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val window = activity.window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalDarkTheme provides darkTheme,
        LocalAppSnackbar provides snackbarController
    ) {
        MaterialTheme(
            colorScheme = colorScheme, typography = AppTypography
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AppSnackbarBridge(controller = snackbarController)
                content()
                AppSnackbarHost(hostState = snackbarController.hostState)
            }
        }
    }
}
