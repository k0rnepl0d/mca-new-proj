package com.example.mcnews.ui.articles

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.mcnews.data.remote.ApiService
import com.example.mcnews.databinding.ActivityArticleDetailBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class ArticleDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityArticleDetailBinding

    @Inject
    lateinit var apiService: ApiService

    private var articleId: Int = -1

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            exportToPdf()
        } else {
            Toast.makeText(this, "Разрешение на запись файлов отклонено", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArticleDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        articleId = intent.getIntExtra("articleId", -1)
        val title = intent.getStringExtra("title") ?: ""
        val body = intent.getStringExtra("body") ?: ""
        val image = intent.getStringExtra("imageUrl")
        val author = intent.getStringExtra("author") ?: ""
        val createdAt = intent.getStringExtra("createdAt") ?: ""
        val tags = intent.getStringArrayListExtra("tags") ?: arrayListOf()

        displayArticleData(title, body, image, author, createdAt, tags)

        setupExportButton()
    }

    private fun displayArticleData(
        title: String,
        body: String,
        image: String?,
        author: String,
        createdAt: String,
        tags: ArrayList<String>
    ) {
        binding.tvTitle.text = title
        binding.tvBody.text = body

        if (author.isNotEmpty()) {
            binding.tvAuthor.text = "Автор: $author"
            binding.tvAuthor.visibility = View.VISIBLE
        } else {
            binding.tvAuthor.visibility = View.GONE
        }

        if (createdAt.isNotEmpty()) {
            try {
                val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val date = inputFormat.parse(createdAt)
                binding.tvDate.text =
                    "Опубликовано: ${date?.let { dateFormat.format(it) } ?: createdAt}"
                binding.tvDate.visibility = View.VISIBLE
            } catch (e: Exception) {
                binding.tvDate.text = "Опубликовано: $createdAt"
                binding.tvDate.visibility = View.VISIBLE
            }
        } else {
            binding.tvDate.visibility = View.GONE
        }

        if (tags.isNotEmpty()) {
            binding.tvTags.text = "Теги: ${tags.joinToString(", ")}"
            binding.tvTags.visibility = View.VISIBLE
        } else {
            binding.tvTags.visibility = View.GONE
        }

        if (!image.isNullOrEmpty()) {
            try {
                val imageBytes = Base64.decode(image, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

                if (bitmap != null) {
                    binding.imgCover.setImageBitmap(bitmap)
                    binding.imgCover.visibility = View.VISIBLE
                } else {
                    binding.imgCover.visibility = View.GONE
                }
            } catch (e: Exception) {
                binding.imgCover.visibility = View.GONE
            }
        } else {
            binding.imgCover.visibility = View.GONE
        }
    }

    private fun setupExportButton() {
        binding.btnExportPdf.setOnClickListener {
            checkStoragePermissionAndExport()
        }
    }

    private fun checkStoragePermissionAndExport() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            exportToPdf()
        } else {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                exportToPdf()
            } else {
                storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    private fun exportToPdf() {
        if (articleId == -1) {
            Toast.makeText(this, "Ошибка: ID статьи не найден", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                binding.btnExportPdf.isEnabled = false
                binding.progressBar.visibility = View.VISIBLE

                println("Attempting to export PDF for article ID: $articleId")

                val response = apiService.getArticlePdf(articleId)

                println("PDF Response code: ${response.code()}")
                println("PDF Response successful: ${response.isSuccessful}")

                if (response.isSuccessful) {
                    val pdfBytes = response.body()?.bytes()
                    println("PDF bytes received: ${pdfBytes?.size ?: 0}")

                    if (pdfBytes != null && pdfBytes.isNotEmpty()) {
                        savePdfFile(pdfBytes)
                        Toast.makeText(
                            this@ArticleDetailActivity, "PDF успешно сохранен!", Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Snackbar.make(binding.root, "Ошибка: PDF файл пуст", Snackbar.LENGTH_LONG)
                            .show()
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    println("PDF Error body: $errorBody")
                    Snackbar.make(
                        binding.root,
                        "Ошибка генерации PDF: ${response.code()}",
                        Snackbar.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                println("PDF Export error: ${e.message}")
                e.printStackTrace()
                Snackbar.make(
                    binding.root, "Ошибка экспорта PDF: ${e.localizedMessage}", Snackbar.LENGTH_LONG
                ).show()
            } finally {
                binding.btnExportPdf.isEnabled = true
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun savePdfFile(pdfBytes: ByteArray) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "article_${articleId}_$timestamp.pdf"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                savePdfToMediaStore(pdfBytes, fileName)
            } else {
                savePdfToDownloads(pdfBytes, fileName)
            }
        } catch (e: Exception) {
            Snackbar.make(
                binding.root, "Ошибка сохранения файла: ${e.localizedMessage}", Snackbar.LENGTH_LONG
            ).show()
        }
    }

    private fun savePdfToMediaStore(pdfBytes: ByteArray, fileName: String) {
        val resolver = contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let { pdfUri ->
            resolver.openOutputStream(pdfUri)?.use { outputStream ->
                outputStream.write(pdfBytes)
                outputStream.flush()
            }

            showPdfSavedDialog(fileName, pdfUri)
        }
    }

    private fun savePdfToDownloads(pdfBytes: ByteArray, fileName: String) {
        val downloadsDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, fileName)

        FileOutputStream(file).use { outputStream ->
            outputStream.write(pdfBytes)
            outputStream.flush()
        }

        val uri = FileProvider.getUriForFile(
            this, "${packageName}.fileprovider", file
        )

        showPdfSavedDialog(fileName, uri)
    }

    private fun showPdfSavedDialog(fileName: String, uri: Uri) {
        AlertDialog.Builder(this).setTitle("PDF сохранен")
            .setMessage("Файл '$fileName' сохранен в папку Downloads")
            .setPositiveButton("Открыть") { _, _ ->
                openPdfFile(uri)
            }.setNegativeButton("OK", null).show()
    }

    private fun openPdfFile(uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Нет приложения для открытия PDF", Toast.LENGTH_SHORT).show()
        }
    }
}   