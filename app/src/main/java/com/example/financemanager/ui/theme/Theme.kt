package com.example.financemanager.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/** Colours that Material's [ColorScheme] has no slot for. */
@Immutable
data class FinanceColors(
    val income: Color,
    val incomeContainer: Color,
    val expense: Color,
    val expenseContainer: Color,
    /** Close to a limit, e.g. a budget that is nearly used up. */
    val warning: Color,
    val warningContainer: Color,
    val gradient: List<Color>,
    /** One colour per category slot, see [com.example.financemanager.data.Category.colorIndex]. */
    val categories: List<Color>,
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF5B5BF6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6E5FF),
    onPrimaryContainer = Color(0xFF1E1A6E),
    secondary = Color(0xFF5B5BF6),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6E5FF),
    onSecondaryContainer = Color(0xFF1E1A6E),
    tertiary = Color(0xFFC026D3),
    error = Color(0xFFE5484D),
    background = Color(0xFFF4F5FA),
    onBackground = Color(0xFF12131A),
    surface = Color(0xFFF4F5FA),
    onSurface = Color(0xFF12131A),
    surfaceVariant = Color(0xFFEEF0F6),
    onSurfaceVariant = Color(0xFF6B7080),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFEEF0F6),
    surfaceContainerHighest = Color(0xFFE6E8F0),
    outline = Color(0xFFDADDE7),
    outlineVariant = Color(0xFFE9EBF2),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFA5A4FF),
    onPrimary = Color(0xFF1A1766),
    primaryContainer = Color(0xFF2E2C80),
    onPrimaryContainer = Color(0xFFE6E5FF),
    secondary = Color(0xFFA5A4FF),
    onSecondary = Color(0xFF1A1766),
    secondaryContainer = Color(0xFF2E2C80),
    onSecondaryContainer = Color(0xFFE6E5FF),
    tertiary = Color(0xFFA21CAF),
    error = Color(0xFFFF6B6F),
    background = Color(0xFF0C0D14),
    onBackground = Color(0xFFECEDF3),
    surface = Color(0xFF0C0D14),
    onSurface = Color(0xFFECEDF3),
    surfaceVariant = Color(0xFF1F2230),
    onSurfaceVariant = Color(0xFF9EA3B5),
    surfaceContainerLowest = Color(0xFF161823),
    surfaceContainerLow = Color(0xFF161823),
    surfaceContainer = Color(0xFF161823),
    surfaceContainerHigh = Color(0xFF1F2230),
    surfaceContainerHighest = Color(0xFF262A38),
    outline = Color(0xFF343849),
    outlineVariant = Color(0xFF262A38),
)

private val LightFinanceColors = FinanceColors(
    income = Color(0xFF12A150),
    incomeContainer = Color(0xFFDDF6E7),
    expense = Color(0xFFE5484D),
    expenseContainer = Color(0xFFFDE4E4),
    warning = Color(0xFFB86E00),
    warningContainer = Color(0xFFFFF0D6),
    gradient = listOf(Color(0xFF4F46E5), Color(0xFF7C3AED), Color(0xFFC026D3)),
    categories = listOf(
        Color(0xFF2A78D6), Color(0xFFEB6834), Color(0xFF1BAF7A), Color(0xFFEDA100),
        Color(0xFFE87BA4), Color(0xFF008300), Color(0xFF4A3AA7), Color(0xFFE34948),
    ),
)

private val DarkFinanceColors = FinanceColors(
    income = Color(0xFF3DD68C),
    incomeContainer = Color(0xFF12321F),
    expense = Color(0xFFFF6B6F),
    expenseContainer = Color(0xFF3A1618),
    warning = Color(0xFFF5B547),
    warningContainer = Color(0xFF3A2A0E),
    gradient = listOf(Color(0xFF4338CA), Color(0xFF6D28D9), Color(0xFFA21CAF)),
    categories = listOf(
        Color(0xFF3987E5), Color(0xFFD95926), Color(0xFF199E70), Color(0xFFC98500),
        Color(0xFFD55181), Color(0xFF008300), Color(0xFF9085E9), Color(0xFFE66767),
    ),
)

private val LocalFinanceColors = staticCompositionLocalOf { LightFinanceColors }

private val BaseTypography = Typography()

private val AppTypography = BaseTypography.copy(
    displaySmall = BaseTypography.displaySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.02).em),
    headlineSmall = BaseTypography.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.01).em),
    titleLarge = BaseTypography.titleLarge.copy(fontWeight = FontWeight.Medium, fontSize = 20.sp),
    titleMedium = BaseTypography.titleMedium.copy(fontWeight = FontWeight.Medium),
    labelLarge = BaseTypography.labelLarge.copy(fontWeight = FontWeight.Medium, fontSize = 16.sp),
    labelMedium = BaseTypography.labelMedium.copy(letterSpacing = 0.04.em),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
)

@Composable
fun FinanceTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalFinanceColors provides if (darkTheme) DarkFinanceColors else LightFinanceColors,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

object FinanceTheme {
    val colors: FinanceColors
        @Composable @ReadOnlyComposable get() = LocalFinanceColors.current
}

