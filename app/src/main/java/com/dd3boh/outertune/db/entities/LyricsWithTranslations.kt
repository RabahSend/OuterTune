package com.dd3boh.outertune.db.entities

import androidx.room.Embedded
import androidx.room.Relation

data class LyricsWithTranslations(
    @Embedded val lyrics: LyricsEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "lyricsId"
    )
    val translations: List<LyricsTranslationEntity>
) 