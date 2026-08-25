package com.hikmah.hadits.data

import com.google.gson.Gson
import com.hikmah.hadits.data.remote.HadithPageDto
import com.hikmah.hadits.data.remote.EditionResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
}
