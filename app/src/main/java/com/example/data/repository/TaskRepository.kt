package com.example.data.repository

import com.example.data.local.dao.TaskDao
import com.example.data.local.entity.TaskEntity
import com.example.data.remote.FirebaseManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class TaskRepository(
    private val taskDao: TaskDao,
    private val firebaseManager: FirebaseManager,
    private val externalScope: CoroutineScope
) {
    fun getAllTasks(): Flow<List<TaskEntity>> {
        return taskDao.getAllTasks(firebaseManager.currentUserId)
    }

    fun getTodayTasks(): Flow<List<TaskEntity>> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val endOfDay = calendar.timeInMillis

        return taskDao.getTodayTasks(firebaseManager.currentUserId, startOfDay, endOfDay)
    }

    fun getTotalTasksCount(): Flow<Int> {
        return taskDao.getTotalTasksCount(firebaseManager.currentUserId)
    }

    fun getCompletedTasksCount(): Flow<Int> {
        return taskDao.getCompletedTasksCount(firebaseManager.currentUserId)
    }

    suspend fun insertTask(task: TaskEntity) = withContext(Dispatchers.IO) {
        val taskWithUser = task.copy(userId = firebaseManager.currentUserId)
        taskDao.insertTask(taskWithUser)
        externalScope.launch(Dispatchers.IO) {
            firebaseManager.syncTaskToFirestore(taskWithUser)
        }
    }

    suspend fun updateTask(task: TaskEntity) = withContext(Dispatchers.IO) {
        val updated = task.copy(updatedAt = System.currentTimeMillis())
        taskDao.updateTask(updated)
        externalScope.launch(Dispatchers.IO) {
            firebaseManager.syncTaskToFirestore(updated)
        }
    }

    suspend fun deleteTask(task: TaskEntity) = withContext(Dispatchers.IO) {
        taskDao.deleteTask(task)
        externalScope.launch(Dispatchers.IO) {
            firebaseManager.deleteTaskFromFirestore(task.id)
        }
    }

    suspend fun toggleTaskCompletion(task: TaskEntity): Boolean = withContext(Dispatchers.IO) {
        val newStatus = !task.isCompleted
        val updated = task.copy(
            isCompleted = newStatus,
            updatedAt = System.currentTimeMillis()
        )
        taskDao.updateTask(updated)
        externalScope.launch(Dispatchers.IO) {
            firebaseManager.syncTaskToFirestore(updated)
        }
        newStatus
    }
}
