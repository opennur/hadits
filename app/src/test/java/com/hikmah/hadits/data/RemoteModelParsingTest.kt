package com.hikmah.hadits.data

import com.google.gson.Gson
import com.hikmah.hadits.data.remote.HadithPageDto
import com.hikmah.hadits.data.remote.EditionResponse
import com.hikmah.hadits.data.remote.HadithDetailDto
import com.hikmah.hadits.data.remote.HadithItemDto
import com.hikmah.hadits.data.remote.HadithIntegrityValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteModelParsingTest {
    @Test
    fun parsesPagedIndonesianHadithResponse() {
        val json = """
            {
              "name": "Ahmad",
              "slug": "ahmad",
              "total": 4305,
              "items": [
                {
                  "number": 1,
                  "arab": "النص العربي",
                  "id": "Terjemahan Indonesia"
                }
              ]
            }
        """.trimIndent()

        val response: HadithPageDto = Gson().fromJson(json, HadithPageDto::class.java)

        assertEquals("ahmad", response.slug)
        assertEquals(4305, response.total)
        assertNotNull(response.items)
        assertEquals(1, response.items?.single()?.number)
        assertEquals("Terjemahan Indonesia", response.items?.single()?.id)
    }

    @Test
    fun parsesCdnEditionResponseForSearchFallback() {
        val json = """
            {
              "metadata": {"name": "Sahih al Bukhari"},
              "hadiths": [
                {"hadithnumber": 1, "text": "Semua perbuatan tergantung niatnya."}
              ]
            }
        """.trimIndent()

        val response: EditionResponse = Gson().fromJson(json, EditionResponse::class.java)

        assertEquals("Sahih al Bukhari", response.metadata?.name)
        assertEquals(1, response.hadiths?.single()?.hadithNumber)
    }

    @Test
    fun validatesPageNumbersAndRejectsDuplicatesOrOutOfRangeItems() {
        val page = HadithPageDto(
            slug = "ahmad",
            total = 4_305,
            items = listOf(
                HadithItemDto(1, "arab 1", "isi 1"),
                HadithItemDto(2, "arab 2", "isi 2"),
                HadithItemDto(2, "duplikat", "duplikat"),
                HadithItemDto(99, "di luar halaman", "isi"),
            ),
        )

        val valid = HadithIntegrityValidator.validPageItems(page, firstNumber = 1, pageSize = 20)

        assertEquals(listOf(1, 2), valid.map { it.number })
    }

    @Test
    fun validatesKnownBukhariOneReferenceAndNumber() {
        val item = HadithItemDto(
            number = 1,
            arab = "حَدَّثَنَا ... إِنَّمَا الْأَعْمَالُ بِالنِّيَّاتِ",
            id = "Semua perbuatan tergantung niatnya, dan balasan bagi tiap-tiap orang tergantung apa yang diniatkan.",
        )
        val page = HadithPageDto(
            name = "Bukhari",
            slug = "bukhari",
            total = 6_638,
            items = listOf(item),
        )

        val valid = HadithIntegrityValidator.validPageItems(page, firstNumber = 1, pageSize = 20).single()

        assertEquals(1, valid.number)
        assertTrue(valid.id!!.contains("Semua perbuatan tergantung niatnya"))
        assertTrue(valid.arab!!.contains("إِنَّمَا الْأَعْمَالُ"))
    }

    @Test
    fun validatesKnownAhmadOneDetailReferenceAndNumber() {
        val detail = HadithDetailDto(
            name = "Ahmad",
            slug = "ahmad",
            number = 1,
            arab = "حَدَّثَنَا ... إِنَّ النَّاسَ إِذَا رَأَوْا الْمُنْكَرَ",
            id = "Sesungguhnya jika manusia melihat kemungkaran kemudian mereka tidak mengingkarinya.",
        )

        assertTrue(HadithIntegrityValidator.isValidDetail(detail, "ahmad", 1))
    }

    @Test
    fun rejectsDetailWithWrongNumberOrBook() {
        val detail = HadithDetailDto(
            name = "Ahmad",
            slug = "ahmad",
            number = 2,
            arab = "arab",
            id = "isi",
        )

        assertTrue(!HadithIntegrityValidator.isValidDetail(detail, "bukhari", 1))
    }
}
