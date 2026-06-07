package ru.fokcont.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import ru.fokcont.app.data.db.entity.DistractionEventEntity

@Dao
interface DistractionEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: DistractionEventEntity): Long

    @Update
    suspend fun updateEvent(event: DistractionEventEntity)

    @Query("SELECT * FROM distraction_events WHERE session_id = :sessionId ORDER BY start_time ASC")
    suspend fun getEventsForSession(sessionId: Int): List<DistractionEventEntity>

    @Query("SELECT * FROM distraction_events WHERE session_id = :sessionId AND package_name = :packageName AND duration_ms = 0 ORDER BY start_time DESC LIMIT 1")
    suspend fun getUnfinishedEvent(sessionId: Int, packageName: String): DistractionEventEntity?
}
