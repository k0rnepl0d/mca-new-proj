package com.example.mcnews.ui.articles

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mcnews.domain.model.Article
import com.example.mcnews.domain.repo.ArticleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import retrofit2.HttpException
import java.io.IOException

sealed interface State {
    object Loading : State
    data class Data(val articles: List<Article>) : State
    data class Error(val message: String) : State
}

@HiltViewModel
class ArticlesViewModel @Inject constructor(
    private val repo: ArticleRepository
) : ViewModel() {

    private val _state = MutableLiveData<State>()
    val state: LiveData<State> = _state

    fun load(status: Int? = null, tagId: Int? = null, search: String? = null, skip: Int = 0, limit: Int = 100) = viewModelScope.launch {
        _state.value = State.Loading
        try {
            val articles = repo.getArticles(skip = skip, limit = limit, status = status, search = search, tagId = tagId)
            _state.value = State.Data(articles)
        } catch (e: HttpException) {
            val errorMessage = when (e.code()) {
                500 -> "Ошибка сервера. Попробуйте позже."
                404 -> "Статьи не найдены"
                else -> "HTTP ошибка: ${e.code()} - ${e.message()}"
            }
            _state.value = State.Error(errorMessage)
        } catch (e: IOException) {
            _state.value = State.Error("Ошибка сети. Проверьте подключение к интернету.")
        } catch (e: Exception) {
            val errorMessage = e.localizedMessage ?: "Неизвестная ошибка: ${e.javaClass.simpleName}"
            _state.value = State.Error(errorMessage)
        }
    }
}