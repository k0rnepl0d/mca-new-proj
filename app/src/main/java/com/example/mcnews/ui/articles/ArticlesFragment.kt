package com.example.mcnews.ui.articles

import android.app.Activity
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
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.view.ViewGroup

@AndroidEntryPoint
class ArticlesFragment : Fragment() {

    private var _binding: FragmentArticlesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ArticlesViewModel by viewModels()

    private val tagNames = mutableListOf("Все теги")
    private val tagIdMap = mutableMapOf<Int, Int?>().apply { this[0] = null }
    private var selectedTagId: Int? = null

    @Inject lateinit var api: ApiService

    // Launcher для редактирования/создания статей с обновлением списка
    private val editArticleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Обновляем список статей
            viewModel.load(tagId = selectedTagId, search = binding.searchView.query.toString().trim().takeIf { it.isNotBlank() })
        }
    }

    private val adapter = ArticlesAdapter(
        onClick = { article ->
            startActivity(Intent(requireContext(), com.example.mcnews.ui.articles.ArticleDetailActivity::class.java).apply {
                putExtra("title", article.title)
                putExtra("body", article.body)
                putExtra("imageUrl", article.imageUrl)
                putExtra("createdAt", article.createdAt)
                putExtra("author", article.authorName ?: "Неизвестный автор")
                putStringArrayListExtra("tags", ArrayList(article.tags.map { it.name }))
            })
        },
        onLongClick = { article ->
            editArticleLauncher.launch(Intent(requireContext(), EditArticleActivity::class.java).apply {
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

        setupRecyclerView()
        setupSearchView()
        setupFab()
        setupObservers()

        viewModel.load()
    }

    private fun setupRecyclerView() {
        binding.recycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ArticlesFragment.adapter
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.load(tagId = selectedTagId, search = query?.trim()?.takeIf { it.isNotBlank() })
                binding.searchView.clearFocus()
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
    }

    private fun setupFab() {
        binding.fabAdd.setOnClickListener {
            editArticleLauncher.launch(Intent(requireContext(), EditArticleActivity::class.java))
        }

        // ИСПРАВЛЕНИЕ: Используем правильные WindowInsets
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            // Устанавливаем отступ FAB с учетом системных панелей
            val fabLayoutParams = binding.fabAdd.layoutParams as ViewGroup.MarginLayoutParams
            fabLayoutParams.bottomMargin = 16.dpToPx() + navigationBars.bottom
            fabLayoutParams.rightMargin = 16.dpToPx() + systemBars.right
            binding.fabAdd.layoutParams = fabLayoutParams

            // Также обновляем padding для RecyclerView
            binding.recycler.setPadding(
                binding.recycler.paddingLeft,
                binding.recycler.paddingTop,
                binding.recycler.paddingRight,
                navigationBars.bottom + 80.dpToPx() // FAB размер + отступ
            )

            insets
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * requireContext().resources.displayMetrics.density).toInt()
    }

    private fun setupObservers() {
        binding.swipe.setOnRefreshListener {
            viewModel.load(tagId = selectedTagId, search = binding.searchView.query.toString().trim().takeIf { it.isNotBlank() })
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is State.Loading -> {
                    binding.swipe.isRefreshing = true
                }
                is State.Data -> {
                    binding.swipe.isRefreshing = false
                    adapter.submitList(state.articles)
                    if (state.articles.isEmpty()) {
                        Snackbar.make(binding.root, R.string.no_articles_found, Snackbar.LENGTH_SHORT).show()
                    }
                }
                is State.Error -> {
                    binding.swipe.isRefreshing = false
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }
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

    override fun onResume() {
        super.onResume()
        // ИСПРАВЛЕНИЕ: Дополнительная проверка при возврате на экран
        binding.fabAdd.visibility = View.VISIBLE
        binding.fabAdd.show()
    }

    override fun onStart() {
        super.onStart()
        // ИСПРАВЛЕНИЕ: Еще одна проверка при старте фрагмента
        binding.fabAdd.visibility = View.VISIBLE
        binding.fabAdd.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}