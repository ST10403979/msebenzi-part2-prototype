package com.msebenzi.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.msebenzi.app.adapters.NotificationAdapter
import com.msebenzi.app.data.RetrofitClient
import com.msebenzi.app.data.SessionManager
import com.msebenzi.app.databinding.ActivityAlertsBinding
import kotlinx.coroutines.launch

/**
 * Feature: Targeted job alerts. Shows the notifications the backend
 * generated for this specific user (see backend/utils/notify.js) — new
 * matching jobs for workers, and job-status updates for both roles.
 */
class AlertsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlertsBinding
    private lateinit var session: SessionManager
    private lateinit var adapter: NotificationAdapter

    companion object {
        private const val TAG = "AlertsActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlertsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)
        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = NotificationAdapter { notification ->
            markRead(notification.id)
            if (notification.jobId != null) {
                val intent = Intent(this, JobDetailActivity::class.java)
                intent.putExtra(JobDetailActivity.EXTRA_JOB_ID, notification.jobId)
                startActivity(intent)
            }
        }
        binding.recyclerNotifications.layoutManager = LinearLayoutManager(this)
        binding.recyclerNotifications.adapter = adapter

        loadNotifications()
    }

    private fun loadNotifications() {
        val token = session.getBearerToken() ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getNotifications(token)
                if (response.isSuccessful && response.body() != null) {
                    val notifications = response.body()!!
                    adapter.submitList(notifications)
                    binding.textEmpty.visibility = if (notifications.isEmpty()) View.VISIBLE else View.GONE
                } else {
                    Toast.makeText(this@AlertsActivity, R.string.error_server_generic, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load notifications: ${e.message}")
                Toast.makeText(this@AlertsActivity, R.string.error_network, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun markRead(id: Int) {
        val token = session.getBearerToken() ?: return
        lifecycleScope.launch {
            try {
                RetrofitClient.api.markNotificationRead(token, id)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to mark notification read: ${e.message}")
            }
        }
    }
}
