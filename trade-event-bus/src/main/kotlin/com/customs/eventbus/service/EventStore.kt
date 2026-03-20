package com.customs.eventbus.service

import com.customs.eventbus.model.EventType
import com.customs.eventbus.model.StoredEvent
import com.customs.eventbus.model.TradeEvent
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class EventStore(val capacity: Int = 10_000) {

    private val buffer = arrayOfNulls<StoredEvent>(capacity)
    private var head = 0
    private var size = 0
    private var nextOffset: Long = 0
    private val lock = ReentrantReadWriteLock()

    fun store(event: TradeEvent): StoredEvent = lock.write {
        val stored = StoredEvent(offset = nextOffset, event = event)
        buffer[head] = stored
        head = (head + 1) % capacity
        if (size < capacity) size++
        nextOffset++
        stored
    }

    fun getAll(): List<StoredEvent> = lock.read {
        buildList()
    }

    fun getByType(type: EventType): List<StoredEvent> = lock.read {
        buildList().filter { it.event.type == type }
    }

    fun getFromOffset(offset: Long): List<StoredEvent> = lock.read {
        buildList().filter { it.offset >= offset }
    }

    fun getFromOffset(offset: Long, type: EventType?): List<StoredEvent> = lock.read {
        val events = buildList().filter { it.offset >= offset }
        if (type != null) events.filter { it.event.type == type } else events
    }

    fun getLatest(count: Int): List<StoredEvent> = lock.read {
        buildList().takeLast(count)
    }

    fun currentOffset(): Long = lock.read { nextOffset }

    fun currentSize(): Int = lock.read { size }

    fun clear() = lock.write {
        buffer.fill(null)
        head = 0
        size = 0
        nextOffset = 0
    }

    private fun buildList(): List<StoredEvent> {
        if (size == 0) return emptyList()

        val result = mutableListOf<StoredEvent>()
        val start = if (size < capacity) 0 else head
        for (i in 0 until size) {
            val index = (start + i) % capacity
            buffer[index]?.let { result.add(it) }
        }
        return result
    }
}
