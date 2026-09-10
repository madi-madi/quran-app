package sa.com.dreams.quran.data.remote.alquran

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import retrofit2.HttpException
import sa.com.dreams.quran.core.network.NetworkModule
import sa.com.dreams.quran.data.remote.QuranDataValidator
import sa.com.dreams.quran.data.remote.QuranRemoteDataSource
import sa.com.dreams.quran.data.remote.RemoteAyah
import sa.com.dreams.quran.data.remote.RemoteAyahAudio
import sa.com.dreams.quran.data.remote.RemoteEdition
import sa.com.dreams.quran.data.remote.RemoteSurah
import sa.com.dreams.quran.data.remote.RemoteSurahEdition
import sa.com.dreams.quran.domain.error.QuranError
import sa.com.dreams.quran.domain.model.AyahRef
import sa.com.dreams.quran.domain.model.EditionType
import sa.com.dreams.quran.domain.model.RevelationType
import sa.com.dreams.quran.domain.model.Sajda
import java.io.IOException

/**
 * [QuranRemoteDataSource] backed by alquran.cloud. Quran text comes from the Tanzil
 * project's verified Uthmani text ("quran-uthmani"), served unchanged by the API.
 */
class AlQuranCloudDataSource internal constructor(
    private val api: AlQuranCloudApi,
) : QuranRemoteDataSource {

    override val sourceId = "alquran.cloud"
    override val displayScriptEdition = "quran-uthmani"
    override val searchScriptEdition = "quran-simple-clean"

    override suspend fun getSurahs(): List<RemoteSurah> = call("surah list") {
        api.surahs().data
            .map { it.toRemoteSurah() }
            .also(QuranDataValidator::validateSurahList)
    }

    override suspend fun getEditions(type: EditionType): List<RemoteEdition> = call("$type editions") {
        val response = when (type) {
            EditionType.QURAN -> api.editions(type = "quran", format = "text")
            EditionType.TRANSLATION -> api.editions(type = "translation", format = "text")
            EditionType.TAFSIR -> api.editions(type = "tafsir", format = null)
            EditionType.AUDIO -> api.editions(type = "versebyverse", format = "audio")
        }
        response.data.map { it.toRemoteEdition(type) }
    }

    override suspend fun getSurahEditions(
        surah: Int,
        editions: List<String>,
    ): List<RemoteSurahEdition> = call("surah $surah") {
        require(editions.isNotEmpty()) { "no editions requested" }
        val byEdition = api.surahEditions(surah, editions.joinToString(",")).data
            .associateBy { it.edition?.identifier }
        editions.map { id ->
            val dto = byEdition[id] ?: throw QuranError.InvalidData("edition $id missing for surah $surah")
            if (dto.number != surah) throw QuranError.InvalidData("asked for surah $surah, got ${dto.number}")
            dto.toRemoteSurahEdition(id).also(QuranDataValidator::validateSurahEdition)
        }
    }

    override suspend fun getAyahEdition(ref: AyahRef, edition: String): RemoteAyah = call("ayah $ref") {
        val dto = api.ayah("${ref.surah}:${ref.ayah}", edition).data
        val surahNumber = dto.surah?.number ?: throw QuranError.InvalidData("ayah $ref has no surah")
        val ayah = dto.toRemoteAyah(surahNumber)
        if (ayah.surah != ref.surah || ayah.numberInSurah != ref.ayah) {
            throw QuranError.InvalidData("asked for ayah $ref, got ${ayah.surah}:${ayah.numberInSurah}")
        }
        ayah.also(QuranDataValidator::validateAyah)
    }

    override suspend fun getSurahAudio(surah: Int, reciter: String): List<RemoteAyahAudio> = call("audio $surah") {
        val dto = api.surah(surah, reciter).data
        val expected = QuranDataValidator.AYAH_COUNTS[surah - 1]
        if (dto.number != surah || dto.ayahs.size != expected) {
            throw QuranError.InvalidData("audio for surah $surah has ${dto.ayahs.size} ayahs, expected $expected")
        }
        dto.ayahs.map {
            RemoteAyahAudio(
                globalNumber = it.number,
                numberInSurah = it.numberInSurah,
                url = it.audio?.takeIf(String::isNotBlank)
                    ?: throw QuranError.InvalidData("no audio URL for $surah:${it.numberInSurah}"),
                lowBitrateUrl = it.audioSecondary.firstOrNull(),
            )
        }
    }

    /** Runs an API call and converts every failure into a [QuranError]. */
    private suspend fun <T> call(what: String, block: suspend () -> T): T = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: QuranError) {
        throw e
    } catch (e: HttpException) {
        throw if (e.code() == 404) QuranError.NotFound(what) else QuranError.Server(e.code(), e)
    } catch (e: IOException) {
        throw QuranError.NoConnection(e)
    } catch (e: SerializationException) {
        throw QuranError.InvalidData("$what: ${e.message}", e)
    } catch (e: IllegalArgumentException) {
        throw QuranError.InvalidData("$what: ${e.message}", e)
    } catch (e: Exception) {
        throw QuranError.Unknown(e)
    }

    companion object {
        fun create(
            client: OkHttpClient,
            baseUrl: String = AlQuranCloudApi.BASE_URL,
        ): AlQuranCloudDataSource = AlQuranCloudDataSource(
            NetworkModule.retrofit(baseUrl, client).create(AlQuranCloudApi::class.java),
        )
    }
}

private fun SurahDto.toRemoteSurah() = RemoteSurah(
    number = number,
    nameArabic = name,
    nameTransliterated = englishName,
    nameTranslated = englishNameTranslation,
    ayahCount = numberOfAyahs,
    revelationType = when (revelationType.lowercase()) {
        "meccan" -> RevelationType.MECCAN
        "medinan" -> RevelationType.MEDINAN
        else -> RevelationType.UNKNOWN
    },
)

private fun SurahDto.toRemoteSurahEdition(editionId: String) = RemoteSurahEdition(
    edition = editionId,
    surah = toRemoteSurah(),
    ayahs = ayahs.map { it.toRemoteAyah(number) },
)

private fun AyahDto.toRemoteAyah(surahNumber: Int) = RemoteAyah(
    globalNumber = number,
    surah = surahNumber,
    numberInSurah = numberInSurah,
    text = text,
    juz = juz,
    hizbQuarter = hizbQuarter,
    page = page,
    manzil = manzil,
    ruku = ruku,
    sajda = sajda.toSajda(),
)

private fun JsonElement.toSajda(): Sajda? {
    if (this !is JsonObject) return null
    val id = this["id"]?.jsonPrimitive?.intOrNull ?: return null
    return Sajda(id = id, obligatory = this["obligatory"]?.jsonPrimitive?.booleanOrNull ?: false)
}

private fun EditionDto.toRemoteEdition(type: EditionType) = RemoteEdition(
    identifier = identifier,
    language = language,
    name = name,
    englishName = englishName,
    type = type,
    isRightToLeft = direction.equals("rtl", ignoreCase = true),
)
