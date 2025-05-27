package com.example.mcnews.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ArticleDto(
    @Json(name = "ArticleId") val articleId: Int,
    @Json(name = "Author")    val author: AuthorDto,
    @Json(name = "Title")     val title: String,
    @Json(name = "Body")      val body: String,
    @Json(name = "Image")     val imageBase64: String?,
    @Json(name = "Status")    val status: StatusDto,
    @Json(name = "Tags")      val tags: List<TagDto>,
    @Json(name = "CreatedAt") val createdAt: String,
    @Json(name = "UpdatedAt") val updatedAt: String?
)
