// app/src/main/java/com/example/mcnews/ui/articles/ArticlesFragment.kt
package com.example.mcnews.ui.articles

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.widget.AppCompatSpinner
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mcnews.R
import com.example.mcnews.data.remote.ApiService
import com.example.mcnews.databinding.FragmentArticlesBinding
import com.example.mcnews.ui.edit.EditArticleActivity
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ArticlesFragment : Fragment() {

    private var _binding: FragmentArticlesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ArticlesViewModel by viewModels()

    private val tagNames = mutableListOf("Все теги")
    private val tagIdMap = mutableMapOf<Int, Int?>().apply { this[0] = null }
    private var selectedTagId: Int? = null

    @Inject lateinit var api: ApiService

    private val adapter = ArticlesAdapter(
        onClick = { article ->
            startActivity(Intent(requireContext(), com.example.mcnews.ui.articles.ArticleDetailActivity::class.java).apply {
                putExtra("title", article.title)
                putExtra("body", article.body)
                putExtra("imageUrl", article.imageUrl)
            })
        },
        onLongClick = { article ->
            startActivity(Intent(requireContext(), EditArticleActivity::class.java).apply {
                putExtra("articleId", article.articleId)
            })
        }
    )

    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentArticlesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            recycler.layoutManager = LinearLayoutManager(requireContext())
            recycler.adapter = adapter

            swipe.setOnRefreshListener {
                viewModel.load(tagId = selectedTagId, search = searchView.query.toString().trim().takeIf { it.isNotBlank() })
            }

            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    viewModel.load(tagId = selectedTagId, search = query?.trim()?.takeIf { it.isNotBlank() })
                    searchView.clearFocus()
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    searchJob?.cancel()
                    searchJob = lifecycleScope.launch {
                        delay(300)
                        viewModel.load(tagId = selectedTagId, search = newText?.trim()?.takeIf { it.isNotBlank() })
                    }
                    return true
                }
            })

            viewModel.state.observe(viewLifecycleOwner) { state ->
                when (state) {
                    is State.Loading -> swipe.isRefreshing = true
                    is State.Data -> {
                        swipe.isRefreshing = false
                        adapter.submitList(state.articles)
                        if (state.articles.isEmpty()) {
                            Snackbar.make(root, R.string.no_articles_found, Snackbar.LENGTH_SHORT).show()
                        }
                    }
                    is State.Error -> {
                        swipe.isRefreshing = false
                        Snackbar.make(root, state.message, Snackbar.LENGTH_LONG).show()
                    }
                }
            }

            fabAdd.setOnClickListener {
                startActivity(Intent(requireContext(), EditArticleActivity::class.java))
            }
        }
        viewModel.load()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_articles, menu)

        val spinner = menu.findItem(R.id.action_filter).actionView as AppCompatSpinner
        val spinnerAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item, tagNames).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinner.adapter = spinnerAdapter

        lifecycleScope.launch {
            try {
                val tags = api.getTags()
                tagNames.clear()
                tagNames.add("Все теги")
                tagIdMap.clear()
                tagIdMap[0] = null

                tags.forEachIndexed { index, tag ->
                    tagNames.add(tag.name)
                    tagIdMap[index + 1] = tag.tagId
                }
                spinnerAdapter.notifyDataSetChanged()
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Не удалось загрузить теги: ${e.localizedMessage}", Snackbar.LENGTH_LONG).show()
            }
        }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedTagId = tagIdMap[position]
                viewModel.load(tagId = selectedTagId, search = binding.searchView.query.toString().trim().takeIf { it.isNotBlank() })
            }

            override fun onNothingSelected(parent: AdapterView<*>) = Unit
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}