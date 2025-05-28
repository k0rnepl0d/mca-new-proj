package com.example.mcnews.domain.model

data class Article(
    val articleId: Int,
    val authorId: Int,
    val title: String,
    val body: String,
    val imageUrl: String?,
    val statusId: Int,
    val createdAt: String,
    val tags: List<Tag> = emptyList(),
    val authorName: String? = null
)