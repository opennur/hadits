package org.opennur.hadits.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path

data class HadithPageDto(
    val name: String? = null,
    val slug: String? = null,
    val total: Int = 0,
    val items: List<HadithItemDto>? = null,
)

data class HadithItemDto(
    val number: Int? = null,
    val arab: String? = null,
    val id: String? = null,
)

data class HadithDetailDto(
    val name: String? = null,
    val slug: String? = null,
    val number: Int? = null,
    val arab: String? = null,
    val id: String? = null,
)

data class EditionResponse(
    val metadata: EditionMetadata? = null,
    val hadiths: List<EditionHadithDto>? = null,
)

data class EditionMetadata(
    val name: String? = null,
)

data class EditionHadithDto(
    @SerializedName("hadithnumber") val hadithNumber: Int? = null,
    val text: String? = null,
)

interface HadithApi {
    @GET("hadith/{book}")
    suspend fun getHadithPage(
        @Path("book") book: String,
        @retrofit2.http.Query("page") page: Int,
        @retrofit2.http.Query("limit") limit: Int,
    ): HadithPageDto

    @GET("hadith/{book}/{number}")
    suspend fun getHadithDetail(
        @Path("book") book: String,
        @Path("number") number: Int,
    ): HadithDetailDto
}

interface HadithSearchApi {
    @GET("editions/{edition}.min.json")
    suspend fun getEdition(@Path("edition") edition: String): EditionResponse
}
