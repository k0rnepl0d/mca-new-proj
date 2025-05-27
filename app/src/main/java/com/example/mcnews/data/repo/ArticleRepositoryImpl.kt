package com.example.mcnews.data.repo

import com.example.mcnews.data.remote.ApiService
import com.example.mcnews.domain.model.Article
import com.example.mcnews.domain.repo.ArticleRepository
import com.example.mcnews.data.remote.dto.toDomain
import com.example.mcnews.data.remote.dto.toCreateDto
import com.example.mcnews.data.remote.dto.toUpdateDto
import javax.inject.Inject

class ArticleRepositoryImpl @Inject constructor(
    private val api: ApiService
) : ArticleRepository {

    override suspend fun getArticles(
        skip: Int,
        limit: Int,
        status: Int?,
        search: String?,
        tagId: Int?
    ): List<Article> {
        return api.getArticles(skip, limit, status, search, tagId).map { it.toDomain() }
    }

    override suspend fun getArticle(id: Int): Article {
        return api.getArticle(id).toDomain()
    }

    override suspend fun createArticle(article: Article): Article {
        val dto = article.toCreateDto()
        return api.createArticle(dto).toDomain()
    }

    override suspend fun updateArticle(article: Article): Article {
        val dto = article.toUpdateDto()
        return api.updateArticle(article.articleId, dto).toDomain()
    }

    override suspend fun deleteArticle(id: Int) {
        api.deleteArticle(id)
    }
}