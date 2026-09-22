package com.msebenzi.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.msebenzi.app.adapters.JobAdapter
import com.msebenzi.app.data.JobRepository
import com.msebenzi.app.data.SessionManager
import com.msebenzi.app.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var session: SessionManager
    private lateinit var repository: JobRepository
    private lateinit var adapter: JobAdapter

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)
        repository = JobRepository(this)

        if (!session.isLoggedIn()) {
            // Defensive check — should not normally happen since SplashActivity
            // already routes logged-out users to LoginActivity.
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setSupportActionBar(binding.toolbar)

        adapter = JobAdapter { job ->
            val intent = Intent(this, JobDetailActivity::class.java)
            intent.putExtra(JobDetailActivity.EXTRA_JOB_ID, job.id)
            startActivity(intent)
        }
        binding.recyclerJobs.layoutManager = LinearLayoutManager(this)
        binding.recyclerJobs.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { loadJobs() }

        binding.fabPostJob.setOnClickListener {
            startActivity(Intent(this, PostJobActivity::class.java))
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_alerts -> {
                    startActivity(Intent(this, AlertsActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }

        loadJobs()
    }

    override fun onResume() {
        super.onResume()
        loadJobs() // refresh in case a job was posted or accepted elsewhere
    }

    private fun loadJobs() {
        val token = session.getBearerToken() ?: return
        binding.swipeRefresh.isRefreshing = true

        lifecycleScope.launch {
            val result = repository.getJobFeed(token)
            binding.swipeRefresh.isRefreshing = false

            result.onSuccess { jobs ->
                adapter.submitList(jobs)
                binding.textEmpty.visibility = if (jobs.isEmpty()) View.VISIBLE else View.GONE
            }.onFailure { e ->
                Log.e(TAG, "Failed to load jobs", e)
                Toast.makeText(this@MainActivity, R.string.error_loading_jobs, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
