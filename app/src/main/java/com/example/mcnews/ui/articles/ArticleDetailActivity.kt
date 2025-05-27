package com.example.mcnews.ui.articles

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.example.mcnews.databinding.ActivityArticleDetailBinding

class ArticleDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityArticleDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArticleDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val title   = intent.getStringExtra("title") ?: ""
        val body    = intent.getStringExtra("body")  ?: ""
        val image   = intent.getStringExtra("imageUrl")

        binding.tvTitle.text = title
        binding.tvBody.text  = body

        if (!image.isNullOrEmpty()) {
            binding.imgCover.apply {
                visibility = android.view.View.VISIBLE
                load(image)
            }
        }
    }
}
