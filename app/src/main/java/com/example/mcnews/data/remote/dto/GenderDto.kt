package com.example.mcnews.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GenderDto(
    @Json(name = "GenderId") val genderId: Int,
    @Json(name = "Name") val name: String
)