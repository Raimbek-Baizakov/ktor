package com.example

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*

fun Application.configureDatabases() {
    val database = Database.connect(
        url = "jdbc:postgresql://localhost:5432/music_users",
        driver = "org.postgresql.Driver",
        user = "postgres",
        password = "1234"
    )
    val userService = MusicUserService(database)
    routing {
        // Create user
        post("/users") {
            val user = call.receive<ExposedMusicUser>()
            val createdUser = userService.create(user)
            call.respond(HttpStatusCode.Created, createdUser)
        }

        // Get all users
        get("/users") {
            val users = userService.getAllUsers() // Используем правильное имя метода
            call.respond(HttpStatusCode.OK, users)
        }

        // Get user by phone or email
        get("/users/find") {
            val phone = call.request.queryParameters["phone"]
            val email = call.request.queryParameters["email"]
            val user = userService.findUser(phone, email)
            if (user != null) {
                call.respond(HttpStatusCode.OK, user)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        // Update user
        put("/users/{id}") {
            val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
            val user = call.receive<ExposedMusicUser>()
            val updated = userService.updateUser(id, user)
            if (updated) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        // Delete user
        delete("/users/{id}") {
            val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
            val deleted = userService.deleteUser(id)
            if (deleted) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}