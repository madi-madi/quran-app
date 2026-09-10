package sa.com.dreams.quran.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Calm green + muted gold on a warm paper background. The dark palette uses a deep
// green-grey instead of pure black so long reading sessions stay comfortable.

internal val LightColors = lightColorScheme(
    primary = Color(0xFF1F5E4F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD4E8DF),
    onPrimaryContainer = Color(0xFF0A2A22),
    secondary = Color(0xFF8A6A33),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF3E6CC),
    onSecondaryContainer = Color(0xFF2E2210),
    background = Color(0xFFFBF8F1),
    onBackground = Color(0xFF1C1B18),
    surface = Color(0xFFFBF8F1),
    onSurface = Color(0xFF1C1B18),
    surfaceVariant = Color(0xFFEFEADF),
    onSurfaceVariant = Color(0xFF4A463D),
    outline = Color(0xFF7B766B),
    outlineVariant = Color(0xFFD8D2C4),
    surfaceBright = Color(0xFFFBF8F1),
    surfaceDim = Color(0xFFE2DDD2),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F3EA),
    surfaceContainer = Color(0xFFF3EEE3),
    surfaceContainerHigh = Color(0xFFEEE8DC),
    surfaceContainerHighest = Color(0xFFE8E2D5),
)

internal val DarkColors = darkColorScheme(
    primary = Color(0xFF8FD0B9),
    onPrimary = Color(0xFF003828),
    primaryContainer = Color(0xFF1F5E4F),
    onPrimaryContainer = Color(0xFFD4E8DF),
    secondary = Color(0xFFD9B26F),
    onSecondary = Color(0xFF3F2E0B),
    secondaryContainer = Color(0xFF5A4520),
    onSecondaryContainer = Color(0xFFF3E6CC),
    background = Color(0xFF141A17),
    onBackground = Color(0xFFE6E1D6),
    surface = Color(0xFF141A17),
    onSurface = Color(0xFFE6E1D6),
    surfaceVariant = Color(0xFF26302B),
    onSurfaceVariant = Color(0xFFC3C8C1),
    outline = Color(0xFF8D928C),
    outlineVariant = Color(0xFF3A443F),
    surfaceBright = Color(0xFF333C37),
    surfaceDim = Color(0xFF141A17),
    surfaceContainerLowest = Color(0xFF0F1412),
    surfaceContainerLow = Color(0xFF1A211E),
    surfaceContainer = Color(0xFF1E2622),
    surfaceContainerHigh = Color(0xFF252E2A),
    surfaceContainerHighest = Color(0xFF2F3934),
)

/** Colours used only by the Quran text surfaces (reader, search results, share preview). */
@Immutable
data class QuranColors(
    val text: Color,
    val ayahMarker: Color,
    val highlight: Color,
    val divider: Color,
)

internal val LightQuranColors = QuranColors(
    text = Color(0xFF1A1A17),
    ayahMarker = Color(0xFF8A6A33),
    highlight = Color(0x40D9B26F),
    divider = Color(0xFFD8D2C4),
)

internal val DarkQuranColors = QuranColors(
    text = Color(0xFFEDE6D6),
    ayahMarker = Color(0xFFD9B26F),
    highlight = Color(0x40D9B26F),
    divider = Color(0xFF3A443F),
)
