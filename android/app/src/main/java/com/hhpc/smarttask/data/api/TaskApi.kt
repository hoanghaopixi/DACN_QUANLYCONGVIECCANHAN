package com.hhpc.smarttask.data.api

import com.hhpc.smarttask.data.model.SubtaskDto
import com.hhpc.smarttask.data.model.TaskDto
import retrofit2.Response
import retrofit2.http.*

interface TaskApi {
    @GET("tasks")
    suspend fun getTasks(
        @Query("workspaceId") workspaceId: Long? = null
    ): Response<List<TaskDto>>

    @POST("tasks")
    suspend fun createTask(@Body task: TaskDto): Response<TaskDto>

    @PUT("tasks/{id}")
    suspend fun updateTask(
        @Path("id") id: Long,
        @Body task: TaskDto
    ): Response<TaskDto>

    @DELETE("tasks/{id}")
    suspend fun deleteTask(@Path("id") id: Long): Response<Unit>

    @GET("tasks/eisenhower")
    suspend fun getEisenhowerMatrix(
        @Query("workspaceId") workspaceId: Long? = null
    ): Response<Map<String, List<TaskDto>>>

    @PATCH("tasks/{id}/quadrant")
    suspend fun updateQuadrant(
        @Path("id") id: Long,
        @Body body: Map<String, String>
    ): Response<TaskDto>

    @GET("tasks/{taskId}/subtasks")
    suspend fun getSubtasks(@Path("taskId") taskId: Long): Response<List<SubtaskDto>>

    @POST("tasks/{taskId}/subtasks")
    suspend fun createSubtask(
        @Path("taskId") taskId: Long,
        @Body subtask: SubtaskDto
    ): Response<SubtaskDto>

    @PATCH("tasks/subtasks/{subtaskId}/toggle")
    suspend fun toggleSubtask(@Path("subtaskId") subtaskId: Long): Response<SubtaskDto>
}
