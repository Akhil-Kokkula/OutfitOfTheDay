package com.example.outfitoftheday

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.outfitoftheday.R
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener





class WardrobeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val databaseReference = FirebaseDatabase.getInstance().getReference("wardrobe")
        val item1 = ClothingItem("1", "T-Shirt", "https://www.google.com/url?sa=i&url=https%3A%2F%2Fen.m.wikipedia.org%2Fwiki%2FFile%3ABlue_Tshirt.jpg&psig=AOvVaw0CPUeaQSiH6woEI3TSt-Cn&ust=1712191587384000&source=images&cd=vfe&opi=89978449&ved=0CBIQjRxqFwoTCIDv5OvopIUDFQAAAAAdAAAAABAE")
        val item2 = ClothingItem("2", "Jeans", "https://www.google.com/imgres?q=jeans%20image%20jpg&imgurl=https%3A%2F%2Ft3.ftcdn.net%2Fjpg%2F04%2F83%2F25%2F50%2F360_F_483255019_m1r1ujM8EOkr8PamCHF85tQ0rHG3Fiqz.jpg&imgrefurl=https%3A%2F%2Fstock.adobe.com%2Fsearch%3Fk%3Djeans&docid=MaCiVRDpVdLXzM&tbnid=iFGYxtXdjk1KBM&vet=12ahUKEwiI-ZWB6aSFAxUlrYkEHQ2HBJMQM3oECBkQAA..i&w=540&h=360&hcb=2&ved=2ahUKEwiI-ZWB6aSFAxUlrYkEHQ2HBJMQM3oECBkQAA")
        databaseReference.child(item1.id).setValue(item1)
        databaseReference.child(item2.id).setValue(item2)


        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val items = mutableListOf<ClothingItem>()
                for (snapshot in dataSnapshot.children) {
                    val item = snapshot.getValue(ClothingItem::class.java)
                    item?.let { items.add(it) }
                }
                // Update your UI with the items list
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle possible errors
            }
        })


        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_wardrobe, container, false)
    }
}
