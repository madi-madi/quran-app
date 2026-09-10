package sa.com.dreams.quran.data.remote.alquran

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** Retrofit definition of the alquran.cloud REST API (https://alquran.cloud/api). */
internal interface AlQuranCloudApi {

    @GET("surah")
    suspend fun surahs(): EnvelopeDto<List<SurahDto>>

    @GET("edition")
    suspend fun editions(
        @Query("type") type: String?,
        @Query("format") format: String?,
    ): EnvelopeDto<List<EditionDto>>

    /** Several editions of one surah in one request, e.g. editions = "quran-uthmani,en.sahih". */
    @GET("surah/{surah}/editions/{editions}")
    suspend fun surahEditions(
        @Path("surah") surah: Int,
        @Path("editions", encoded = true) editions: String,
    ): EnvelopeDto<List<SurahDto>>

    /** One surah in one edition. Audio editions include per-ayah URLs. */
    @GET("surah/{surah}/{edition}")
    suspend fun surah(
        @Path("surah") surah: Int,
        @Path("edition") edition: String,
    ): EnvelopeDto<SurahDto>

    /** One ayah by reference ("2:255") in one edition. */
    @GET("ayah/{ref}/{edition}")
    suspend fun ayah(
        @Path("ref", encoded = true) ref: String,
        @Path("edition") edition: String,
    ): EnvelopeDto<AyahDto>

    companion object {
        const val BASE_URL = "https://api.alquran.cloud/v1/"
    }
}
