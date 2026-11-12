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
                val msg = com.hhpc.smarttask.data.api.ApiErrorParser.parseError(res, "Không thể tải danh sách công việc.")
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            val msg = com.hhpc.smarttask.data.api.ApiErrorParser.parseException(e, "Không thể kết nối đến máy chủ.")
            Result.failure(Exception(msg))
        }
    }

    suspend fun createTask(task: TaskDto): Result<TaskDto> {
        return try {
            val res = taskApi.createTask(task)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                val msg = com.hhpc.smarttask.data.api.ApiErrorParser.parseError(res, "Không thể tạo công việc mới.")
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            val msg = com.hhpc.smarttask.data.api.ApiErrorParser.parseException(e, "Lỗi kết nối khi tạo công việc.")
            Result.failure(Exception(msg))
        }
    }

    suspend fun updateTask(id: Long, task: TaskDto): Result<TaskDto> {
        return try {
            val res = taskApi.updateTask(id, task)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                val msg = com.hhpc.smarttask.data.api.ApiErrorParser.parseError(res, "Không thể cập nhật công việc.")
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            val msg = com.hhpc.smarttask.data.api.ApiErrorParser.parseException(e, "Lỗi kết nối khi cập nhật công việc.")
            Result.failure(Exception(msg))
        }
    }

    suspend fun deleteTask(id: Long): Result<Unit> {
        return try {
            val res = taskApi.deleteTask(id)
            if (res.isSuccessful) {
                Result.success(Unit)
            } else {
                val msg = com.hhpc.smarttask.data.api.ApiErrorParser.parseError(res, "Không thể xóa công việc.")
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            val msg = com.hhpc.smarttask.data.api.ApiErrorParser.parseException(e, "Lỗi kết nối khi xóa công việc.")
            Result.failure(Exception(msg))
        }
    }

    suspend fun getEisenhowerMatrix(workspaceId: Long? = null): Result<Map<String, List<TaskDto>>> {
        return try {
            val res = taskApi.getEisenhowerMatrix(workspaceId)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                val msg = com.hhpc.smarttask.data.api.ApiErrorParser.parseError(res, "Không thể tải ma trận Eisenhower.")
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            val msg = com.hhpc.smarttask.data.api.ApiErrorParser.parseException(e, "Lỗi kết nối khi tải ma trận.")
            Result.failure(Exception(msg))
        }
    }

    suspend fun updateQuadrant(id: Long, quadrant: String): Result<TaskDto> {
        return try {
            val res = taskApi.updateQuadrant(id, mapOf("quadrant" to quadrant))
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                val msg = com.hhpc.smarttask.data.api.ApiErrorParser.parseError(res, "Không thể cập nhật góc phần tư.")
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            val msg = com.hhpc.smarttask.data.api.ApiErrorParser.parseException(e, "Lỗi kết nối khi cập nhật góc phần tư.")
            Result.failure(Exception(msg))
        }
    }

    suspend fun parseAiTask(prompt: String, workspaceId: Long?, currentDateTime: String? = null): Result<AiParseTaskResponse> {
        return try {
            val res = aiApi.parseTask(AiParseTaskRequest(prompt = prompt, workspaceId = workspaceId, currentDateTime = currentDateTime))
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                val msg = com.hhpc.smarttask.data.api.ApiErrorParser.parseError(res, "Trợ lý AI chưa thể phân tích câu lệnh này. Vui lòng diễn đạt lại.")
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            val msg = com.hhpc.smarttask.data.api.ApiErrorParser.parseException(e, "Không thể kết nối đến máy chủ AI.")
            Result.failure(Exception(msg))
        }
    }

    suspend fun autoSchedule(workspaceId: Long?, applyImmediately: Boolean = true): Result<AutoScheduleResponse> {
        return try {
            val request = AutoScheduleRequest(workspaceId = workspaceId, applyImmediately = applyImmediately)
            val res = aiApi.autoSchedule(request)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                val msg = com.hhpc.smarttask.data.api.ApiErrorParser.parseError(res, "Không thể tự động xếp lịch bằng AI vào lúc này.")
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            val msg = com.hhpc.smarttask.data.api.ApiErrorParser.parseException(e, "Lỗi kết nối khi xếp lịch AI.")
            Result.failure(Exception(msg))
        }
    }

    suspend fun breakdownTask(taskId: Long): Result<List<SubtaskDto>> {
        return try {
            val res = aiApi.breakdownTask(taskId)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                val msg = com.hhpc.smarttask.data.api.ApiErrorParser.parseError(res, "Không thể phân rã công việc này.")
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            val msg = com.hhpc.smarttask.data.api.ApiErrorParser.parseException(e, "Lỗi kết nối khi phân rã công việc.")
            Result.failure(Exception(msg))
        }
    }

    suspend fun getSubtasks(taskId: Long): Result<List<SubtaskDto>> {
        return try {
            val res = taskApi.getSubtasks(taskId)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                val msg = com.hhpc.smarttask.data.api.ApiErrorParser.parseError(res, "Không thể tải danh sách bước con.")
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            val msg = com.hhpc.smarttask.data.api.ApiErrorParser.parseException(e, "Lỗi kết nối khi tải bước con.")
            Result.failure(Exception(msg))
        }
    }

    suspend fun createSubtask(taskId: Long, subtask: SubtaskDto): Result<SubtaskDto> {
        return try {
            val res = taskApi.createSubtask(taskId, subtask)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                val msg = com.hhpc.smarttask.data.api.ApiErrorParser.parseError(res, "Không thể tạo bước con.")
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            val msg = com.hhpc.smarttask.data.api.ApiErrorParser.parseException(e, "Lỗi kết nối khi tạo bước con.")
            Result.failure(Exception(msg))
        }
    }

    suspend fun toggleSubtask(subtaskId: Long): Result<SubtaskDto> {
        return try {
            val res = taskApi.toggleSubtask(subtaskId)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                val msg = com.hhpc.smarttask.data.api.ApiErrorParser.parseError(res, "Không thể cập nhật bước con.")
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            val msg = com.hhpc.smarttask.data.api.ApiErrorParser.parseException(e, "Lỗi kết nối khi cập nhật bước con.")
            Result.failure(Exception(msg))
        }
    }
}
