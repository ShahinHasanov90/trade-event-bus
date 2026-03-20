package com.customs.eventbus

import com.customs.eventbus.routes.configureEventRoutes
import com.customs.eventbus.service.Dispatcher
import com.customs.eventbus.service.EventBus
import com.customs.eventbus.service.EventStore
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.http.*
import kotlinx.serialization.json.Json

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    val capacity = System.getenv("EVENT_STORE_CAPACITY")?.toIntOrNull() ?: 10_000

    embeddedServer(Netty, port = port) {
        configureApp(capacity)
    }.start(wait = true)
}

fun Application.configureApp(eventStoreCapacity: Int = 10_000) {
    val eventStore = EventStore(capacity = eventStoreCapacity)
    val dispatcher = Dispatcher()
    val eventBus = EventBus(eventStore, dispatcher)

    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    install(DefaultHeaders) {
        header("X-Service", "trade-event-bus")
    }

    install(CallLogging)

    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to (cause.message ?: "Bad request")))
        }
        exception<Throwable> { call, cause ->
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to (cause.message ?: "Internal server error"))
            )
        }
    }

    configureEventRoutes(eventBus, eventStore)
}
