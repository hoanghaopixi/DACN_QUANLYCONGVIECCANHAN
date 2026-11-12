package com.hhpc.smarttask.data.repository

import android.content.Context
import com.hhpc.smarttask.data.api.ApiClient
import com.hhpc.smarttask.data.api.ApiErrorParser
import com.hhpc.smarttask.data.api.HabitLogDto

class HabitRepository(context: Context) {
    private val habitApi = ApiClient.getHabitLogApi(context)

    suspend fun getHabitLogs(): Result<List<HabitLogDto>> {
        return try {
            val res = habitApi.getHabitLogs()
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                val msg = ApiErrorParser.parseError(res, "Không thể tải danh sách thói quen.")
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            val msg = ApiErrorParser.parseException(e, "Lỗi kết nối khi tải thói quen.")
            Result.failure(Exception(msg))
        }
    }

    suspend fun checkInHabit(taskId: Long, checkInDate: String, note: String? = null): Result<HabitLogDto> {
        return try {
            val dto = HabitLogDto(taskId = taskId, checkInDate = checkInDate, note = note)
            val res = habitApi.checkInHabit(dto)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                val msg = ApiErrorParser.parseError(res, "Không thể check-in thói quen.")
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            val msg = ApiErrorParser.parseException(e, "Lỗi kết nối khi check-in thói quen.")
            Result.failure(Exception(msg))
        }
    }
}
