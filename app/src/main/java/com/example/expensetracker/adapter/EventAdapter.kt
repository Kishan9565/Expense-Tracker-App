package com.example.expensetracker.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetracker.data.groupModels.GroupEvent
import com.example.expensetracker.databinding.ItemSplitterBinding
import java.text.SimpleDateFormat
import java.util.*

class EventAdapter(
    private val onViewDetailsClicked: (GroupEvent) -> Unit
) : ListAdapter<GroupEvent, EventAdapter.EventViewHolder>(EventDiffCallback()) {

    inner class EventViewHolder(val binding: ItemSplitterBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(event: GroupEvent) {
            binding.tvEventName.text = event.name
            binding.tvEventBudget.text = "Budget: ₹${event.budget}"
            binding.tvMemberCount.text = "Members: ${event.members.size}"

            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            binding.tvEventDate.text = "Created on: ${sdf.format(Date(event.createdAt))}"

            binding.btnViewDetails.setOnClickListener {
                onViewDetailsClicked(event)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemSplitterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class EventDiffCallback : DiffUtil.ItemCallback<GroupEvent>() {
    override fun areItemsTheSame(oldItem: GroupEvent, newItem: GroupEvent): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: GroupEvent, newItem: GroupEvent): Boolean {
        return oldItem == newItem
    }
}
