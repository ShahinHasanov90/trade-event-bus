package com.customs.eventbus.routes

import com.customs.eventbus.model.*
import com.customs.eventbus.service.EventBus
import com.customs.eventbus.service.EventStore
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun Application.configureEventRoutes(eventBus: EventBus, eventStore: EventStore) {
    val json = Json { prettyPrint = false }

    routing {
        // Health check
        get("/health") {
            call.respond(
                mapOf(
                    "status" to "UP",
                    "service" to "trade-event-bus",
                    "eventStoreSize" to eventStore.currentSize().toString(),
                    "currentOffset" to eventStore.currentOffset().toString()
                )
            )
        }

        // Publish a trade event
        post("/publish") {
            val request = call.receive<PublishRequest>()
            val stored = eventBus.publish(request)
            call.respond(HttpStatusCode.Created, stored)
        }

        // Query stored events
        get("/events") {
            val type = call.request.queryParameters["type"]?.let {
                try {
                    EventType.valueOf(it)
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid event type: $it"))
                    return@get
                }
            }
            val fromOffset = call.request.queryParameters["from"]?.toLongOrNull()
            val limit = call.request.queryParameters["limit"]?.toIntOrNull()

            val events = eventBus.getEvents(type = type, fromOffset = fromOffset, limit = limit)
            call.respond(events)
        }

        // SSE stream of real-time events
        get("/events/stream") {
            call.respondTextWriter(contentType = ContentType.Text.EventStream) {
                write("event: connected\ndata: {\"status\":\"connected\"}\n\n")
                flush()

                eventBus.eventFlow.onEach { storedEvent ->
                    val data = json.encodeToString(storedEvent)
                    write("event: trade-event\ndata: $data\n\n")
                    flush()
                }.collect()
            }
        }

        // Subscribe to events
        post("/subscribe") {
            val request = call.receive<SubscribeRequest>()
            val subscription = eventBus.subscribe(request)
            call.respond(
                HttpStatusCode.Created,
                SubscriptionResponse(
                    id = subscription.id,
                    eventTypes = subscription.eventTypes,
                    callbackUrl = subscription.callbackUrl,
                    active = subscription.active
                )
            )
        }

        // Unsubscribe
        delete("/subscribe/{id}") {
            val id = call.parameters["id"]
                ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing subscriber ID"))

            val removed = eventBus.unsubscribe(id)
            if (removed) {
                call.respond(HttpStatusCode.OK, mapOf("status" to "unsubscribed", "id" to id))
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Subscriber not found: $id"))
            }
        }

        // List all subscriptions
        get("/subscriptions") {
            val subs = eventBus.getAllSubscriptions().map {
                SubscriptionResponse(
                    id = it.id,
                    eventTypes = it.eventTypes,
                    callbackUrl = it.callbackUrl,
                    active = it.active
                )
            }
            call.respond(subs)
        }

        // Replay events from offset
        get("/replay") {
            val fromOffset = call.request.queryParameters["from"]?.toLongOrNull()
                ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Query parameter 'from' (offset) is required")
                )
            val type = call.request.queryParameters["type"]?.let {
                try {
                    EventType.valueOf(it)
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid event type: $it"))
                    return@get
                }
            }

            val events = eventBus.replay(fromOffset, type)
            call.respond(events)
        }
    }
}
