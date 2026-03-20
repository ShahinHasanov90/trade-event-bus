package com.customs.eventbus.service

import com.customs.eventbus.model.Subscription
import com.customs.eventbus.model.TradeEvent
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

class Dispatcher(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    private val logger = LoggerFactory.getLogger(Dispatcher::class.java)

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = false
                ignoreUnknownKeys = true
            })
        }
        engine {
            requestTimeout = 5_000
        }
    }

    fun dispatch(event: TradeEvent, subscribers: List<Subscription>) {
        val targets = subscribers.filter { sub ->
            sub.active && sub.eventTypes.contains(event.type)
        }

        targets.forEach { subscriber ->
            scope.launch {
                try {
                    client.post(subscriber.callbackUrl) {
                        contentType(ContentType.Application.Json)
                        setBody(event)
                    }
                    logger.info("Dispatched event ${event.id} to subscriber ${subscriber.id}")
                } catch (e: Exception) {
                    logger.error(
                        "Failed to dispatch event ${event.id} to subscriber ${subscriber.id} " +
                                "at ${subscriber.callbackUrl}: ${e.message}"
                    )
                }
            }
        }
    }

    fun close() {
        client.close()
        scope.cancel()
    }
}
