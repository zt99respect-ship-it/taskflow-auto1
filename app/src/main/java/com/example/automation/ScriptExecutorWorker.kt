package com.example.automation

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.data.local.entity.LogEntity
import com.example.data.remote.FirebaseManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.util.UUID

class ScriptExecutorWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "ScriptExecutorWorker"
        const val KEY_SCRIPT_ID = "key_script_id"
        const val KEY_TRIGGER_TYPE = "key_trigger_type"
        const val CHANNEL_ID = "taskflow_automation_channel"
        const val CHANNEL_NAME = "TaskFlow Automation Notifications"
    }

    override suspend fun doWork(): Result {
        val scriptId = inputData.getString(KEY_SCRIPT_ID) ?: return Result.failure()
        val triggerType = inputData.getString(KEY_TRIGGER_TYPE) ?: "WORKMANAGER"

        Log.d(TAG, "Executing scheduled script: $scriptId triggered by $triggerType")

        val database = AppDatabase.getDatabase(context, CoroutineScope(Dispatchers.IO + SupervisorJob()))
        val scriptDao = database.scriptDao()
        val logDao = database.logDao()
        val firebaseManager = FirebaseManager()

        val script = scriptDao.getScriptById(scriptId) ?: return Result.failure()

        // Update script status to RUNNING
        scriptDao.updateLastRun(script.id, "RUNNING", System.currentTimeMillis())

        val termuxManager = TermuxIntentManager(context)
        val webhookManager = WebhookManager()

        val executionResult: ExecutionResult = when (script.type.uppercase()) {
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

        val finalStatus = if (executionResult.isSuccess) "SUCCESS" else "FAILED"
        val timestamp = System.currentTimeMillis()

        // Update script
        scriptDao.updateLastRun(script.id, finalStatus, timestamp)

        // Insert log
        val logEntity = LogEntity(
            id = UUID.randomUUID().toString(),
            scriptId = script.id,
            scriptName = script.name,
            triggerType = triggerType,
            output = executionResult.output,
            status = if (executionResult.isSuccess) "SUCCESS" else "ERROR",
            executionDurationMs = executionResult.durationMs,
            timestamp = timestamp,
            userId = script.userId
        )
        logDao.insertLog(logEntity)

        // Upload to Firestore
        try {
            firebaseManager.uploadLogToFirestore(logEntity)
            val updatedScript = script.copy(lastRunStatus = finalStatus, lastRunTime = timestamp)
            firebaseManager.syncScriptToFirestore(updatedScript)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing execution to Firestore", e)
        }

        // Send local notification
        sendNotification(script.name, finalStatus, executionResult.output.take(120))

        return if (executionResult.isSuccess) Result.success() else Result.retry()
    }

    private fun sendNotification(scriptName: String, status: String, preview: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Shows execution status of automated TaskFlow scripts"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val icon = if (status == "SUCCESS") {
            android.R.drawable.stat_sys_upload_done
        } else {
            android.R.drawable.stat_notify_error
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle("TaskFlow: $scriptName")
            .setContentText("Status: $status - $preview")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Status: $status\n$preview"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify((System.currentTimeMillis() % 10000).toInt(), notification)
    }
}
