package com.hhpc.smarttask.data.api

import retrofit2.Response
import retrofit2.http.*

data class ThongBaoDto(
    val id: Long? = null,
    val userId: Long? = null,
    val taskId: Long? = null,
    val title: String,
    val message: String? = null,
    val type: String? = "reminder",
    val isRead: Boolean? = false,
    val isDeleted: Boolean? = false,
    val createdAt: String? = null
)

interface NotificationApi {
    @GET("notifications")
    suspend fun getNotifications(): Response<List<ThongBaoDto>>

    @POST("notifications/evaluate")
    suspend fun evaluateNotifications(): Response<List<ThongBaoDto>>

    @GET("notifications/unread-count")
    suspend fun getUnreadCount(): Response<Map<String, Any>>

    @PUT("notifications/{id}/read")
    suspend fun markAsRead(@Path("id") id: Long): Response<ThongBaoDto>

    @PUT("notifications/read-all")
    suspend fun markAllAsRead(): Response<Map<String, Any>>

    @DELETE("notifications/{id}")
    suspend fun deleteNotification(@Path("id") id: Long): Response<Map<String, Any>>

    @DELETE("notifications/clear-all")
    suspend fun clearAllNotifications(): Response<Map<String, Any>>

    @POST("notifications")
    suspend fun createNotification(@Body body: Map<String, Any>): Response<ThongBaoDto>
}
