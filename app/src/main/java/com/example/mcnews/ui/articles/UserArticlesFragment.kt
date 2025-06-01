package com.example.mcnews.ui.articles

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mcnews.data.remote.ApiService
import com.example.mcnews.databinding.FragmentArticlesBinding
import com.example.mcnews.ui.edit.EditArticleActivity
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class UserArticlesFragment : Fragment() {
    private var _binding: FragmentArticlesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ArticlesViewModel by viewModels()

    @Inject lateinit var api: ApiService

    private val editArticleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            loadUserArticles()
        }
    }

    private val adapter = ArticlesAdapter(
        onClick = { article ->
            startActivity(Intent(requireContext(), com.example.mcnews.ui.articles.ArticleDetailActivity::class.java).apply {
                putExtra("title", article.title)
                putExtra("body", article.body)
                putExtra("imageUrl", article.imageUrl)
                putExtra("author", "Вы")
                putExtra("createdAt", article.createdAt)
                putStringArrayListExtra("tags", ArrayList(article.tags.map { it.name }))
            })
        },
        onLongClick = { article ->
            editArticleLauncher.launch(Intent(requireContext(), EditArticleActivity::class.java).apply {
                putExtra("articleId", article.articleId)
            })
        }
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentArticlesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.searchView.visibility = View.GONE

        binding.fabAdd.visibility = View.VISIBLE
        binding.fabAdd.setOnClickListener {
            editArticleLauncher.launch(Intent(requireContext(), EditArticleActivity::class.java))
        }

        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter

        binding.swipe.setOnRefreshListener {
            loadUserArticles()
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is State.Loading -> binding.swipe.isRefreshing = true
                is State.Data -> {
                    binding.swipe.isRefreshing = false
                    adapter.submitList(state.articles)
                    if (state.articles.isEmpty()) {
                        Snackbar.make(binding.root, "У вас пока нет статей", Snackbar.LENGTH_SHORT).show()
                    }
                }
                is State.Error -> {
                    binding.swipe.isRefreshing = false
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }

        loadUserArticles()
    }

    private fun loadUserArticles() {
        lifecycleScope.launch {
            try {
                val currentUser = api.getCurrentUser()
                viewModel.loadUserArticles(currentUser.userId)
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Ошибка загрузки ваших статей: ${e.localizedMessage}", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}