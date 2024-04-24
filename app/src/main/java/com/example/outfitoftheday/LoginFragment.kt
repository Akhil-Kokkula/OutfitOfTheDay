package com.example.outfitoftheday

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.outfitoftheday.databinding.FragmentLoginBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = checkNotNull(_binding) {
        "Cannot access binding because it is null. Is the view visible?"
    }

    private lateinit var logInBtn: MaterialButton
    private lateinit var signUpBtn: MaterialButton // Initialize sign-up button
    private lateinit var emailInputField: EditText
    private lateinit var passwordInputField: EditText
    private lateinit var auth: FirebaseAuth
    private val TAG = "LoginFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)

        logInBtn = binding.btnLogIn
        signUpBtn = binding.btnSignUp // Bind the sign-up button
        emailInputField = binding.tilEmail.editText!!
        passwordInputField = binding.tilPassword.editText!!
        auth = FirebaseAuth.getInstance()

        logInBtn.setOnClickListener {
            logIn()
        }

        signUpBtn.setOnClickListener {
            goToSignUpFragment()
        }

        return binding.root
    }

    private fun logIn() {
        val email = emailInputField.text.toString().trim()
        val password = passwordInputField.text.toString().trim()

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "successfully logged in user")
                    goToHomeFragment()
                } else {
                    Toast.makeText(getContext(), "Could not login. Please try again.", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "could not log in user", task.exception)
                }
            }
    }

    private fun goToHomeFragment() {
        val activityRootView = requireActivity().findViewById<View>(android.R.id.content)
        val bottomNavigationView = activityRootView.findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.visibility = View.VISIBLE
        requireActivity().supportFragmentManager.beginTransaction().replace(R.id.nav_host_fragment, HomeFragment()).commit()
    }

    private fun goToSignUpFragment() {
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, SignUpFragment())
            .addToBackStack(null)
            .commit()
    }
}
