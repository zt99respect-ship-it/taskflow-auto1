package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.LogEntity
import com.example.data.local.entity.ScriptEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.remote.FirebaseManager
import com.example.data.repository.ScriptRepository
import com.example.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class DashboardStats(
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
    val completionPercentage: Float = 0f,
    val activeScriptsCount: Int = 0,
    val firebaseSyncStatus: String = "Online & Synced",
    val lastExecutionStatus: String = "Idle",
    val lastExecutionTime: Long = 0L
)

class MainViewModel(
    private val taskRepository: TaskRepository,
    private val scriptRepository: ScriptRepository,
    private val firebaseManager: FirebaseManager
) : ViewModel() {

    // Tasks Flow
    val allTasks: StateFlow<List<TaskEntity>> = taskRepository.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayTasks: StateFlow<List<TaskEntity>> = taskRepository.getTodayTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Scripts Flow
    val allScripts: StateFlow<List<ScriptEntity>> = scriptRepository.getAllScripts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val quickActionScripts: StateFlow<List<ScriptEntity>> = scriptRepository.getQuickActionScripts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Logs Flow
    val executionLogs: StateFlow<List<LogEntity>> = scriptRepository.getAllLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestLog: StateFlow<LogEntity?> = scriptRepository.getLatestLog()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Running State
    private val _isExecuting = MutableStateFlow(false)
    val isExecuting: StateFlow<Boolean> = _isExecuting.asStateFlow()

    private val _activeTerminalOutput = MutableStateFlow("TaskFlow Automation Engine Ready.\nWaiting for trigger or manual execution...")
    val activeTerminalOutput: StateFlow<String> = _activeTerminalOutput.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow("ALL")
    val selectedCategoryFilter: StateFlow<String> = _selectedCategoryFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Dashboard Statistics calculation
    val dashboardStats: StateFlow<DashboardStats> = combine(
        allTasks,
        allScripts,
        latestLog
    ) { tasks, scripts, lastLog ->
        val total = tasks.size
        val completed = tasks.count { it.isCompleted }
        val percentage = if (total > 0) (completed.toFloat() / total.toFloat()) * 100f else 0f
        val activeScripts = scripts.count { it.isActive }
        val lastStatus = lastLog?.status ?: "Idle"
        val lastTime = lastLog?.timestamp ?: 0L

        DashboardStats(
            totalTasks = total,
            completedTasks = completed,
            completionPercentage = percentage,
            activeScriptsCount = activeScripts,
            firebaseSyncStatus = if (firebaseManager.isUserLoggedIn) "Firebase Connected" else "Local / Guest Mode",
            lastExecutionStatus = lastStatus,
            lastExecutionTime = lastTime
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    fun setCategoryFilter(category: String) {
        _selectedCategoryFilter.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun createTask(
        title: String,
        description: String,
        category: String,
        priority: String,
        dueDateMillis: Long,
        linkedScriptId: String? = null,
        linkedScriptName: String? = null
    ) {
        viewModelScope.launch {
            val task = TaskEntity(
                id = UUID.randomUUID().toString(),
                title = title,
                description = description,
                category = category,
                priority = priority,
                dueDateMillis = dueDateMillis,
                isCompleted = false,
                linkedScriptId = linkedScriptId,
                linkedScriptName = linkedScriptName,
                userId = firebaseManager.currentUserId
            )
            taskRepository.insertTask(task)
        }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.updateTask(task)
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.deleteTask(task)
        }
    }

    fun toggleTask(task: TaskEntity) {
        viewModelScope.launch {
            val newStatus = taskRepository.toggleTaskCompletion(task)
            // If now marked completed and has linked script, trigger automation!
            if (newStatus && !task.linkedScriptId.isNullOrBlank()) {
                _activeTerminalOutput.value = "[Automated Task Trigger] Task '${task.title}' marked COMPLETED.\nLaunching linked automation: ${task.linkedScriptName ?: task.linkedScriptId}...\n"
                val result = scriptRepository.executeScriptById(task.linkedScriptId, triggerType = "TASK_TRIGGER")
                if (result != null) {
                    _activeTerminalOutput.value += "\n" + result.output
                }
            }
        }
    }

    fun createScript(
        name: String,
        description: String,
        type: String,
        content: String,
        webhookMethod: String = "POST",
        webhookHeaders: String = "{}",
        webhookPayload: String = "",
        scheduleType: String = "MANUAL",
        dailyHour: Int = 8,
        dailyMinute: Int = 0,
        isQuickAction: Boolean = true
    ) {
        viewModelScope.launch {
            val script = ScriptEntity(
                id = UUID.randomUUID().toString(),
                name = name,
                description = description,
                type = type,
                content = content,
                webhookMethod = webhookMethod,
                webhookHeaders = webhookHeaders,
                webhookPayload = webhookPayload,
                scheduleType = scheduleType,
                dailyHour = dailyHour,
                dailyMinute = dailyMinute,
                isActive = true,
                isQuickAction = isQuickAction,
                userId = firebaseManager.currentUserId
            )
            scriptRepository.insertScript(script)
        }
    }

    fun updateScript(script: ScriptEntity) {
        viewModelScope.launch {
            scriptRepository.updateScript(script)
        }
    }

    fun deleteScript(script: ScriptEntity) {
        viewModelScope.launch {
            scriptRepository.deleteScript(script)
        }
    }

    fun runScript(script: ScriptEntity, triggerType: String = "MANUAL") {
        _isExecuting.value = true
        _activeTerminalOutput.value = "[*] Dispatching script '${script.name}' (${script.type})...\n"
        viewModelScope.launch {
            try {
                val result = scriptRepository.executeScriptDirectly(script, triggerType)
                _activeTerminalOutput.value = result.output
            } catch (e: Exception) {
                _activeTerminalOutput.value = "[-] Execution Failed: ${e.localizedMessage ?: e.message}"
            } finally {
                _isExecuting.value = false
            }
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            scriptRepository.clearLogs()
            _activeTerminalOutput.value = "[*] Terminal Logs Cleared."
        }
    }

    fun refreshTerminalWithLatestLog() {
        latestLog.value?.let { log ->
            _activeTerminalOutput.value = log.output
        }
    }

    class Factory(
        private val taskRepository: TaskRepository,
        private val scriptRepository: ScriptRepository,
        private val firebaseManager: FirebaseManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(taskRepository, scriptRepository, firebaseManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
