package com.dopamind.app.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// NOTE: Inter/SF Pro font files are not bundled (no network access to fetch them
// in this build environment). Falls back to the platform default (Roboto on
// Android), which shares Inter's geometry closely enough for the MVP.
// To switch to the real Inter typeface: drop the .ttf files into
// app/src/main/res/font/ and replace this with:
//   FontFamily(Font(R.font.inter_regular, FontWeight.Normal), ...)
val DopaMindFontFamily: FontFamily = FontFamily.Default

val DopaMindTypography = Typography(
    // H1 28px bold
    headlineLarge = TextStyle(
        fontFamily = DopaMindFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
    ),
    // H2 20px semibold
    titleLarge = TextStyle(
        fontFamily = DopaMindFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    // Body 15px regular
    bodyLarge = TextStyle(
        fontFamily = DopaMindFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = DopaMindFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp,
    ),
    // Caption 13px medium
    labelMedium = TextStyle(
        fontFamily = DopaMindFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = DopaMindFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
)
