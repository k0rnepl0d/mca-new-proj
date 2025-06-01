package com.example.mcnews.ui.edit

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.mcnews.R
import com.example.mcnews.data.remote.ApiService
import com.example.mcnews.databinding.ActivityArticleEditBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.inject.Inject

@AndroidEntryPoint
class EditArticleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityArticleEditBinding
    private val viewModel: EditArticleViewModel by viewModels()

    @Inject lateinit var api: ApiService

    private val statusPairs = listOf(
        "Черновик" to 1,
        "Модерация" to 2,
        "Отклонено" to 3,
        "Опубликовано" to 4
    )

    private var authorNames = mutableListOf<String>()
    private var authorIds = mutableListOf<Int>()
    private var tagNames = mutableListOf<String>()
    private var tagIds = mutableListOf<Int>()
    private var selectedTagIds = mutableListOf<Int>()

    private var selectedImageUri: Uri? = null
    private var selectedImageBase64: String? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { handleImageSelection(it) }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            selectedImageUri?.let { uri ->
                handleImageSelection(uri)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showImageSourceDialog()
        } else {
            Toast.makeText(this, "Разрешение на камеру отклонено", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArticleEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        loadData()

        val articleId = intent.getIntExtra("articleId", -1)
        if (articleId != -1) {
            loadExistingArticle(articleId)
        } else {
            setupForNewArticle()
        }

        setupClickListeners(articleId)
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (intent.getIntExtra("articleId", -1) != -1) {
            "Редактировать статью"
        } else {
            "Новая статья"
        }

        binding.spStatus.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            statusPairs.map { it.first }
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        val authorAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, authorNames).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spAuthor.adapter = authorAdapter

        updateSelectedTagsDisplay()
    }

    private fun loadData() {
        lifecycleScope.launch {
            try {
                val authors = api.getAuthors()
                authorNames.clear()
                authorIds.clear()
                authors.forEach {
                    authorNames.add("${it.firstName} ${it.lastName}")
                    authorIds.add(it.userId)
                }
                (binding.spAuthor.adapter as ArrayAdapter<*>).notifyDataSetChanged()

                val tags = api.getTags()
                tagNames.clear()
                tagIds.clear()
                tags.forEach {
                    tagNames.add(it.name)
                    tagIds.add(it.tagId)
                }

                updateSelectedTagsDisplay()

            } catch (e: Exception) {
                Snackbar.make(binding.root, "Ошибка загрузки данных: ${e.localizedMessage}", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun loadExistingArticle(articleId: Int) {
        binding.btnDelete.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                viewModel.load(articleId)
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Ошибка загрузки статьи: ${e.localizedMessage}", Snackbar.LENGTH_LONG).show()
            }
        }

        viewModel.article.observe(this) { article ->
            article?.let {
                binding.etTitle.setText(it.title)
                binding.etBody.setText(it.body)

                val statusIndex = statusPairs.indexOfFirst { pair -> pair.second == it.statusId }
                if (statusIndex >= 0) binding.spStatus.setSelection(statusIndex)

                val authorIndex = authorIds.indexOf(it.authorId)
                if (authorIndex >= 0) binding.spAuthor.setSelection(authorIndex)

                lifecycleScope.launch {
                    while (tagIds.isEmpty()) {
                        delay(100)
                    }

                    selectedTagIds.clear()
                    it.tags.forEach { tag ->
                        selectedTagIds.add(tag.tagId)
                    }

                    updateSelectedTagsDisplay()
                }

                if (!it.imageUrl.isNullOrEmpty()) {
                    try {
                        selectedImageBase64 = it.imageUrl
                        displaySelectedImage(it.imageUrl)
                    } catch (e: Exception) { }
                }
            }
        }
    }

    private fun setupForNewArticle() {
        binding.btnDelete.visibility = View.GONE
        supportActionBar?.title = "Новая статья"
    }

    private fun setupClickListeners(articleId: Int) {
        binding.btnSelectImage.setOnClickListener {
            checkPermissionAndShowImageDialog()
        }

        binding.btnRemoveImage.setOnClickListener {
            clearSelectedImage()
        }

        binding.btnSelectTags.setOnClickListener {
            showTagSelectionDialog()
        }

        binding.btnSave.setOnClickListener {
            if (validateInput()) {
                saveArticle(articleId)
            }
        }

        binding.btnDelete.setOnClickListener {
            showDeleteConfirmationDialog(articleId)
        }

        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun showTagSelectionDialog() {
        if (tagNames.isEmpty()) {
            Toast.makeText(this, "Теги не загружены", Toast.LENGTH_SHORT).show()
            return
        }

        val checkedItems = BooleanArray(tagNames.size) { index ->
            selectedTagIds.contains(tagIds[index])
        }

        AlertDialog.Builder(this)
            .setTitle("Выберите теги")
            .setMultiChoiceItems(tagNames.toTypedArray(), checkedItems) { _, which, isChecked ->
                val tagId = tagIds[which]
                if (isChecked) {
                    if (!selectedTagIds.contains(tagId)) {
                        selectedTagIds.add(tagId)
                    }
                } else {
                    selectedTagIds.remove(tagId)
                }
            }
            .setPositiveButton("ОК") { _, _ ->
                updateSelectedTagsDisplay()
            }
            .setNegativeButton("Отмена", null)
            .setNeutralButton("Добавить новый тег") { _, _ ->
                showAddNewTagDialog()
            }
            .show()
    }

    private fun showAddNewTagDialog() {
        val input = EditText(this).apply {
            hint = "Название нового тега"
            setPadding(60, 40, 60, 40)
        }

        AlertDialog.Builder(this)
            .setTitle("Добавить новый тег")
            .setView(input)
            .setPositiveButton("Создать") { _, _ ->
                val newTagName = input.text.toString().trim()
                if (newTagName.isNotEmpty()) {
                    createNewTag(newTagName)
                } else {
                    Toast.makeText(this, "Введите название тега", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun createNewTag(tagName: String) {
        lifecycleScope.launch {
            try {
                val newTag = api.createTag(tagName)
                tagNames.add(newTag.name)
                tagIds.add(newTag.tagId)
                selectedTagIds.add(newTag.tagId)
                updateSelectedTagsDisplay()
                Toast.makeText(this@EditArticleActivity, "Тег '$tagName' создан и добавлен", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@EditArticleActivity, "Ошибка создания тега: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateSelectedTagsDisplay() {
        if (tagIds.isEmpty()) {
            binding.tvSelectedTags.text = "Загрузка тегов..."
            return
        }

        val selectedTagNames = selectedTagIds.mapNotNull { selectedId ->
            val index = tagIds.indexOf(selectedId)
            if (index >= 0 && index < tagNames.size) tagNames[index] else null
        }

        binding.tvSelectedTags.text = if (selectedTagNames.isEmpty()) {
            "Теги не выбраны"
        } else {
            selectedTagNames.joinToString(", ")
        }
    }

    private fun checkPermissionAndShowImageDialog() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)
        } else {
            showImageSourceDialog()
        }
    }

    private fun showImageSourceDialog() {
        AlertDialog.Builder(this)
            .setTitle("Выберите источник")
            .setItems(arrayOf("Галерея", "Камера")) { _, which ->
                when (which) {
                    0 -> imagePickerLauncher.launch("image/*")
                    1 -> {
                        val imageUri = createImageUri()
                        selectedImageUri = imageUri
                        cameraLauncher.launch(imageUri)
                    }
                }
            }
            .show()
    }

    private fun createImageUri(): Uri {
        val resolver = contentResolver
        val contentValues = android.content.ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, "article_image_${System.currentTimeMillis()}")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }
        return resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)!!
    }

    private fun handleImageSelection(uri: Uri) {
        try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)

            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            selectedImageBase64 = Base64.encodeToString(byteArray, Base64.DEFAULT)

            displaySelectedImage(selectedImageBase64!!)

        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка обработки изображения", Toast.LENGTH_SHORT).show()
        }
    }

    private fun displaySelectedImage(base64: String) {
        try {
            val imageBytes = Base64.decode(base64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            binding.imgPreview.setImageBitmap(bitmap)
            binding.imgPreview.visibility = View.VISIBLE
            binding.btnRemoveImage.visibility = View.VISIBLE
            binding.tvNoImage.visibility = View.GONE
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка отображения изображения", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearSelectedImage() {
        selectedImageBase64 = null
        selectedImageUri = null
        binding.imgPreview.setImageBitmap(null)
        binding.imgPreview.visibility = View.GONE
        binding.btnRemoveImage.visibility = View.GONE
        binding.tvNoImage.visibility = View.VISIBLE
    }

    private fun validateInput(): Boolean {
        return when {
            binding.etTitle.text.toString().trim().isEmpty() -> {
                binding.etTitle.error = "Введите заголовок"
                false
            }
            binding.etBody.text.toString().trim().isEmpty() -> {
                binding.etBody.error = "Введите содержание"
                false
            }
            authorIds.isEmpty() -> {
                Toast.makeText(this, "Не загружен список авторов", Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }

    private fun saveArticle(articleId: Int) {
        lifecycleScope.launch {
            try {
                binding.btnSave.isEnabled = false
                binding.progressBar.visibility = View.VISIBLE

                val selectedAuthorId = authorIds.getOrNull(binding.spAuthor.selectedItemPosition) ?: return@launch

                val success = viewModel.save(
                    articleId = articleId,
                    title = binding.etTitle.text.toString().trim(),
                    body = binding.etBody.text.toString().trim(),
                    statusId = statusPairs[binding.spStatus.selectedItemPosition].second,
                    authorId = selectedAuthorId,
                    imageBase64 = selectedImageBase64,
                    tagIds = selectedTagIds
                )

                if (success) {
                    Toast.makeText(this@EditArticleActivity, "Статья сохранена", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    Snackbar.make(binding.root, "Ошибка сохранения", Snackbar.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                Snackbar.make(binding.root, "Ошибка: ${e.localizedMessage}", Snackbar.LENGTH_LONG).show()
            } finally {
                binding.btnSave.isEnabled = true
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun showDeleteConfirmationDialog(articleId: Int) {
        AlertDialog.Builder(this)
            .setTitle("Удалить статью?")
            .setMessage("Это действие нельзя отменить")
            .setPositiveButton("Удалить") { _, _ ->
                deleteArticle(articleId)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun deleteArticle(articleId: Int) {
        lifecycleScope.launch {
            try {
                viewModel.delete(articleId)
                Toast.makeText(this@EditArticleActivity, "Статья удалена", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Ошибка удаления: ${e.localizedMessage}", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            showImageSourceDialog()
        } else {
            Toast.makeText(this, "Разрешение на камеру отклонено", Toast.LENGTH_SHORT).show()
        }
    }
}