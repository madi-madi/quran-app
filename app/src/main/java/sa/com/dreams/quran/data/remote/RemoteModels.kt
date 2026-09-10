package sa.com.dreams.quran.data.remote

import sa.com.dreams.quran.domain.model.EditionType
import sa.com.dreams.quran.domain.model.RevelationType
import sa.com.dreams.quran.domain.model.Sajda

// Source-neutral data schema returned by every QuranRemoteDataSource.
// See docs/DATA_SCHEMA.md for field meanings and validation rules.

data class RemoteSurah(
    val number: Int,
    val nameArabic: String,
    val nameTransliterated: String,
    val nameTranslated: String,
    val ayahCount: Int,
    val revelationType: RevelationType,
)

data class RemoteAyah(
    /** Global ayah number, 1..6236. */
    val globalNumber: Int,
    val surah: Int,
    val numberInSurah: Int,
    /** Exact text of this ayah in the requested edition. */
    val text: String,
    val juz: Int,
    val hizbQuarter: Int,
    val page: Int,
    val manzil: Int,
    val ruku: Int,
    val sajda: Sajda?,
)

/** One surah in one edition (Quran script, translation or tafsir). */
data class RemoteSurahEdition(
    val edition: String,
    val surah: RemoteSurah,
    val ayahs: List<RemoteAyah>,
)

data class RemoteEdition(
    val identifier: String,
    val language: String,
    val name: String,
    val englishName: String,
    val type: EditionType,
    val isRightToLeft: Boolean,
)

data class RemoteAyahAudio(
    val globalNumber: Int,
    val numberInSurah: Int,
    val url: String,
    /** Smaller file for the "data saver" quality setting, when the source has one. */
    val lowBitrateUrl: String?,
)
