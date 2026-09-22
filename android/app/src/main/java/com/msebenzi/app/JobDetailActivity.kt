package com.msebenzi.app

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.msebenzi.app.data.Job
import com.msebenzi.app.data.JobRepository
import com.msebenzi.app.data.RetrofitClient
import com.msebenzi.app.data.SessionManager
import com.msebenzi.app.data.SubmitRatingRequest
import com.msebenzi.app.databinding.ActivityJobDetailBinding
import com.msebenzi.app.databinding.DialogRateJobBinding
import kotlinx.coroutines.launch

class JobDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityJobDetailBinding
    private lateinit var session: SessionManager
    private lateinit var repository: JobRepository
    private var jobId: Int = -1
    private var currentJob: Job? = null

    companion object {
        const val EXTRA_JOB_ID = "extra_job_id"
        private const val TAG = "JobDetailActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityJobDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)
        repository = JobRepository(this)
        jobId = intent.getIntExtra(EXTRA_JOB_ID, -1)

        if (jobId == -1) {
            finish()
            return
        }

        loadJobDetails()

        binding.buttonAccept.setOnClickListener { acceptJob() }
        binding.buttonComplete.setOnClickListener { completeJob() }
        binding.buttonRate.setOnClickListener { showRateDialog() }
        binding.buttonViewProfile.setOnClickListener { openOtherPartyProfile() }
    }

    private fun loadJobDetails() {
        val token = session.getBearerToken() ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getJob(token, jobId)
                if (response.isSuccessful && response.body() != null) {
                    val job = response.body()!!
                    currentJob = job
                    bindJob(job)
                } else {
                    showMessage(getString(R.string.error_loading_job))
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load job detail (likely offline): ${e.message}")
                showMessage(getString(R.string.error_network))
            }
        }
    }

    private fun bindJob(job: Job) {
        binding.textTitle.text = job.title
        binding.textStatus.text = job.status.replaceFirstChar { it.uppercase() }
        binding.textCategoryLocation.text = "${job.category} • ${job.location}"
        binding.textBudget.text = "R${"%.2f".format(job.budget)}"
        binding.textDescription.text = job.description

        val myId = session.getUserId()
        val isParticipant = myId == job.postedById || myId == job.acceptedById

        binding.buttonAccept.visibility = if (job.status == "open") View.VISIBLE else View.GONE
        binding.buttonAccept.isEnabled = job.status == "open"

        binding.buttonComplete.visibility =
            if (job.status == "accepted" && isParticipant) View.VISIBLE else View.GONE

        binding.buttonRate.visibility =
            if (job.status == "completed" && isParticipant) View.VISIBLE else View.GONE

        binding.buttonViewProfile.visibility =
            if (isParticipant && job.acceptedById != null) View.VISIBLE else View.GONE
    }

    private fun acceptJob() {
        val token = session.getBearerToken() ?: return
        binding.buttonAccept.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = repository.acceptJob(token, jobId)
            binding.progressBar.visibility = View.GONE

            result.onSuccess { message ->
                showMessage(message)
                loadJobDetails()
            }.onFailure { e ->
                Log.e(TAG, "Accept job failed", e)
                showMessage(getString(R.string.error_accepting_job))
                binding.buttonAccept.isEnabled = true
            }
        }
    }

    private fun completeJob() {
        val token = session.getBearerToken() ?: return
        binding.buttonComplete.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.completeJob(token, jobId)
                binding.progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    showMessage(getString(R.string.job_marked_complete))
                    loadJobDetails()
                } else {
                    showMessage(getString(R.string.error_server_generic))
                    binding.buttonComplete.isEnabled = true
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Log.w(TAG, "Failed to complete job: ${e.message}")
                showMessage(getString(R.string.error_network))
                binding.buttonComplete.isEnabled = true
            }
        }
    }

    private fun showRateDialog() {
        val dialogBinding = DialogRateJobBinding.inflate(LayoutInflater.from(this))

        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.submit) { _, _ ->
                val score = dialogBinding.ratingBar.rating.toInt().coerceIn(1, 5)
                val comment = dialogBinding.inputComment.text?.toString()?.trim()
                submitRating(score, comment)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun submitRating(score: Int, comment: String?) {
        val token = session.getBearerToken() ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.rateJob(
                    token, jobId, SubmitRatingRequest(score = score, comment = comment?.ifBlank { null })
                )
                if (response.isSuccessful) {
                    showMessage(getString(R.string.rating_submitted))
                    binding.buttonRate.isEnabled = false
                } else if (response.code() == 409) {
                    showMessage(getString(R.string.error_already_rated))
                } else {
                    showMessage(getString(R.string.error_server_generic))
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to submit rating: ${e.message}")
                showMessage(getString(R.string.error_network))
            }
        }
    }

    private fun openOtherPartyProfile() {
        val job = currentJob ?: return
        val myId = session.getUserId()
        val otherId = if (myId == job.postedById) job.acceptedById else job.postedById
        if (otherId == null) return

        val intent = Intent(this, ProfileActivity::class.java)
        intent.putExtra(ProfileActivity.EXTRA_USER_ID, otherId)
        startActivity(intent)
    }

    private fun showMessage(message: String) {
        binding.textMessage.text = message
        binding.textMessage.visibility = View.VISIBLE
    }
}
