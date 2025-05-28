package com.example.mcnews.ui.articles

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.mcnews.databinding.ActivityArticleDetailBinding
import java.text.SimpleDateFormat
import java.util.*

class ArticleDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityArticleDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArticleDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val title = intent.getStringExtra("title") ?: ""
        val body = intent.getStringExtra("body") ?: ""
        val image = intent.getStringExtra("imageUrl")
        val author = intent.getStringExtra("author") ?: ""
        val createdAt = intent.getStringExtra("createdAt") ?: ""
        val tags = intent.getStringArrayListExtra("tags") ?: arrayListOf()

        binding.tvTitle.text = title
        binding.tvBody.text = body

        // Отображение автора
        if (author.isNotEmpty()) {
            binding.tvAuthor.text = "Автор: $author"
            binding.tvAuthor.visibility = View.VISIBLE
        } else {
            binding.tvAuthor.visibility = View.GONE
        }

        // Отображение даты
        if (createdAt.isNotEmpty()) {
            try {
                val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val date = inputFormat.parse(createdAt)
                binding.tvDate.text = "Опубликовано: ${date?.let { dateFormat.format(it) } ?: createdAt}"
                binding.tvDate.visibility = View.VISIBLE
            } catch (e: Exception) {
                binding.tvDate.text = "Опубликовано: $createdAt"
                binding.tvDate.visibility = View.VISIBLE
            }
        } else {
            binding.tvDate.visibility = View.GONE
        }

        // Отображение тегов
        if (tags.isNotEmpty()) {
            binding.tvTags.text = "Теги: ${tags.joinToString(", ")}"
            binding.tvTags.visibility = View.VISIBLE
        } else {
            binding.tvTags.visibility = View.GONE
        }

        // Отображение изображения
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
}