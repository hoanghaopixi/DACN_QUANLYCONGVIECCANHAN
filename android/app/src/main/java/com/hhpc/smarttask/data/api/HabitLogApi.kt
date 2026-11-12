package com.hhpc.smarttask.data.api

import retrofit2.Response
import retrofit2.http.*

data class HabitLogDto(
    val id: Long? = null,
    val taskId: Long,
    val checkInDate: String,
    val status: String? = "COMPLETED",
    val note: String? = null
)

interface HabitLogApi {
    @GET("habits")
    suspend fun getHabitLogs(): Response<List<HabitLogDto>>

    @GET("habits/tasks/{taskId}")
    suspend fun getTaskHabitLogs(@Path("taskId") taskId: Long): Response<List<HabitLogDto>>

    @POST("habits/check-in")
    suspend fun checkInHabit(@Body dto: HabitLogDto): Response<HabitLogDto>
}
