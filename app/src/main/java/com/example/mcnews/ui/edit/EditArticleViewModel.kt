package com.example.mcnews.ui.edit

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mcnews.domain.model.Article
import com.example.mcnews.domain.repo.ArticleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import android.util.Base64
import com.example.mcnews.data.remote.ApiService
import javax.inject.Inject

@HiltViewModel
class EditArticleViewModel @Inject constructor(
    private val repo: ArticleRepository,
    private val apiService: ApiService
) : ViewModel() {

    val article = MutableLiveData<Article>()

    fun load(id: Int) {
        viewModelScope.launch {
            try {
                article.value = repo.getArticle(id)
            } catch (e: Exception) {
                // Handle error
                throw e
            }
        }
    }

    suspend fun save(
        articleId: Int,
        title: String,
        body: String,
        statusId: Int,
        authorId: Int,
        imageBase64: String? = null,
        tagIds: List<Int> = emptyList() // ИЗМЕНЕНИЕ: принимаем список тегов
    ): Boolean {
        return try {
            val titlePart = title.toRequestBody("text/plain".toMediaTypeOrNull())
            val bodyPart = body.toRequestBody("text/plain".toMediaTypeOrNull())
            val authorIdPart = authorId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val statusIdPart = statusId.toString().toRequestBody("text/plain".toMediaTypeOrNull())

            // ИСПРАВЛЕНИЕ: Правильно конвертируем теги в JSON строку
            val tagIdsJson = if (tagIds.isNotEmpty()) {
                "[${tagIds.joinToString(",")}]"
            } else {
                "[]"
            }
            val tagIdsPart = tagIdsJson.toRequestBody("text/plain".toMediaTypeOrNull())

            // Обрабатываем изображение
            val imagePart = imageBase64?.let { base64 ->
                try {
                    val imageBytes = Base64.decode(base64, Base64.DEFAULT)
                    val requestBody = imageBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("image", "article_image.jpg", requestBody)
                } catch (e: Exception) {
                    null // Если не удалось декодировать base64, игнорируем изображение
                }
            }

            if (articleId == -1) {
                // Создание новой статьи
                apiService.createArticleMultipart(
                    title = titlePart,
                    body = bodyPart,
                    authorId = authorIdPart,
                    statusId = statusIdPart,
                    tagIds = tagIdsPart,
                    image = imagePart
                )
            } else {
                // Обновление существующей статьи
                apiService.updateArticleMultipart(
                    id = articleId,
                    title = titlePart,
                    body = bodyPart,
                    statusId = statusIdPart,
                    tagIds = tagIdsPart,
                    image = imagePart
                )
            }
            true
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun delete(id: Int) {
        try {
            repo.deleteArticle(id)
        } catch (e: Exception) {
            throw e
        }
    }
}