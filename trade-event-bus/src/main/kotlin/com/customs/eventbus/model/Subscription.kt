package com.customs.eventbus.model

import kotlinx.serialization.Serializable

@Serializable
data class Subscription(
    val id: String,
    val eventTypes: List<EventType>,
    val callbackUrl: String,
    val active: Boolean = true
)

@Serializable
data class SubscribeRequest(
    val eventTypes: List<EventType>,
    val callbackUrl: String
)

@Serializable
data class SubscriptionResponse(
    val id: String,
    val eventTypes: List<EventType>,
    val callbackUrl: String,
    val active: Boolean
)
