package com.example.mcnews.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    @Json(name = "FirstName") val firstName: String,
    @Json(name = "LastName") val lastName: String,
    @Json(name = "MiddleName") val middleName: String?,
    @Json(name = "BirthDate") val birthDate: String,
    @Json(name = "GenderId") val genderId: Int,
    @Json(name = "Email") val email: String,
    @Json(name = "Login") val login: String,
    @Json(name = "Password") val password: String
)