package com.example.expensetracker.data.groupModels


import androidx.room.*

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val eventId: Int = 0,
    val eventName: String,
    val budget: Double,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "event_members",
    foreignKeys = [ForeignKey(
        entity = EventEntity::class,
        parentColumns = ["eventId"],
        childColumns = ["eventId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("eventId")]
)
data class EventMemberEntity(
    @PrimaryKey(autoGenerate = true) val memberId: Int = 0,
    val eventId: Int,
    val memberName: String
)

@Entity(
    tableName = "group_expenses",
    foreignKeys = [ForeignKey(
        entity = EventEntity::class,
        parentColumns = ["eventId"],
        childColumns = ["eventId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("eventId")]
)
data class GroupExpenseEntity(
    @PrimaryKey(autoGenerate = true) val expenseId: Int = 0,
    val eventId: Int,
    val amount: Double,
    val payerId: Int,
    val note: String,
    val category: String,
    val date: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "group_expense_splits",
    foreignKeys = [
        ForeignKey(
            entity = GroupExpenseEntity::class,
            parentColumns = ["expenseId"],
            childColumns = ["expenseId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = EventMemberEntity::class,
            parentColumns = ["memberId"],
            childColumns = ["memberId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("expenseId"), Index("memberId")]
)
data class GroupExpenseSplitEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val expenseId: Int,
    val memberId: Int,
    val contributedAmount: Double
)


data class GroupEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val name: String,

    val budget: Double,

    val members: List<String>,

    val createdAt: Long = System.currentTimeMillis()
)

data class EventWithMembers(
    @Embedded val event: EventEntity,
    @Relation(
        parentColumn = "eventId",
        entityColumn = "eventId"
    )
    val members: List<EventMemberEntity>
)

data class ExpenseDisplay(
    val expenseId: Int,
    val memberName: String,
    val amount: Double,
    val category: String,
    val note: String,
    val date: Long
)

data class Settlement(
    val memberName: String,
    val netAmount: Double
)

data class PaymentTransaction(
    val from: String,
    val to: String,
    val amount: Double
)


