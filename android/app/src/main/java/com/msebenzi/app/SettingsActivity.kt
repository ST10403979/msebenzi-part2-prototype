package com.msebenzi.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.msebenzi.app.data.RetrofitClient
import com.msebenzi.app.data.SessionManager
import com.msebenzi.app.data.UpdateProfileRequest
import com.msebenzi.app.databinding.ActivitySettingsBinding
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var session: SessionManager

    companion object {
        private const val TAG = "SettingsActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)

        when (session.getLanguage()) {
            "af" -> binding.radioAfrikaans.isChecked = true
            "xh" -> binding.radioXhosa.isChecked = true
            else -> binding.radioEnglish.isChecked = true
        }

        binding.buttonSave.setOnClickListener { saveSettings() }
        binding.buttonLogout.setOnClickListener { logout() }
    }

    private fun saveSettings() {
        val language = when (binding.radioLanguage.checkedRadioButtonId) {
            R.id.radioAfrikaans -> "af"
            R.id.radioXhosa -> "xh"
            else -> "en"
        }

        session.setLanguage(language)
        val token = session.getBearerToken()

        if (token != null) {
            lifecycleScope.launch {
                try {
                    RetrofitClient.api.updateProfile(token, UpdateProfileRequest(language = language))
                } catch (e: Exception) {
                    // Non-fatal — the language preference is already saved locally
                    // and will sync to the server next time there's connectivity.
                    Log.w(TAG, "Could not sync language setting to server: ${e.message}")
                }
            }
        }

        showMessage(getString(R.string.settings_saved))
    }

    private fun logout() {
        session.clear()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showMessage(message: String) {
        binding.textMessage.text = message
        binding.textMessage.visibility = View.VISIBLE
    }
}
