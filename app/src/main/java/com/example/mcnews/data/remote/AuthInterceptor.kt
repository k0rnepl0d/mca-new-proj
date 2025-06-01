package com.example.mcnews.data.remote

import com.example.mcnews.utils.TokenManager
import okhttp3.Interceptor
import okhttp3.Response
import android.util.Log
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val token = tokenManager.getToken()

        Log.d("AuthInterceptor", "Token: ${if (token != null) "Present" else "Null"}")
        Log.d("AuthInterceptor", "Request URL: ${originalRequest.url}")

        return if (token != null) {
            val authenticatedRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
            Log.d("AuthInterceptor", "Added Authorization header")
            chain.proceed(authenticatedRequest)
        } else {
            Log.d("AuthInterceptor", "No token available")
            chain.proceed(originalRequest)
        }
    }
}