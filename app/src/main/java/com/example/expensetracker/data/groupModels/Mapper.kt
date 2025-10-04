package com.example.expensetracker.data.groupModels

fun GroupEvent.toEntity(): EventEntity {
    return EventEntity(
        eventId = this.id,
        eventName = this.name,
        budget = this.budget,
        createdAt = this.createdAt
    )
}
