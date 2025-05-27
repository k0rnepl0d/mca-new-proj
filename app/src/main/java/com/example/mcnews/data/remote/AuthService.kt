package com.example.mcnews.data.remote

import com.example.mcnews.data.remote.dto.LoginRequest
import com.example.mcnews.data.remote.dto.LoginResponse
import com.example.mcnews.data.remote.dto.RegisterRequest
import com.example.mcnews.data.remote.dto.UserDto
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthService {

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): UserDto

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse
}