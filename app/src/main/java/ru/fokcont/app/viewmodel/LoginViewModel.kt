package ru.fokcont.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ru.fokcont.app.data.db.entity.UserEntity
import ru.fokcont.app.data.repository.UserRepository
import ru.fokcont.app.security.HashUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class LoginState {
    object Idle : LoginState()
    object NeedSetup : LoginState()
    object Success : LoginState()
    object WrongPin : LoginState()
    object Loading : LoginState()
}

class LoginViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    fun checkIfUserExists() {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            val hasUser = userRepository.hasUser()
            _loginState.value = if (hasUser) LoginState.Idle else LoginState.NeedSetup
        }
    }

    fun setupPin(pin: String) {
        viewModelScope.launch {
            val hash = HashUtils.hashPin(pin)
            userRepository.insertUser(UserEntity(pinHash = hash))
            _loginState.value = LoginState.Success
        }
    }

    fun verifyPin(pin: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            val user = userRepository.getUser()
            if (user != null && HashUtils.verifyPin(pin, user.pinHash)) {
                _loginState.value = LoginState.Success
            } else {
                _loginState.value = LoginState.WrongPin
            }
        }
    }
}