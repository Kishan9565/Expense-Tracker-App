package com.example.expensetracker.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.expensetracker.adapter.TransactionTableAdapter
import com.example.expensetracker.data.AppDatabase
import com.example.expensetracker.databinding.ActivityViewReportBinding
import com.example.expensetracker.repository.TransactionRepository
import com.example.expensetracker.viewmodel.TransactionViewModel
import com.example.expensetracker.viewmodel.TransactionViewModelFactory

class ViewReport : AppCompatActivity() {

    private lateinit var binding: ActivityViewReportBinding
    private lateinit var viewModel: TransactionViewModel

    private lateinit var incomeAdapter: TransactionTableAdapter
    private lateinit var expenseAdapter: TransactionTableAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        binding = ActivityViewReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
        }


        val repository = TransactionRepository(AppDatabase.getInstance(this).transactionDao())
        viewModel = ViewModelProvider(
            this,
            TransactionViewModelFactory(repository)
        )[TransactionViewModel::class.java]


        incomeAdapter = TransactionTableAdapter()
        expenseAdapter = TransactionTableAdapter()

        setupRecyclerViews()

        val (start, end) = getIntentRange()
        val type = intent.getStringExtra("type") ?: "All"


        observeData(start, end, type)

        binding.navHome.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        binding.navGroup.setOnClickListener {
            startActivity(Intent(this, GroupSplitter::class.java))
            finish()
        }
    }

    private fun setupRecyclerViews() {
        binding.incomeRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ViewReport)
            adapter = incomeAdapter
            addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
        }

        binding.expenseRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ViewReport)
            adapter = expenseAdapter
            addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
        }
    }

    private fun observeData(start: Long, end: Long, type: String) {
        viewModel.getTransactionsForRange(start, end).observe(this) { list ->
            val income = list.filter { it.type == "Income" }
            val expense = list.filter { it.type == "Expense" }

            when (type) {
                "Income" -> {
                    incomeAdapter.setData(income)
                    binding.incomeRecyclerView.visibility = View.VISIBLE
                    binding.expenseRecyclerView.visibility = View.GONE
                    binding.tvExpenseHeader.visibility = View.GONE
                    binding.expenseHeaderRow.visibility = View.GONE
                }
                "Expense" -> {
                    expenseAdapter.setData(expense)
                    binding.incomeRecyclerView.visibility = View.GONE
                    binding.tvIncomeHeader.visibility = View.GONE
                    binding.incomeHeaderRow.visibility = View.GONE
                    binding.expenseRecyclerView.visibility = View.VISIBLE
                }
                else -> {
                    incomeAdapter.setData(income)
                    expenseAdapter.setData(expense)
                    binding.incomeRecyclerView.visibility = View.VISIBLE
                    binding.expenseRecyclerView.visibility = View.VISIBLE
                }
            }
        }
    }


    private fun getIntentRange(): Pair<Long, Long> {
        val start = intent.getLongExtra("startDate", System.currentTimeMillis())
        val end = intent.getLongExtra("endDate", System.currentTimeMillis())
        return start to end
    }
}
