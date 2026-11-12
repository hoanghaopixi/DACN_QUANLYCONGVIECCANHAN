package com.hhpc.smarttask.data.repository

import android.content.Context
import com.hhpc.smarttask.data.api.ApiClient
import com.hhpc.smarttask.data.api.ApiErrorParser
import com.hhpc.smarttask.data.model.ProductivityReportDto

class ProductivityRepository(context: Context) {
    private val productivityApi = ApiClient.getProductivityApi(context)

    suspend fun getDailyReport(date: String? = null): Result<ProductivityReportDto> {
        return try {
            val response = productivityApi.getDailyReport(date)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(ApiErrorParser.parseError(response, "Không thể tải báo cáo năng suất")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.parseException(e, "Không thể tải báo cáo năng suất")))
        }
    }
}
