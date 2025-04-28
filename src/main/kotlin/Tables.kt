package com.example

import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
data class Track(
    val id: Int? = null,
    val title: String,
    val author: String,
    val duration: Int,
    val image: String,
    val downloaded: Boolean = false,
    val favorite: Boolean = false,
    val inPlaylist: String? = null
)

class TrackService(database: Database) {
    object Tracks : Table("tracks") {
        val id = integer("id").autoIncrement()
        val title = varchar("title", length = 255)
        val author = varchar("author", length = 255)
        val duration = integer("duration")
        val image = varchar("image", length = 255)
        val downloaded = bool("downloaded").default(false)
        val favorite = bool("favorite").default(false)
        val inPlaylist = varchar("in_playlist", length = 255).nullable()

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.create(Tracks)
        }
    }

    suspend fun create(track: Track): Int = dbQuery {
        Tracks.insert { insert ->
            insert[title] = track.title
            insert[author] = track.author
            insert[duration] = track.duration
            insert[image] = track.image
        }[Tracks.id]
    }

    suspend fun read(id: Int): Track? = dbQuery {
        Tracks.selectAll().where{ Tracks.id eq id }
            .map { row ->
                Track(
                    id = row[Tracks.id],
                    title = row[Tracks.title],
                    author = row[Tracks.author],
                    duration = row[Tracks.duration],
                    image = row[Tracks.image],
                    downloaded = row[Tracks.downloaded],
                    favorite = row[Tracks.favorite],
                    inPlaylist = row[Tracks.inPlaylist]
                )
            }
            .singleOrNull()
    }

    suspend fun update(id: Int, updatedFields: Map<String, Any?>) = dbQuery {
        Tracks.update({ Tracks.id eq id }) { update ->
            updatedFields.forEach { (key, value) ->
                when (key) {
                    "downloaded" -> update[downloaded] = value as Boolean
                    "favorite" -> update[favorite] = value as Boolean
                    "inPlaylist" -> update[inPlaylist] = value as String?
                }
            }
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}