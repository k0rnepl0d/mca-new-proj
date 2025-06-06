package com.example.mcnews.data.remote.dto

import com.example.mcnews.domain.model.Article
import com.example.mcnews.domain.model.Tag

fun ArticleDto.toDomain() = Article(
    articleId = articleId,
    authorId  = author.userId,
    title     = title,
    body      = body,
    imageUrl  = imageBase64,
    statusId  = status.statusId,
    createdAt = createdAt,
    tags      = tags.map { Tag(it.tagId, it.name) },
    authorName = "${author.firstName} ${author.lastName}".trim()
)

fun Article.toCreateDto() = ArticleCreateDto(
    authorId = authorId,
    title    = title,
    body     = body,
    statusId = statusId,
    tagIds   = null,
    image    = null
)

fun Article.toUpdateDto() = ArticleUpdateDto(
    title    = title,
    body     = body,
    statusId = statusId,
    tagIds   = null,
    image    = null
)

fun TagDto.toDomain() = Tag(tagId = tagId, name = name)