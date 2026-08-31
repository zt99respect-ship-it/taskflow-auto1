package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "execution_logs")
data class LogEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val scriptId: String = "",
    val scriptName: String = "Terminal Task",
    val triggerType: String = "MANUAL", // MANUAL, TASK_TRIGGER, WORKMANAGER, FCM, QUICK_ACTION
    val output: String = "",
    val status: String = "SUCCESS", // SUCCESS, ERROR, RUNNING
    val executionDurationMs: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val userId: String = "local_user"
)
