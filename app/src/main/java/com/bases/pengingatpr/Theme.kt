package com.bases.pengingatpr

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

/** Warna aksen: nama dan hue (derajat). Indeks terakhir = warna sistem (Android 12+). */
val ACCENT_NAMES = listOf("Ungu", "Biru", "Hijau", "Jingga", "Merah muda", "Toska")
val ACCENT_HUES = listOf(265f, 215f, 140f, 28f, 335f, 180f)

fun hsl(h: Float, s: Float, l: Float): Color {
    val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
    val hp = ((h % 360f) + 360f) % 360f / 60f
    val x = c * (1f - kotlin.math.abs(hp % 2f - 1f))
    val (r1, g1, b1) = when {
        hp < 1f -> Triple(c, x, 0f)
        hp < 2f -> Triple(x, c, 0f)
        hp < 3f -> Triple(0f, c, x)
        hp < 4f -> Triple(0f, x, c)
        hp < 5f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    val m = l - c / 2f
    return Color(r1 + m, g1 + m, b1 + m)
}

fun schemeFor(hue: Float, dark: Boolean): ColorScheme {
    val h2 = hue + 35f
    val h3 = hue + 85f
    return if (!dark) {
        lightColorScheme(
            primary = hsl(hue, 0.55f, 0.40f),
            onPrimary = Color.White,
            primaryContainer = hsl(hue, 0.85f, 0.88f),
            onPrimaryContainer = hsl(hue, 0.70f, 0.13f),
            secondary = hsl(h2, 0.30f, 0.40f),
            onSecondary = Color.White,
            secondaryContainer = hsl(h2, 0.55f, 0.88f),
            onSecondaryContainer = hsl(h2, 0.45f, 0.13f),
            tertiary = hsl(h3, 0.40f, 0.40f),
            onTertiary = Color.White,
            tertiaryContainer = hsl(h3, 0.75f, 0.88f),
            onTertiaryContainer = hsl(h3, 0.55f, 0.13f),
            background = hsl(hue, 0.40f, 0.98f),
            onBackground = hsl(hue, 0.15f, 0.11f),
            surface = hsl(hue, 0.40f, 0.98f),
            onSurface = hsl(hue, 0.15f, 0.11f),
            surfaceVariant = hsl(hue, 0.30f, 0.90f),
            onSurfaceVariant = hsl(hue, 0.15f, 0.30f),
            outline = hsl(hue, 0.10f, 0.47f),
            outlineVariant = hsl(hue, 0.20f, 0.80f),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = hsl(hue, 0.45f, 0.96f),
            surfaceContainer = hsl(hue, 0.40f, 0.94f),
            surfaceContainerHigh = hsl(hue, 0.35f, 0.92f),
            surfaceContainerHighest = hsl(hue, 0.30f, 0.90f),
            error = Color(0xFFBA1A1A),
            onError = Color.White,
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002),
        )
    } else {
        darkColorScheme(
            primary = hsl(hue, 0.90f, 0.80f),
            onPrimary = hsl(hue, 0.70f, 0.18f),
            primaryContainer = hsl(hue, 0.50f, 0.28f),
            onPrimaryContainer = hsl(hue, 0.85f, 0.90f),
            secondary = hsl(h2, 0.45f, 0.80f),
            onSecondary = hsl(h2, 0.40f, 0.18f),
            secondaryContainer = hsl(h2, 0.30f, 0.28f),
            onSecondaryContainer = hsl(h2, 0.55f, 0.90f),
            tertiary = hsl(h3, 0.55f, 0.80f),
            onTertiary = hsl(h3, 0.45f, 0.18f),
            tertiaryContainer = hsl(h3, 0.35f, 0.28f),
            onTertiaryContainer = hsl(h3, 0.70f, 0.90f),
            background = hsl(hue, 0.15f, 0.08f),
            onBackground = hsl(hue, 0.15f, 0.90f),
            surface = hsl(hue, 0.15f, 0.08f),
            onSurface = hsl(hue, 0.15f, 0.90f),
            surfaceVariant = hsl(hue, 0.12f, 0.27f),
            onSurfaceVariant = hsl(hue, 0.20f, 0.80f),
            outline = hsl(hue, 0.10f, 0.60f),
            outlineVariant = hsl(hue, 0.12f, 0.27f),
            surfaceContainerLowest = hsl(hue, 0.15f, 0.05f),
            surfaceContainerLow = hsl(hue, 0.15f, 0.10f),
            surfaceContainer = hsl(hue, 0.15f, 0.12f),
            surfaceContainerHigh = hsl(hue, 0.14f, 0.16f),
            surfaceContainerHighest = hsl(hue, 0.13f, 0.20f),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6),
        )
    }
}

val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(32.dp),
    extraLarge = RoundedCornerShape(44.dp),
)

private fun st(size: Int, line: Int, weight: FontWeight, spacing: Float = 0f) = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = spacing.sp,
)

/** Tipografi ekspresif: bobot tebal, ukuran judul besar. */
val ExpressiveTypography = Typography(
    displayLarge = st(57, 64, FontWeight.ExtraBold, -0.5f),
    displayMedium = st(45, 52, FontWeight.ExtraBold),
    displaySmall = st(36, 44, FontWeight.ExtraBold),
    headlineLarge = st(34, 40, FontWeight.ExtraBold),
    headlineMedium = st(28, 36, FontWeight.Bold),
    headlineSmall = st(24, 32, FontWeight.Bold),
    titleLarge = st(22, 28, FontWeight.Bold),
    titleMedium = st(17, 24, FontWeight.Bold, 0.1f),
    titleSmall = st(14, 20, FontWeight.Bold, 0.1f),
    bodyLarge = st(16, 24, FontWeight.Normal, 0.3f),
    bodyMedium = st(14, 20, FontWeight.Normal, 0.2f),
    bodySmall = st(12, 16, FontWeight.Normal, 0.3f),
    labelLarge = st(14, 20, FontWeight.Bold, 0.1f),
    labelMedium = st(12, 16, FontWeight.Bold, 0.4f),
    labelSmall = st(11, 16, FontWeight.Bold, 0.5f),
)

@Composable
fun ExpressiveTheme(content: @Composable () -> Unit) {
    val s = Repo.settings
    val dark = when (s.themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val ctx = LocalContext.current
    val scheme = if (s.accent >= ACCENT_HUES.size && Build.VERSION.SDK_INT >= 31) {
        if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
    } else {
        schemeFor(ACCENT_HUES[s.accent.coerceIn(0, ACCENT_HUES.size - 1)], dark)
    }
    MaterialTheme(colorScheme = scheme, shapes = ExpressiveShapes, typography = ExpressiveTypography, content = content)
}

/** Warna mata pelajaran yang konsisten (berdasarkan nama). */
fun subjectHue(name: String): Float = ((name.trim().lowercase().hashCode() and 0x7FFFFFFF) % 360).toFloat()

fun subjectContainer(name: String, dark: Boolean): Color =
    if (dark) hsl(subjectHue(name), 0.45f, 0.30f) else hsl(subjectHue(name), 0.75f, 0.86f)

fun subjectOn(name: String, dark: Boolean): Color =
    if (dark) hsl(subjectHue(name), 0.80f, 0.88f) else hsl(subjectHue(name), 0.70f, 0.20f)
