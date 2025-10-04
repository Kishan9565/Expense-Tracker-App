package com.example.expensetracker.ui

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.expensetracker.R
import com.example.expensetracker.adapter.ExpenseAdapter
import com.example.expensetracker.data.AppDatabase
import com.example.expensetracker.data.DailyExpense
import com.example.expensetracker.data.Transaction
import com.example.expensetracker.databinding.ActivityMainBinding
import com.example.expensetracker.databinding.DialogExpenseBinding
import com.example.expensetracker.databinding.DialogPdfReportBinding
import com.example.expensetracker.databinding.DilogIncomeBinding
import com.example.expensetracker.repository.TransactionRepository
import com.example.expensetracker.viewmodel.TransactionViewModel
import com.example.expensetracker.viewmodel.TransactionViewModelFactory
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: TransactionViewModel
    private lateinit var adapter: ExpenseAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val repository = TransactionRepository(AppDatabase.getInstance(this).transactionDao())
        viewModel = ViewModelProvider(
            this,
            TransactionViewModelFactory(repository)
        )[TransactionViewModel::class.java]

        restData()
        setupRecyclerView()
        observeData()
        setupTopCards()
        setupMonthSelector()
        viewReport()
        groupSplitter()

    }

    override fun onResume() {
        super.onResume()
        val months = viewModel.getMonthList()
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        binding.spinnerMonth.setText(months[currentMonth], false)
    }


    private fun setupMonthSelector() {
        val months = viewModel.getMonthList()

        val monthAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, months)
        binding.spinnerMonth.setAdapter(monthAdapter)
        binding.spinnerMonth.dropDownHeight = 500

        binding.spinnerMonth.setOnClickListener {
            binding.spinnerMonth.showDropDown()
        }


        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        binding.spinnerMonth.setText(months[currentMonth], false)


        binding.spinnerMonth.setOnItemClickListener { _, _, position, _ ->
            val year = Calendar.getInstance().get(Calendar.YEAR)
            viewModel.setSelectedMonth(year, position)
        }
    }

    private fun viewReport(){
        binding.navViewReport.setOnClickListener {
            showPdfReportDialog()
        }
    }

    private fun groupSplitter() {
        binding.navGroup.setOnClickListener {
            val intent = Intent(this, GroupSplitter::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun restData(){
        binding.ivDropDown.setOnClickListener {
            val popup = PopupMenu(this, it)
            popup.menuInflater.inflate(R.menu.menu_main, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_reset -> {
                        viewModel.deleteAllTransactions()
                        Toast.makeText(this, "All data deleted!", Toast.LENGTH_SHORT).show()
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    private fun setupRecyclerView() {
        adapter = ExpenseAdapter { transaction ->
            viewModel.deleteTransaction(transaction)
        }
        binding.rvExpenseList.layoutManager = LinearLayoutManager(this)
        binding.rvExpenseList.adapter = adapter
    }


    private fun observeData() {

        viewModel.transactionsForMonth.observe(this) { transactions ->
            adapter.setData(transactions)
        }


        viewModel.totalBalance.observe(this) { balance ->
            binding.tvTotalBalance.text = "₹$balance"
        }


        viewModel.dailyExpensesForChart.observe(this) { dailyList ->
            if (dailyList.isNotEmpty()) {
                val labels = dailyList.map {
                    SimpleDateFormat("dd", Locale.getDefault()).format(Date(it.dayTimestamp))
                }
                val values = dailyList.map { it.totalAmount.toFloat() }

                val chartData = labels.zip(values)
                binding.lineChart.gradientFillColors = intArrayOf(
                    Color.parseColor("#80426D6A"),
                    Color.TRANSPARENT
                )
                binding.lineChart.animation.duration = 1000L
                binding.lineChart.animate(chartData)
            }
        }


        val now = Calendar.getInstance()
        val startOfDay = now.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val endOfDay = now.apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        viewModel.getExpenseByDay(startOfDay, endOfDay).observe(this) { amount ->
            binding.tvDayExpense.text = "₹$amount"
        }

        val (startOfWeek, endOfWeek) = viewModel.getCurrentWeekRange()
        viewModel.getExpenseByWeek(startOfWeek, endOfWeek).observe(this) { amount ->
            binding.tvWeekExpense.text = "₹$amount"
        }

        val startOfMonth = now.apply { set(Calendar.DAY_OF_MONTH, 1) }.timeInMillis
        val endOfMonth = now.apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        }.timeInMillis
        viewModel.getExpenseByMonth(startOfMonth, endOfMonth).observe(this) { amount ->
            binding.tvMonthExpense.text = "₹$amount"
        }

        viewModel.loadTransactions()
    }

    private fun setupTopCards() {
        binding.cardExpense.setOnClickListener { showExpenseDialog() }
        binding.cardIncome.setOnClickListener { showIncomeDialog() }
    }





    private fun showIncomeDialog() {
        val dialogBinding = DilogIncomeBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()

        val sources = listOf("Salary", "Business", "Gift", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, sources)
        dialogBinding.etIncomeSource.setAdapter(adapter)
        dialogBinding.etIncomeSource.setOnClickListener { dialogBinding.etIncomeSource.showDropDown() }

        dialogBinding.etIncomeDate.setOnClickListener {
            val calendar = Calendar.getInstance()


            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, day ->
                    val selectedDate = "$day/${month + 1}/$year"
                    dialogBinding.etIncomeDate.setText(selectedDate)
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    dialogBinding.etIncomeDate.tag =
                        sdf.parse(selectedDate)?.time ?: System.currentTimeMillis()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )


            datePickerDialog.datePicker.minDate = calendar.timeInMillis
            datePickerDialog.datePicker.maxDate = calendar.timeInMillis

            datePickerDialog.show()
        }


        dialogBinding.btnCancelIncome.setOnClickListener { dialog.dismiss() }
        dialogBinding.btnAddIncome.setOnClickListener {
            val amount = dialogBinding.etIncomeAmount.text.toString().toDoubleOrNull()
            val source = dialogBinding.etIncomeSource.text.toString()
            val note = dialogBinding.etIncomeNote.text.toString()
            val date = dialogBinding.etIncomeDate.tag as? Long ?: System.currentTimeMillis()

            if (amount != null && source.isNotEmpty() && note.isNotEmpty()) {
                val transaction = Transaction(
                    type = "Income",
                    category = source,
                    paymentMethod = "N/A",
                    amount = amount,
                    note = note,
                    date = date
                )
                viewModel.addTransaction(transaction)
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Please enter valid details", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun showExpenseDialog() {
        val dialogBinding = DialogExpenseBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()

        val categories = listOf("Food", "Shopping", "Transport", "Bills", "Other")
        val catAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        dialogBinding.spinnerCategory.setAdapter(catAdapter)
        dialogBinding.spinnerCategory.setOnClickListener { dialogBinding.spinnerCategory.showDropDown() }

        val payments = listOf("Cash", "Card", "UPI", "Wallet")
        val payAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, payments)
        dialogBinding.spinnerPayment.setAdapter(payAdapter)
        dialogBinding.spinnerPayment.setOnClickListener { dialogBinding.spinnerPayment.showDropDown() }

        dialogBinding.etDate.setOnClickListener {
            val calendar = Calendar.getInstance()

            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, day ->
                    val selectedDate = "$day/${month + 1}/$year"
                    dialogBinding.etDate.setText(selectedDate)
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    dialogBinding.etDate.tag = sdf.parse(selectedDate)?.time ?: System.currentTimeMillis()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )


            datePickerDialog.datePicker.minDate = calendar.timeInMillis
            datePickerDialog.datePicker.maxDate = calendar.timeInMillis

            datePickerDialog.show()
        }


        dialogBinding.btnCancel.setOnClickListener { dialog.dismiss() }
        dialogBinding.btnSave.setOnClickListener {
            val amount = dialogBinding.etAmount.text.toString().toDoubleOrNull()
            val category = dialogBinding.spinnerCategory.text.toString()
            val payment = dialogBinding.spinnerPayment.text.toString()
            val comment = dialogBinding.etComment.text.toString()
            val date = dialogBinding.etDate.tag as? Long ?: System.currentTimeMillis()

            if (amount != null && category.isNotEmpty() && payment.isNotEmpty()) {
                val transaction = Transaction(
                    type = "Expense",
                    category = category,
                    paymentMethod = payment,
                    amount = amount,
                    note = comment,
                    date = date
                )
                viewModel.addTransaction(transaction)
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Please enter valid details", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun showPdfReportDialog() {
        val dialogBinding = DialogPdfReportBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

        val calendar = Calendar.getInstance()

        fun showDatePicker(editText: EditText) {
            DatePickerDialog(this, { _, year, month, day ->
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                calendar.set(year, month, day)
                editText.setText(sdf.format(calendar.time))
                editText.tag = calendar.timeInMillis
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        dialogBinding.etStartDate.setOnClickListener { showDatePicker(dialogBinding.etStartDate) }
        dialogBinding.etEndDate.setOnClickListener { showDatePicker(dialogBinding.etEndDate) }

        dialogBinding.btnGeneratePDF.setOnClickListener {
            val startDate = dialogBinding.etStartDate.tag as? Long ?: System.currentTimeMillis()
            val endDate = dialogBinding.etEndDate.tag as? Long ?: System.currentTimeMillis()

            val startCal = Calendar.getInstance().apply {
                timeInMillis = startDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val endCal = Calendar.getInstance().apply {
                timeInMillis = endDate
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }

            val start = startCal.timeInMillis
            val end = endCal.timeInMillis

            val type = if (dialogBinding.radioType.checkedRadioButtonId == R.id.radioIncome) "Income" else "Expense"


            dialog.dismiss()
            val intent = Intent(this, ViewReport::class.java)
            intent.putExtra("startDate", start)
            intent.putExtra("endDate", end)
            intent.putExtra("type", type)
            startActivity(intent)
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }


}
