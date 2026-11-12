package com.hhpc.smarttask.data.api

import retrofit2.Response
import retrofit2.http.GET

interface DatabaseStatsApi {
    @GET("system/table-stats")
    suspend fun getTableStats(): Response<Map<String, Any>>
}
