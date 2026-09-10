package sa.com.dreams.quran.domain.model

/**
 * Domain models used by the UI and business logic. They are independent of any API
 * or database so the data source can be replaced without touching the UI.
 *
 * Quran text fields ([Ayah.text], [AyahText.text] for translations/tafsir) always hold
 * the exact string delivered by the verified source; nothing in the app rewrites them.
 */

object QuranConstants {
    const val SURAH_COUNT = 114
    const val AYAH_COUNT = 6236
    const val JUZ_COUNT = 30
    const val HIZB_COUNT = 60
    const val HIZB_QUARTER_COUNT = 240
    const val PAGE_COUNT = 604
    const val MANZIL_COUNT = 7
}

enum class RevelationType { MECCAN, MEDINAN, UNKNOWN }

data class Surah(
    val number: Int,
    /** Arabic name exactly as provided by the source, e.g. "سُورَةُ ٱلْفَاتِحَةِ". */
    val nameArabic: String,
    /** Latin transliteration, e.g. "Al-Faatiha". */
    val nameTransliterated: String,
    /** Meaning of the name, e.g. "The Opening". */
    val nameTranslated: String,
    val ayahCount: Int,
    val revelationType: RevelationType,
)

/** A sajdah (prostration) position. */
data class Sajda(val id: Int, val obligatory: Boolean)

data class Ayah(
    /** Global ayah number, 1..6236. Stable key used across all editions and audio. */
    val id: Int,
    val surah: Int,
    val numberInSurah: Int,
    val text: String,
    val juz: Int,
    /** Rub' al-hizb, 1..240. */
    val hizbQuarter: Int,
    val page: Int,
    val manzil: Int,
    val ruku: Int,
    val sajda: Sajda?,
) {
    /** Hizb, 1..60 (each hizb has four quarters). */
    val hizb: Int get() = hizbOf(hizbQuarter)

    val ref: AyahRef get() = AyahRef(surah, numberInSurah)

    companion object {
        fun hizbOf(hizbQuarter: Int): Int = (hizbQuarter - 1) / 4 + 1
    }
}

/** Position of an ayah as a surah/ayah pair (e.g. 2:255). */
data class AyahRef(val surah: Int, val ayah: Int) {
    override fun toString() = "$surah:$ayah"
}

enum class EditionType { QURAN, TRANSLATION, TAFSIR, AUDIO }

/** A translation, tafsir, Quran script or recitation offered by the data source. */
data class Edition(
    val identifier: String,
    /** ISO 639-1 language code, e.g. "en", "ar". */
    val language: String,
    val name: String,
    val englishName: String,
    val type: EditionType,
    val isRightToLeft: Boolean,
)

/** Translation or tafsir text for one ayah in one edition. */
data class AyahText(
    val ayahId: Int,
    val edition: String,
    val text: String,
)
