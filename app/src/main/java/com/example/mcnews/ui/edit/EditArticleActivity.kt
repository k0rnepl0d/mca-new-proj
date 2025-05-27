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

    private var selectedImageUri: Uri? = null
    private var selectedImageBase64: String? = null

    // Activity result launchers
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

        // Setup spinners
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

        val tagAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, tagNames).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spTags.adapter = tagAdapter
    }

    private fun loadData() {
        lifecycleScope.launch {
            try {
                // Load authors
                val authors = api.getAuthors()
                authorNames.clear()
                authorIds.clear()
                authors.forEach {
                    authorNames.add("${it.firstName} ${it.lastName}")
                    authorIds.add(it.userId)
                }
                (binding.spAuthor.adapter as ArrayAdapter<*>).notifyDataSetChanged()

                // Load tags
                val tags = api.getTags()
                tagNames.clear()
                tagIds.clear()
                tags.forEach {
                    tagNames.add(it.name)
                    tagIds.add(it.tagId)
                }
                (binding.spTags.adapter as ArrayAdapter<*>).notifyDataSetChanged()

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

                // Set status
                val statusIndex = statusPairs.indexOfFirst { pair -> pair.second == it.statusId }
                if (statusIndex >= 0) binding.spStatus.setSelection(statusIndex)

                // Set author
                val authorIndex = authorIds.indexOf(it.authorId)
                if (authorIndex >= 0) binding.spAuthor.setSelection(authorIndex)

                // Load image if exists
                if (!it.imageUrl.isNullOrEmpty() && it.imageUrl.startsWith("data:image")) {
                    try {
                        // Extract base64 from data URL
                        val base64Data = it.imageUrl.substring(it.imageUrl.indexOf(",") + 1)
                        selectedImageBase64 = base64Data
                        displaySelectedImage(base64Data)
                    } catch (e: Exception) {
                        // Ignore image loading errors
                    }
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

            // Convert to base64
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
                val selectedTagId = if (tagIds.isNotEmpty()) tagIds.getOrNull(binding.spTags.selectedItemPosition) else null

                val success = viewModel.save(
                    articleId = articleId,
                    title = binding.etTitle.text.toString().trim(),
                    body = binding.etBody.text.toString().trim(),
                    statusId = statusPairs[binding.spStatus.selectedItemPosition].second,
                    authorId = selectedAuthorId,
                    imageBase64 = selectedImageBase64,
                    tagIds = selectedTagId?.let { listOf(it) } ?: emptyList()
                )

                if (success) {
                    Toast.makeText(this@EditArticleActivity, "Статья сохранена", Toast.LENGTH_SHORT).show()
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