package com.example.expensetracker.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetracker.databinding.ItemSettlementBinding
import com.example.expensetracker.adapter.SettlementAdapter.SettlementViewHolder
import com.example.expensetracker.data.groupModels.Settlement


class SettlementAdapter(private var settlements: List<Settlement>) :
    RecyclerView.Adapter<SettlementViewHolder>() {



    inner class SettlementViewHolder(val binding: ItemSettlementBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettlementViewHolder {
        val binding = ItemSettlementBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SettlementViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SettlementViewHolder, position: Int) {
        val item = settlements[position]

        val absAmount = String.format("₹%.2f", kotlin.math.abs(item.netAmount))
        holder.binding.tvMemberName.text = item.memberName
        holder.binding.tvAmount.text = absAmount

        if (item.netAmount > 0) {

            holder.binding.tvStatus.text = "Gets"
            holder.binding.tvStatus.setTextColor(Color.parseColor("#2E7D32")) // Green
            holder.binding.tvAmount.setTextColor(Color.parseColor("#2E7D32"))
            holder.binding.ivDirection.setImageResource(android.R.drawable.arrow_down_float)
            holder.binding.ivDirection.setColorFilter(Color.parseColor("#2E7D32"))
        } else if (item.netAmount < 0) {

            holder.binding.tvStatus.text = "Owes"
            holder.binding.tvStatus.setTextColor(Color.parseColor("#C62828")) // Red
            holder.binding.tvAmount.setTextColor(Color.parseColor("#C62828"))
            holder.binding.ivDirection.setImageResource(android.R.drawable.arrow_up_float)
            holder.binding.ivDirection.setColorFilter(Color.parseColor("#C62828"))
        } else {

            holder.binding.tvStatus.text = "Settled"
            holder.binding.tvStatus.setTextColor(Color.GRAY)
            holder.binding.tvAmount.setTextColor(Color.GRAY)
            holder.binding.ivDirection.setImageResource(android.R.drawable.checkbox_on_background)
            holder.binding.ivDirection.setColorFilter(Color.GRAY)
        }
    }

    override fun getItemCount(): Int = settlements.size

    fun updateData(newList: List<Settlement>) {
        settlements = newList
        notifyDataSetChanged()
    }
}
