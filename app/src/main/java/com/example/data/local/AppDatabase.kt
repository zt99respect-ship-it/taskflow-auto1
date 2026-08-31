package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.LogDao
import com.example.data.local.dao.ScriptDao
import com.example.data.local.dao.TaskDao
import com.example.data.local.entity.LogEntity
import com.example.data.local.entity.ScriptEntity
import com.example.data.local.entity.TaskEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [TaskEntity::class, ScriptEntity::class, LogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun scriptDao(): ScriptDao
    abstract fun logDao(): LogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "taskflow_auto_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.taskDao(), database.scriptDao(), database.logDao())
                    }
                }
            }

            private suspend fun populateInitialData(taskDao: TaskDao, scriptDao: ScriptDao, logDao: LogDao) {
                val sampleScript1 = ScriptEntity(
                    id = "script_system_check",
                    name = "System Diagnostics",
                    description = "Performs device memory, battery & uptime status checks via Bash",
                    type = "BASH",
                    content = "echo '[*] Starting System Diagnostics...'\necho 'Timestamp: '\$(date)\necho 'Memory Free: '\$(free -m 2>/dev/null || echo 'Available: Normal')\necho 'Uptime: '\$(uptime 2>/dev/null || echo 'Active')\necho '[+] Diagnostics complete. All systems operational.'",
                    scheduleType = "DAILY",
                    dailyHour = 9,
                    dailyMinute = 0,
                    isActive = true,
                    isQuickAction = true,
                    lastRunStatus = "SUCCESS",
                    lastRunTime = System.currentTimeMillis() - 3600000L
                )

                val sampleScript2 = ScriptEntity(
                    id = "script_webhook_sync",
                    name = "Cloud Webhook Trigger",
                    description = "Sends automation dispatch payload to HTTP endpoint",
                    type = "WEBHOOK",
                    content = "https://httpbin.org/post",
                    webhookMethod = "POST",
                    webhookHeaders = "{\"Content-Type\":\"application/json\", \"X-TaskFlow-Client\":\"Android\"}",
                    webhookPayload = "{\"event\":\"task_auto_completed\", \"source\":\"TaskFlow_Mobile\", \"status\":\"verified\"}",
                    scheduleType = "MANUAL",
                    isActive = true,
                    isQuickAction = true,
                    lastRunStatus = "SUCCESS",
                    lastRunTime = System.currentTimeMillis() - 7200000L
                )

                val sampleScript3 = ScriptEntity(
                    id = "script_termux_backup",
                    name = "Termux Workspace Sync",
                    description = "Triggers local Termux workflow intent for workspace sync",
                    type = "TERMUX_INTENT",
                    content = "termux-backup.sh",
                    scheduleType = "HOURLY",
                    isActive = true,
                    isQuickAction = true,
                    lastRunStatus = "IDLE",
                    lastRunTime = 0L
                )

                val sampleScript4 = ScriptEntity(
                    id = "script_python_report",
                    name = "Python Analytics Job",
                    description = "Runs local Python data calculation routine",
                    type = "PYTHON",
                    content = "import datetime\nprint(f'[PY] TaskFlow Automation Analytics Generated at {datetime.datetime.now()}')\nprint('[PY] Productivity Index: 98.4%')\nprint('[PY] Success rate: 100%')",
                    scheduleType = "MANUAL",
                    isActive = true,
                    isQuickAction = false,
                    lastRunStatus = "IDLE",
                    lastRunTime = 0L
                )

                scriptDao.insertScripts(listOf(sampleScript1, sampleScript2, sampleScript3, sampleScript4))

                val sampleTask1 = TaskEntity(
                    id = "task_morning_sync",
                    title = "Morning Workflow Diagnostics",
                    description = "Run automated server diagnostics & check local cache",
                    category = "Automation",
                    priority = "HIGH",
                    dueDateMillis = System.currentTimeMillis(),
                    isCompleted = false,
                    linkedScriptId = "script_system_check",
                    linkedScriptName = "System Diagnostics"
                )

                val sampleTask2 = TaskEntity(
                    id = "task_deploy_webhook",
                    title = "Send Deployment Webhook",
                    description = "Notify cloud services upon code milestone completion",
                    category = "Dev",
                    priority = "MEDIUM",
                    dueDateMillis = System.currentTimeMillis() + 3600000L,
                    isCompleted = false,
                    linkedScriptId = "script_webhook_sync",
                    linkedScriptName = "Cloud Webhook Trigger"
                )

                val sampleTask3 = TaskEntity(
                    id = "task_review_logs",
                    title = "Review Security & Automation Logs",
                    description = "Ensure all scheduled WorkManager jobs executed smoothly",
                    category = "Routine",
                    priority = "LOW",
                    dueDateMillis = System.currentTimeMillis() + 86400000L,
                    isCompleted = true,
                    linkedScriptId = null,
                    linkedScriptName = null
                )

                taskDao.insertTasks(listOf(sampleTask1, sampleTask2, sampleTask3))

                val initialLog = LogEntity(
                    scriptId = "script_system_check",
                    scriptName = "System Diagnostics",
                    triggerType = "MANUAL",
                    output = "[*] Starting System Diagnostics...\nTimestamp: Sun Aug 30 18:30:00 UTC\nUptime: System operational\nMemory Free: Available\n[+] Diagnostics complete. All systems operational.",
                    status = "SUCCESS",
                    executionDurationMs = 142L,
                    timestamp = System.currentTimeMillis() - 3600000L
                )
                logDao.insertLog(initialLog)
            }
        }
    }
}
