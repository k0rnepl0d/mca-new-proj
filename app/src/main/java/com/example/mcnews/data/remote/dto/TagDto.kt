package com.example.mcnews.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TagDto(
    @Json(name = "TagId") val tagId: Int,
    @Json(name = "Name")  val name: String
)