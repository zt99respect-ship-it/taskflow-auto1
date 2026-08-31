package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "scripts")
data class ScriptEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val type: String = "BASH", // BASH, PYTHON, WEBHOOK, TERMUX_INTENT
    val content: String = "", // Script body, Termux command, or Webhook URL
    val webhookMethod: String = "POST", // GET, POST, PUT, DELETE
    val webhookHeaders: String = "{}", // JSON Map string
    val webhookPayload: String = "", // JSON Body
    val scheduleType: String = "MANUAL", // MANUAL, HOURLY, DAILY, SCHEDULED_TIME, FCM_TRIGGER
    val scheduledTimeMillis: Long = 0L,
    val dailyHour: Int = 8,
    val dailyMinute: Int = 0,
    val isActive: Boolean = true,
    val isQuickAction: Boolean = true,
    val lastRunStatus: String = "IDLE", // IDLE, SUCCESS, FAILED, RUNNING
    val lastRunTime: Long = 0L,
    val userId: String = "local_user",
    val updatedAt: Long = System.currentTimeMillis()
)
