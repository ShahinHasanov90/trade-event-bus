package com.customs.eventbus

import com.customs.eventbus.model.EventType
import com.customs.eventbus.model.TradeEvent
import com.customs.eventbus.service.EventStore
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EventStoreTest {

    private lateinit var store: EventStore

    @BeforeEach
    fun setUp() {
        store = EventStore(capacity = 5)
    }

    private fun createEvent(type: EventType = EventType.DECLARATION_SUBMITTED, id: String = "evt-1"): TradeEvent {
        return TradeEvent(
            id = id,
            type = type,
            payload = """{"test": true}""",
            timestamp = System.currentTimeMillis(),
            source = "test-source"
        )
    }

    @Test
    fun `store assigns sequential offsets`() {
        val first = store.store(createEvent(id = "e1"))
        val second = store.store(createEvent(id = "e2"))
        val third = store.store(createEvent(id = "e3"))

        assertEquals(0L, first.offset)
        assertEquals(1L, second.offset)
        assertEquals(2L, third.offset)
    }

    @Test
    fun `getAll returns all stored events in order`() {
        store.store(createEvent(id = "e1"))
        store.store(createEvent(id = "e2"))
        store.store(createEvent(id = "e3"))

        val all = store.getAll()
        assertEquals(3, all.size)
        assertEquals("e1", all[0].event.id)
        assertEquals("e2", all[1].event.id)
        assertEquals("e3", all[2].event.id)
    }

    @Test
    fun `circular buffer overwrites oldest events when capacity exceeded`() {
        // Capacity is 5
        repeat(7) { i ->
            store.store(createEvent(id = "e$i"))
        }

        val all = store.getAll()
        assertEquals(5, all.size)
        // Oldest two (e0, e1) should be overwritten
        assertEquals("e2", all[0].event.id)
        assertEquals("e6", all[4].event.id)
        assertEquals(2L, all[0].offset)
        assertEquals(6L, all[4].offset)
    }

    @Test
    fun `getByType filters events correctly`() {
        store.store(createEvent(EventType.DECLARATION_SUBMITTED, "e1"))
        store.store(createEvent(EventType.RISK_SCORED, "e2"))
        store.store(createEvent(EventType.DECLARATION_SUBMITTED, "e3"))
        store.store(createEvent(EventType.ALERT_RAISED, "e4"))

        val declarations = store.getByType(EventType.DECLARATION_SUBMITTED)
        assertEquals(2, declarations.size)
        assertTrue(declarations.all { it.event.type == EventType.DECLARATION_SUBMITTED })
    }

    @Test
    fun `getFromOffset returns events starting from given offset`() {
        repeat(4) { i ->
            store.store(createEvent(id = "e$i"))
        }

        val fromOffset2 = store.getFromOffset(2)
        assertEquals(2, fromOffset2.size)
        assertEquals(2L, fromOffset2[0].offset)
        assertEquals(3L, fromOffset2[1].offset)
    }

    @Test
    fun `getFromOffset with type filters by both offset and type`() {
        store.store(createEvent(EventType.DECLARATION_SUBMITTED, "e0"))
        store.store(createEvent(EventType.RISK_SCORED, "e1"))
        store.store(createEvent(EventType.DECLARATION_SUBMITTED, "e2"))
        store.store(createEvent(EventType.RISK_SCORED, "e3"))

        val result = store.getFromOffset(1, EventType.RISK_SCORED)
        assertEquals(2, result.size)
        assertEquals("e1", result[0].event.id)
        assertEquals("e3", result[1].event.id)
    }

    @Test
    fun `getLatest returns last N events`() {
        repeat(4) { i ->
            store.store(createEvent(id = "e$i"))
        }

        val latest = store.getLatest(2)
        assertEquals(2, latest.size)
        assertEquals("e2", latest[0].event.id)
        assertEquals("e3", latest[1].event.id)
    }

    @Test
    fun `currentOffset reflects total events stored`() {
        assertEquals(0L, store.currentOffset())
        store.store(createEvent(id = "e0"))
        store.store(createEvent(id = "e1"))
        assertEquals(2L, store.currentOffset())
    }

    @Test
    fun `currentSize respects capacity limit`() {
        repeat(7) { i ->
            store.store(createEvent(id = "e$i"))
        }
        assertEquals(5, store.currentSize())
    }

    @Test
    fun `clear resets the store entirely`() {
        repeat(3) { i ->
            store.store(createEvent(id = "e$i"))
        }
        store.clear()

        assertEquals(0, store.currentSize())
        assertEquals(0L, store.currentOffset())
        assertTrue(store.getAll().isEmpty())
    }
}
