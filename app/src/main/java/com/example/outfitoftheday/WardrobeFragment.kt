package com.example.outfitoftheday

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWardrobeBinding.inflate(inflater, container, false)
        recyclerView = binding.wardrobeRecyclerView
        recyclerView.layoutManager = GridLayoutManager(context, 2)
        auth = FirebaseAuth.getInstance()

        setupFirebaseDatabase()

        return binding.root
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
                val items = mutableListOf<ClothingItem>()
                for (snapshot in dataSnapshot.children) {
                    val item = snapshot.getValue(ClothingItem::class.java)
                    item?.let {
                        it.id = snapshot.key
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

    private fun updateUI(items: List<ClothingItem>) {
        if (::adapter.isInitialized) {
            adapter.items = items
            adapter.notifyDataSetChanged()
        } else {
            adapter = WardrobeAdapter(items)
            recyclerView.adapter = adapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
