package com.example.outfitoftheday

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.outfitoftheday.databinding.FragmentSignUpBinding
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth


class SignUpFragment : Fragment() {
    private var _binding: FragmentSignUpBinding? = null
    private val binding
        get() = checkNotNull(_binding) {
            "Cannot access binding because it is null. Is the view visible?"
        }
    private lateinit var logInBtn : MaterialButton
    private lateinit var createAcctBtn : MaterialButton
    private lateinit var emailInputField : EditText
    private lateinit var passwordInputField : EditText
    private lateinit var auth : FirebaseAuth
    private val TAG = "SignUpFragment"


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        emailInputField = binding.tilEmail.editText!!
        passwordInputField = binding.tilPassword.editText!!



        logInBtn = binding.btnLogIn
        logInBtn.setOnClickListener {
            goToLoginFragment()
        }

        createAcctBtn = binding.btnCreateAccount
        createAcctBtn.setOnClickListener {
            createAccount()
        }

        auth = FirebaseAuth.getInstance()



        return binding.root
    }

    private fun createAccount() {
        val email = emailInputField.text.toString().trim()
        val password = passwordInputField.text.toString().trim()

        if (email == "" || password == "") {
            Toast.makeText(getContext(), getString(R.string.signUp_incomplete), Toast.LENGTH_SHORT).show();
            println("could not create new user account since fields were empty")
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "successfully created new user account")
                    goToLoginFragment()
                } else {
                    Toast.makeText(getContext(),getString(R.string.signUp_accountCreationFail) , Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "could not create new user account due to ", task.exception)
                }
            }


    }

    private fun goToLoginFragment() {
       requireActivity().supportFragmentManager.beginTransaction()
           .replace(R.id.nav_host_fragment, LoginFragment())
           .addToBackStack(null)
           .commit()
    }



}