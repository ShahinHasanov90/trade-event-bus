package com.customs.eventbus.model

import kotlinx.serialization.Serializable

@Serializable
enum class EventType {
    DECLARATION_SUBMITTED,
    RISK_SCORED,
    INSPECTION_SCHEDULED,
    CLEARANCE_GRANTED,
    ALERT_RAISED
}

@Serializable
data class TradeEvent(
    val id: String,
    val type: EventType,
    val payload: String,
    val timestamp: Long,
    val source: String
)

@Serializable
data class PublishRequest(
    val type: EventType,
    val payload: String,
    val source: String
)

@Serializable
data class StoredEvent(
    val offset: Long,
    val event: TradeEvent
)
