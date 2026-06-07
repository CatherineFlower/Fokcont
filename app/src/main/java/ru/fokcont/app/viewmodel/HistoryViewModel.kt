package ru.fokcont.app.viewmodel

import androidx.lifecycle.ViewModel
import ru.fokcont.app.data.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import ru.fokcont.app.data.db.entity.SessionEntity

class HistoryViewModel(private val sessionRepository: SessionRepository) : ViewModel() {

    val allSessions: Flow<List<SessionEntity>> = sessionRepository.getAllSessions()
}