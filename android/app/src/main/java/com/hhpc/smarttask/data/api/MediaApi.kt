package com.hhpc.smarttask.data.api

import retrofit2.Response
import retrofit2.http.*

data class LichSuMediaDto(
    val id: Long? = null,
    val userId: Long? = null,
    val mediaType: String? = null,
    val mediaUrl: String? = null,
    val fileName: String? = null,
    val fileSize: Long? = null,
    val createdAt: String? = null
)

interface MediaApi {
    @GET("media/history")
    suspend fun getMediaHistory(
        @Query("type") type: String? = null
    ): Response<List<LichSuMediaDto>>

    @DELETE("media/history/{id}")
    suspend fun deleteMediaHistory(
        @Path("id") id: Long
    ): Response<Map<String, Any>>
}
