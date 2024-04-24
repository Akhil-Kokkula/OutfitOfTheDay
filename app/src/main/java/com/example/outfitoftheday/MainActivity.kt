package com.example.outfitoftheday

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        setContentView(R.layout.activity_main)

        bottomNavigationView = findViewById(R.id.bottom_navigation)

        // Check current Firebase user
        val currUser = FirebaseAuth.getInstance().currentUser
        println("Current user is " + currUser?.email)

        // Navigation item selection handling with fragments
        bottomNavigationView.setOnItemSelectedListener { item ->
            var selectedFragment: Fragment? = null

            when (item.itemId) {
                R.id.navigation_home -> selectedFragment = HomeFragment()
                R.id.navigation_generate_outfit -> selectedFragment = GenerateOutfitFragment()
                R.id.navigation_wardrobe -> selectedFragment = WardrobeFragment()
                R.id.navigation_add_outfit -> selectedFragment = AddOutfitFragment()
            }

            selectedFragment?.let {
                supportFragmentManager.beginTransaction().replace(R.id.nav_host_fragment, it).commit()
            }

            true
        }

        // Initial fragment loading based on authentication status
        if (savedInstanceState == null) {
            if (currUser == null) {
                // Hide bottom navigation and show the SignUpFragment if not logged in
                bottomNavigationView.visibility = View.GONE
                supportFragmentManager.beginTransaction().replace(R.id.nav_host_fragment, SignUpFragment()).commit()
            } else {
                // Show HomeFragment if logged in
                supportFragmentManager.beginTransaction().replace(R.id.nav_host_fragment, HomeFragment()).commit()
            }
        }
    }
}