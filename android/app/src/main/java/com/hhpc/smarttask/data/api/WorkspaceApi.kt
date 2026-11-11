package com.hhpc.smarttask.data.api

import com.hhpc.smarttask.data.model.Workspace
import retrofit2.Response
import retrofit2.http.*

interface WorkspaceApi {
    @GET("workspaces")
    suspend fun getWorkspaces(): Response<List<Workspace>>

    @POST("workspaces")
    suspend fun createWorkspace(@Body workspace: Workspace): Response<Workspace>

    @PUT("workspaces/{id}")
    suspend fun updateWorkspace(@Path("id") id: Long, @Body workspace: Workspace): Response<Workspace>

    @DELETE("workspaces/{id}")
    suspend fun deleteWorkspace(@Path("id") id: Long): Response<Unit>
}
