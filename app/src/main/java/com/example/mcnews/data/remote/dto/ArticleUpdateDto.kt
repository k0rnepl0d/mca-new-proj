
package com.example.mcnews.data.remote.dto

import com.squareup.moshi.Json
data class ArticleUpdateDto(
    @Json(name = "Title") val title: String? = null,
    @Json(name = "Body") val body: String? = null,
    @Json(name = "StatusId") val statusId: Int? = null,
    @Json(name = "TagIds") val tagIds: List<Int>? = null,
    @Json(name = "Image") val image: String? = null
)