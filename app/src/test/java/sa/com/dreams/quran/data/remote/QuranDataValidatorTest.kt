package sa.com.dreams.quran.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import sa.com.dreams.quran.domain.error.QuranError
import sa.com.dreams.quran.domain.model.Ayah
import sa.com.dreams.quran.domain.model.RevelationType

class QuranDataValidatorTest {

    @Test
    fun `canonical ayah counts cover the whole Quran`() {
        assertEquals(114, QuranDataValidator.AYAH_COUNTS.size)
        assertEquals(6236, QuranDataValidator.AYAH_COUNTS.sum())
    }

    @Test
    fun `first global ayah of each surah`() {
        assertEquals(1, QuranDataValidator.firstGlobalAyah(1))
        assertEquals(8, QuranDataValidator.firstGlobalAyah(2))
        assertEquals(3504, QuranDataValidator.firstGlobalAyah(32))
        assertEquals(6231, QuranDataValidator.firstGlobalAyah(114))
    }

    @Test
    fun `valid surah passes`() {
        QuranDataValidator.validateSurahEdition(surah112())
    }

    @Test
    fun `wrong ayah count is rejected`() {
        val edition = surah112().let { it.copy(ayahs = it.ayahs.dropLast(1)) }
        assertInvalid { QuranDataValidator.validateSurahEdition(edition) }
    }

    @Test
    fun `out of order ayah numbers are rejected`() {
        val edition = surah112().let { e ->
            e.copy(ayahs = e.ayahs.mapIndexed { i, a -> if (i == 1) a.copy(numberInSurah = 3) else a })
        }
        assertInvalid { QuranDataValidator.validateSurahEdition(edition) }
    }

    @Test
    fun `blank text is rejected`() {
        val edition = surah112().let { e -> e.copy(ayahs = e.ayahs.mapIndexed { i, a -> if (i == 0) a.copy(text = " ") else a }) }
        assertInvalid { QuranDataValidator.validateSurahEdition(edition) }
    }

    @Test
    fun `page outside the 604-page mushaf is rejected`() {
        val edition = surah112().let { e -> e.copy(ayahs = e.ayahs.map { it.copy(page = 605) }) }
        assertInvalid { QuranDataValidator.validateSurahEdition(edition) }
    }

    @Test
    fun `surah metadata with a non-canonical ayah count is rejected`() {
        val edition = surah112().let { it.copy(surah = it.surah.copy(ayahCount = 5)) }
        assertInvalid { QuranDataValidator.validateSurahEdition(edition) }
    }

    @Test
    fun `hizb is derived from the hizb quarter`() {
        assertEquals(1, Ayah.hizbOf(1))
        assertEquals(1, Ayah.hizbOf(4))
        assertEquals(2, Ayah.hizbOf(5))
        assertEquals(60, Ayah.hizbOf(240))
    }

    /** Synthetic placeholder data (not Quran text) shaped like surah 112, which has 4 ayahs. */
    private fun surah112(): RemoteSurahEdition {
        val first = QuranDataValidator.firstGlobalAyah(112)
        return RemoteSurahEdition(
            edition = "test",
            surah = RemoteSurah(112, "name", "Al-Ikhlaas", "Sincerity", 4, RevelationType.MECCAN),
            ayahs = (1..4).map { n ->
                RemoteAyah(
                    globalNumber = first + n - 1, surah = 112, numberInSurah = n, text = "text $n",
                    juz = 30, hizbQuarter = 240, page = 604, manzil = 7, ruku = 550, sajda = null,
                )
            },
        )
    }

    private fun assertInvalid(block: () -> Unit) {
        val error = runCatching(block).exceptionOrNull()
        assertTrue("expected InvalidData but got $error", error is QuranError.InvalidData)
    }
}
