package com.msebenzi.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns as AndroidPatterns
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.msebenzi.app.data.LoginRequest
import com.msebenzi.app.data.RetrofitClient
import com.msebenzi.app.data.SessionManager
import com.msebenzi.app.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch
import java.io.IOException

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var session: SessionManager

    companion object {
        private const val TAG = "LoginActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        session = SessionManager(this)

        binding.buttonLogin.setOnClickListener { attemptLogin() }
        binding.buttonGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun attemptLogin() {
        val email = binding.inputEmail.text?.toString()?.trim().orEmpty()
        val password = binding.inputPassword.text?.toString().orEmpty()

        // ---- Input validation: never crash on bad input, just inform the user ----
        if (email.isEmpty() || !AndroidPatterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError(getString(R.string.error_invalid_email))
            return
        }
        if (password.isEmpty()) {
            showError(getString(R.string.error_empty_password))
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.login(LoginRequest(email, password))
                if (response.isSuccessful && response.body()?.token != null) {
                    val body = response.body()!!
                    session.saveSession(body.token!!, body.user)
                    Log.i(TAG, "Login succeeded for $email")
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                } else if (response.code() == 401) {
                    showError(getString(R.string.error_bad_credentials))
                } else {
                    showError(getString(R.string.error_server_generic))
                }
            } catch (e: IOException) {
                // No connectivity or the server could not be reached at all.
                Log.w(TAG, "Network error during login: ${e.message}")
                showError(getString(R.string.error_network))
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during login", e)
                showError(getString(R.string.error_unexpected))
            } finally {
                setLoading(false)
            }
        }
    }

    private fun showError(message: String) {
        binding.textError.text = message
        binding.textError.visibility = android.view.View.VISIBLE
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) android.view.View.VISIBLE else android.view.View.GONE
        binding.buttonLogin.isEnabled = !loading
    }
}
