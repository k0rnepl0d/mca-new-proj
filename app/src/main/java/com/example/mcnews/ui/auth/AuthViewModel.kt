package com.example.mcnews.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mcnews.domain.model.User
import com.example.mcnews.domain.repo.AuthRepository
import com.example.mcnews.utils.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import retrofit2.HttpException
import android.util.Log
import javax.inject.Inject

sealed interface AuthState {
    object Idle : AuthState
    object Loading : AuthState
    object Success : AuthState
    data class Error(val message: String) : AuthState
}

// Для совместимости с существующим кодом
val AuthState.isLoggedIn: Boolean get() = this is AuthState.Success

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _authState = MutableLiveData<AuthState>(AuthState.Idle)
    val authState: LiveData<AuthState> = _authState

    private val _isLoggedIn = MutableLiveData<Boolean>(tokenManager.isLoggedIn())
    val isLoggedIn: LiveData<Boolean> = _isLoggedIn

    fun login(login: String, password: String) = viewModelScope.launch {
        _authState.value = AuthState.Loading
        try {
            val token = authRepository.login(login, password)
            Log.d("AuthViewModel", "Login successful. Token: ${token.take(20)}...")

            // Сохраняем токен
            tokenManager.saveToken(token, login)
            Log.d("AuthViewModel", "Token saved. IsLoggedIn: ${tokenManager.isLoggedIn()}")

            _isLoggedIn.value = true
            _authState.value = AuthState.Success
        } catch (e: retrofit2.HttpException) {
            Log.e("AuthViewModel", "Login failed: ${e.message()}")
            _isLoggedIn.value = false
            val errorMessage = when (e.code()) {
                400 -> {
                    // Попытаемся извлечь детали ошибки из тела ответа
                    try {
                        val errorBody = e.response()?.errorBody()?.string()
                        if (errorBody?.contains("Invalid credentials") == true) {
                            "Неверный логин или пароль"
                        } else {
                            "Ошибка входа: проверьте данные"
                        }
                    } catch (ex: Exception) {
                        "Неверный логин или пароль"
                    }
                }
                401 -> "Неверный логин или пароль"
                500 -> "Ошибка сервера. Попробуйте позже"
                else -> "Ошибка входа: ${e.message()}"
            }
            _authState.value = AuthState.Error(errorMessage)
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Login error: ${e.message}")
            _isLoggedIn.value = false
            _authState.value = AuthState.Error(e.localizedMessage ?: "Ошибка входа")
        }
    }

    fun register(user: User, password: String) = viewModelScope.launch {
        _authState.value = AuthState.Loading
        try {
            val registeredUser = authRepository.register(user, password)
            Log.d("AuthViewModel", "Registration successful")

            // После регистрации автоматически логинимся
            val token = authRepository.login(user.login, password)
            Log.d("AuthViewModel", "Auto-login successful. Token: ${token.take(20)}...")

            tokenManager.saveToken(token, user.login)
            _isLoggedIn.value = true
            _authState.value = AuthState.Success
        } catch (e: retrofit2.HttpException) {
            Log.e("AuthViewModel", "Registration failed: ${e.message()}")
            _isLoggedIn.value = false
            val errorMessage = when (e.code()) {
                400 -> {
                    // Попытаемся извлечь детали ошибки из тела ответа
                    try {
                        val errorBody = e.response()?.errorBody()?.string()
                        when {
                            errorBody?.contains("Login already exists") == true ->
                                "Пользователь с таким логином уже существует"
                            errorBody?.contains("Email already exists") == true ->
                                "Пользователь с таким email уже существует"
                            else -> "Ошибка регистрации: проверьте введенные данные"
                        }
                    } catch (ex: Exception) {
                        "Ошибка регистрации: проверьте введенные данные"
                    }
                }
                500 -> "Ошибка сервера. Попробуйте позже"
                else -> "Ошибка регистрации: ${e.message()}"
            }
            _authState.value = AuthState.Error(errorMessage)
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Registration error: ${e.message}")
            _isLoggedIn.value = false
            _authState.value = AuthState.Error(e.localizedMessage ?: "Ошибка регистрации")
        }
    }

    fun logout() {
        Log.d("AuthViewModel", "Logging out...")
        tokenManager.logout()
        _isLoggedIn.value = false
        _authState.value = AuthState.Idle
    }

    fun checkAuthStatus() {
        val isLoggedIn = tokenManager.isLoggedIn()
        Log.d("AuthViewModel", "Auth status check: $isLoggedIn")
        _isLoggedIn.value = isLoggedIn
    }
}