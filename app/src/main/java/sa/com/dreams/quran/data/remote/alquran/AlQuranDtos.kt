package sa.com.dreams.quran.data.remote.alquran

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

// Wire format of https://api.alquran.cloud/v1 — used only inside this package.

@Serializable
internal data class EnvelopeDto<T>(
    val code: Int,
    val status: String = "",
    val data: T,
)

@Serializable
internal data class EditionDto(
    val identifier: String,
    val language: String,
    val name: String,
    val englishName: String,
    val format: String = "",
    val type: String = "",
    val direction: String? = null,
)

@Serializable
internal data class SurahDto(
    val number: Int,
    val name: String,
    val englishName: String,
    val englishNameTranslation: String,
    val numberOfAyahs: Int,
    val revelationType: String,
    val ayahs: List<AyahDto> = emptyList(),
    val edition: EditionDto? = null,
)

@Serializable
internal data class AyahDto(
    val number: Int,
    val text: String = "",
    val numberInSurah: Int,
    val juz: Int,
    val manzil: Int,
    val page: Int,
    val ruku: Int,
    val hizbQuarter: Int,
    /** `false`, or an object `{id, recommended, obligatory}` for sajdah ayahs. */
    val sajda: JsonElement = JsonPrimitive(false),
    val audio: String? = null,
    val audioSecondary: List<String> = emptyList(),
    /** Present only on the single-ayah endpoint. */
    val surah: SurahDto? = null,
    val edition: EditionDto? = null,
)
