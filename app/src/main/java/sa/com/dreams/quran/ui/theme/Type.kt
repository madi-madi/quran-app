package sa.com.dreams.quran.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import sa.com.dreams.quran.R

/** Fonts available for rendering Quran text. Amiri Quran (SIL OFL) is bundled (~135 KB). */
object QuranFonts {
    val AmiriQuran = FontFamily(Font(R.font.amiri_quran))

    /** The device's own Arabic font — no extra memory, useful on very old devices. */
    val System = FontFamily.Default
}
