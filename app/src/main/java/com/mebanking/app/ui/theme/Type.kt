package com.mebanking.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mebanking.app.R

val GoogleSansFlexFontFamily = FontFamily(
    Font(R.font.google_sans_flex_regular, FontWeight.Normal),
    Font(R.font.google_sans_flex_medium, FontWeight.Medium),
    Font(R.font.google_sans_flex_semibold, FontWeight.SemiBold),
    Font(R.font.google_sans_flex_bold, FontWeight.Bold),
    Font(R.font.google_sans_flex_extrabold, FontWeight.ExtraBold),
)

private val DefaultTypography = Typography()

val Typography = Typography(
    displayLarge = DefaultTypography.displayLarge.copy(fontFamily = GoogleSansFlexFontFamily),
    displayMedium = DefaultTypography.displayMedium.copy(fontFamily = GoogleSansFlexFontFamily),
    displaySmall = DefaultTypography.displaySmall.copy(fontFamily = GoogleSansFlexFontFamily),
    headlineLarge = DefaultTypography.headlineLarge.copy(fontFamily = GoogleSansFlexFontFamily),
    headlineMedium = DefaultTypography.headlineMedium.copy(
        fontFamily = GoogleSansFlexFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    headlineSmall = DefaultTypography.headlineSmall.copy(fontFamily = GoogleSansFlexFontFamily),
    titleLarge = DefaultTypography.titleLarge.copy(
        fontFamily = GoogleSansFlexFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = DefaultTypography.titleMedium.copy(fontFamily = GoogleSansFlexFontFamily),
    titleSmall = DefaultTypography.titleSmall.copy(fontFamily = GoogleSansFlexFontFamily),
    bodyLarge = DefaultTypography.bodyLarge.copy(
        fontFamily = GoogleSansFlexFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = DefaultTypography.bodyMedium.copy(fontFamily = GoogleSansFlexFontFamily),
    bodySmall = DefaultTypography.bodySmall.copy(fontFamily = GoogleSansFlexFontFamily),
    labelLarge = DefaultTypography.labelLarge.copy(fontFamily = GoogleSansFlexFontFamily),
    labelMedium = DefaultTypography.labelMedium.copy(fontFamily = GoogleSansFlexFontFamily),
    labelSmall = DefaultTypography.labelSmall.copy(
        fontFamily = GoogleSansFlexFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
)
