package com.hhpc.smarttask.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hhpc.smarttask.data.api.ApiClient
import com.hhpc.smarttask.data.model.Category
import com.hhpc.smarttask.data.model.ProductivityReportDto
import com.hhpc.smarttask.data.model.TaskDto
import com.hhpc.smarttask.data.model.UserPreferenceDto
import com.hhpc.smarttask.data.model.Workspace
import com.hhpc.smarttask.data.repository.ProductivityRepository
import com.hhpc.smarttask.data.repository.TaskRepository
import com.hhpc.smarttask.data.repository.UserPreferenceRepository
import com.hhpc.smarttask.data.repository.WorkspaceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val taskRepository = TaskRepository(application)
    private val workspaceRepository = WorkspaceRepository(application)
    private val productivityRepository = ProductivityRepository(application)
    private val userPreferenceRepository = UserPreferenceRepository(application)
    private val categoryApi = ApiClient.getCategoryApi(application)

    private val _tasks = MutableStateFlow<List<TaskDto>>(emptyList())
    val tasks: StateFlow<List<TaskDto>> = _tasks.asStateFlow()

    private val _workspaces = MutableStateFlow<List<Workspace>>(emptyList())
    val workspaces: StateFlow<List<Workspace>> = _workspaces.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _selectedWorkspaceId = MutableStateFlow<Long?>(null)
    val selectedWorkspaceId: StateFlow<Long?> = _selectedWorkspaceId.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _eisenhowerMap = MutableStateFlow<Map<String, List<TaskDto>>>(emptyMap())
    val eisenhowerMap: StateFlow<Map<String, List<TaskDto>>> = _eisenhowerMap.asStateFlow()

    private val _productivityReport = MutableStateFlow<ProductivityReportDto?>(null)
    val productivityReport: StateFlow<ProductivityReportDto?> = _productivityReport.asStateFlow()

    private val _userPreferences = MutableStateFlow(UserPreferenceDto())
    val userPreferences: StateFlow<UserPreferenceDto> = _userPreferences.asStateFlow()

    private val _selectedFocusTask = MutableStateFlow<TaskDto?>(null)
    val selectedFocusTask: StateFlow<TaskDto?> = _selectedFocusTask.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            loadWorkspaces()
            loadCategories()
            loadTasks()
            loadEisenhower()
            loadProductivityReport()
            loadUserPreferences()
            _isLoading.value = false
        }
    }

    fun selectWorkspace(workspaceId: Long?) {
        _selectedWorkspaceId.value = workspaceId
        loadCategories()
        loadTasks()
        loadEisenhower()
    }

    fun selectFocusTask(task: TaskDto?) {
        _selectedFocusTask.value = task
    }

    fun loadWorkspaces() {
        viewModelScope.launch {
            val res = workspaceRepository.getWorkspaces()
            res.onSuccess { list ->
                _workspaces.value = list
                if (_selectedWorkspaceId.value == null && list.isNotEmpty()) {
                    _selectedWorkspaceId.value = list.first().id
                }
            }
        }
    }

    fun loadCategories() {
        viewModelScope.launch {
            try {
                val res = categoryApi.getCategories(_selectedWorkspaceId.value)
                if (res.isSuccessful && res.body() != null) {
                    _categories.value = res.body()!!
                }
            } catch (e: Exception) {
                // Ignore fallback
            }
        }
    }

    fun loadTasks() {
        viewModelScope.launch {
            _isLoading.value = true
            val res = taskRepository.getTasks(_selectedWorkspaceId.value)
            res.onSuccess { list ->
                _tasks.value = list
            }
            _isLoading.value = false
        }
    }

    fun loadEisenhower() {
        viewModelScope.launch {
            val res = taskRepository.getEisenhowerMatrix(_selectedWorkspaceId.value)
            res.onSuccess { map ->
                _eisenhowerMap.value = map
            }
        }
    }

    fun loadProductivityReport() {
        viewModelScope.launch {
            val res = productivityRepository.getDailyReport()
            res.onSuccess { report ->
                _productivityReport.value = report
            }
        }
    }

    fun loadUserPreferences() {
        viewModelScope.launch {
            val res = userPreferenceRepository.getPreferences()
            res.onSuccess { pref ->
                _userPreferences.value = pref
            }
        }
    }

    fun updateUserPreferences(pref: UserPreferenceDto) {
        viewModelScope.launch {
            val res = userPreferenceRepository.updatePreferences(pref)
            res.onSuccess { updated ->
                _userPreferences.value = updated
            }
        }
    }

    fun toggleTaskCompleted(task: TaskDto) {
        val newStatus = if (task.status == "COMPLETED") "TODO" else "COMPLETED"
        val updated = task.copy(status = newStatus)
        viewModelScope.launch {
            val res = taskRepository.updateTask(task.id ?: return@launch, updated)
            res.onSuccess {
                loadTasks()
                loadEisenhower()
                loadProductivityReport()
            }
        }
    }

    fun updateTask(id: Long, updated: TaskDto) {
        viewModelScope.launch {
            val res = taskRepository.updateTask(id, updated)
            res.onSuccess {
                loadTasks()
                loadEisenhower()
                loadProductivityReport()
            }
        }
    }

    fun createTask(title: String, description: String?, priority: String, quadrant: String) {
        if (title.isBlank()) return
        val newTask = TaskDto(
            title = title,
            description = description,
            priority = priority,
            status = "TODO",
            eisenhowerQuadrant = quadrant,
            workspaceId = _selectedWorkspaceId.value
        )
        viewModelScope.launch {
            val res = taskRepository.createTask(newTask)
            res.onSuccess {
                loadTasks()
                loadEisenhower()
            }
        }
    }

    fun createWorkspace(name: String, iconName: String? = "Briefcase") {
        if (name.isBlank()) return
        val newWs = Workspace(name = name, iconName = iconName)
        viewModelScope.launch {
            val res = workspaceRepository.createWorkspace(newWs)
            res.onSuccess {
                loadWorkspaces()
            }
        }
    }

    fun createCategory(name: String, colorCode: String = "#10B981", iconName: String = "Folder") {
        if (name.isBlank()) return
        val newCat = Category(
            name = name,
            colorCode = colorCode,
            iconName = iconName,
            workspaceId = _selectedWorkspaceId.value
        )
        viewModelScope.launch {
            try {
                val res = categoryApi.createCategory(newCat, _selectedWorkspaceId.value)
                if (res.isSuccessful) {
                    loadCategories()
                }
            } catch (e: Exception) {
                // Ignore fallback
            }
        }
    }

    fun deleteTask(id: Long) {
        viewModelScope.launch {
            val res = taskRepository.deleteTask(id)
            res.onSuccess {
                loadTasks()
                loadEisenhower()
                loadProductivityReport()
            }
        }
    }

    fun updateQuadrant(taskId: Long, newQuadrant: String) {
        viewModelScope.launch {
            val res = taskRepository.updateQuadrant(taskId, newQuadrant)
            res.onSuccess {
                loadTasks()
                loadEisenhower()
            }
        }
    }
}
