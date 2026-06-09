package com.masar.portal.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val MasarColors = darkColorScheme(
    primary       = BrandRed,
    onPrimary     = TxtPrimary,
    primaryContainer = BrandRedDark,
    onPrimaryContainer = TxtPrimary,
    secondary     = BrandRedSoft,
    onSecondary   = TxtPrimary,
    background    = Ink,
    onBackground  = TxtPrimary,
    surface       = Ink2,
    onSurface     = TxtPrimary,
    surfaceVariant= Ink3,
    onSurfaceVariant = TxtSoft,
    outline       = LineDim,
    error         = Red,
    onError       = TxtPrimary,
)

@Composable
fun MasarTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = MasarColors,
            typography = MasarTypography,
            content = content,
        )
    }
}
