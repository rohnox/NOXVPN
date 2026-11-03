package com.v2ray.ang.ui.compose

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.v2ray.ang.R

/** Bundled OFL font gives Persian and English the same predictable reading metrics. */
private val BrandFonts = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
)

internal val AppTypography = Typography().let { base ->
    Typography(
        displayLarge = base.displayLarge.copy(fontFamily = BrandFonts),
        displayMedium = base.displayMedium.copy(fontFamily = BrandFonts),
        displaySmall = base.displaySmall.copy(fontFamily = BrandFonts),
        headlineLarge = base.headlineLarge.copy(fontFamily = BrandFonts),
        headlineMedium = base.headlineMedium.copy(fontFamily = BrandFonts),
        headlineSmall = base.headlineSmall.copy(fontFamily = BrandFonts),
        titleLarge = base.titleLarge.copy(fontFamily = BrandFonts),
        titleMedium = base.titleMedium.copy(fontFamily = BrandFonts),
        titleSmall = base.titleSmall.copy(fontFamily = BrandFonts),
        bodyLarge = base.bodyLarge.copy(fontFamily = BrandFonts),
        bodyMedium = base.bodyMedium.copy(fontFamily = BrandFonts),
        bodySmall = base.bodySmall.copy(fontFamily = BrandFonts),
        labelLarge = base.labelLarge.copy(fontFamily = BrandFonts),
        labelMedium = base.labelMedium.copy(fontFamily = BrandFonts),
        labelSmall = base.labelSmall.copy(fontFamily = BrandFonts),
    )
}
