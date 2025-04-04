package com.dd3boh.outertune.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(
    tableName = "lyrics",
    foreignKeys = [
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class LyricsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: String,
    val languageCode: String,
    val content: String,
    val timingData: String?, // JSON string for timing data
    val isSynced: Boolean = false,
    val source: LyricsSource,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

enum class LyricsSource {
    USER,
    API,
    AUTO_TRANSLATED
}

@Entity(
    tableName = "lyrics_translations",
    foreignKeys = [
        ForeignKey(
            entity = LyricsEntity::class,
            parentColumns = ["id"],
            childColumns = ["lyricsId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class LyricsTranslationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val lyricsId: Long,
    val targetLanguageCode: String,
    val translatedContent: String,
    val createdAt: LocalDateTime = LocalDateTime.now()
)

data class LyricLine(
    val startTime: Long,  // en millisecondes
    val endTime: Long,    // en millisecondes
    val text: String
)