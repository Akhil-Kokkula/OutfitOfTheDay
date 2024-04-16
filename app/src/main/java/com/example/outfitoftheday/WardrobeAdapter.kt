package com.example.outfitoftheday

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.outfitoftheday.databinding.ItemWardrobeBinding

class WardrobeAdapter(var items: List<ClothingItem>) :
    RecyclerView.Adapter<WardrobeAdapter.WardrobeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WardrobeViewHolder {
        val binding = ItemWardrobeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WardrobeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WardrobeViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = items.size

    class WardrobeViewHolder(private val binding: ItemWardrobeBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ClothingItem) {
            binding.textViewLabel.text = item.label ?: "No Label"
            binding.textViewColor.text = item.color ?: "No Color"
            binding.textViewBrand.text = item.brand ?: "No Brand"

            if (!item.imageBase64.isNullOrEmpty()) {
                try {
                    val bytes = Base64.decode(item.imageBase64, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    binding.imageView.setImageBitmap(bitmap)
                } catch (e: IllegalArgumentException) {
                    e.printStackTrace()
                }
            } else if (!item.imageUrl.isNullOrEmpty()) {
                Glide.with(binding.imageView.context)
                    .load(item.imageUrl)
                    .apply(RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
                    .into(binding.imageView)
            }
        }
    }
}
