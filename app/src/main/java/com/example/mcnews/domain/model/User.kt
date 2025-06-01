package com.example.mcnews.domain.model

data class User(
    val userId: Int = 0,
    val firstName: String,
    val lastName: String,
    val middleName: String? = null,
    val birthDate: String,
    val genderId: Int,
    val email: String,
    val login: String,
    val createdAt: String = ""
)