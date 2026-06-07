package ru.fokcont.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ru.fokcont.app.data.db.entity.SessionEntity
import ru.fokcont.app.data.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class SessionState {
    object Idle : SessionState()
    object Running : SessionState()
    object Finished : SessionState()
}

class SessionViewModel(private val sessionRepository: SessionRepository) : ViewModel() {

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Idle)
    val sessionState: StateFlow<SessionState> = _sessionState

    private val _currentSession = MutableStateFlow<SessionEntity?>(null)
    val currentSession: StateFlow<SessionEntity?> = _currentSession

    fun checkActiveSession() {
        viewModelScope.launch {
            val active = sessionRepository.getActiveSession()
            if (active != null) {
                _currentSession.value = active
                _sessionState.value = SessionState.Running
            }
        }
    }

    fun startSession(title: String) {
        viewModelScope.launch {
            val session = SessionEntity(
                title = title,
                startTime = System.currentTimeMillis()
            )
            val id = sessionRepository.insertSession(session)
            _currentSession.value = session.copy(id = id.toInt())
            _sessionState.value = SessionState.Running
        }
    }

    fun incrementSwitchCount() {
        viewModelScope.launch {
            val session = _currentSession.value ?: return@launch
            val updated = session.copy(switchCount = session.switchCount + 1)
            sessionRepository.updateSession(updated)
            _currentSession.value = updated
        }
    }

    fun finishSession(note: String = "") {
        viewModelScope.launch {
            val session = _currentSession.value ?: return@launch
            val endTime = System.currentTimeMillis()
            val durationSec = (endTime - session.startTime) / 1000
            val updated = session.copy(
                endTime = endTime,
                durationSec = durationSec,
                note = note
            )
            sessionRepository.updateSession(updated)
            _currentSession.value = null
            _sessionState.value = SessionState.Finished
        }
    }
}
