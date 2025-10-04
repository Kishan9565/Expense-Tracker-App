package com.example.expensetracker.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.groupModels.EventEntity
import com.example.expensetracker.data.groupModels.EventMemberEntity
import com.example.expensetracker.data.groupModels.EventWithMembers
import com.example.expensetracker.data.groupModels.GroupExpenseEntity
import com.example.expensetracker.data.groupModels.GroupExpenseSplitEntity
import com.example.expensetracker.data.groupModels.PaymentTransaction
import com.example.expensetracker.data.groupModels.Settlement
import com.example.expensetracker.repository.GroupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GroupViewModel(private val repo: GroupRepository) : ViewModel() {

    private val _settlements = MutableLiveData<List<Settlement>>()
    val settlements: LiveData<List<Settlement>> get() = _settlements

    private val _totalExpense = MutableLiveData<Double>()
    val totalExpense: LiveData<Double> = _totalExpense

    private val _whoPaysWhom = MutableLiveData<List<PaymentTransaction>>()
    val whoPaysWhom: LiveData<List<PaymentTransaction>> get() = _whoPaysWhom

    fun getAllEvents() = repo.getAllEvents()

    fun getMembers(eventId: Int) = repo.getMembers(eventId)

    fun getExpenses(eventId: Int) = repo.getExpenses(eventId)

    fun getSplits(expenseId: Int) = repo.getSplits(expenseId)

    fun addEvent(event: EventEntity) = viewModelScope.launch {
        repo.insertEvent(event)
    }

    fun addMember(member: EventMemberEntity) = viewModelScope.launch {
        repo.insertMember(member)
    }

    fun addExpenseWithSplits(
        expense: GroupExpenseEntity,
        splits: List<GroupExpenseSplitEntity>
    ) = viewModelScope.launch {
        val expenseId = repo.insertExpense(expense)
        val updatedSplits = splits.map { it.copy(expenseId = expenseId.toInt()) }
        repo.insertSplits(updatedSplits)

        loadNetSettlementForEvent(expense.eventId)
    }


    fun addEventAndGetId(event: EventEntity, onResult: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repo.insertEvent(event)
            onResult(id)
        }
    }

    fun getEventsWithMembers(): LiveData<List<EventWithMembers>> {
        return repo.getEventsWithMembers()
    }

    fun deleteEvent(event: EventEntity) = viewModelScope.launch {
        repo.deleteEvent(event)
    }

    fun getNetSettlementForEvent(eventId: Int): LiveData<List<Settlement>> {
        val result = MutableLiveData<List<Settlement>>()
        viewModelScope.launch {
            val settlements = withContext(Dispatchers.IO) {
                repo.calculateSettlement(eventId)
            }
            result.postValue(settlements)
        }
        return result
    }

    fun getMembersList(eventId: Int, onResult: (List<EventMemberEntity>) -> Unit) {
        viewModelScope.launch {
            val members = repo.getMembersList(eventId)
            onResult(members)
        }
    }

    fun loadNetSettlementForEvent(eventId: Int) {
        viewModelScope.launch {
            val settlements = withContext(Dispatchers.IO) {
                repo.calculateSettlement(eventId)
            }
            _settlements.postValue(settlements)
        }
    }


    fun getWhoPaysWhom(eventId: Int, onResult: (List<PaymentTransaction>) -> Unit) {
        viewModelScope.launch {
            val transactions = repo.calculateWhoPaysWhom(eventId)
            onResult(transactions)
        }

    }

        fun getExpenseDisplayList(eventId: Int) = repo.getExpenseDisplayList(eventId)

     fun getTotalExpense(eventId: Int): LiveData<Double> {
        return repo.getTotalExpense(eventId)
    }

    fun deleteExpenseById(expenseId: Int, eventId: Int) {
        viewModelScope.launch {
            repo.deleteExpenseById(expenseId)
            loadNetSettlementForEvent(eventId)
        }
    }

    fun loadWhoPaysWhom(eventId: Int) {
        viewModelScope.launch {
            val transactions = repo.calculateWhoPaysWhom(eventId)
            _whoPaysWhom.postValue(transactions)
        }
    }

}
