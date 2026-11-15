package org.openmausbot.android.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.openmausbot.android.data.Repo

private val Dark = darkColorScheme(
    primary = Color(0xFFF1F2F4),
    onPrimary = Color(0xFF111215),
    primaryContainer = Color(0xFF292C32),
    onPrimaryContainer = Color(0xFFF3F4F6),
    secondary = Color(0xFFB4BAC4),
    onSecondary = Color(0xFF15171B),
    secondaryContainer = Color(0xFF252930),
    onSecondaryContainer = Color(0xFFE7EAF0),
    background = Color(0xFF090A0C),
    onBackground = Color(0xFFF4F5F7),
    surface = Color(0xFF111317),
    onSurface = Color(0xFFF0F1F3),
    surfaceVariant = Color(0xFF1C2026),
    onSurfaceVariant = Color(0xFFA8AFBA),
    outline = Color(0xFF343942),
    error = Color(0xFFFF8B8B),
    onError = Color(0xFF310A0A),
)

private val Light = lightColorScheme(
    primary = Color(0xFF17191D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5E7EB),
    onPrimaryContainer = Color(0xFF17191D),
    secondary = Color(0xFF525B67),
    onSecondary = Color.White,
    background = Color(0xFFF7F8FA),
    onBackground = Color(0xFF14171B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF14171B),
    surfaceVariant = Color(0xFFEDF0F4),
    onSurfaceVariant = Color(0xFF4C5663),
    outline = Color(0xFFD4D9E1),
    error = Color(0xFFB3261E),
)

fun botColor(name: String, dark: Boolean): Color = when (name) {
    "blue" -> if (dark) Color(0xFF8FB8FF) else Color(0xFF2F5DA8)
    "red" -> if (dark) Color(0xFFFF9B94) else Color(0xFFB3261E)
    "orange" -> if (dark) Color(0xFFFFB877) else Color(0xFFB25E00)
    "purple" -> if (dark) Color(0xFFC8A6FF) else Color(0xFF6A3FB5)
    "cyan" -> if (dark) Color(0xFF7FE3EA) else Color(0xFF00707A)
    "pink" -> if (dark) Color(0xFFFFA6D2) else Color(0xFFA8266B)
    "yellow" -> if (dark) Color(0xFFF2DE7B) else Color(0xFF7A6300)
    "teal" -> if (dark) Color(0xFF7BDCC3) else Color(0xFF0A6E5C)
    "coral" -> if (dark) Color(0xFFFFA894) else Color(0xFFB3422B)
    "silver" -> if (dark) Color(0xFFCBD3DC) else Color(0xFF5A6673)
    "chrome" -> if (dark) Color(0xFFDDE6F2) else Color(0xFF3C4A5A)
    else -> if (dark) Color(0xFFF1F2F4) else Color(0xFF17191D)
}

private val AppTypography = Typography(
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 21.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.5.sp, lineHeight = 22.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 11.5.sp),
)

@Composable
fun MausTheme(content: @Composable () -> Unit) {
    // Grok-inspired dark UI is the first-run default; the user can still
    // switch to light mode or follow the system in Settings.
    val saved = Repo.store.darkTheme
    val dark = saved ?: true
    val scheme = remember(dark) { if (dark) Dark else Light }
    MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
}

@Composable
fun isDark(): Boolean {
    val saved = Repo.store.darkTheme
    return saved ?: true
}
