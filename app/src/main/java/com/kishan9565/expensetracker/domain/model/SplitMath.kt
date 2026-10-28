package com.kishan9565.expensetracker.domain.model

import com.kishan9565.expensetracker.data.db.SettlementEntity

data class SplitShareModel(val memberId: Long, val sharePaise: Long)
data class ExpenseWithShares(val paidByMemberId: Long, val amountPaise: Long, val shares: List<SplitShareModel>)
data class Transfer(val from: Long, val to: Long, val amountPaise: Long)

fun allocate(totalPaise: Long, weights: List<Long>): List<Long> {
    require(weights.isNotEmpty() && weights.all { it >= 0 } && weights.sum() > 0)
    val sum = weights.sum()
    val result = weights.map { totalPaise * it / sum }.toMutableList()
    var remainder = totalPaise - result.sum()
    val order = weights.indices.sortedByDescending { (totalPaise * weights[it]) % sum }
    var i = 0
    while (remainder > 0) { 
        result[order[i++ % order.size]] += 1
        remainder-- 
    }
    return result
}

fun computeNet(memberIds: List<Long>, expenses: List<ExpenseWithShares>, settlements: List<SettlementEntity>): Map<Long, Long> {
    val net = memberIds.associateWith { 0L }.toMutableMap()
    for (e in expenses) {
        net.merge(e.paidByMemberId, e.amountPaise, Long::plus)
        for (s in e.shares) net.merge(s.memberId, -s.sharePaise, Long::plus)
    }
    for (s in settlements) { 
        net.merge(s.fromMemberId, s.amountPaise, Long::plus)
        net.merge(s.toMemberId, -s.amountPaise, Long::plus) 
    }
    return net
}

fun minimizeTransfers(net: Map<Long, Long>): List<Transfer> {
    require(net.values.sum() == 0L)
    val debtors = net.filterValues { it < 0 }.map { it.key to -it.value }.sortedByDescending { it.second }.toMutableList()
    val creditors = net.filterValues { it > 0 }.map { it.key to it.value }.sortedByDescending { it.second }.toMutableList()
    val out = mutableListOf<Transfer>()
    var i = 0
    var j = 0
    while (i < debtors.size && j < creditors.size) {
        val (d, dAmt) = debtors[i]
        val (c, cAmt) = creditors[j]
        val pay = minOf(dAmt, cAmt)
        out += Transfer(d, c, pay)
        debtors[i] = d to dAmt - pay
        creditors[j] = c to cAmt - pay
        if (debtors[i].second == 0L) i++
        if (creditors[j].second == 0L) j++
    }
    return out
}
