package com.mebanking.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.mebanking.app.R

/**
 * Google Sans Flex variable font configured with the 4 core Material 3 Expressive axes:
 * `opsz` (optical size), `wght` (weight), `wdth` (width), and `ROND` (roundness).
 */
@OptIn(ExperimentalTextApi::class)
fun googleSansFlexFontFamily(
    opsz: TextUnit = 16.sp,
    wdth: Float = 100f,
    rond: Float = 0f,
): FontFamily {
    val clampedOpsz = opsz.value.coerceIn(6f, 144f).sp
    val clampedWdth = wdth.coerceIn(25f, 151f)
    val clampedRond = rond.coerceIn(0f, 100f)

    fun variableFont(weight: FontWeight): Font = Font(
        resId = R.font.google_sans_flex_variable,
        weight = weight,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(weight.weight),
            FontVariation.width(clampedWdth),
            FontVariation.opticalSizing(clampedOpsz),
            FontVariation.Setting("ROND", clampedRond),
        ),
    )

    return FontFamily(
        variableFont(FontWeight.Light),
        variableFont(FontWeight.Normal),
        variableFont(FontWeight.Medium),
        variableFont(FontWeight.SemiBold),
        variableFont(FontWeight.Bold),
        variableFont(FontWeight.ExtraBold),
        variableFont(FontWeight.Black),
    )
}

/**
 * Full-roundness (`ROND = 100`) expressive Google Sans Flex family for monetary inputs,
 * account balances, checkout totals, and prominent numeric figures.
 */
val RoundedNumericFontFamily: FontFamily = googleSansFlexFontFamily(
    opsz = 30.sp,
    wdth = 104f,
    rond = 100f,
)

private val DisplayLargeFont = googleSansFlexFontFamily(opsz = 57.sp, wdth = 102f, rond = 50f)
private val DisplayMediumFont = googleSansFlexFontFamily(opsz = 45.sp, wdth = 102f, rond = 50f)
private val DisplaySmallFont = googleSansFlexFontFamily(opsz = 36.sp, wdth = 100f, rond = 45f)
private val HeadlineLargeFont = googleSansFlexFontFamily(opsz = 32.sp, wdth = 100f, rond = 35f)
private val HeadlineMediumFont = googleSansFlexFontFamily(opsz = 28.sp, wdth = 100f, rond = 35f)
private val HeadlineSmallFont = googleSansFlexFontFamily(opsz = 24.sp, wdth = 100f, rond = 35f)
private val TitleLargeFont = googleSansFlexFontFamily(opsz = 22.sp, wdth = 100f, rond = 20f)
private val TitleMediumFont = googleSansFlexFontFamily(opsz = 16.sp, wdth = 100f, rond = 20f)
private val TitleSmallFont = googleSansFlexFontFamily(opsz = 14.sp, wdth = 100f, rond = 20f)
private val BodyLargeFont = googleSansFlexFontFamily(opsz = 16.sp, wdth = 100f, rond = 0f)
private val BodyMediumFont = googleSansFlexFontFamily(opsz = 14.sp, wdth = 100f, rond = 0f)
private val BodySmallFont = googleSansFlexFontFamily(opsz = 12.sp, wdth = 100f, rond = 0f)
private val LabelLargeFont = googleSansFlexFontFamily(opsz = 14.sp, wdth = 100f, rond = 25f)
private val LabelMediumFont = googleSansFlexFontFamily(opsz = 12.sp, wdth = 100f, rond = 25f)
private val LabelSmallFont = googleSansFlexFontFamily(opsz = 11.sp, wdth = 100f, rond = 25f)

private val DefaultTypography = Typography()

val Typography = Typography(
    displayLarge = DefaultTypography.displayLarge.copy(fontFamily = DisplayLargeFont),
    displayMedium = DefaultTypography.displayMedium.copy(fontFamily = DisplayMediumFont),
    displaySmall = DefaultTypography.displaySmall.copy(fontFamily = DisplaySmallFont),
    headlineLarge = DefaultTypography.headlineLarge.copy(fontFamily = HeadlineLargeFont, fontWeight = FontWeight.SemiBold),
    headlineMedium = DefaultTypography.headlineMedium.copy(
        fontFamily = HeadlineMediumFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    headlineSmall = DefaultTypography.headlineSmall.copy(fontFamily = HeadlineSmallFont, fontWeight = FontWeight.SemiBold),
    titleLarge = DefaultTypography.titleLarge.copy(
        fontFamily = TitleLargeFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = DefaultTypography.titleMedium.copy(fontFamily = TitleMediumFont, fontWeight = FontWeight.SemiBold),
    titleSmall = DefaultTypography.titleSmall.copy(fontFamily = TitleSmallFont, fontWeight = FontWeight.Medium),
    bodyLarge = DefaultTypography.bodyLarge.copy(
        fontFamily = BodyLargeFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.25.sp,
    ),
    bodyMedium = DefaultTypography.bodyMedium.copy(fontFamily = BodyMediumFont),
    bodySmall = DefaultTypography.bodySmall.copy(fontFamily = BodySmallFont),
    labelLarge = DefaultTypography.labelLarge.copy(fontFamily = LabelLargeFont, fontWeight = FontWeight.SemiBold),
    labelMedium = DefaultTypography.labelMedium.copy(fontFamily = LabelMediumFont, fontWeight = FontWeight.Medium),
    labelSmall = DefaultTypography.labelSmall.copy(
        fontFamily = LabelSmallFont,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
)
