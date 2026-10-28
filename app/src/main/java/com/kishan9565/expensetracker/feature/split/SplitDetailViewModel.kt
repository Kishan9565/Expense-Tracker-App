package com.kishan9565.expensetracker.feature.split

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kishan9565.expensetracker.core.time.AppClock
import com.kishan9565.expensetracker.data.db.SettlementEntity
import com.kishan9565.expensetracker.data.db.SplitExpenseEntity
import com.kishan9565.expensetracker.data.db.SplitGroupEntity
import com.kishan9565.expensetracker.data.db.SplitMemberEntity
import com.kishan9565.expensetracker.data.db.SplitShareEntity
import com.kishan9565.expensetracker.data.db.SplitType
import com.kishan9565.expensetracker.domain.model.ExpenseWithShares
import com.kishan9565.expensetracker.domain.model.SplitShareModel
import com.kishan9565.expensetracker.domain.model.Transfer
import com.kishan9565.expensetracker.domain.model.allocate
import com.kishan9565.expensetracker.domain.model.computeNet
import com.kishan9565.expensetracker.domain.model.minimizeTransfers
import com.kishan9565.expensetracker.domain.repository.SplitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SplitDetailState(
    val group: SplitGroupEntity? = null,
    val members: List<SplitMemberEntity> = emptyList(),
    val expenses: List<ExpenseWithSharesUi> = emptyList(),
    val transfers: List<TransferUi> = emptyList()
)

data class ExpenseWithSharesUi(
    val expense: SplitExpenseEntity,
    val payerName: String,
    val sharesSummary: String
)

data class TransferUi(
    val fromMemberId: Long,
    val fromName: String,
    val toMemberId: Long,
    val toName: String,
    val amountPaise: Long,
    val toVpa: String?
)

sealed interface SplitDetailAction {
    data class AddMember(val name: String) : SplitDetailAction
    data class AddExpense(
        val amountPaise: Long,
        val note: String,
        val paidByMemberId: Long,
        val selectedMemberIds: List<Long>
    ) : SplitDetailAction
    data class Settle(val transfer: TransferUi, val viaUpi: Boolean) : SplitDetailAction
    data class DeleteExpense(val expenseId: Long) : SplitDetailAction
}

sealed interface SplitDetailEvent {
    data class Snack(val message: String) : SplitDetailEvent
}

@HiltViewModel
class SplitDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: SplitRepository,
    private val clock: AppClock
) : ViewModel() {

    private val groupId: Long = checkNotNull(savedStateHandle["groupId"])

    val state: StateFlow<SplitDetailState> = combine(
        repo.observeGroup(groupId),
        repo.observeMembers(groupId),
        repo.observeExpensesWithShares(groupId),
        repo.observeSettlements(groupId)
    ) { group, members, expensesWithShares, settlements ->
        val memberMap = members.associateBy { it.id }

        // Map DB models to Domain models for math
        val domainExpenses = expensesWithShares.map { e ->
            ExpenseWithShares(
                paidByMemberId = e.expense.paidByMemberId,
                amountPaise = e.expense.amountPaise,
                shares = e.shares.map { SplitShareModel(it.memberId, it.sharePaise) }
            )
        }

        // Run math
        val net = computeNet(members.map { it.id }, domainExpenses, settlements)
        val transfers = minimizeTransfers(net)

        val transfersUi = transfers.map { t ->
            val fromM = memberMap[t.from]
            val toM = memberMap[t.to]
            TransferUi(
                fromMemberId = t.from,
                fromName = fromM?.name ?: "?",
                toMemberId = t.to,
                toName = toM?.name ?: "?",
                amountPaise = t.amountPaise,
                toVpa = toM?.upiVpa
            )
        }

        val expensesUi = expensesWithShares.map { e ->
            val payer = memberMap[e.expense.paidByMemberId]?.name ?: "?"
            val summary = if (e.shares.size == members.size) "Split equally" else "Split between ${e.shares.size}"
            ExpenseWithSharesUi(e.expense, payer, summary)
        }

        SplitDetailState(
            group = group,
            members = members,
            expenses = expensesUi,
            transfers = transfersUi
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SplitDetailState())

    private val _events = Channel<SplitDetailEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: SplitDetailAction) {
        when (action) {
            is SplitDetailAction.AddMember -> {
                viewModelScope.launch {
                    val name = action.name.trim()
                    if (name.isNotEmpty()) {
                        try {
                            repo.addMember(SplitMemberEntity(groupId = groupId, name = name, nameKey = name.lowercase()))
                            _events.send(SplitDetailEvent.Snack("Member added"))
                        } catch (e: Exception) {
                            _events.send(SplitDetailEvent.Snack("Name must be unique"))
                        }
                    }
                }
            }
            is SplitDetailAction.AddExpense -> {
                viewModelScope.launch {
                    val allocations = allocate(action.amountPaise, action.selectedMemberIds.map { 1L })
                    val shares = action.selectedMemberIds.zip(allocations).map { (mId, amt) ->
                        SplitShareEntity(expenseId = 0, memberId = mId, sharePaise = amt)
                    }
                    val expense = SplitExpenseEntity(
                        groupId = groupId,
                        paidByMemberId = action.paidByMemberId,
                        amountPaise = action.amountPaise,
                        note = action.note,
                        splitType = SplitType.EQUAL,
                        localDate = clock.today().toEpochDay(),
                        occurredAtMillis = clock.now().toEpochMilli()
                    )
                    repo.addExpense(expense, shares)
                    _events.send(SplitDetailEvent.Snack("Expense added"))
                }
            }
            is SplitDetailAction.DeleteExpense -> {
                viewModelScope.launch {
                    repo.markExpenseDeleted(action.expenseId, clock.now().toEpochMilli())
                    _events.send(SplitDetailEvent.Snack("Expense deleted"))
                }
            }
            is SplitDetailAction.Settle -> {
                viewModelScope.launch {
                    val settlement = SettlementEntity(
                        groupId = groupId,
                        fromMemberId = action.transfer.fromMemberId,
                        toMemberId = action.transfer.toMemberId,
                        amountPaise = action.transfer.amountPaise,
                        occurredAtMillis = clock.now().toEpochMilli(),
                        viaUpi = action.viaUpi
                    )
                    repo.addSettlement(settlement)
                    _events.send(SplitDetailEvent.Snack("Settlement recorded"))
                }
            }
        }
    }
}
