package com.hhpc.smarttask.data.api

import com.hhpc.smarttask.data.model.ProductivityReportDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ProductivityApi {
    @GET("productivity/daily")
    suspend fun getDailyReport(
        @Query("date") date: String? = null
    ): Response<ProductivityReportDto>
}
