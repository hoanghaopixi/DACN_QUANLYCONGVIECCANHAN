package com.hhpc.smarttask.data.repository

import android.content.Context
import com.hhpc.smarttask.data.api.ApiClient
import com.hhpc.smarttask.data.model.AiParseTaskRequest
import com.hhpc.smarttask.data.model.AiParseTaskResponse
import com.hhpc.smarttask.data.model.AutoScheduleRequest
import com.hhpc.smarttask.data.model.AutoScheduleResponse
import com.hhpc.smarttask.data.model.SubtaskDto
import com.hhpc.smarttask.data.model.TaskDto

class TaskRepository(context: Context) {
    private val taskApi = ApiClient.getTaskApi(context)
    private val aiApi = ApiClient.getAiApi(context)

    suspend fun getTasks(workspaceId: Long? = null): Result<List<TaskDto>> {
        return try {
            val res = taskApi.getTasks(workspaceId)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.failure(Exception("Lỗi tải danh sách công việc"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createTask(task: TaskDto): Result<TaskDto> {
        return try {
            val res = taskApi.createTask(task)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.failure(Exception("Không thể tạo công việc"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTask(id: Long, task: TaskDto): Result<TaskDto> {
        return try {
            val res = taskApi.updateTask(id, task)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.failure(Exception("Không thể cập nhật công việc"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTask(id: Long): Result<Unit> {
        return try {
            val res = taskApi.deleteTask(id)
            if (res.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Không thể xóa công việc"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getEisenhowerMatrix(workspaceId: Long? = null): Result<Map<String, List<TaskDto>>> {
        return try {
            val res = taskApi.getEisenhowerMatrix(workspaceId)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.failure(Exception("Lỗi tải ma trận Eisenhower"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateQuadrant(id: Long, quadrant: String): Result<TaskDto> {
        return try {
            val res = taskApi.updateQuadrant(id, mapOf("quadrant" to quadrant))
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.failure(Exception("Lỗi cập nhật góc phần tư"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun parseAiTask(prompt: String, workspaceId: Long?, currentDateTime: String? = null): Result<AiParseTaskResponse> {
        return try {
            val res = aiApi.parseTask(AiParseTaskRequest(prompt = prompt, workspaceId = workspaceId, currentDateTime = currentDateTime))
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.failure(Exception("AI không thể xử lý câu lệnh"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun autoSchedule(workspaceId: Long?, applyImmediately: Boolean = true): Result<AutoScheduleResponse> {
        return try {
            val request = AutoScheduleRequest(workspaceId = workspaceId, applyImmediately = applyImmediately)
            val res = aiApi.autoSchedule(request)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.failure(Exception("Không thể tự động xếp lịch bằng AI"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun breakdownTask(taskId: Long): Result<List<SubtaskDto>> {
        return try {
            val res = aiApi.breakdownTask(taskId)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.failure(Exception("Không thể phân rã công việc"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
