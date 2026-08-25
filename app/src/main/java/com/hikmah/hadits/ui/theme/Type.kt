package com.hikmah.hadits.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val HikmahTypography = Typography().run {
    copy(
        displaySmall = displaySmall.copy(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp,
        ),
        headlineSmall = headlineSmall.copy(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
        ),
        titleLarge = titleLarge.copy(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
        ),
        bodyLarge = bodyLarge.copy(lineHeight = 26.sp),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}
