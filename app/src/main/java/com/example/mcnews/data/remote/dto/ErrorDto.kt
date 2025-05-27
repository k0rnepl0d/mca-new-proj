// app/src/main/java/com/example/mcnews/data/remote/dto/ErrorDto.kt
package com.example.mcnews.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ErrorDto(
    @Json(name = "detail") val detail: String
)