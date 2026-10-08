package uz.apk.antivirus.security.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val CyberDarkBackground = Color(0xFF080D14)
val CyberCardSurface = Color(0xFF0F172A)
val CyberNeonGreen = Color(0xFF00FF88)
val CyberNeonCyan = Color(0xFF00E5FF)
val CyberWarningYellow = Color(0xFFFFD600)
val CyberWarningOrange = Color(0xFFFF9100)
val CyberDangerRed = Color(0xFFFF1744)

val CyberTextPrimary = Color(0xFFE2E8F0)
val CyberTextSecondary = Color(0xFF94A3B8)
val CyberTextMuted = Color(0xFF64748B)

private val DarkColorScheme = darkColorScheme(
    primary = CyberNeonGreen,
    secondary = CyberNeonCyan,
    background = CyberDarkBackground,
    surface = CyberCardSurface,
    error = CyberDangerRed,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = CyberTextPrimary,
    onSurface = CyberTextPrimary
)

@Composable
fun CyberTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography(),
        content = content
    )
}
