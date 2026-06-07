package ru.fokcont.app.data.repository

import kotlinx.coroutines.flow.Flow
import ru.fokcont.app.data.db.dao.SessionDao
import ru.fokcont.app.data.db.entity.SessionEntity

class SessionRepository(private val sessionDao: SessionDao) {

    suspend fun insertSession(session: SessionEntity): Long {
        return sessionDao.insertSession(session)
    }

    suspend fun updateSession(session: SessionEntity) {
        sessionDao.updateSession(session)
    }

    fun getAllSessions(): Flow<List<SessionEntity>> {
        return sessionDao.getAllSessions()
    }

    suspend fun getSessionById(sessionId: Int): SessionEntity? {
        return sessionDao.getSessionById(sessionId)
    }

    suspend fun getActiveSession(): SessionEntity? {
        return sessionDao.getActiveSession()
    }

    suspend fun deleteSession(sessionId: Int) {
        sessionDao.deleteSession(sessionId)
    }

    suspend fun getSessionCount(): Int {
        return sessionDao.getSessionCount()
    }

    suspend fun getTotalDurationSec(): Long {
        return sessionDao.getTotalDurationSec() ?: 0L
    }

    suspend fun getAverageDurationSec(): Double {
        return sessionDao.getAverageDurationSec() ?: 0.0
    }

    suspend fun getTotalSwitchCount(): Int {
        return sessionDao.getTotalSwitchCount() ?: 0
    }

    suspend fun getSessionsSince(since: Long): List<SessionEntity> {
        return sessionDao.getSessionsSince(since)
    }
}
