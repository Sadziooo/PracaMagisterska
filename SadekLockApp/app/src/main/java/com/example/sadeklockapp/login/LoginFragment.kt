package com.example.sadeklockapp.login

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.sadeklockapp.customclass.BaseFragment
import com.example.sadeklockapp.databinding.FragmentLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginFragment : BaseFragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val fbAuth = FirebaseAuth.getInstance()
    private val LOG_DEBUG = "LOG_DEBUG"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupLoginClick()
    }


    private fun setupLoginClick() {
        binding.btnLogIn.setOnClickListener {
            val email = binding.textViewEmail.text?.trim().toString()
            val pass = binding.textViewPasswd.text?.trim().toString()

            if (email.isNotEmpty() && pass.isNotEmpty()){
                fbAuth.signInWithEmailAndPassword(email, pass)
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            Toast.makeText(requireContext(), "You are logged in successfully.", Toast.LENGTH_SHORT).show()
                            startApp()
                        } else {
                            Toast.makeText(requireContext(), it.exception!!.message.toString(), Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { exc ->
                        Log.d(LOG_DEBUG, exc.message.toString())
                    }
            } else {
                Toast.makeText(requireContext(), "Fill empty fields!", Toast.LENGTH_SHORT).show()
            }

        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}