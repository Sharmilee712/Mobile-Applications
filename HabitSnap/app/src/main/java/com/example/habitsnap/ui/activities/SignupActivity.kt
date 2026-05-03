package com.example.habitsnap.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.example.habitsnap.databinding.ActivitySignupBinding
import com.google.android.material.snackbar.Snackbar

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.btnSignup.setOnClickListener {
            val name    = binding.etDisplayName.text.toString().trim()
            val email   = binding.etEmail.text.toString().trim()
            val pass    = binding.etPassword.text.toString().trim()
            val confirm = binding.etConfirmPassword.text.toString().trim()

            when {
                name.isEmpty() || email.isEmpty() || pass.isEmpty() ->
                    showSnack("Please fill all fields")
                pass != confirm ->
                    showSnack("Passwords do not match")
                pass.length < 6 ->
                    showSnack("Password must be at least 6 characters")
                else -> createAccount(name, email, pass)
            }
        }

        binding.tvLogin.setOnClickListener { finish() }
    }

    private fun createAccount(name: String, email: String, password: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSignup.isEnabled = false

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(name).build()
                    auth.currentUser?.updateProfile(profileUpdates)
                        ?.addOnCompleteListener {
                            binding.progressBar.visibility = View.GONE
                            startActivity(
                                Intent(this, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                            )
                            finish()
                        }
                } else {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSignup.isEnabled = true
                    showSnack("Signup failed: ${task.exception?.message}")
                }
            }
    }

    private fun showSnack(msg: String) =
        Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
}