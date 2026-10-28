package com.kishan9565.expensetracker.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "split_groups")
data class SplitGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0, 
    val name: String,
    val createdAtMillis: Long, 
    val archivedAtMillis: Long? = null
)

@Entity(
    tableName = "split_members",
    foreignKeys = [ForeignKey(entity = SplitGroupEntity::class, parentColumns = ["id"], childColumns = ["groupId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("groupId"), Index(value = ["groupId", "nameKey"], unique = true)]
)
data class SplitMemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0, 
    val groupId: Long,
    val name: String, 
    val nameKey: String,
    val isSelf: Boolean = false, 
    val upiVpa: String? = null
)

enum class SplitType { EQUAL, EXACT, PERCENT, SHARES }

@Entity(tableName = "split_expenses", indices = [Index("groupId")])
data class SplitExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0, 
    val groupId: Long,
    val paidByMemberId: Long, 
    val amountPaise: Long, 
    val note: String, 
    val splitType: SplitType,
    val localDate: Long, 
    val occurredAtMillis: Long, 
    val deletedAtMillis: Long? = null
)

@Entity(tableName = "split_shares", primaryKeys = ["expenseId", "memberId"])
data class SplitShareEntity(
    val expenseId: Long, 
    val memberId: Long, 
    val sharePaise: Long
)

@Entity(tableName = "split_settlements", indices = [Index("groupId")])
data class SettlementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0, 
    val groupId: Long,
    val fromMemberId: Long, 
    val toMemberId: Long, 
    val amountPaise: Long, 
    val occurredAtMillis: Long, 
    val viaUpi: Boolean
)

data class SplitExpenseWithShares(
    @Embedded val expense: SplitExpenseEntity,
    @Relation(parentColumn = "id", entityColumn = "expenseId")
    val shares: List<SplitShareEntity>
)
