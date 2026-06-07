package ru.fokcont.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ru.fokcont.app.data.repository.NoteRepository
import ru.fokcont.app.data.repository.SessionRepository
import ru.fokcont.app.data.repository.UserRepository

class ViewModelFactory(
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository,
    private val noteRepository: NoteRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(LoginViewModel::class.java) ->
                LoginViewModel(userRepository) as T

            modelClass.isAssignableFrom(SessionViewModel::class.java) ->
                SessionViewModel(sessionRepository) as T

            modelClass.isAssignableFrom(HistoryViewModel::class.java) ->
                HistoryViewModel(sessionRepository) as T

            modelClass.isAssignableFrom(StatsViewModel::class.java) ->
                StatsViewModel(sessionRepository) as T

            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}