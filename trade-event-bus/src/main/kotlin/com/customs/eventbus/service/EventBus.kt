package com.customs.eventbus.service

import com.customs.eventbus.model.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class EventBus(
    private val eventStore: EventStore,
    private val dispatcher: Dispatcher
) {
    private val subscribers = ConcurrentHashMap<String, Subscription>()
    private val _eventFlow = MutableSharedFlow<StoredEvent>(extraBufferCapacity = 1000)
    val eventFlow: SharedFlow<StoredEvent> = _eventFlow.asSharedFlow()

    fun publish(request: PublishRequest): StoredEvent {
        val event = TradeEvent(
            id = UUID.randomUUID().toString(),
            type = request.type,
            payload = request.payload,
            timestamp = System.currentTimeMillis(),
            source = request.source
        )

        val stored = eventStore.store(event)
        dispatcher.dispatch(event, subscribers.values.toList())
        _eventFlow.tryEmit(stored)
        return stored
    }

    fun subscribe(request: SubscribeRequest): Subscription {
        require(request.eventTypes.isNotEmpty()) { "At least one event type must be specified" }
        require(request.callbackUrl.isNotBlank()) { "Callback URL must not be blank" }

        val subscription = Subscription(
            id = UUID.randomUUID().toString(),
            eventTypes = request.eventTypes,
            callbackUrl = request.callbackUrl,
            active = true
        )
        subscribers[subscription.id] = subscription
        return subscription
    }

    fun unsubscribe(id: String): Boolean {
        return subscribers.remove(id) != null
    }

    fun getSubscription(id: String): Subscription? {
        return subscribers[id]
    }

    fun getAllSubscriptions(): List<Subscription> {
        return subscribers.values.toList()
    }

    fun replay(fromOffset: Long, type: EventType? = null): List<StoredEvent> {
        return eventStore.getFromOffset(fromOffset, type)
    }

    fun filterEvents(type: EventType): List<StoredEvent> {
        return eventStore.getByType(type)
    }

    fun getEvents(
        type: EventType? = null,
        fromOffset: Long? = null,
        limit: Int? = null
    ): List<StoredEvent> {
        var events = when {
            fromOffset != null && type != null -> eventStore.getFromOffset(fromOffset, type)
            fromOffset != null -> eventStore.getFromOffset(fromOffset)
            type != null -> eventStore.getByType(type)
            else -> eventStore.getAll()
        }
        if (limit != null) {
            events = events.takeLast(limit)
        }
        return events
    }
}
