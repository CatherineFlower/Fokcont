package ru.fokcont.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ru.fokcont.app.data.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class StatsData(
    val totalSessions: Int = 0,
    val totalDurationSec: Long = 0L,
    val averageDurationSec: Double = 0.0,
    val totalSwitchCount: Int = 0
)

class StatsViewModel(private val sessionRepository: SessionRepository) : ViewModel() {

    private val _stats = MutableStateFlow(StatsData())
    val stats: StateFlow<StatsData> = _stats

    fun loadStats() {
        viewModelScope.launch {
            _stats.value = StatsData(
                totalSessions = sessionRepository.getSessionCount(),
                totalDurationSec = sessionRepository.getTotalDurationSec(),
                averageDurationSec = sessionRepository.getAverageDurationSec(),
                totalSwitchCount = sessionRepository.getTotalSwitchCount()
            )
        }
    }
}