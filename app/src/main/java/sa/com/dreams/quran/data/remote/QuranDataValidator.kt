package sa.com.dreams.quran.data.remote

import sa.com.dreams.quran.domain.error.QuranError
import sa.com.dreams.quran.domain.model.QuranConstants

/**
 * Integrity checks applied to every downloaded surah, whatever the source. Data that
 * fails is rejected before it reaches the database, so a truncated or corrupted
 * response can never be shown as Quran text.
 */
object QuranDataValidator {

    /** Ayah count of each surah in the Hafs 'an 'Asim numbering (6236 ayahs in total). */
    val AYAH_COUNTS: IntArray = intArrayOf(
        7, 286, 200, 176, 120, 165, 206, 75, 129, 109, 123, 111, 43, 52, 99, 128, 111, 110, 98, 135,
        112, 78, 118, 64, 77, 227, 93, 88, 69, 60, 34, 30, 73, 54, 45, 83, 182, 88, 75, 85,
        54, 53, 89, 59, 37, 35, 38, 29, 18, 45, 60, 49, 62, 55, 78, 96, 29, 22, 24, 13,
        14, 11, 11, 18, 12, 12, 30, 52, 52, 44, 28, 28, 20, 56, 40, 31, 50, 40, 46, 42,
        29, 19, 36, 25, 22, 17, 19, 26, 30, 20, 15, 21, 11, 8, 8, 19, 5, 8, 8, 11,
        11, 8, 3, 9, 5, 4, 7, 3, 6, 3, 5, 4, 5, 6,
    )

    /** Global number of the first ayah of [surah] (1-based). */
    fun firstGlobalAyah(surah: Int): Int {
        var total = 1
        for (i in 0 until surah - 1) total += AYAH_COUNTS[i]
        return total
    }

    fun validateSurahList(surahs: List<RemoteSurah>) {
        if (surahs.size != QuranConstants.SURAH_COUNT) fail("expected 114 surahs, got ${surahs.size}")
        surahs.forEachIndexed { index, surah ->
            if (surah.number != index + 1) fail("surah list out of order at ${index + 1}")
            validateSurahMeta(surah)
        }
    }

    fun validateSurahEdition(edition: RemoteSurahEdition) {
        val surah = edition.surah
        validateSurahMeta(surah)
        val ayahs = edition.ayahs
        if (ayahs.size != surah.ayahCount) {
            fail("${edition.edition} surah ${surah.number}: expected ${surah.ayahCount} ayahs, got ${ayahs.size}")
        }
        val firstGlobal = firstGlobalAyah(surah.number)
        ayahs.forEachIndexed { index, ayah ->
            val where = "${edition.edition} ${surah.number}:${index + 1}"
            if (ayah.surah != surah.number) fail("$where belongs to surah ${ayah.surah}")
            if (ayah.numberInSurah != index + 1) fail("$where has number ${ayah.numberInSurah}")
            if (ayah.globalNumber != firstGlobal + index) fail("$where has global number ${ayah.globalNumber}")
            validateAyahPosition(ayah, where)
        }
    }

    fun validateAyah(ayah: RemoteAyah) {
        val where = "${ayah.surah}:${ayah.numberInSurah}"
        if (ayah.surah !in 1..QuranConstants.SURAH_COUNT) fail("$where has invalid surah")
        if (ayah.numberInSurah !in 1..AYAH_COUNTS[ayah.surah - 1]) fail("$where has invalid ayah number")
        if (ayah.globalNumber != firstGlobalAyah(ayah.surah) + ayah.numberInSurah - 1) {
            fail("$where has global number ${ayah.globalNumber}")
        }
        validateAyahPosition(ayah, where)
    }

    private fun validateSurahMeta(surah: RemoteSurah) {
        if (surah.number !in 1..QuranConstants.SURAH_COUNT) fail("invalid surah number ${surah.number}")
        val expected = AYAH_COUNTS[surah.number - 1]
        if (surah.ayahCount != expected) fail("surah ${surah.number}: ayah count ${surah.ayahCount}, expected $expected")
        if (surah.nameArabic.isBlank()) fail("surah ${surah.number} has no Arabic name")
    }

    private fun validateAyahPosition(ayah: RemoteAyah, where: String) {
        if (ayah.text.isBlank()) fail("$where has empty text")
        if (ayah.juz !in 1..QuranConstants.JUZ_COUNT) fail("$where has juz ${ayah.juz}")
        if (ayah.page !in 1..QuranConstants.PAGE_COUNT) fail("$where has page ${ayah.page}")
        if (ayah.hizbQuarter !in 1..QuranConstants.HIZB_QUARTER_COUNT) fail("$where has hizb quarter ${ayah.hizbQuarter}")
        if (ayah.manzil !in 1..QuranConstants.MANZIL_COUNT) fail("$where has manzil ${ayah.manzil}")
    }

    private fun fail(reason: String): Nothing = throw QuranError.InvalidData(reason)
}
