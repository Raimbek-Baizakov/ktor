package com.example

import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class ExposedMusicUser(
    val phone: String?,
    val email: String?,
    val role: String = "Listener"
)

@Serializable
data class UserResponse(
    val id: Int,
    val phone: String?,
    val email: String?,
    val role: String
)

class MusicUserService(database: Database) {
    object MusicUsers : Table("music_users") {
        val id = integer("id").autoIncrement()
        val phone = varchar("phone", length = 15).nullable()
        val email = varchar("email", length = 100).nullable()
        val role = varchar("role", length = 50).default("Listener")

        override val primaryKey = PrimaryKey(id)

        init {
            check { (phone neq null) or (email neq null) }
        }
    }

    init {
        transaction(database) {
            SchemaUtils.create(MusicUsers)
        }
    }

    suspend fun create(user: ExposedMusicUser): UserResponse = dbQuery {
        val id = MusicUsers.insert { insert ->
            user.phone?.let { insert[phone] = it }
            user.email?.let { insert[email] = it }
            insert[role] = user.role
        }[MusicUsers.id]

        UserResponse(id, user.phone, user.email, user.role)
    }

    suspend fun findUser(phone: String? = null, email: String? = null): UserResponse? {
    return dbQuery {
        val query = MusicUsers.selectAll()

        when {
            phone != null && email != null ->
                query.where { (MusicUsers.phone eq phone) or (MusicUsers.email eq email) }
            phone != null ->
                query.where { MusicUsers.phone eq phone }
            email != null ->
                query.where { MusicUsers.email eq email }
            else ->
                return@dbQuery null
        }

        query.singleOrNull()?.let { row ->
            UserResponse(
                id = row[MusicUsers.id],
                phone = row[MusicUsers.phone],
                email = row[MusicUsers.email],
                role = row[MusicUsers.role]
            )
        }
    }
}

    suspend fun getAllUsers(): List<UserResponse> {
        return dbQuery {
            MusicUsers.selectAll().map { row ->
                UserResponse(
                    id = row[MusicUsers.id],
                    phone = row[MusicUsers.phone],
                    email = row[MusicUsers.email],
                    role = row[MusicUsers.role]
                )
            }
        }
    }

    suspend fun updateUser(id: Int, user: ExposedMusicUser): Boolean {
        return dbQuery {
            MusicUsers.update({ MusicUsers.id eq id }) {
                user.phone?.let { phone -> it[MusicUsers.phone] = phone }
                user.email?.let { email -> it[MusicUsers.email] = email }
                it[role] = user.role
            } > 0
        }
    }

    suspend fun deleteUser(id: Int): Boolean {
        return dbQuery {
            MusicUsers.deleteWhere { MusicUsers.id eq id } > 0
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}