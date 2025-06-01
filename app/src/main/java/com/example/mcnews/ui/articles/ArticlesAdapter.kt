package com.example.mcnews.ui.articles

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mcnews.R
import com.example.mcnews.databinding.ItemArticleBinding
import com.example.mcnews.domain.model.Article
import java.text.SimpleDateFormat
import java.util.*

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

            try {
                val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val date = inputFormat.parse(article.createdAt)
                tvDate.text = date?.let { dateFormat.format(it) } ?: article.createdAt
                tvDate.visibility = View.VISIBLE
            } catch (e: Exception) {
                tvDate.text = article.createdAt
                tvDate.visibility = View.VISIBLE
            }

            if (!article.imageUrl.isNullOrEmpty()) {
                try {
                    val imageBytes = Base64.decode(article.imageUrl, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

                    if (bitmap != null) {
                        imgPreview.setImageBitmap(bitmap)
                        imgPreview.visibility = View.VISIBLE
                    } else {
                        imgPreview.setImageResource(R.drawable.ic_image_placeholder)
                        imgPreview.visibility = View.VISIBLE
                    }
                } catch (e: Exception) {
                    imgPreview.setImageResource(R.drawable.ic_image_placeholder)
                    imgPreview.visibility = View.VISIBLE
                }
            } else {
                imgPreview.setImageResource(R.drawable.ic_image_placeholder)
                imgPreview.visibility = View.VISIBLE
            }
        }
    }
}