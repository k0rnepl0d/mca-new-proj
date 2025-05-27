package com.example.mcnews.domain.repo

import com.example.mcnews.domain.model.User

interface AuthRepository {
    suspend fun login(login: String, password: String): String // returns token
    suspend fun register(user: User, password: String): User
}