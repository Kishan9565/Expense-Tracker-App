package com.example.expensetracker.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.expensetracker.adapter.EventAdapter
import com.example.expensetracker.adapter.MemberAdapter
import com.example.expensetracker.data.AppDatabase
import com.example.expensetracker.data.groupModels.GroupEvent
import com.example.expensetracker.databinding.ActivityGroupSplitterBinding
import com.example.expensetracker.databinding.BottomSheetAddEventBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.view.LayoutInflater
import android.view.WindowManager
import com.example.expensetracker.data.groupModels.toEntity
import android.widget.Toast
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetracker.data.groupModels.EventEntity
import com.example.expensetracker.data.groupModels.EventMemberEntity
import com.example.expensetracker.repository.GroupRepository
import com.example.expensetracker.repository.TransactionRepository
import com.example.expensetracker.viewmodel.GroupViewModel
import com.example.expensetracker.viewmodel.GroupViewModelFactory
import com.example.expensetracker.viewmodel.TransactionViewModel
import com.example.expensetracker.viewmodel.TransactionViewModelFactory
import com.google.android.material.snackbar.Snackbar

class GroupSplitter : AppCompatActivity() {

    private lateinit var binding: ActivityGroupSplitterBinding
    private lateinit var viewModel: GroupViewModel
    private lateinit var eventAdapter: EventAdapter
    private lateinit var memberAdapter: MemberAdapter
    private val eventsList = mutableListOf<GroupEvent>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupSplitterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
        }

        val repository = GroupRepository(AppDatabase.getInstance(this).groupDao())
        viewModel = ViewModelProvider(
            this,
            GroupViewModelFactory(repository)
        )[GroupViewModel::class.java]

        setupRecyclerView()
        observeEvents()

        binding.btnAddEvent.setOnClickListener {
            showAddEventBottomSheet()
        }
    }

    private fun setupRecyclerView() {
        eventAdapter = EventAdapter { selectedEvent ->
            val intent = Intent(this, GroupSplitterDetails::class.java).apply {
                putExtra("eventId", selectedEvent.id)
                putExtra("eventName", selectedEvent.name)
                putExtra("budget", selectedEvent.budget)
                putExtra("createdAt", selectedEvent.createdAt)
                putStringArrayListExtra("members", ArrayList(selectedEvent.members))
            }
            startActivity(intent)
        }
        binding.rvEvents.layoutManager = LinearLayoutManager(this)
        binding.rvEvents.adapter = eventAdapter


        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val deletedEvent = eventAdapter.currentList[position]

                viewModel.deleteEvent(deletedEvent.toEntity())

                Snackbar.make(binding.root, "Event deleted", Snackbar.LENGTH_LONG)
                    .setAction("Undo") {
                        viewModel.addEventAndGetId(deletedEvent.toEntity()) { eventId ->
                            deletedEvent.members.forEach { name ->
                                val member = EventMemberEntity(eventId = eventId.toInt(), memberName = name)
                                viewModel.addMember(member)
                            }
                        }
                    }.show()
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.rvEvents)

    }


    private fun observeEvents() {
        viewModel.getEventsWithMembers().observe(this) { eventWithMembersList ->
            val groupEvents = eventWithMembersList.map { eventWithMembers ->
                GroupEvent(
                    id = eventWithMembers.event.eventId,
                    name = eventWithMembers.event.eventName,
                    budget = eventWithMembers.event.budget,
                    createdAt = eventWithMembers.event.createdAt,
                    members = eventWithMembers.members.map { it.memberName }
                )
            }

            eventAdapter.submitList(groupEvents)
        }
    }


    private fun showAddEventBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val bottomSheetBinding = BottomSheetAddEventBinding.inflate(LayoutInflater.from(this))
        dialog.setContentView(bottomSheetBinding.root)

        val memberList = mutableListOf<String>()
        memberAdapter = MemberAdapter(memberList) { memberName, position ->
            memberList.removeAt(position)
            memberAdapter.notifyItemRemoved(position)
        }

        bottomSheetBinding.rvMembers.apply {
            layoutManager = LinearLayoutManager(this@GroupSplitter)
            adapter = memberAdapter
        }

        bottomSheetBinding.btnAddMember.setOnClickListener {
            val member = bottomSheetBinding.etMemberName.text.toString().trim()
            if (member.isNotEmpty()) {
                memberList.add(member)
                memberAdapter.notifyItemInserted(memberList.size - 1)
                bottomSheetBinding.etMemberName.setText("")
            }
        }

        bottomSheetBinding.btnSaveEvent.setOnClickListener {
            val name = bottomSheetBinding.etEventName.text.toString().trim()
            val budgetStr = bottomSheetBinding.etEventBudget.text.toString().trim()

            if (name.isEmpty() || budgetStr.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val budget = budgetStr.toDoubleOrNull()
            if (budget == null) {
                Toast.makeText(this, "Invalid budget", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val event = EventEntity(
                eventName = name,
                budget = budget,
                createdAt = System.currentTimeMillis()
            )


            viewModel.addEventAndGetId(event) { eventId ->
                memberList.forEach { memberName ->
                    val memberEntity = EventMemberEntity(
                        eventId = eventId.toInt(),
                        memberName = memberName
                    )
                    viewModel.addMember(memberEntity)
                }
            }

            dialog.dismiss()
        }

        bottomSheetBinding.btnCancelEvent.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

}
