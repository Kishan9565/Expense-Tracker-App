package com.example.expensetracker.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetracker.data.groupModels.PaymentTransaction
import com.example.expensetracker.databinding.ItemWhoPaysWhomBinding

class WhoPaysWhomAdapter(
    private var transactions: List<PaymentTransaction>
) : RecyclerView.Adapter<WhoPaysWhomAdapter.TransactionViewHolder>() {

    inner class TransactionViewHolder(val binding: ItemWhoPaysWhomBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemWhoPaysWhomBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val item = transactions[position]

        holder.binding.tvFromInitial.text = item.from.firstOrNull()?.toString() ?: "?"


        holder.binding.tvToInitial.text = item.to.firstOrNull()?.toString() ?: "?"


        holder.binding.tvFrom.text = item.from


        holder.binding.tvTo.text = item.to

        holder.binding.tvAmount.text = "₹%.2f".format(item.amount)
    }


    override fun getItemCount() = transactions.size

    fun updateData(newList: List<PaymentTransaction>) {
        transactions = newList
        notifyDataSetChanged()
    }
}
