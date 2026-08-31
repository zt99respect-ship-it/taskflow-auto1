package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val category: String = "Routine",
    val priority: String = "MEDIUM", // HIGH, MEDIUM, LOW
    val dueDateMillis: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false,
    val linkedScriptId: String? = null,
    val linkedScriptName: String? = null,
    val userId: String = "local_user",
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
