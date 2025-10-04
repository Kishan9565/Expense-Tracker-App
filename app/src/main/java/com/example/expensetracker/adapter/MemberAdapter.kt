package com.example.expensetracker.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetracker.databinding.ItemMemberBinding

class MemberAdapter(
    private val members: MutableList<String>,
    private val onDelete: (String, Int) -> Unit
) : RecyclerView.Adapter<MemberAdapter.MemberViewHolder>() {

    inner class MemberViewHolder(val binding: ItemMemberBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val binding = ItemMemberBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MemberViewHolder(binding)
    }

    override fun getItemCount(): Int = members.size

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        val name = members[position]
        holder.binding.tvMemberName.text = name

        holder.binding.ivDeleteMember.setOnClickListener {
            onDelete(name, position)
        }
    }
}
