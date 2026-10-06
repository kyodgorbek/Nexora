package com.yodgorbek.nexora.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Nexora Curated Color Palette
val NexoraTeal = Color(0xFF00E5FF)
val NexoraCyan = Color(0xFF00B0FF)
val NexoraDarkBackground = Color(0xFF0D1117)
val NexoraDarkSurface = Color(0xFF161B22)
val NexoraDarkSurfaceVariant = Color(0xFF21262D)
val NexoraCardBorder = Color(0xFF30363D)

val NexoraGreen = Color(0xFF00E676)
val NexoraAmber = Color(0xFFFFB300)
val NexoraRed = Color(0xFFFF5252)

val DarkColorScheme = darkColorScheme(
    primary = NexoraTeal,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF004D5A),
    onPrimaryContainer = NexoraTeal,
    secondary = NexoraCyan,
    onSecondary = Color.Black,
    background = NexoraDarkBackground,
    surface = NexoraDarkSurface,
    surfaceVariant = NexoraDarkSurfaceVariant,
    onBackground = Color(0xFFF0F6FC),
    onSurface = Color(0xFFF0F6FC),
    onSurfaceVariant = Color(0xFF8B949E),
    outline = NexoraCardBorder,
    error = NexoraRed
)

val LightColorScheme = lightColorScheme(
    primary = Color(0xFF00838F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F7FA),
    onPrimaryContainer = Color(0xFF004D40),
    secondary = Color(0xFF0277BD),
    onSecondary = Color.White,
    background = Color(0xFFF6F8FA),
    surface = Color.White,
    surfaceVariant = Color(0xFFF0F2F5),
    onBackground = Color(0xFF1F2328),
    onSurface = Color(0xFF1F2328),
    onSurfaceVariant = Color(0xFF656D76),
    outline = Color(0xFFD0D7DE),
    error = Color(0xFFD32F2F)
)

val NexoraShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

val NexoraTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )
)

@Composable
fun NexoraTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = NexoraShapes,
        typography = NexoraTypography,
        content = content
    )
}
