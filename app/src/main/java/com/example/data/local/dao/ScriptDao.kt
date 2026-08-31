package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ScriptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScriptDao {
    @Query("SELECT * FROM scripts WHERE userId = :userId ORDER BY updatedAt DESC")
    fun getAllScripts(userId: String): Flow<List<ScriptEntity>>

    @Query("SELECT * FROM scripts WHERE userId = :userId AND isQuickAction = 1 AND isActive = 1 ORDER BY updatedAt DESC")
    fun getQuickActionScripts(userId: String): Flow<List<ScriptEntity>>

    @Query("SELECT * FROM scripts WHERE id = :id LIMIT 1")
    suspend fun getScriptById(id: String): ScriptEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScript(script: ScriptEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScripts(scripts: List<ScriptEntity>)

    @Update
    suspend fun updateScript(script: ScriptEntity)

    @Delete
    suspend fun deleteScript(script: ScriptEntity)

    @Query("DELETE FROM scripts WHERE id = :id")
    suspend fun deleteScriptById(id: String)

    @Query("UPDATE scripts SET lastRunStatus = :status, lastRunTime = :time, updatedAt = :time WHERE id = :id")
    suspend fun updateLastRun(id: String, status: String, time: Long)

    @Query("SELECT COUNT(*) FROM scripts WHERE userId = :userId AND isActive = 1")
    fun getActiveScriptsCount(userId: String): Flow<Int>
}
