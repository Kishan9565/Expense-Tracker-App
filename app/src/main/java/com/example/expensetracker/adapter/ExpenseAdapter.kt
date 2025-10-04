package com.example.expensetracker.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetracker.data.Transaction
import com.example.expensetracker.databinding.ItemExpenseBinding
import com.example.expensetracker.databinding.ItemIncomeBinding
import java.text.SimpleDateFormat
import java.util.*

class ExpenseAdapter(
    private val onDeleteClick: (Transaction) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var transactionList: List<Transaction> = emptyList()

    fun setData(transactions: List<Transaction>) {
        transactionList = transactions
        notifyDataSetChanged()
    }

    // View Types
    private val VIEW_TYPE_EXPENSE = 0
    private val VIEW_TYPE_INCOME = 1

    override fun getItemViewType(position: Int): Int {
        return if (transactionList[position].type == "Expense") VIEW_TYPE_EXPENSE else VIEW_TYPE_INCOME
    }

    // Expense ViewHolder
    inner class ExpenseViewHolder(val binding: ItemExpenseBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(transaction: Transaction) {
            val calendar = Calendar.getInstance().apply { timeInMillis = transaction.date }

            val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
            val dateFormat = SimpleDateFormat("dd", Locale.getDefault())
            val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())

            binding.day.text = dayFormat.format(calendar.time)
            binding.date.text = dateFormat.format(calendar.time)
            binding.month.text = monthFormat.format(calendar.time)

            binding.tvCategory.text = transaction.category
            binding.tvAmount.text = "- ₹${transaction.amount}"
            binding.tvPaymentMethod.text = transaction.paymentMethod
            binding.tvComent.text = transaction.note

            binding.tvDelete.setOnClickListener { onDeleteClick(transaction) }
        }
    }


    inner class IncomeViewHolder(val binding: ItemIncomeBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(transaction: Transaction) {
            val calendar = Calendar.getInstance().apply { timeInMillis = transaction.date }

            val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
            val dateFormat = SimpleDateFormat("dd", Locale.getDefault())
            val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())

            binding.day.text = dayFormat.format(calendar.time)
            binding.date.text = dateFormat.format(calendar.time)
            binding.month.text = monthFormat.format(calendar.time)

            binding.tvCategory.text = transaction.category
            binding.tvAmount.text = "+ ₹${transaction.amount}"
            binding.tvComent.text = transaction.note

            binding.tvDelete.setOnClickListener { onDeleteClick(transaction) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_EXPENSE) {
            val binding = ItemExpenseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            ExpenseViewHolder(binding)
        } else {
            val binding = ItemIncomeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            IncomeViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val transaction = transactionList[position]
        if (holder is ExpenseViewHolder) {
            holder.bind(transaction)
        } else if (holder is IncomeViewHolder) {
            holder.bind(transaction)
        }
    }

    override fun getItemCount(): Int = transactionList.size
}
