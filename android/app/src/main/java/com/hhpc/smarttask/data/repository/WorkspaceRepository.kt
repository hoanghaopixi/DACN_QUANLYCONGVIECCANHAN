package com.hhpc.smarttask.data.repository

import android.content.Context
import com.hhpc.smarttask.data.api.ApiClient
import com.hhpc.smarttask.data.api.ApiErrorParser
import com.hhpc.smarttask.data.model.Category
import com.hhpc.smarttask.data.model.Workspace

class WorkspaceRepository(context: Context) {
    private val workspaceApi = ApiClient.getWorkspaceApi(context)
    private val categoryApi = ApiClient.getCategoryApi(context)

    suspend fun getWorkspaces(): Result<List<Workspace>> {
        return try {
            val res = workspaceApi.getWorkspaces()
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.failure(Exception(ApiErrorParser.parseError(res, "Không thể tải danh sách Không gian làm việc")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.parseException(e, "Không thể tải danh sách Không gian làm việc")))
        }
    }

    suspend fun createWorkspace(workspace: Workspace): Result<Workspace> {
        return try {
            val res = workspaceApi.createWorkspace(workspace)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.failure(Exception(ApiErrorParser.parseError(res, "Không thể tạo Không gian làm việc")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.parseException(e, "Không thể tạo Không gian làm việc")))
        }
    }

    suspend fun getCategories(workspaceId: Long? = null): Result<List<Category>> {
        return try {
            val res = categoryApi.getCategories(workspaceId)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.failure(Exception(ApiErrorParser.parseError(res, "Không thể tải danh sách Danh mục")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorParser.parseException(e, "Không thể tải danh sách Danh mục")))
        }
    }
}
