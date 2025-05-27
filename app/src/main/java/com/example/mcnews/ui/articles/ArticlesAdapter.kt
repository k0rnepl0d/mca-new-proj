package com.example.mcnews.ui.articles

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.mcnews.R
import com.example.mcnews.databinding.ItemArticleBinding
import com.example.mcnews.domain.model.Article

class ArticlesAdapter(
    private val onClick: (Article) -> Unit,
    private val onLongClick: (Article) -> Unit
) : ListAdapter<Article, ArticlesAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Article>() {
            override fun areItemsTheSame(old: Article, new: Article) = old.articleId == new.articleId
            override fun areContentsTheSame(old: Article, new: Article) = old == new
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemArticleBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val article = getItem(position)
        holder.bind(article)
        holder.itemView.setOnClickListener { onClick(article) }
        holder.itemView.setOnLongClickListener { onLongClick(article); true }
    }

    inner class VH(private val binding: ItemArticleBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(article: Article) = with(binding) {
            tvTitle.text = article.title
            tvBody.text = if (article.body.length > 120) article.body.take(120) + "…" else article.body

            // Обрабатываем изображение
            if (!article.imageUrl.isNullOrEmpty()) {
                try {
                    // Декодируем base64 в bitmap
                    val imageBytes = Base64.decode(article.imageUrl, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

                    if (bitmap != null) {
                        imgPreview.setImageBitmap(bitmap)
                        imgPreview.visibility = View.VISIBLE
                    } else {
                        // Если bitmap null, показываем placeholder
                        imgPreview.setImageResource(R.drawable.ic_image_placeholder)
                        imgPreview.visibility = View.VISIBLE
                    }
                } catch (e: Exception) {
                    // Если не удалось декодировать, показываем placeholder
                    imgPreview.setImageResource(R.drawable.ic_image_placeholder)
                    imgPreview.visibility = View.VISIBLE
                }
            } else {
                // Если изображения нет, показываем placeholder
                imgPreview.setImageResource(R.drawable.ic_image_placeholder)
                imgPreview.visibility = View.VISIBLE
            }
        }
    }
}