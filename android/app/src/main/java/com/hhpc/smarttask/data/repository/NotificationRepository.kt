package com.hhpc.smarttask.data.repository

import android.content.Context
import com.hhpc.smarttask.data.api.ApiClient
import com.hhpc.smarttask.data.api.ApiErrorParser
import com.hhpc.smarttask.data.api.ThongBaoDto

class NotificationRepository(context: Context) {
    private val notificationApi = ApiClient.getNotificationApi(context)

    suspend fun getNotifications(): Result<List<ThongBaoDto>> {
        return try {
            val res = notificationApi.getNotifications()
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                val msg = ApiErrorParser.parseError(res, "Không thể tải danh sách thông báo.")
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            val msg = ApiErrorParser.parseException(e, "Lỗi kết nối khi tải thông báo.")
            Result.failure(Exception(msg))
        }
    }

    suspend fun evaluateNotifications(): Result<List<ThongBaoDto>> {
        return try {
            val res = notificationApi.evaluateNotifications()
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                val msg = ApiErrorParser.parseError(res, "Không thể đánh giá thông báo mới.")
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            val msg = ApiErrorParser.parseException(e, "Lỗi kết nối khi đánh giá thông báo.")
            Result.failure(Exception(msg))
        }
    }

    suspend fun markAsRead(id: Long): Result<ThongBaoDto> {
        return try {
            val res = notificationApi.markAsRead(id)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                val msg = ApiErrorParser.parseError(res, "Không thể đánh dấu đã đọc.")
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            val msg = ApiErrorParser.parseException(e, "Lỗi kết nối khi cập nhật thông báo.")
            Result.failure(Exception(msg))
        }
    }

    suspend fun markAllAsRead(): Result<Unit> {
        return try {
            val res = notificationApi.markAllAsRead()
            if (res.isSuccessful) {
                Result.success(Unit)
            } else {
                val msg = ApiErrorParser.parseError(res, "Không thể đánh dấu tất cả là đã đọc.")
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            val msg = ApiErrorParser.parseException(e, "Lỗi kết nối khi cập nhật thông báo.")
            Result.failure(Exception(msg))
        }
    }

    suspend fun deleteNotification(id: Long): Result<Unit> {
        return try {
            val res = notificationApi.deleteNotification(id)
            if (res.isSuccessful) {
                Result.success(Unit)
            } else {
                val msg = ApiErrorParser.parseError(res, "Không thể xóa thông báo.")
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            val msg = ApiErrorParser.parseException(e, "Lỗi kết nối khi xóa thông báo.")
            Result.failure(Exception(msg))
        }
    }

    suspend fun clearAllNotifications(): Result<Unit> {
        return try {
            val res = notificationApi.clearAllNotifications()
            if (res.isSuccessful) {
                Result.success(Unit)
            } else {
                val msg = ApiErrorParser.parseError(res, "Không thể xóa tất cả thông báo.")
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            val msg = ApiErrorParser.parseException(e, "Lỗi kết nối khi xóa thông báo.")
            Result.failure(Exception(msg))
        }
    }
}
