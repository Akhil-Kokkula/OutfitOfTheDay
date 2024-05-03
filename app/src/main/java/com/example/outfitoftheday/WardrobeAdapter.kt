package com.example.outfitoftheday

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.outfitoftheday.databinding.ItemWardrobeBinding

class WardrobeAdapter(
    var items: MutableList<ClothingItem>,
    private val onItemLongClicked: (ClothingItem) -> Unit
) : RecyclerView.Adapter<WardrobeAdapter.WardrobeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WardrobeViewHolder {
        val binding = ItemWardrobeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WardrobeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WardrobeViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
        // Long press listener for delete confirmation
        holder.itemView.setOnLongClickListener {
            onItemLongClicked(item)
            true
        }
        // Click listener for toggling visibility
        holder.binding.cardView.setOnClickListener {
            val isImageVisible = holder.binding.imageView.visibility == View.VISIBLE
            holder.binding.imageView.visibility = if (isImageVisible) View.GONE else View.VISIBLE
            holder.binding.textLayout.visibility = if (isImageVisible) View.VISIBLE else View.GONE
        }
    }

    override fun getItemCount(): Int = items.size

    class WardrobeViewHolder(val binding: ItemWardrobeBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ClothingItem) {
            binding.textViewLabel.text = item.label ?: "No Label"
            binding.textViewColor.text = item.color ?: "No Color"
            binding.textViewBrand.text = item.brand ?: "No Brand"

            // Load images from base64 or URL
            if (!item.imageBase64.isNullOrEmpty()) {
                val bytes = Base64.decode(item.imageBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                binding.imageView.setImageBitmap(bitmap)
            } else if (!item.imageUrl.isNullOrEmpty()) {
                Glide.with(binding.imageView.context)
                    .load(item.imageUrl)
                    .apply(RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
                    .into(binding.imageView)
            }
        }
    }
}
