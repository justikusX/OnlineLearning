package com.example.onlinelearning.view.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.onlinelearning.R
import com.example.onlinelearning.model.repository.ThemeMode

// Palette taken from the Figma "Online Learning App" design.
val Primary = Color(0xFF3D5CFF)
val PrimaryLight = Color(0xFFEAEEFF)
val Ink = Color(0xFF1F1F39)
val Grey = Color(0xFF858597)
val GreyLight = Color(0xFFB8B8D2)
val Surface = Color(0xFFF4F3FD)
val Orange = Color(0xFFFF6905)
val PinkLight = Color(0xFFFFEBF0)
val DarkBackground = Color(0xFF1F1F39)
val DarkSurface = Color(0xFF2F2F42)

private val LightColors = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimaryLight,
    onPrimaryContainer = Primary,
    secondary = Orange,
    background = Color.White,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Surface,
    onSurfaceVariant = Grey,
    outline = GreyLight,
)

private val DarkColors = darkColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3E3E55),
    onPrimaryContainer = Color.White,
    secondary = Orange,
    background = DarkBackground,
    onBackground = Color.White,
    surface = DarkBackground,
    onSurface = Color.White,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = GreyLight,
    outline = Color(0xFF3E3E55),
)

// Poppins, as in the Figma design (OFL, see licenses/Poppins-OFL.txt). It has no Cyrillic glyphs,
// so Russian text falls back to the system font automatically.
val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold),
)

private val Base = Typography()

private val AppTypography = Typography(
    displayLarge = Base.displayLarge.copy(fontFamily = Poppins),
    displayMedium = Base.displayMedium.copy(fontFamily = Poppins),
    displaySmall = Base.displaySmall.copy(fontFamily = Poppins),
    headlineLarge = Base.headlineLarge.copy(fontFamily = Poppins),
    headlineMedium = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineSmall = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = Base.titleSmall.copy(fontFamily = Poppins),
    bodyLarge = Base.bodyLarge.copy(fontFamily = Poppins),
    bodyMedium = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Medium, fontSize = 16.sp),
    labelMedium = Base.labelMedium.copy(fontFamily = Poppins),
    labelSmall = Base.labelSmall.copy(fontFamily = Poppins),
)

@Composable
fun OnlineLearningTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}

/** Whether [mode] resolves to the dark color scheme right now. */
@Composable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}
