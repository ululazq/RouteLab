package io.routelab.app.presentation.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BgDark = Color(0xFF0F172A)
val SurfaceDark = Color(0xFF1E293B)
val SurfaceElevated = Color(0xFF334155)
val AccentBlue = Color(0xFF2563EB)
val AccentBlueBright = Color(0xFF3B82F6)
val AccentCyan = Color(0xFF06B6D4)

val ColorFlat = Color(0xFF10B981)
val ColorRolling = Color(0xFFF59E0B)
val ColorClimb = Color(0xFFEF4444)
val ColorExtreme = Color(0xFF8B5CF6)

val TextPrimaryDark = Color(0xFFF8FAFC)
val TextSecondaryDark = Color(0xFF94A3B8)
val TextMutedDark = Color(0xFF64748B)

private val DarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = Color.White,
    primaryContainer = SurfaceElevated,
    onPrimaryContainer = TextPrimaryDark,
    secondary = AccentCyan,
    onSecondary = Color.Black,
    background = BgDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondaryDark,
    outline = Color(0xFF334155),
    error = ColorClimb
)

@Composable
fun RouteLabTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
