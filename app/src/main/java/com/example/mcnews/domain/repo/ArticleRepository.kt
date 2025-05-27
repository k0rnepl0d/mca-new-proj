package com.example.mcnews.domain.repo

import com.example.mcnews.domain.model.Article

interface ArticleRepository {
    suspend fun getArticles(
        skip: Int = 0,
        limit: Int = 100,
        status: Int? = null,
        search: String? = null,
        tagId: Int? = null
    ): List<Article>
    suspend fun getArticle(id: Int): Article
    suspend fun createArticle(article: Article): Article
    suspend fun updateArticle(article: Article): Article
    suspend fun deleteArticle(id: Int)
}