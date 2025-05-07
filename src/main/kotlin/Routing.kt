package com.example

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        // Простой тестовый маршрут
        get("/") {
            call.respondText("hi!")
        }

        // Маршруты для работы с треками
        trackRoutes()
    }
}