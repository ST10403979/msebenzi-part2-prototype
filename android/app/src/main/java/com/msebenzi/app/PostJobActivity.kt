package com.msebenzi.app

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.msebenzi.app.data.CreateJobRequest
import com.msebenzi.app.data.JobRepository
import com.msebenzi.app.data.SessionManager
import com.msebenzi.app.databinding.ActivityPostJobBinding
import kotlinx.coroutines.launch

class PostJobActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPostJobBinding
    private lateinit var session: SessionManager
    private lateinit var repository: JobRepository

    companion object {
        private const val TAG = "PostJobActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostJobBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)
        repository = JobRepository(this)

        binding.buttonSubmit.setOnClickListener { submitJob() }
    }

    private fun submitJob() {
        val title = binding.inputTitle.text?.toString()?.trim().orEmpty()
        val category = binding.inputCategory.text?.toString()?.trim().orEmpty()
        val location = binding.inputLocation.text?.toString()?.trim().orEmpty()
        val budgetText = binding.inputBudget.text?.toString()?.trim().orEmpty()
        val description = binding.inputDescription.text?.toString()?.trim().orEmpty()

        // ---- Validate every field so a bad/empty input can never crash the app ----
        if (title.isEmpty() || category.isEmpty() || location.isEmpty() || description.isEmpty()) {
            showError(getString(R.string.error_all_fields_required))
            return
        }
        val budget = budgetText.toDoubleOrNull()
        if (budget == null || budget < 0) {
            showError(getString(R.string.error_invalid_budget))
            return
        }

        val token = session.getBearerToken() ?: return

        setLoading(true)
        lifecycleScope.launch {
            val result = repository.createJob(
                token,
                CreateJobRequest(title, description, category, location, budget)
            )
            setLoading(false)

            result.onSuccess {
                Log.i(TAG, "Job posted: $title")
                finish() // return to job feed, which refreshes onResume
            }.onFailure { e ->
                Log.e(TAG, "Failed to post job", e)
                showError(getString(R.string.error_posting_job))
            }
        }
    }

    private fun showError(message: String) {
        binding.textError.text = message
        binding.textError.visibility = View.VISIBLE
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.buttonSubmit.isEnabled = !loading
    }
}
