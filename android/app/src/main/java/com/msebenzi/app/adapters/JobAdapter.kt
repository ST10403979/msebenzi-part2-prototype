package com.msebenzi.app.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.msebenzi.app.R
import com.msebenzi.app.data.Job
import com.msebenzi.app.databinding.ItemJobCardBinding

/**
 * Displays the "transparent job card" from the Part 1 design: service,
 * location, budget and status, all visible before a worker decides.
 */
class JobAdapter(private val onJobClick: (Job) -> Unit) :
    ListAdapter<Job, JobAdapter.JobViewHolder>(JobDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobViewHolder {
        val binding = ItemJobCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return JobViewHolder(binding)
    }

    override fun onBindViewHolder(holder: JobViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class JobViewHolder(private val binding: ItemJobCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(job: Job) {
            binding.textTitle.text = job.title
            binding.textCategory.text = job.category.replaceFirstChar { it.uppercase() }
            binding.textLocation.text = job.location
            binding.textBudget.text = "R${"%.2f".format(job.budget)}"
            binding.textStatus.text = job.status.replaceFirstChar { it.uppercase() }

            val statusColorRes = when (job.status) {
                "open" -> R.color.status_open
                "accepted" -> R.color.status_accepted
                "completed" -> R.color.status_completed
                else -> R.color.status_cancelled
            }
            binding.textStatus.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(binding.root.context, statusColorRes)
            )

            binding.root.setOnClickListener { onJobClick(job) }
        }
    }

    private class JobDiffCallback : DiffUtil.ItemCallback<Job>() {
        override fun areItemsTheSame(oldItem: Job, newItem: Job) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Job, newItem: Job) = oldItem == newItem
    }
}
