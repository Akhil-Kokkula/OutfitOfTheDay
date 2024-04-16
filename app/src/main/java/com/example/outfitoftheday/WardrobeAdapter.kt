package com.example.outfitoftheday

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.outfitoftheday.databinding.ItemWardrobeBinding

class WardrobeAdapter(private var items: List<ClothingItem>) :
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
            binding.textViewName.text = item.name
            if (item.imageUrl != null) {
                Glide.with(binding.imageView.context)
                    .load(item.imageUrl)
                    .into(binding.imageView)
            }
        }
    }
}
