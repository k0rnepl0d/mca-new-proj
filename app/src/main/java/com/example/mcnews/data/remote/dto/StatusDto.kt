package com.example.mcnews.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StatusDto(
    @Json(name = "StatusId") val statusId: Int,
    @Json(name = "Name")     val name: String
)