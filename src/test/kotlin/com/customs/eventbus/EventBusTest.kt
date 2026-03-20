package com.customs.eventbus

import com.customs.eventbus.model.*
import com.customs.eventbus.service.Dispatcher
import com.customs.eventbus.service.EventBus
import com.customs.eventbus.service.EventStore
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EventBusTest {

    private lateinit var eventStore: EventStore
    private lateinit var dispatcher: Dispatcher
    private lateinit var eventBus: EventBus

    @BeforeEach
    fun setUp() {
        eventStore = EventStore(capacity = 100)
        dispatcher = Dispatcher()
        eventBus = EventBus(eventStore, dispatcher)
    }

    @Test
    fun `publish stores event and returns stored event with offset`() {
        val request = PublishRequest(
            type = EventType.DECLARATION_SUBMITTED,
            payload = """{"declarationId": "DEC-001"}""",
            source = "customs-portal"
        )
        val stored = eventBus.publish(request)

        assertEquals(0L, stored.offset)
        assertEquals(EventType.DECLARATION_SUBMITTED, stored.event.type)
        assertEquals("customs-portal", stored.event.source)
        assertNotNull(stored.event.id)
        assertTrue(stored.event.timestamp > 0)
    }

    @Test
    fun `publish increments offset for each event`() {
        val request = PublishRequest(EventType.RISK_SCORED, "{}", "risk-engine")
        val first = eventBus.publish(request)
        val second = eventBus.publish(request)
        val third = eventBus.publish(request)

        assertEquals(0L, first.offset)
        assertEquals(1L, second.offset)
        assertEquals(2L, third.offset)
    }

    @Test
    fun `subscribe creates subscription with unique id`() {
        val request = SubscribeRequest(
            eventTypes = listOf(EventType.ALERT_RAISED, EventType.RISK_SCORED),
            callbackUrl = "http://localhost:9090/webhook"
        )
        val subscription = eventBus.subscribe(request)

        assertNotNull(subscription.id)
        assertEquals(2, subscription.eventTypes.size)
        assertTrue(subscription.active)
        assertEquals("http://localhost:9090/webhook", subscription.callbackUrl)
    }

    @Test
    fun `subscribe rejects empty event types`() {
        val request = SubscribeRequest(
            eventTypes = emptyList(),
            callbackUrl = "http://localhost:9090/webhook"
        )
        assertThrows(IllegalArgumentException::class.java) {
            eventBus.subscribe(request)
        }
    }

    @Test
    fun `subscribe rejects blank callback URL`() {
        val request = SubscribeRequest(
            eventTypes = listOf(EventType.CLEARANCE_GRANTED),
            callbackUrl = "  "
        )
        assertThrows(IllegalArgumentException::class.java) {
            eventBus.subscribe(request)
        }
    }

    @Test
    fun `unsubscribe removes existing subscription`() {
        val sub = eventBus.subscribe(
            SubscribeRequest(listOf(EventType.ALERT_RAISED), "http://localhost/hook")
        )
        assertTrue(eventBus.unsubscribe(sub.id))
        assertNull(eventBus.getSubscription(sub.id))
    }

    @Test
    fun `unsubscribe returns false for unknown id`() {
        assertFalse(eventBus.unsubscribe("non-existent-id"))
    }

    @Test
    fun `replay returns events from given offset`() {
        eventBus.publish(PublishRequest(EventType.DECLARATION_SUBMITTED, "{}", "src"))
        eventBus.publish(PublishRequest(EventType.RISK_SCORED, "{}", "src"))
        eventBus.publish(PublishRequest(EventType.CLEARANCE_GRANTED, "{}", "src"))

        val replayed = eventBus.replay(fromOffset = 1)
        assertEquals(2, replayed.size)
        assertEquals(1L, replayed[0].offset)
        assertEquals(2L, replayed[1].offset)
    }

    @Test
    fun `filterEvents returns only matching event types`() {
        eventBus.publish(PublishRequest(EventType.DECLARATION_SUBMITTED, "{}", "src"))
        eventBus.publish(PublishRequest(EventType.RISK_SCORED, "{}", "src"))
        eventBus.publish(PublishRequest(EventType.DECLARATION_SUBMITTED, "{}", "src"))

        val filtered = eventBus.filterEvents(EventType.DECLARATION_SUBMITTED)
        assertEquals(2, filtered.size)
        assertTrue(filtered.all { it.event.type == EventType.DECLARATION_SUBMITTED })
    }

    @Test
    fun `getEvents with type and offset filters correctly`() {
        eventBus.publish(PublishRequest(EventType.DECLARATION_SUBMITTED, "{}", "src"))
        eventBus.publish(PublishRequest(EventType.RISK_SCORED, "{}", "src"))
        eventBus.publish(PublishRequest(EventType.DECLARATION_SUBMITTED, "{}", "src"))
        eventBus.publish(PublishRequest(EventType.RISK_SCORED, "{}", "src"))

        val events = eventBus.getEvents(type = EventType.RISK_SCORED, fromOffset = 2)
        assertEquals(1, events.size)
        assertEquals(EventType.RISK_SCORED, events[0].event.type)
        assertEquals(3L, events[0].offset)
    }

    @Test
    fun `getEvents with limit returns last N events`() {
        repeat(10) {
            eventBus.publish(PublishRequest(EventType.ALERT_RAISED, """{"seq":$it}""", "src"))
        }

        val events = eventBus.getEvents(limit = 3)
        assertEquals(3, events.size)
        assertEquals(7L, events[0].offset)
        assertEquals(8L, events[1].offset)
        assertEquals(9L, events[2].offset)
    }

    @Test
    fun `getAllSubscriptions returns all registered subscribers`() {
        eventBus.subscribe(SubscribeRequest(listOf(EventType.ALERT_RAISED), "http://a.com/hook"))
        eventBus.subscribe(SubscribeRequest(listOf(EventType.RISK_SCORED), "http://b.com/hook"))

        val subs = eventBus.getAllSubscriptions()
        assertEquals(2, subs.size)
    }
}
