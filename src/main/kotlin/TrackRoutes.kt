package com.example

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.trackRoutes() {
    val trackDAO = TrackDAO()

    route("/tracks") {
        // Получить все треки
        get {
            val tracks = trackDAO.getAll()
            call.respond(tracks)
        }

        get("/search") {
            val title = call.request.queryParameters["title"]
            println("Received title: $title")
            if (title.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Название трека не указано")
                return@get
            }

            try {
                val tracks = trackDAO.getByTitle(title)
                println("Результат поиска: найдено ${tracks.size} треков")
                if (tracks.isEmpty()) {
                    call.respond(HttpStatusCode.NotFound, "Треки не найдены")
                } else {
                    call.respond(tracks)
                }
            } catch (e: Exception) {
                call.application.environment.log.error("Ошибка при поиске трека: ${e.message}", e)
                println("Стек ошибки: ${e.stackTraceToString()}")
                call.respond(HttpStatusCode.InternalServerError, "Произошла ошибка на сервере: ${e.message}")
            }
        }



        // Получить трек по ID
        get("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respondText("Invalid ID", status = HttpStatusCode.BadRequest)

            val track = trackDAO.getById(id)
                ?: return@get call.respondText("Track not found", status = HttpStatusCode.NotFound)

            call.respond(track)
        }

        // Добавить трек
        post {
            val track = call.receive<Track>()
            val newTrack = trackDAO.create(track)
            call.respond(newTrack)
        }

        // Обновить трек
        put("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@put call.respondText("Invalid ID", status = HttpStatusCode.BadRequest)

            val track = call.receive<Track>()
            val updated = trackDAO.update(id, track)

            if (updated) {
                call.respondText("Track updated", status = HttpStatusCode.OK)
            } else {
                call.respondText("Track not found", status = HttpStatusCode.NotFound)
            }
        }

        // Удалить трек
        delete("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respondText("Invalid ID", status = HttpStatusCode.BadRequest)

            val deleted = trackDAO.delete(id)

            if (deleted) {
                call.respondText("Track deleted", status = HttpStatusCode.OK)
            } else {
                call.respondText("Track not found", status = HttpStatusCode.NotFound)
            }
        }

    }
}