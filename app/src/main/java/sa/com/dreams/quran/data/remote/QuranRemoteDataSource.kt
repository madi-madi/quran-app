package sa.com.dreams.quran.data.remote

import sa.com.dreams.quran.domain.model.AyahRef
import sa.com.dreams.quran.domain.model.EditionType

/**
 * The contract every Quran content provider implements. Repositories depend only on
 * this interface, so switching from alquran.cloud to another API (or a bundled
 * dataset) means writing one new implementation — no UI or business-logic changes.
 *
 * Rules for implementations:
 *  - Throw only [sa.com.dreams.quran.domain.error.QuranError].
 *  - Return text exactly as the source delivers it. Never trim, normalize or edit it.
 *  - Run [QuranDataValidator] on every surah before returning it, so incomplete or
 *    corrupted data is rejected instead of stored.
 *  - Be main-safe (suspend + a non-blocking HTTP client).
 */
interface QuranRemoteDataSource {

    /** Stable id of this source, e.g. "alquran.cloud". */
    val sourceId: String

    /** Edition id of the Uthmani script shown in the reader. */
    val displayScriptEdition: String

    /**
     * Edition id of a plain script (standard spelling, no diacritics). It is only used
     * to build the search index and is never displayed.
     */
    val searchScriptEdition: String

    /** Metadata of all 114 surahs (no ayah text). */
    suspend fun getSurahs(): List<RemoteSurah>

    /** Editions (scripts, translations, tafsir, reciters) offered by the source. */
    suspend fun getEditions(type: EditionType): List<RemoteEdition>

    /**
     * All ayahs of one surah in each requested edition. Sources that support it should
     * fetch every edition in a single request to save round trips on slow networks.
     */
    suspend fun getSurahEditions(surah: Int, editions: List<String>): List<RemoteSurahEdition>

    /** One ayah in one edition — used for on-demand tafsir so nothing extra is downloaded. */
    suspend fun getAyahEdition(ref: AyahRef, edition: String): RemoteAyah

    /** Per-ayah audio URLs for one surah by one reciter. */
    suspend fun getSurahAudio(surah: Int, reciter: String): List<RemoteAyahAudio>
}
