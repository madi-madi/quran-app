package sa.com.dreams.quran.data.remote.alquran

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import sa.com.dreams.quran.domain.error.QuranError
import sa.com.dreams.quran.domain.model.AyahRef
import sa.com.dreams.quran.domain.model.EditionType
import sa.com.dreams.quran.domain.model.RevelationType
import sa.com.dreams.quran.domain.model.Sajda

/**
 * Exercises the real Retrofit stack against recorded alquran.cloud responses
 * (src/test/resources/api), so paths, JSON mapping and validation are all covered.
 */
class AlQuranCloudDataSourceTest {

    private lateinit var server: MockWebServer
    private lateinit var source: AlQuranCloudDataSource

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        val client = OkHttpClient.Builder().retryOnConnectionFailure(false).build()
        source = AlQuranCloudDataSource.create(client, server.url("/v1/").toString())
    }

    @After
    fun tearDown() = server.shutdown()

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResource("/api/$name")) { "missing fixture $name" }.readText()

    private fun enqueueFixture(name: String) = server.enqueue(MockResponse().setBody(fixture(name)))

    @Test
    fun `surah list has 114 surahs with the canonical ayah counts`() = runTest {
        enqueueFixture("surahs.json")

        val surahs = source.getSurahs()

        assertEquals("/v1/surah", server.takeRequest().path)
        assertEquals(114, surahs.size)
        assertEquals(6236, surahs.sumOf { it.ayahCount })
        with(surahs.first()) {
            assertEquals("Al-Faatiha", nameTransliterated)
            assertEquals("The Opening", nameTranslated)
            assertEquals(RevelationType.MECCAN, revelationType)
        }
        assertEquals(RevelationType.MEDINAN, surahs[1].revelationType)
    }

    @Test
    fun `several editions come from one request and text is kept verbatim`() = runTest {
        enqueueFixture("s1_editions.json")

        val editions = source.getSurahEditions(1, listOf("quran-uthmani", "quran-simple-clean"))

        assertEquals("/v1/surah/1/editions/quran-uthmani,quran-simple-clean", server.takeRequest().path)
        assertEquals(listOf("quran-uthmani", "quran-simple-clean"), editions.map { it.edition })

        // Every ayah string must be identical to the raw JSON value — nothing trimmed or rewritten.
        val raw = Json.parseToJsonElement(fixture("s1_editions.json")).jsonObject.getValue("data").jsonArray
        editions.forEachIndexed { i, edition ->
            val rawAyahs = raw[i].jsonObject.getValue("ayahs").jsonArray
            assertEquals(rawAyahs.size, edition.ayahs.size)
            edition.ayahs.forEachIndexed { j, ayah ->
                assertEquals(rawAyahs[j].jsonObject.getValue("text").jsonPrimitive.content, ayah.text)
            }
        }
        with(editions[0].ayahs[0]) {
            assertEquals(1, globalNumber)
            assertEquals(1, page)
            assertEquals(1, juz)
            assertEquals(1, hizbQuarter)
            assertNull(sajda)
        }
    }

    @Test
    fun `sajdah ayahs are parsed`() = runTest {
        enqueueFixture("s32_editions.json")

        val surah = source.getSurahEditions(32, listOf("quran-uthmani")).single()

        val sajdas = surah.ayahs.filter { it.sajda != null }
        assertEquals(listOf(15), sajdas.map { it.numberInSurah })
        assertEquals(Sajda(id = 10, obligatory = true), sajdas.single().sajda)
    }

    @Test
    fun `truncated surah is rejected instead of stored`() = runTest {
        val root = Json.parseToJsonElement(fixture("s1_editions.json")).jsonObject
        val truncated = root.getValue("data").jsonArray.map { edition ->
            val obj = edition.jsonObject
            JsonObject(obj + ("ayahs" to JsonArray(obj.getValue("ayahs").jsonArray.dropLast(1))))
        }
        server.enqueue(MockResponse().setBody(JsonObject(root + ("data" to JsonArray(truncated))).toString()))

        assertFails<QuranError.InvalidData> {
            source.getSurahEditions(1, listOf("quran-uthmani", "quran-simple-clean"))
        }
    }

    @Test
    fun `single ayah tafsir is fetched on demand`() = runTest {
        enqueueFixture("tafsir_sample.json")

        val ayah = source.getAyahEdition(AyahRef(2, 255), "ar.muyassar")

        assertEquals("/v1/ayah/2:255/ar.muyassar", server.takeRequest().path)
        assertEquals(262, ayah.globalNumber)
        assertEquals(2, ayah.surah)
        assertEquals(255, ayah.numberInSurah)
        assertTrue(ayah.text.isNotBlank())
    }

    @Test
    fun `audio urls include the low bitrate alternative`() = runTest {
        enqueueFixture("s1_audio.json")

        val audio = source.getSurahAudio(1, "ar.alafasy")

        assertEquals("/v1/surah/1/ar.alafasy", server.takeRequest().path)
        assertEquals(7, audio.size)
        assertEquals("https://cdn.islamic.network/quran/audio/128/ar.alafasy/1.mp3", audio[0].url)
        assertEquals("https://cdn.islamic.network/quran/audio/64/ar.alafasy/1.mp3", audio[0].lowBitrateUrl)
    }

    @Test
    fun `editions are typed and carry their text direction`() = runTest {
        enqueueFixture("tafsir_editions.json")

        val tafsirs = source.getEditions(EditionType.TAFSIR)

        assertEquals("/v1/edition?type=tafsir", server.takeRequest().path)
        assertEquals(6, tafsirs.size)
        assertTrue(tafsirs.all { it.type == EditionType.TAFSIR && it.language == "ar" && it.isRightToLeft })
    }

    @Test
    fun `reciter list uses the verse-by-verse audio query`() = runTest {
        enqueueFixture("audio_editions.json")

        val reciters = source.getEditions(EditionType.AUDIO)

        assertEquals("/v1/edition?type=versebyverse&format=audio", server.takeRequest().path)
        assertTrue(reciters.any { it.identifier == "ar.alafasy" })
    }

    @Test
    fun `http 404 maps to NotFound`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404).setBody("""{"code":404,"status":"NOT FOUND","data":"x"}"""))
        assertFails<QuranError.NotFound> { source.getAyahEdition(AyahRef(1, 1), "xx.missing") }
    }

    @Test
    fun `http 503 maps to a retryable Server error`() = runTest {
        server.enqueue(MockResponse().setResponseCode(503))
        val error = assertFails<QuranError.Server> { source.getSurahs() }
        assertEquals(503, error.httpCode)
        assertTrue(error.isRetryable)
    }

    @Test
    fun `dropped connection maps to NoConnection`() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        val error = assertFails<QuranError.NoConnection> { source.getSurahs() }
        assertTrue(error.isRetryable)
    }

    @Test
    fun `malformed json maps to InvalidData`() = runTest {
        server.enqueue(MockResponse().setBody("{not json"))
        assertFails<QuranError.InvalidData> { source.getSurahs() }
    }

    private suspend inline fun <reified T : QuranError> assertFails(block: suspend () -> Unit): T {
        val error = runCatching { block() }.exceptionOrNull()
        assertTrue("expected ${T::class.simpleName} but got $error", error is T)
        return error as T
    }
}
