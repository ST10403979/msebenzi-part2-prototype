package com.msebenzi.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.msebenzi.app.data.RegisterRequest
import com.msebenzi.app.data.RetrofitClient
import com.msebenzi.app.databinding.ActivityRegisterBinding
import kotlinx.coroutines.launch
import java.io.IOException

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    companion object {
        private const val TAG = "RegisterActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonRegister.setOnClickListener { attemptRegister() }
    }

    private fun attemptRegister() {
        val name = binding.inputName.text?.toString()?.trim().orEmpty()
        val email = binding.inputEmail.text?.toString()?.trim().orEmpty()
        val password = binding.inputPassword.text?.toString().orEmpty()
        val role = if (binding.radioHousehold.isChecked) "household" else "worker"

        if (name.isEmpty()) {
            showError(getString(R.string.error_empty_name)); return
        }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError(getString(R.string.error_invalid_email)); return
        }
        if (password.length < 6) {
            showError(getString(R.string.error_short_password)); return
        }

        setLoading(true)
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.register(
                    RegisterRequest(name = name, email = email, password = password, role = role)
                )
                if (response.isSuccessful) {
                    Log.i(TAG, "Registration succeeded for $email")
                    // Send the user to log in with their new credentials.
                    startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                    finish()
                } else if (response.code() == 409) {
                    showError(getString(R.string.error_email_taken))
                } else {
                    showError(getString(R.string.error_server_generic))
                }
            } catch (e: IOException) {
                Log.w(TAG, "Network error during registration: ${e.message}")
                showError(getString(R.string.error_network))
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during registration", e)
                showError(getString(R.string.error_unexpected))
            } finally {
                setLoading(false)
            }
        }
    }

    private fun showError(message: String) {
        binding.textError.text = message
        binding.textError.visibility = View.VISIBLE
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.buttonRegister.isEnabled = !loading
    }
}
