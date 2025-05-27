// app/src/main/java/com/example/mcnews/data/repo/AuthRepositoryImpl.kt
package com.example.mcnews.data.repo

import com.example.mcnews.data.remote.AuthService
import com.example.mcnews.data.remote.dto.LoginRequest
import com.example.mcnews.data.remote.dto.RegisterRequest
import com.example.mcnews.data.remote.dto.ErrorDto
import com.example.mcnews.domain.model.User
import com.example.mcnews.domain.repo.AuthRepository
import com.squareup.moshi.Moshi
import retrofit2.HttpException
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authService: AuthService,
    private val moshi: Moshi
) : AuthRepository {

    override suspend fun login(login: String, password: String): String {
        try {
            val response = authService.login(LoginRequest(login, password))
            return response.accessToken
        } catch (e: HttpException) {
            // Попытаемся извлечь детали ошибки
            val errorBody = e.response()?.errorBody()?.string()
            if (errorBody != null) {
                try {
                    val errorDto = moshi.adapter(ErrorDto::class.java).fromJson(errorBody)
                    throw Exception(errorDto?.detail ?: e.message())
                } catch (ex: Exception) {
                    // Если не удалось распарсить, используем стандартное сообщение
                    throw Exception("Ошибка входа: ${e.code()}")
                }
            }
            throw e
        }
    }

    override suspend fun register(user: User, password: String): User {
        try {
            val request = RegisterRequest(
                firstName = user.firstName,
                lastName = user.lastName,
                middleName = user.middleName,
                birthDate = user.birthDate,
                genderId = user.genderId,
                email = user.email,
                login = user.login,
                password = password
            )

            val response = authService.register(request)

            return User(
                userId = response.userId,
                firstName = response.firstName,
                lastName = response.lastName,
                middleName = response.middleName,
                birthDate = response.birthDate,
                genderId = response.genderId,
                email = response.email,
                login = response.login,
                createdAt = response.createdAt
            )
        } catch (e: HttpException) {
            // Попытаемся извлечь детали ошибки
            val errorBody = e.response()?.errorBody()?.string()
            if (errorBody != null) {
                try {
                    val errorDto = moshi.adapter(ErrorDto::class.java).fromJson(errorBody)
                    throw Exception(errorDto?.detail ?: e.message())
                } catch (ex: Exception) {
                    // Если не удалось распарсить, используем стандартное сообщение
                    throw Exception("Ошибка регистрации: ${e.code()}")
                }
            }
            throw e
        }
    }
}