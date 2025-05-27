package com.example.mcnews.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AuthorDto(
    @Json(name = "UserId") val userId: Int,
    @Json(name = "FirstName") val firstName: String,
    @Json(name = "LastName") val lastName: String,
    @Json(name = "MiddleName") val middleName: String? = null,
    @Json(name = "BirthDate") val birthDate: String,
    @Json(name = "GenderId") val genderId: Int,
    @Json(name = "Email") val email: String,
    @Json(name = "Login") val login: String,
    @Json(name = "CreatedAt") val createdAt: String
)