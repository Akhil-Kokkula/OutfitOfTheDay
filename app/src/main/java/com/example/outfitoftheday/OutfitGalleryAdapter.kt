package com.example.outfitoftheday

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.InputStream
import com.google.firebase.storage.ktx.component1
import com.google.firebase.storage.ktx.component2



class OutfitGalleryAdapter (
    private var outfitPhotosList: MutableList<OutfitPhoto>,
): RecyclerView.Adapter<OutfitGalleryAdapter.ClothingItemHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClothingItemHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.outfitoutput_image_rv, parent, false)
        return ClothingItemHolder(view)
    }

    override fun onBindViewHolder(holder: ClothingItemHolder, position: Int) {
        val photo = outfitPhotosList[position]
        if (!photo.imageBase64.isNullOrEmpty()) {
            try {
                val bytes = Base64.decode(photo.imageBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                holder.ivPhoto.setImageBitmap(bitmap)
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            }
        }
//        Glide.with(holder.itemView.context)
//            .load(photo.photoUrl)
//            .apply(RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
//            .into(holder.ivPhoto)

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

