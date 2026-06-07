package ru.fokcont.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import ru.fokcont.app.data.db.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity): Long

    @Update
    suspend fun updateSession(session: SessionEntity)

    @Query("SELECT * FROM sessions ORDER BY start_time DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: Int): SessionEntity?

    @Query("SELECT * FROM sessions WHERE end_time IS NULL LIMIT 1")
    suspend fun getActiveSession(): SessionEntity?

    @Query("DELETE FROM sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Int)

    @Query("SELECT COUNT(*) FROM sessions")
    suspend fun getSessionCount(): Int

    @Query("SELECT SUM(duration_sec) FROM sessions")
    suspend fun getTotalDurationSec(): Long?

    @Query("SELECT AVG(duration_sec) FROM sessions")
    suspend fun getAverageDurationSec(): Double?

    @Query("SELECT SUM(switch_count) FROM sessions")
    suspend fun getTotalSwitchCount(): Int?

    @Query("SELECT * FROM sessions WHERE start_time >= :since")
    suspend fun getSessionsSince(since: Long): List<SessionEntity>
}