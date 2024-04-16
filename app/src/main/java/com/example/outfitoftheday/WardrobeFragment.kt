package com.example.outfitoftheday

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.example.outfitoftheday.databinding.FragmentWardrobeBinding
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class WardrobeFragment : Fragment() {
    private var _binding: FragmentWardrobeBinding? = null
    private val binding get() = _binding!!

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: WardrobeAdapter
    private lateinit var auth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference
    private lateinit var addDummyDataBtn: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWardrobeBinding.inflate(inflater, container, false)
        recyclerView = binding.wardrobeRecyclerView
        auth = FirebaseAuth.getInstance()

        // Initialize the button and set up its click listener
        addDummyDataBtn = binding.btnAddDummyData
        addDummyDataBtn.setOnClickListener {
            addDummyItemsToDatabase()
        }

        setupFirebaseDatabase()

        return binding.root
    }

    private fun setupFirebaseDatabase() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e("WardrobeFragment", "User is not logged in.")
            return
        }

        databaseReference = FirebaseDatabase.getInstance().getReference("wardrobes").child(userId)
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val items = mutableListOf<ClothingItem>()
                for (snapshot in dataSnapshot.children) {
                    val item = snapshot.getValue(ClothingItem::class.java)
                    item?.let {
                        // Assigning the key or a default value if the key is null
                        it.id = snapshot.key ?: "default_id"
                        items.add(it)
                    }
                }
                updateUI(items)
            }


            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("WardrobeFragment", "Failed to read wardrobe data", databaseError.toException())
            }
        })
    }

    private fun addDummyItemsToDatabase() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val items = listOf(
                ClothingItem("1", "T-Shirt", "https://example.com/tshirt.jpg"),
                ClothingItem("2", "Jeans", "https://example.com/jeans.jpg"),
                ClothingItem("3", "Sneakers", "https://example.com/sneakers.jpg")
            )

            items.forEach { item ->
                val itemId = item.id ?: throw IllegalStateException("Item ID is null")
                databaseReference.child(itemId).setValue(item)
                    .addOnSuccessListener {
                        Log.d("WardrobeFragment", "Item added successfully: ${item.name}")
                    }
                    .addOnFailureListener { e ->
                        Log.e("WardrobeFragment", "Failed to add item: ${item.name}", e)
                    }
            }
        } else {
            Log.e("WardrobeFragment", "User not logged in")
        }
    }



    private fun updateUI(items: List<ClothingItem>) {
        adapter = WardrobeAdapter(items)
        recyclerView.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
