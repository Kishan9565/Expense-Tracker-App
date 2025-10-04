package com.example.expensetracker.data.groupModels

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface GroupDao {



    @Insert suspend fun insertEvent(event: EventEntity): Long
    @Query("SELECT * FROM events ORDER BY createdAt DESC")
    fun getAllEvents(): LiveData<List<EventEntity>>
    @Delete suspend fun deleteEvent(event: EventEntity)


    @Query("""
    SELECT e.expenseId, e.amount, e.category, e.note, e.date, m.memberName
    FROM group_expenses e
    JOIN event_members m ON e.payerId = m.memberId
    WHERE e.eventId = :eventId
""")
    fun getExpenseDisplayList(eventId: Int): LiveData<List<ExpenseDisplay>>


    @Transaction
    @Query("SELECT * FROM events ORDER BY createdAt DESC")
    fun getEventsWithMembers(): LiveData<List<EventWithMembers>>


    @Insert suspend fun insertMember(member: EventMemberEntity)
    @Query("SELECT * FROM event_members WHERE eventId = :eventId")
    fun getMembers(eventId: Int): LiveData<List<EventMemberEntity>>
    @Delete suspend fun deleteMember(member: EventMemberEntity)


    @Insert suspend fun insertGroupExpense(expense: GroupExpenseEntity): Long
    @Query("SELECT * FROM group_expenses WHERE eventId = :eventId ORDER BY date DESC")
    fun getExpenses(eventId: Int): LiveData<List<GroupExpenseEntity>>
    @Delete suspend fun deleteGroupExpense(expense: GroupExpenseEntity)


    @Insert suspend fun insertSplits(splits: List<GroupExpenseSplitEntity>)
    @Query("SELECT * FROM group_expense_splits WHERE expenseId = :expenseId")
    fun getSplits(expenseId: Int): LiveData<List<GroupExpenseSplitEntity>>

    @Query("SELECT * FROM event_members WHERE eventId = :eventId")
    suspend fun getMembersList(eventId: Int): List<EventMemberEntity>

    @Query("SELECT * FROM group_expenses WHERE eventId = :eventId")
    suspend fun getExpensesList(eventId: Int): List<GroupExpenseEntity>

    @Query("""
    SELECT ges.*
    FROM group_expense_splits ges
    INNER JOIN group_expenses ge ON ge.expenseId = ges.expenseId
    WHERE ge.eventId = :eventId
""")
    suspend fun getSplitsForEvent(eventId: Int): List<GroupExpenseSplitEntity>



    @Query("""
        SELECT s.contributedAmount, m.memberName
        FROM group_expense_splits s
        JOIN event_members m ON s.memberId = m.memberId
        WHERE s.expenseId = :expenseId
    """)
    fun getSplitWithNames(expenseId: Int): LiveData<List<SplitDisplay>>



    @Query("SELECT * FROM event_members WHERE eventId = :eventId")
    suspend fun getMembersRaw(eventId: Int): List<EventMemberEntity>

    @Query("SELECT * FROM group_expenses WHERE eventId = :eventId")
    suspend fun getExpensesRaw(eventId: Int): List<GroupExpenseEntity>

    @Query("""
    SELECT * FROM group_expense_splits 
    WHERE expenseId IN (SELECT expenseId FROM group_expenses WHERE eventId = :eventId)
""")
    suspend fun getAllSplitsForEvent(eventId: Int): List<GroupExpenseSplitEntity>

    @Query("DELETE FROM group_expenses WHERE expenseId = :expenseId")
    suspend fun deleteExpenseById(expenseId: Int)



    @Query("SELECT SUM(amount) FROM group_expenses WHERE eventId = :eventId")
    fun getTotalExpensesForEvent(eventId: Int): LiveData<Double>
}




data class SplitDisplay(
    val memberName: String,
    val contributedAmount: Double
)


