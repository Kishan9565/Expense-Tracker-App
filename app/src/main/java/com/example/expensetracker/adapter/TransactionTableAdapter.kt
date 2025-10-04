package com.example.expensetracker.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetracker.data.Transaction
import com.example.expensetracker.databinding.ItemTransactionBinding
import java.text.SimpleDateFormat
import java.util.*

class TransactionTableAdapter(
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var transactionList: List<Transaction> = emptyList()

    fun setData(transactions: List<Transaction>) {
        transactionList = transactions
        notifyDataSetChanged()
    }

    private val VIEW_TYPE_EXPENSE = 0
    private val VIEW_TYPE_INCOME = 1

    override fun getItemViewType(position: Int): Int {
        return if (transactionList[position].type == "Expense") VIEW_TYPE_EXPENSE else VIEW_TYPE_INCOME
    }

    // Expense ViewHolder
    inner class ExpenseViewHolder(val binding: ItemTransactionBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(transaction: Transaction) {
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val dateString = dateFormat.format(Date(transaction.date))

            binding.tvDate.text = dateString
            binding.tvAmount.text = "- ₹${transaction.amount}"
            binding.tvPaymentMethod.text = transaction.paymentMethod
        }
    }

    // Income ViewHolder
    inner class IncomeViewHolder(val binding: ItemTransactionBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(transaction: Transaction) {
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val dateString = dateFormat.format(Date(transaction.date))

            binding.tvDate.text = dateString
            binding.tvAmount.text = "+ ₹${transaction.amount}"
            binding.tvPaymentMethod.text = transaction.category
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_EXPENSE) {
            val binding = ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            ExpenseViewHolder(binding)
        } else {
            val binding = ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            IncomeViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val transaction = transactionList[position]
        when (holder) {
            is ExpenseViewHolder -> holder.bind(transaction)
            is IncomeViewHolder -> holder.bind(transaction)
        }
    }

    override fun getItemCount(): Int = transactionList.size
}
