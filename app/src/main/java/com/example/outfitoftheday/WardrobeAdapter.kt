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
    private val onItemLongClicked: (ClothingItem, String) -> Unit
) : RecyclerView.Adapter<WardrobeAdapter.WardrobeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WardrobeViewHolder {
        val binding = ItemWardrobeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WardrobeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WardrobeViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)

        // Long press listener to show options dialog
        holder.itemView.setOnLongClickListener {
            showOptionsDialog(holder.itemView.context, item)
            true
        }
    }

    override fun getItemCount(): Int = items.size

    private fun showOptionsDialog(context: android.content.Context, item: ClothingItem) {
        val options = arrayOf<CharSequence>("Delete Clothing Item", "Modify Clothing Item")
        AlertDialog.Builder(context)
            .setTitle("What would you like to do?")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> onItemLongClicked(item, "delete")
                    1 -> onItemLongClicked(item, "modify")
                }
            }
            .show()
    }

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

            // Click listener for toggling visibility
            binding.cardView.setOnClickListener {
                val isImageVisible = binding.imageView.visibility == View.VISIBLE
                binding.imageView.visibility = if (isImageVisible) View.GONE else View.VISIBLE
                binding.textLayout.visibility = if (isImageVisible) View.VISIBLE else View.GONE
            }
        }
    }
}
