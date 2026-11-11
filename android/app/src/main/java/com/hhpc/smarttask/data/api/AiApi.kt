package com.hhpc.smarttask.data.api

import com.hhpc.smarttask.data.model.AiParseTaskRequest
import com.hhpc.smarttask.data.model.AiParseTaskResponse
import com.hhpc.smarttask.data.model.AutoScheduleRequest
import com.hhpc.smarttask.data.model.AutoScheduleResponse
import com.hhpc.smarttask.data.model.SubtaskDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface AiApi {
    @POST("ai/parse-task")
    suspend fun parseTask(@Body request: AiParseTaskRequest): Response<AiParseTaskResponse>

    @POST("ai/tasks/{taskId}/breakdown")
    suspend fun breakdownTask(@Path("taskId") taskId: Long): Response<List<SubtaskDto>>

    @POST("ai/auto-schedule")
    suspend fun autoSchedule(@Body request: AutoScheduleRequest): Response<AutoScheduleResponse>
}
