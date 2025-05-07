package com.example

import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.TransactionManager

@Serializable
data class Track(
    val id: Int,
    val title: String,
    val author: String,
    val duration: Int,  // Продолжительность в секундах
    val imagePath: String?,
    var downloaded: Boolean,
    var favorite: Boolean,
    val playlistName: String?,
    val genre: String?,
    val file_Path: String  // Новое поле для пути к файлу

)

object Tracks : Table("tracks") {
    val id = integer("id").autoIncrement()
    val title = varchar("title", 255)
    val author = varchar("author", 255)
    val duration = integer("duration")
    val imagePath = varchar("image_path", 512).nullable()
    val downloaded = bool("downloaded").default(false)
    val favorite = bool("favorite").default(false)
    val playlistName = varchar("playlist_name", 255).nullable()
    val genre = varchar("genre", 100).nullable()
    val file_Path = varchar("file_path", 512)  // Новое поле для пути к файлу

    override val primaryKey = PrimaryKey(id)
}

class TrackDAO {
    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    private fun resultRowToTrack(row: ResultRow) = Track(
        id = row[Tracks.id],
        title = row[Tracks.title],
        author = row[Tracks.author],
        duration = row[Tracks.duration],
        imagePath = row[Tracks.imagePath],
        downloaded = row[Tracks.downloaded],
        favorite = row[Tracks.favorite],
        playlistName = row[Tracks.playlistName],
        genre = row[Tracks.genre],
        file_Path = row[Tracks.file_Path]
    )


    suspend fun getAll(): List<Track> = dbQuery {
        Tracks.selectAll().map(::resultRowToTrack)
    }

    suspend fun getById(id: Int): Track? = dbQuery {
        Tracks.select(Tracks.id eq id)
            .map(::resultRowToTrack)
            .singleOrNull()
    }

    suspend fun getByTitle(title: String): List<Track> = dbQuery {
        try {
            val searchPattern = "%${title.lowercase()}%"
            println("Поиск по шаблону: $searchPattern")

            // Попробуем использовать SQL DSL по-другому
            val query = Tracks.selectAll().where {
                (Tracks.title.lowerCase() like searchPattern)
            }

            println("SQL запрос: ${query.prepareSQL(TransactionManager.current())}")

            val result = query.toList()

            println("Найдено записей: ${result.size}")
            result.forEach {
                println("ID: ${it[Tracks.id]}, Title: ${it[Tracks.title]}")
            }

            result.map(::resultRowToTrack)
        } catch (e: Exception) {
            println("Ошибка при поиске трека: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }



    suspend fun create(track: Track): Track = dbQuery {
        val id = Tracks.insert {
            it[title] = track.title
            it[author] = track.author
            it[duration] = track.duration
            it[imagePath] = track.imagePath
            it[downloaded] = track.downloaded
            it[favorite] = track.favorite
            it[playlistName] = track.playlistName
            it[genre] = track.genre
            it[file_Path] = track.file_Path
        }[Tracks.id]

        track.copy(id = id)
    }

    suspend fun update(id: Int, track: Track): Boolean = dbQuery {
        Tracks.update({ Tracks.id eq id }) {
            it[title] = track.title
            it[author] = track.author
            it[duration] = track.duration
            it[imagePath] = track.imagePath
            it[downloaded] = track.downloaded
            it[favorite] = track.favorite
            it[playlistName] = track.playlistName
            it[genre] = track.genre
            it[file_Path] = track.file_Path
        } > 0
    }


    suspend fun delete(id: Int): Boolean = dbQuery {
        Tracks.deleteWhere { Tracks.id eq id } > 0
    }
}