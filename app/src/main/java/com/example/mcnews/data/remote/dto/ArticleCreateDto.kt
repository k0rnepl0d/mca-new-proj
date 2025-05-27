
package com.example.mcnews.data.remote.dto

import com.squareup.moshi.Json
data class ArticleCreateDto(
    @Json(name = "AuthorId") val authorId: Int,
    @Json(name = "Title") val title: String,
    @Json(name = "Body") val body: String,
    @Json(name = "StatusId") val statusId: Int,
    @Json(name = "TagIds") val tagIds: List<Int>? = null,
    @Json(name = "Image") val image: String? = null
)