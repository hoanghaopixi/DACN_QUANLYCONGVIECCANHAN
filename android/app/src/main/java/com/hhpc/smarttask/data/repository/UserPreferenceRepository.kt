package com.hhpc.smarttask.data.repository

import android.content.Context
import com.hhpc.smarttask.data.api.ApiClient
import com.hhpc.smarttask.data.model.UserPreferenceDto

class UserPreferenceRepository(context: Context) {
    private val userPreferenceApi = ApiClient.getUserPreferenceApi(context)

    suspend fun getPreferences(): Result<UserPreferenceDto> {
        return try {
            val response = userPreferenceApi.getPreferences()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Không thể tải cài đặt thói quen sinh hoạt."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePreferences(preference: UserPreferenceDto): Result<UserPreferenceDto> {
        return try {
            val response = userPreferenceApi.updatePreferences(preference)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Không thể cập nhật cài đặt thói quen."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
