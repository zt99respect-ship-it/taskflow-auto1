package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.automation.ScriptExecutorWorker
import com.example.data.remote.FirebaseManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCM_Service"
        private const val CHANNEL_ID = "taskflow_fcm_channel"
        private const val CHANNEL_NAME = "TaskFlow Cloud Triggers"
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Token received: $token")
        serviceScope.launch {
            val firebaseManager = FirebaseManager()
            firebaseManager.updateFcmToken(token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        val data = remoteMessage.data
        val scriptId = data["script_id"] ?: data["scriptId"]
        val triggerTitle = remoteMessage.notification?.title ?: data["title"] ?: "TaskFlow Remote Cloud Trigger"
        val triggerBody = remoteMessage.notification?.body ?: data["message"] ?: "Triggering automated script via FCM"

        showNotification(triggerTitle, triggerBody)

        if (!scriptId.isNullOrBlank()) {
            Log.d(TAG, "Enqueueing script execution for scriptId: $scriptId via FCM")
            val workInput = Data.Builder()
                .putString(ScriptExecutorWorker.KEY_SCRIPT_ID, scriptId)
                .putString(ScriptExecutorWorker.KEY_TRIGGER_TYPE, "FCM_REMOTE")
                .build()

            val workRequest = OneTimeWorkRequestBuilder<ScriptExecutorWorker>()
                .setInputData(workInput)
                .addTag("FCM_EXECUTION")
                .build()

            WorkManager.getInstance(applicationContext).enqueue(workRequest)
        }
    }

    private fun showNotification(title: String, message: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when automation commands arrive from Cloud FCM"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify((System.currentTimeMillis() % 10000).toInt(), notification)
    }
}
