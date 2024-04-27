package com.example.outfitoftheday

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
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
    private var outfitPhotosList: MutableList<ClothingItem>,
): RecyclerView.Adapter<OutfitGalleryAdapter.ClothingItemHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClothingItemHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.outfitoutput_image_rv, parent, false)
        return ClothingItemHolder(view)
    }

    override fun onBindViewHolder(holder: ClothingItemHolder, position: Int) {
        val clothingItem = outfitPhotosList[position]
        if (!clothingItem.imageBase64.isNullOrEmpty()) {
            try {
                val bytes = Base64.decode(clothingItem.imageBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                holder.ivPhoto.setImageBitmap(bitmap)
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            }
        }

        holder.labelTxt.text = clothingItem.label ?: "No Label"
        holder.colorTxt.text = clothingItem.color ?: "No Color"
        holder.brandTxt.text = clothingItem.brand ?: "No Brand"

        holder.cardView.setOnClickListener {
            val isImageVisible = holder.ivPhoto.visibility == View.VISIBLE
            holder.ivPhoto.visibility = if (isImageVisible) View.GONE else View.VISIBLE
            holder.textLayout.visibility = if (isImageVisible) View.VISIBLE else View.GONE
        }

    }

    override fun getItemCount(): Int = outfitPhotosList.size

    fun updatingOutfitList(updatedOutfitPhotosList: MutableList<ClothingItem>) {
        outfitPhotosList = updatedOutfitPhotosList
        notifyDataSetChanged()
    }

    inner class ClothingItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivPhoto: ImageView = itemView.findViewById(R.id.ivClothingItem)
        val labelTxt : TextView = itemView.findViewById(R.id.textViewLabel)
        val colorTxt : TextView = itemView.findViewById(R.id.textViewColor)
        val brandTxt : TextView = itemView.findViewById(R.id.textViewBrand)
        val cardView : CardView = itemView.findViewById(R.id.cardView)
        val textLayout : LinearLayout = itemView.findViewById(R.id.textLayout)
    }

}

