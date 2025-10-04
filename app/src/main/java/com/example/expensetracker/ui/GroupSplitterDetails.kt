package com.example.expensetracker.ui

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.expensetracker.R
import com.example.expensetracker.adapter.ExpenseAdapter
import com.example.expensetracker.adapter.SettlementAdapter
import com.example.expensetracker.data.AppDatabase
import com.example.expensetracker.data.groupModels.EventMemberEntity
import com.example.expensetracker.data.groupModels.GroupExpenseEntity
import com.example.expensetracker.data.groupModels.GroupExpenseSplitEntity
import com.example.expensetracker.data.groupModels.Settlement
import com.example.expensetracker.databinding.DialogAddExpenseBinding
import com.example.expensetracker.databinding.ActivityGroupSplitterDetailsBinding
import com.example.expensetracker.repository.GroupRepository
import com.example.expensetracker.viewmodel.GroupViewModel
import com.example.expensetracker.viewmodel.GroupViewModelFactory
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetracker.adapter.ExpenseDetailsAdapter
import com.example.expensetracker.adapter.WhoPaysWhomAdapter
import com.example.expensetracker.databinding.DialogPdfReportBinding
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class GroupSplitterDetails : AppCompatActivity() {

    private lateinit var binding: ActivityGroupSplitterDetailsBinding
    private lateinit var viewModel: GroupViewModel
    private lateinit var splitAdapter: SettlementAdapter
    private lateinit var expenseAdapter: ExpenseDetailsAdapter
    private lateinit var whoPaysWhomAdapter: WhoPaysWhomAdapter

    private var eventId: Int = -1
    private lateinit var eventName: String
    private var eventBudget: Double = 0.0
    private var members: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        binding = ActivityGroupSplitterDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom)
            insets
        }

        eventId = intent.getIntExtra("eventId", -1)
        eventName = intent.getStringExtra("eventName") ?: "Unnamed Event"
        eventBudget = intent.getDoubleExtra("budget", 0.0)
        members = intent.getStringArrayListExtra("members") ?: emptyList()


        val repo = GroupRepository(AppDatabase.getInstance(this).groupDao())
        viewModel = ViewModelProvider(this, GroupViewModelFactory(repo))[GroupViewModel::class.java]


        binding.tvEventName.text = eventName
        binding.tvBudget.text = "Budget: ₹$eventBudget"
        binding.tvMembers.text = "Members: ${members.joinToString(", ")}"



        setupAdaptersAndRecyclerViews()

        setupClickListeners()

        binding.btnAddExpense.setOnClickListener {
            viewModel.getMembersList(eventId) { memberEntities ->
                showAddExpenseDialog(memberEntities)
            }
        }

        showSection(Section.EXPENSES)

        viewModel.getTotalExpense(eventId).observe(this) { total ->
            binding.tvTotalSpent.text = "Total Spent: ₹${String.format("%.2f", total ?: 0.0)}"
        }

        binding.navHome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.navViewReport.setOnClickListener {
            showPdfReportDialog()
        }

    }


    private fun setupAdaptersAndRecyclerViews() {

        expenseAdapter = ExpenseDetailsAdapter(emptyList())
        binding.rvExpenses.apply {
            layoutManager = LinearLayoutManager(this@GroupSplitterDetails)
            adapter = expenseAdapter
        }
        viewModel.getExpenseDisplayList(eventId).observe(this) { list ->
            expenseAdapter.updateData(list)
        }

        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val deletedExpense = expenseAdapter.getExpenseAt(position)
                val expenseId = deletedExpense.expenseId


                expenseAdapter.removeAt(position)
                Snackbar.make(binding.root, "Expense deleted", Snackbar.LENGTH_LONG)
                    .setAction("Undo") {
                        expenseAdapter.restoreAt(deletedExpense, position)

                        viewModel.getWhoPaysWhom(eventId) { transactions ->
                            whoPaysWhomAdapter.updateData(transactions)
                        }
                    }
                    .addCallback(object : Snackbar.Callback() {
                        override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                            if (event != DISMISS_EVENT_ACTION) {
                                viewModel.deleteExpenseById(expenseId, eventId)
                                viewModel.getWhoPaysWhom(eventId) { transactions ->
                                    whoPaysWhomAdapter.updateData(transactions)
                                }
                            }
                        }
                    }).show()
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {

                val itemView = viewHolder.itemView
                val background = ColorDrawable(Color.parseColor("#FF5252"))
                background.setBounds(
                    itemView.right + dX.toInt(),
                    itemView.top,
                    itemView.right,
                    itemView.bottom
                )
                background.draw(c)

                val icon = ContextCompat.getDrawable(this@GroupSplitterDetails, R.drawable.delete)
                icon?.let {
                    val iconMargin = (itemView.height - it.intrinsicHeight) / 2
                    val iconTop = itemView.top + iconMargin
                    val iconBottom = iconTop + it.intrinsicHeight
                    val iconLeft = itemView.right - iconMargin - it.intrinsicWidth
                    val iconRight = itemView.right - iconMargin

                    it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    it.draw(c)
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(binding.rvExpenses)

        splitAdapter = SettlementAdapter(emptyList())
        binding.rvSplitDetails.apply {
            layoutManager = LinearLayoutManager(this@GroupSplitterDetails)
            adapter = splitAdapter
        }
        viewModel.settlements.observe(this) { settlements ->
            splitAdapter.updateData(settlements)
        }
        viewModel.loadNetSettlementForEvent(eventId)


        whoPaysWhomAdapter = WhoPaysWhomAdapter(emptyList())
        binding.rvWhoPaysWhom.apply {
            layoutManager = LinearLayoutManager(this@GroupSplitterDetails)
            adapter = whoPaysWhomAdapter
        }

        viewModel.getWhoPaysWhom(eventId) { transactions ->
            whoPaysWhomAdapter.updateData(transactions)
        }

    }

    private fun setupClickListeners() {
        binding.cardExpenses.setOnClickListener {
            showSection(Section.EXPENSES)
        }

        binding.cardSplitDetails.setOnClickListener {
            showSection(Section.SPLIT_DETAILS)
        }

        binding.cardWhoPaysWhom.setOnClickListener {
            showSection(Section.PAYMENTS)
        }
    }

    private enum class Section { EXPENSES, SPLIT_DETAILS, PAYMENTS }

    private fun showSection(section: Section) {
        when (section) {
            Section.EXPENSES -> {
                binding.rvExpenses.visibility = android.view.View.VISIBLE
                binding.rvSplitDetails.visibility = android.view.View.GONE
                binding.rvWhoPaysWhom.visibility = android.view.View.GONE
                binding.btnAddExpense.visibility = android.view.View.VISIBLE
            }
            Section.SPLIT_DETAILS -> {
                binding.rvExpenses.visibility = android.view.View.GONE
                binding.rvSplitDetails.visibility = android.view.View.VISIBLE
                binding.rvWhoPaysWhom.visibility = android.view.View.GONE
                binding.btnAddExpense.visibility = android.view.View.GONE
            }
            Section.PAYMENTS -> {
                binding.rvExpenses.visibility = android.view.View.GONE
                binding.rvSplitDetails.visibility = android.view.View.GONE
                binding.rvWhoPaysWhom.visibility = android.view.View.VISIBLE
                binding.btnAddExpense.visibility = android.view.View.GONE
            }
        }
    }

    private fun showAddExpenseDialog(memberEntities: List<EventMemberEntity>) {
        val dialogBinding = DialogAddExpenseBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()


        val categories = listOf("Food", "Travel", "Stay", "Shopping", "Activities", "Others")
        dialogBinding.actvCategory.setAdapter(
            android.widget.ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        )
        dialogBinding.actvCategory.setText("Others", false)

        val memberNames = memberEntities.map { it.memberName }
        dialogBinding.actvPaidBy.setAdapter(
            android.widget.ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, memberNames)
        )

        dialogBinding.btnSaveExpense.setOnClickListener {
            val amount = dialogBinding.etAmount.text.toString().toDoubleOrNull()
            val note = dialogBinding.etNote.text.toString().trim()
            val category = dialogBinding.actvCategory.text.toString().ifBlank { "Others" }
            val paidByName = dialogBinding.actvPaidBy.text.toString().trim()

            if (amount == null || paidByName.isEmpty()) {
                Toast.makeText(this, "Please fill valid data", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val payer = memberEntities.find { it.memberName == paidByName }
            if (payer == null) {
                Toast.makeText(this, "Invalid payer selected", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val expense = GroupExpenseEntity(
                eventId = eventId,
                payerId = payer.memberId,
                amount = amount,
                note = note,
                category = category
            )


            val splits = memberEntities.map {
                GroupExpenseSplitEntity(
                    expenseId = 0,
                    memberId = it.memberId,
                    contributedAmount = if (it.memberId == payer.memberId) amount else 0.0
                )
            }


            viewModel.addExpenseWithSplits(expense, splits)
            viewModel.getWhoPaysWhom(eventId) { transactions ->
                whoPaysWhomAdapter.updateData(transactions)
            }
            dialog.dismiss()
        }

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
