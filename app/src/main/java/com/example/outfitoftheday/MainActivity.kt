package com.example.outfitoftheday

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            var selectedFragment: Fragment? = null

            when (item.itemId) {
                R.id.navigation_home -> selectedFragment = HomeFragment()
                R.id.navigation_generate_outfit -> selectedFragment = GenerateOutfitFragment()
                R.id.navigation_wardrobe -> selectedFragment = WardrobeFragment()
                R.id.navigation_add_outfit -> selectedFragment = AddOutfitFragment()
            }

            selectedFragment?.let {
                supportFragmentManager.beginTransaction().replace(R.id.fragment_container, it).commit()
            }

            true
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().replace(R.id.fragment_container, HomeFragment()).commit()
        }
    }
}
