package com.example.expensetracker.repository

import androidx.lifecycle.LiveData
import com.example.expensetracker.data.*
import com.example.expensetracker.data.groupModels.EventEntity
import com.example.expensetracker.data.groupModels.EventMemberEntity
import com.example.expensetracker.data.groupModels.EventWithMembers
import com.example.expensetracker.data.groupModels.ExpenseDisplay
import com.example.expensetracker.data.groupModels.GroupDao
import com.example.expensetracker.data.groupModels.GroupExpenseEntity
import com.example.expensetracker.data.groupModels.GroupExpenseSplitEntity
import com.example.expensetracker.data.groupModels.PaymentTransaction
import com.example.expensetracker.data.groupModels.Settlement
import com.example.expensetracker.data.groupModels.SplitDisplay

class GroupRepository(private val dao: GroupDao) {

    fun getAllEvents(): LiveData<List<EventEntity>> = dao.getAllEvents()

    suspend fun insertEvent(event: EventEntity): Long = dao.insertEvent(event)

    suspend fun insertMember(member: EventMemberEntity) = dao.insertMember(member)

    fun getMembers(eventId: Int): LiveData<List<EventMemberEntity>> = dao.getMembers(eventId)

    suspend fun insertExpense(expense: GroupExpenseEntity): Long = dao.insertGroupExpense(expense)

    fun getExpenses(eventId: Int): LiveData<List<GroupExpenseEntity>> = dao.getExpenses(eventId)

    suspend fun insertSplits(splits: List<GroupExpenseSplitEntity>) = dao.insertSplits(splits)

    fun getExpenseDisplayList(eventId: Int): LiveData<List<ExpenseDisplay>> = dao.getExpenseDisplayList(eventId)

    fun getSplits(expenseId: Int): LiveData<List<SplitDisplay>> = dao.getSplitWithNames(expenseId)

    fun getEventsWithMembers(): LiveData<List<EventWithMembers>> {
        return dao.getEventsWithMembers()
    }

    suspend fun deleteEvent(event: EventEntity) {
        dao.deleteEvent(event)
    }

    suspend fun calculateSettlement(eventId: Int): List<Settlement> {
        val members = dao.getMembersRaw(eventId)
        val expenses = dao.getExpensesRaw(eventId)
        val splits = dao.getAllSplitsForEvent(eventId)

        val memberContributions = mutableMapOf<Int, Double>()
        val memberOwed = mutableMapOf<Int, Double>()

        members.forEach {
            memberContributions[it.memberId] = 0.0
            memberOwed[it.memberId] = 0.0
        }

        expenses.forEach { expense ->
            val expenseSplits = splits.filter { it.expenseId == expense.expenseId }

            val totalAmount = expense.amount
            val perHead = if (expenseSplits.isNotEmpty()) totalAmount / expenseSplits.size else 0.0

            expenseSplits.forEach { split ->

                memberContributions[split.memberId] =
                    memberContributions[split.memberId]?.plus(split.contributedAmount) ?: 0.0


                memberOwed[split.memberId] =
                    memberOwed[split.memberId]?.plus(perHead) ?: perHead
            }
        }

        return members.map { member ->
            val contributed = memberContributions[member.memberId] ?: 0.0
            val shouldPay = memberOwed[member.memberId] ?: 0.0
            Settlement(
                memberName = member.memberName,
                netAmount = contributed - shouldPay
            )
        }
    }

    suspend fun getMembersList(eventId: Int): List<EventMemberEntity> {
        return dao.getMembersList(eventId)
    }

    suspend fun calculateWhoPaysWhom(eventId: Int): List<PaymentTransaction> {
        val settlements = calculateSettlement(eventId)

        val creditors = mutableListOf<Pair<String, Double>>()
        val debtors = mutableListOf<Pair<String, Double>>()

        for (settlement in settlements) {
            val name = settlement.memberName
            val net = settlement.netAmount

            if (net > 0.0) {
                creditors.add(name to net)
            } else if (net < 0.0) {
                debtors.add(name to net)
            }
        }

        val transactions = mutableListOf<PaymentTransaction>()

        var i = 0
        var j = 0

        while (i < debtors.size && j < creditors.size) {
            val (debtorName, debtAmount) = debtors[i]
            val (creditorName, creditAmount) = creditors[j]

            val settleAmount = minOf(-debtAmount, creditAmount)
            transactions.add(PaymentTransaction(debtorName, creditorName, settleAmount))

            val updatedDebt = debtAmount + settleAmount
            val updatedCredit = creditAmount - settleAmount

            if (updatedDebt == 0.0) i++ else debtors[i] = debtorName to updatedDebt
            if (updatedCredit == 0.0) j++ else creditors[j] = creditorName to updatedCredit
        }

        return transactions
    }

    suspend fun deleteExpense(expense: GroupExpenseEntity) {
        dao.deleteGroupExpense(expense)
    }

     fun getTotalExpense(eventId: Int): LiveData<Double> {
        return dao.getTotalExpensesForEvent(eventId)
    }
    suspend fun deleteExpenseById(expenseId: Int) {
        dao.deleteExpenseById(expenseId)
    }



}
