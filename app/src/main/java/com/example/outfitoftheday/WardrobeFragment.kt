package com.example.outfitoftheday

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.outfitoftheday.databinding.FragmentWardrobeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class WardrobeFragment : Fragment() {
    private var _binding: FragmentWardrobeBinding? = null
    private val binding get() = _binding!!

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: WardrobeAdapter
    private lateinit var auth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference

    private var allItems = mutableListOf<ClothingItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWardrobeBinding.inflate(inflater, container, false)
        recyclerView = binding.wardrobeRecyclerView
        recyclerView.layoutManager = GridLayoutManager(context, 2)
        auth = FirebaseAuth.getInstance()

        setupFirebaseDatabase()
        setupIconClickListeners()
        setupSearchView()
        setupAdapter()

        return binding.root
    }

    private fun setupAdapter() {
        adapter = WardrobeAdapter(allItems, this::showDeleteConfirmationDialog)
        recyclerView.adapter = adapter
    }

    private fun showDeleteConfirmationDialog(item: ClothingItem) {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Delete")
            .setMessage("Are you sure you want to delete this item?")
            .setPositiveButton("Delete") { dialog, which ->
                deleteItemFromFirebase(item)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteItemFromFirebase(item: ClothingItem) {
        val userId = auth.currentUser?.uid
        if (userId != null && item.id != null) {
            FirebaseDatabase.getInstance().getReference("users/$userId/outfits/${item.id}")
                .removeValue()
                .addOnSuccessListener {
                    allItems.remove(item)
                    adapter.notifyDataSetChanged()
                    Log.d("WardrobeFragment", "Item deleted successfully")
                }
                .addOnFailureListener {
                    Log.e("WardrobeFragment", "Failed to delete item", it)
                }
        }
    }

    private fun setupFirebaseDatabase() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e("WardrobeFragment", "User is not logged in.")
            return
        }

        databaseReference = FirebaseDatabase.getInstance().getReference("users/$userId/outfits")
        fetchDataFromDatabase()
    }

    private fun fetchDataFromDatabase() {
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                allItems.clear()
                for (snapshot in dataSnapshot.children) {
                    val item = snapshot.getValue(ClothingItem::class.java)
                    item?.let {
                        it.id = snapshot.key
                        allItems.add(it)
                    }
                }
                updateUI(allItems)  // Initial load with all items
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("WardrobeFragment", "Failed to read wardrobe data", databaseError.toException())
            }
        })
    }

    private fun setupIconClickListeners() {
        binding.iconAll.setOnClickListener { filterWardrobe("all") }
        binding.iconHats.setOnClickListener { filterWardrobe("hats") }
        binding.iconShirts.setOnClickListener { filterWardrobe("tops") }
        binding.iconBottoms.setOnClickListener { filterWardrobe("bottoms") }
        binding.iconFootwear.setOnClickListener { filterWardrobe("footwear") }
        binding.iconMisc.setOnClickListener { filterWardrobe("miscellaneous") }
    }

    private fun filterWardrobe(category: String) {
        val filteredItems = if (category == "all") {
            allItems
        } else {
            allItems.filter { it.category?.equals(category, ignoreCase = true) ?: false }
        }
        updateUI(filteredItems)
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false // Let the SearchView handle the default behavior of the query text submission
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterWardrobeBySearch(newText)
                return true
            }
        })
    }

    private fun filterWardrobeBySearch(query: String?) {
        val filteredList = if (!query.isNullOrEmpty()) {
            allItems.filter {
                it.label?.contains(query, ignoreCase = true) == true ||
                        it.color?.contains(query, ignoreCase = true) == true ||
                        it.brand?.contains(query, ignoreCase = true) == true
            }
        } else {
            allItems // Return all items if search query is empty
        }
        updateUI(filteredList)
    }

    private fun updateUI(items: List<ClothingItem>) {
        adapter.items = items.toMutableList()
        adapter.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
