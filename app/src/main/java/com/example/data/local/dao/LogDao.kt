package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.LogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Query("SELECT * FROM execution_logs WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllLogs(userId: String): Flow<List<LogEntity>>

    @Query("SELECT * FROM execution_logs WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(userId: String, limit: Int = 50): Flow<List<LogEntity>>

    @Query("SELECT * FROM execution_logs WHERE userId = :userId ORDER BY timestamp DESC LIMIT 1")
    fun getLatestLog(userId: String): Flow<LogEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: LogEntity)

    @Query("DELETE FROM execution_logs WHERE userId = :userId")
    suspend fun clearLogs(userId: String)
}
