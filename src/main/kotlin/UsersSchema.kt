package com.example

import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class ExposedMusicUser(val phone: String?, val email: String?)

class MusicUserService(database: Database) {
    object MusicUsers : Table("music_user") {
        val id = integer("id").autoIncrement()
        val phone = varchar("phone", length = 15).nullable()
        val email = varchar("email", length = 100).nullable()

        override val primaryKey = PrimaryKey(id)

        init {
            check {
                (phone neq null) or (email neq null)
            }
        }
    }

    init {
        transaction(database) {
            SchemaUtils.create(MusicUsers)
        }
    }

    suspend fun create(user: ExposedMusicUser): Int = dbQuery {
    MusicUsers.insert { insert ->
        user.phone?.let { insert[phone] = it }
        user.email?.let { insert[email] = it }

        // Проверка, что хотя бы одно поле заполнено
        if (user.phone == null && user.email == null) {
            throw IllegalArgumentException("Должен быть указан phone или email")
        }
    }[MusicUsers.id]
}

    suspend fun read(phone: String? = null, email: String? = null): ExposedMusicUser? {
        return dbQuery {
            val query = MusicUsers.selectAll().where {
                (MusicUsers.phone eq phone) or (MusicUsers.email eq email)
            }

            query
                .map { ExposedMusicUser(it[MusicUsers.phone], it[MusicUsers.email]) }
                .singleOrNull()
        }
    }

    suspend fun readAll(): List<ExposedMusicUser> {
        return dbQuery {
            MusicUsers.selectAll()
                .map { row ->
                    ExposedMusicUser(
                        phone = row[MusicUsers.phone],
                        email = row[MusicUsers.email]
                    )
                }
        }
    }

    suspend fun update(id: Int, user: ExposedMusicUser) {
        dbQuery {
            MusicUsers.update({ MusicUsers.id eq id }) {
                it[phone] = user.phone
                it[email] = user.email
            }
        }
    }

    suspend fun delete(id: Int) {
        dbQuery {
            MusicUsers.deleteWhere { MusicUsers.id eq id }
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
