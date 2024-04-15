package com.example.outfitoftheday

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class OutfitGalleryAdapter(
    private var outfitPhotosList: MutableList<OutfitPhoto>,
): RecyclerView.Adapter<OutfitGalleryAdapter.ClothingItemHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClothingItemHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.outfitoutput_image_rv, parent, false)
        return ClothingItemHolder(view)
    }

    override fun onBindViewHolder(holder: ClothingItemHolder, position: Int) {
        val photo = outfitPhotosList[position]
        Glide.with(holder.itemView.context)
            .load(photo.photoUrl)
            .into(holder.ivPhoto)
    }

    override fun getItemCount(): Int = outfitPhotosList.size

    fun updatingOutfitList(updatedOutfitPhotosList: MutableList<OutfitPhoto>) {
        outfitPhotosList = updatedOutfitPhotosList
        notifyDataSetChanged()
    }

    inner class ClothingItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivPhoto: ImageView = itemView.findViewById(R.id.ivClothingItem)
    }

}