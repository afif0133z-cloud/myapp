package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val HindSiliguriFontFamily = FontFamily(
    Font(R.font.hind_siliguri, FontWeight.Normal),
    Font(R.font.hind_siliguri, FontWeight.Medium),
    Font(R.font.hind_siliguri, FontWeight.SemiBold),
    Font(R.font.hind_siliguri, FontWeight.Bold)
)

val NotoSansBengaliFontFamily = FontFamily(
    Font(R.font.noto_sans_bengali, FontWeight.Normal),
    Font(R.font.noto_sans_bengali, FontWeight.Bold)
)

val JinwoTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = HindSiliguriFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 42.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = HindSiliguriFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 25.sp,
        lineHeight = 32.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = HindSiliguriFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = HindSiliguriFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.5.sp,
        lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontFamily = HindSiliguriFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = HindSiliguriFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.5.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = HindSiliguriFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 21.sp
    ),
    bodySmall = TextStyle(
        fontFamily = HindSiliguriFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp
    ),
    labelLarge = TextStyle(
        fontFamily = HindSiliguriFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.5.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = HindSiliguriFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.5.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = HindSiliguriFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )
)
