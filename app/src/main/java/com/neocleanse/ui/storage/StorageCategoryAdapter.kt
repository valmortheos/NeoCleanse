package com.neocleanse.ui.storage

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.neocleanse.R
import com.neocleanse.databinding.ItemStorageCategoryBinding
import com.neocleanse.domain.model.CategoryStats
import com.neocleanse.util.Formatter

class StorageCategoryAdapter(
    private var totalUsedBytes: Long = 0L
) : ListAdapter<CategoryStats, StorageCategoryAdapter.ViewHolder>(CategoryDiffCallback()) {

    fun setTotalUsedBytes(total: Long) {
        totalUsedBytes = total
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStorageCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemStorageCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CategoryStats) {
            val name = item.category.name.lowercase().replaceFirstChar { it.uppercase() }
            binding.tvCategoryName.text = name
            binding.tvCategorySize.text = Formatter.formatSize(item.sizeBytes)

            val iconRes = when (item.category.name) {
                "IMAGES" -> R.drawable.ic_storage // Use appropriate icons when available
                "VIDEOS" -> R.drawable.ic_storage
                "DOCUMENTS" -> R.drawable.ic_storage
                "AUDIO" -> R.drawable.ic_storage
                else -> R.drawable.ic_storage
            }
            binding.ivCategoryIcon.setImageResource(iconRes)

            if (totalUsedBytes > 0) {
                 val percentage = (item.sizeBytes.toFloat() / totalUsedBytes.toFloat() * 100).toInt()
                 binding.pbCategory.progress = percentage
            } else {
                 binding.pbCategory.progress = 0
            }
        }
    }
}

class CategoryDiffCallback : DiffUtil.ItemCallback<CategoryStats>() {
    override fun areItemsTheSame(oldItem: CategoryStats, newItem: CategoryStats): Boolean {
        return oldItem.category == newItem.category
    }

    override fun areContentsTheSame(oldItem: CategoryStats, newItem: CategoryStats): Boolean {
        return oldItem == newItem
    }
}
