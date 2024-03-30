package com.example.outfitoftheday

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private lateinit var bottomNavigationView: BottomNavigationView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavigationView = findViewById(R.id.bottom_navigation)

        val currUser = FirebaseAuth.getInstance().currentUser
        println("current user is " + currUser?.email)


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

        if (savedInstanceState == null && currUser == null) {
            bottomNavigationView.visibility = View.GONE
            supportFragmentManager.beginTransaction().replace(R.id.nav_host_fragment, SignUpFragment()).commit()
        } else if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().replace(R.id.nav_host_fragment, HomeFragment()).commit()
        }



    }

}
