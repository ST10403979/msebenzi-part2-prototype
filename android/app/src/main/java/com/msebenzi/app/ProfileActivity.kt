package com.msebenzi.app

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.msebenzi.app.adapters.RatingAdapter
import com.msebenzi.app.data.RetrofitClient
import com.msebenzi.app.data.SessionManager
import com.msebenzi.app.data.UpdateProfileRequest
import com.msebenzi.app.data.User
import com.msebenzi.app.databinding.ActivityProfileBinding
import kotlinx.coroutines.launch

/**
 * Feature: Worker profile and verification.
 * Shows a worker's skills, bio, reputation (rating average + reviews) and
 * verification badge. When viewing your own profile, also lets you edit
 * your skills/bio and request verification.
 */
class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var session: SessionManager
    private lateinit var ratingAdapter: RatingAdapter
    private var profileUserId: Int = -1
    private var isOwnProfile: Boolean = false

    companion object {
        const val EXTRA_USER_ID = "extra_user_id"
        private const val TAG = "ProfileActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)
        binding.toolbar.setNavigationOnClickListener { finish() }

        // If no user id was passed in, this is "my own profile".
        profileUserId = intent.getIntExtra(EXTRA_USER_ID, -1)

        ratingAdapter = RatingAdapter(emptyList())
        binding.recyclerRatings.layoutManager = LinearLayoutManager(this)
        binding.recyclerRatings.adapter = ratingAdapter

        binding.buttonSaveProfile.setOnClickListener { saveProfile() }
        binding.buttonRequestVerification.setOnClickListener { requestVerification() }

        loadProfile()
    }

    private fun loadProfile() {
        val token = session.getBearerToken() ?: return
        lifecycleScope.launch {
            try {
                val user = if (profileUserId == -1) {
                    isOwnProfile = true
                    RetrofitClient.api.getProfile(token).body()
                } else {
                    isOwnProfile = false
                    RetrofitClient.api.getUserProfile(token, profileUserId).body()
                }

                if (user != null) {
                    bindUser(user)
                    loadRatings(user.id)
                } else {
                    showMessage(getString(R.string.error_loading_job))
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load profile: ${e.message}")
                showMessage(getString(R.string.error_network))
            }
        }
    }

    private fun bindUser(user: User) {
        binding.textName.text = user.name
        binding.textVerifiedBadge.visibility = if (user.verificationStatus == "verified") View.VISIBLE else View.GONE

        val ratingText = if (user.ratingCount > 0) {
            "★".repeat(Math.round(user.ratingAverage).toInt()) + " ${user.ratingAverage} (${user.ratingCount} review${if (user.ratingCount == 1) "" else "s"})"
        } else {
            getString(R.string.no_reviews_yet)
        }
        binding.textRoleAndRating.text = "${user.role.replaceFirstChar { it.uppercase() }} • $ratingText"

        binding.textSkills.text = if (!user.skills.isNullOrBlank()) {
            "${getString(R.string.skills_label)}: ${user.skills}"
        } else {
            "${getString(R.string.skills_label)}: —"
        }
        binding.textBio.text = user.bio ?: ""

        if (isOwnProfile) {
            binding.editSection.visibility = View.VISIBLE
            binding.inputSkills.setText(user.skills ?: "")
            binding.inputBio.setText(user.bio ?: "")

            if (user.role != "worker" || user.verificationStatus != "unverified") {
                binding.buttonRequestVerification.isEnabled = false
                binding.buttonRequestVerification.text = when (user.verificationStatus) {
                    "pending" -> getString(R.string.verification_pending)
                    "verified" -> getString(R.string.verified)
                    else -> getString(R.string.request_verification)
                }
            }
        }
    }

    private fun loadRatings(userId: Int) {
        val token = session.getBearerToken() ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getRatingsForUser(token, userId)
                if (response.isSuccessful && response.body() != null) {
                    val ratings = response.body()!!
                    ratingAdapter.updateData(ratings)
                    binding.textNoRatings.visibility = if (ratings.isEmpty()) View.VISIBLE else View.GONE
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load ratings: ${e.message}")
            }
        }
    }

    private fun saveProfile() {
        val token = session.getBearerToken() ?: return
        val skills = binding.inputSkills.text?.toString()?.trim().orEmpty()
        val bio = binding.inputBio.text?.toString()?.trim().orEmpty()

        if (bio.length > 500) {
            showMessage(getString(R.string.error_bio_too_long))
            return
        }

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.updateProfile(token, UpdateProfileRequest(skills = skills, bio = bio))
                if (response.isSuccessful) {
                    showMessage(getString(R.string.profile_saved))
                } else {
                    showMessage(getString(R.string.error_server_generic))
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to save profile: ${e.message}")
                showMessage(getString(R.string.error_network))
            }
        }
    }

    private fun requestVerification() {
        val token = session.getBearerToken() ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.requestVerification(token)
                if (response.isSuccessful) {
                    binding.buttonRequestVerification.isEnabled = false
                    binding.buttonRequestVerification.text = getString(R.string.verification_pending)
                    showMessage(getString(R.string.verification_requested))
                } else {
                    showMessage(getString(R.string.error_server_generic))
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to request verification: ${e.message}")
                showMessage(getString(R.string.error_network))
            }
        }
    }

    private fun showMessage(message: String) {
        binding.textMessage.text = message
        binding.textMessage.visibility = View.VISIBLE
    }
}
