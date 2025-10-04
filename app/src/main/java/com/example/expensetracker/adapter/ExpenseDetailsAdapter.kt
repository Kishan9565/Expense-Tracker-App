package com.example.expensetracker.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetracker.data.groupModels.ExpenseDisplay
import com.example.expensetracker.databinding.ItemExpenssDetailsBinding
import java.text.SimpleDateFormat
import java.util.*

class ExpenseDetailsAdapter(
    private var expenses: List<ExpenseDisplay>
) : RecyclerView.Adapter<ExpenseDetailsAdapter.ExpenseViewHolder>() {

    inner class ExpenseViewHolder(val binding: ItemExpenssDetailsBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val binding = ItemExpenssDetailsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ExpenseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val item = expenses[position]
        holder.binding.apply {
            tvMemberName.text = item.memberName
            tvAmount.text = "₹${item.amount}"
            tvCategory.text = item.category
            tvNote.text = item.note
            tvDate.text = formatDate(item.date)
        }
    }

    override fun getItemCount(): Int = expenses.size

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun getExpenseIdAt(position: Int): Int {
        return expenses[position].expenseId
    }

    fun getExpenseAt(position: Int): ExpenseDisplay {
        return expenses[position]
    }

    fun updateData(newList: List<ExpenseDisplay>) {
         expenses = newList
        notifyDataSetChanged()
    }

    fun removeAt(position: Int) {
        expenses = expenses.toMutableList().also { it.removeAt(position) }
        notifyItemRemoved(position)
    }

    fun restoreAt(expense: ExpenseDisplay, position: Int) {
        expenses = expenses.toMutableList().also { it.add(position, expense) }
        notifyItemInserted(position)
    }
}
