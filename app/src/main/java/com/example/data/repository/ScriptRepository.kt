package com.example.data.repository

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.automation.ExecutionResult
import com.example.automation.ScriptExecutorWorker
import com.example.automation.TermuxIntentManager
import com.example.automation.WebhookManager
import com.example.data.local.dao.LogDao
import com.example.data.local.dao.ScriptDao
import com.example.data.local.entity.LogEntity
import com.example.data.local.entity.ScriptEntity
import com.example.data.remote.FirebaseManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.UUID
import java.util.concurrent.TimeUnit

class ScriptRepository(
    private val context: Context,
    private val scriptDao: ScriptDao,
    private val logDao: LogDao,
    private val firebaseManager: FirebaseManager,
    private val externalScope: CoroutineScope
) {
    companion object {
        private const val TAG = "ScriptRepository"
    }

    private val termuxManager = TermuxIntentManager(context)
    private val webhookManager = WebhookManager()
    private val workManager = WorkManager.getInstance(context)

    fun getAllScripts(): Flow<List<ScriptEntity>> {
        return scriptDao.getAllScripts(firebaseManager.currentUserId)
    }

    fun getQuickActionScripts(): Flow<List<ScriptEntity>> {
        return scriptDao.getQuickActionScripts(firebaseManager.currentUserId)
    }

    fun getAllLogs(): Flow<List<LogEntity>> {
        return logDao.getAllLogs(firebaseManager.currentUserId)
    }

    fun getLatestLog(): Flow<LogEntity?> {
        return logDao.getLatestLog(firebaseManager.currentUserId)
    }

    fun getActiveScriptsCount(): Flow<Int> {
        return scriptDao.getActiveScriptsCount(firebaseManager.currentUserId)
    }

    suspend fun insertScript(script: ScriptEntity) = withContext(Dispatchers.IO) {
        val scriptWithUser = script.copy(userId = firebaseManager.currentUserId)
        scriptDao.insertScript(scriptWithUser)
        scheduleScriptWorkManager(scriptWithUser)
        externalScope.launch(Dispatchers.IO) {
            firebaseManager.syncScriptToFirestore(scriptWithUser)
        }
    }

    suspend fun updateScript(script: ScriptEntity) = withContext(Dispatchers.IO) {
        val updated = script.copy(updatedAt = System.currentTimeMillis())
        scriptDao.updateScript(updated)
        scheduleScriptWorkManager(updated)
        externalScope.launch(Dispatchers.IO) {
            firebaseManager.syncScriptToFirestore(updated)
        }
    }

    suspend fun deleteScript(script: ScriptEntity) = withContext(Dispatchers.IO) {
        cancelScriptSchedule(script.id)
        scriptDao.deleteScript(script)
        externalScope.launch(Dispatchers.IO) {
            firebaseManager.deleteScriptFromFirestore(script.id)
        }
    }

    suspend fun clearLogs() = withContext(Dispatchers.IO) {
        logDao.clearLogs(firebaseManager.currentUserId)
    }

    suspend fun executeScriptDirectly(
        script: ScriptEntity,
        triggerType: String = "MANUAL"
    ): ExecutionResult = withContext(Dispatchers.IO) {
        // Mark as RUNNING
        scriptDao.updateLastRun(script.id, "RUNNING", System.currentTimeMillis())

        val result: ExecutionResult = when (script.type.uppercase()) {
            "WEBHOOK" -> {
                webhookManager.executeWebhook(
                    url = script.content,
                    method = script.webhookMethod,
                    headersJson = script.webhookHeaders,
                    payloadJson = script.webhookPayload
                )
            }
            else -> {
                termuxManager.executeScriptLocally(
                    scriptType = script.type,
                    content = script.content
                )
            }
        }

        val status = if (result.isSuccess) "SUCCESS" else "FAILED"
        val timestamp = System.currentTimeMillis()
        scriptDao.updateLastRun(script.id, status, timestamp)

        val log = LogEntity(
            id = UUID.randomUUID().toString(),
            scriptId = script.id,
            scriptName = script.name,
            triggerType = triggerType,
            output = result.output,
            status = if (result.isSuccess) "SUCCESS" else "ERROR",
            executionDurationMs = result.durationMs,
            timestamp = timestamp,
            userId = firebaseManager.currentUserId
        )
        logDao.insertLog(log)

        externalScope.launch(Dispatchers.IO) {
            try {
                firebaseManager.uploadLogToFirestore(log)
                val updated = script.copy(lastRunStatus = status, lastRunTime = timestamp)
                firebaseManager.syncScriptToFirestore(updated)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload log or script status", e)
            }
        }

        result
    }

    suspend fun executeScriptById(scriptId: String, triggerType: String = "TASK_TRIGGER"): ExecutionResult? = withContext(Dispatchers.IO) {
        val script = scriptDao.getScriptById(scriptId) ?: return@withContext null
        executeScriptDirectly(script, triggerType)
    }

    fun scheduleScriptWorkManager(script: ScriptEntity) {
        if (!script.isActive) {
            cancelScriptSchedule(script.id)
            return
        }

        val uniqueWorkName = "taskflow_script_${script.id}"
        val workInput = Data.Builder()
            .putString(ScriptExecutorWorker.KEY_SCRIPT_ID, script.id)
            .putString(ScriptExecutorWorker.KEY_TRIGGER_TYPE, "WORKMANAGER")
            .build()

        when (script.scheduleType.uppercase()) {
            "HOURLY" -> {
                val periodicWork = PeriodicWorkRequestBuilder<ScriptExecutorWorker>(1, TimeUnit.HOURS)
                    .setInputData(workInput)
                    .build()
                workManager.enqueueUniquePeriodicWork(
                    uniqueWorkName,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    periodicWork
                )
            }
            "DAILY" -> {
                val currentDate = Calendar.getInstance()
                val dueDate = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, script.dailyHour)
                    set(Calendar.MINUTE, script.dailyMinute)
                    set(Calendar.SECOND, 0)
                }
                if (dueDate.before(currentDate)) {
                    dueDate.add(Calendar.HOUR_OF_DAY, 24)
                }
                val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis

                val dailyWork = PeriodicWorkRequestBuilder<ScriptExecutorWorker>(24, TimeUnit.HOURS)
                    .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
                    .setInputData(workInput)
                    .build()

                workManager.enqueueUniquePeriodicWork(
                    uniqueWorkName,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    dailyWork
                )
            }
            "SCHEDULED_TIME" -> {
                val delay = script.scheduledTimeMillis - System.currentTimeMillis()
                if (delay > 0) {
                    val oneTimeWork = OneTimeWorkRequestBuilder<ScriptExecutorWorker>()
                        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                        .setInputData(workInput)
                        .addTag(uniqueWorkName)
                        .build()
                    workManager.enqueue(oneTimeWork)
                }
            }
            else -> {
                cancelScriptSchedule(script.id)
            }
        }
    }

    fun cancelScriptSchedule(scriptId: String) {
        workManager.cancelUniqueWork("taskflow_script_$scriptId")
    }
}
