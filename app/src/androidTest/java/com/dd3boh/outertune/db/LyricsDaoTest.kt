package com.dd3boh.outertune.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dd3boh.outertune.db.entities.LyricsEntity
import com.dd3boh.outertune.db.entities.LyricsTranslationEntity
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
class LyricsDaoTest {
    private lateinit var db: MusicDatabase
    private lateinit var dao: DatabaseDao
    
    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context,
            MusicDatabase::class.java
        ).build()
        dao = db.databaseDao()
    }
    
    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }
    
    @Test
    fun insertAndGetLyrics() = runTest {
        // Given
        val lyrics = LyricsEntity(
            songId = "test_song",
            languageCode = "fr",
            content = "Test lyrics",
            timingData = null,
            isSynced = false
        )
        
        // When
        dao.insertLyrics(lyrics)
        val retrieved = dao.getLyrics("test_song", "fr")
        
        // Then
        assertNotNull(retrieved)
        assertEquals(lyrics.songId, retrieved.songId)
        assertEquals(lyrics.languageCode, retrieved.languageCode)
        assertEquals(lyrics.content, retrieved.content)
    }
    
    @Test
    fun insertAndGetLyricsWithTiming() = runTest {
        // Given
        val timingData = listOf(
            mapOf(
                "startTime" to 0L,
                "endTime" to 1000L,
                "text" to "Line 1"
            ),
            mapOf(
                "startTime" to 1000L,
                "endTime" to 2000L,
                "text" to "Line 2"
            )
        )
        val lyrics = LyricsEntity(
            songId = "test_song",
            languageCode = "fr",
            content = "Line 1\nLine 2",
            timingData = Gson().toJson(timingData),
            isSynced = true
        )
        
        // When
        dao.insertLyrics(lyrics)
        val retrieved = dao.getLyrics("test_song", "fr")
        
        // Then
        assertNotNull(retrieved)
        assertEquals(lyrics.timingData, retrieved.timingData)
        assertEquals(true, retrieved.isSynced)
    }
    
    @Test
    fun insertAndGetTranslation() = runTest {
        // Given
        val lyrics = LyricsEntity(
            songId = "test_song",
            languageCode = "fr",
            content = "Paroles de test",
            timingData = null,
            isSynced = false
        )
        dao.insertLyrics(lyrics)
        val lyricsWithId = dao.getLyrics("test_song", "fr")
        
        val translation = LyricsTranslationEntity(
            lyricsId = lyricsWithId.id,
            targetLanguageCode = "en",
            translatedContent = "Test lyrics"
        )
        
        // When
        dao.insertTranslation(translation)
        val retrieved = dao.getTranslation(lyricsWithId.id, "en")
        
        // Then
        assertNotNull(retrieved)
        assertEquals(translation.lyricsId, retrieved.lyricsId)
        assertEquals(translation.targetLanguageCode, retrieved.targetLanguageCode)
        assertEquals(translation.translatedContent, retrieved.translatedContent)
    }
    
    @Test
    fun getLyricsWithTranslations() = runTest {
        // Given
        val lyrics = LyricsEntity(
            songId = "test_song",
            languageCode = "fr",
            content = "Paroles de test",
            timingData = null,
            isSynced = false
        )
        dao.insertLyrics(lyrics)
        val lyricsWithId = dao.getLyrics("test_song", "fr")
        
        val translations = listOf(
            LyricsTranslationEntity(
                lyricsId = lyricsWithId.id,
                targetLanguageCode = "en",
                translatedContent = "Test lyrics"
            ),
            LyricsTranslationEntity(
                lyricsId = lyricsWithId.id,
                targetLanguageCode = "es",
                translatedContent = "Letras de prueba"
            )
        )
        translations.forEach { dao.insertTranslation(it) }
        
        // When
        val lyricsWithTranslations = dao.getLyricsWithTranslations("test_song").first()
        
        // Then
        assertEquals(1, lyricsWithTranslations.size)
        assertEquals(2, lyricsWithTranslations[0].translations.size)
        assertEquals(
            setOf("en", "es"),
            lyricsWithTranslations[0].translations.map { it.targetLanguageCode }.toSet()
        )
    }
    
    @Test
    fun getAvailableLanguages() = runTest {
        // Given
        val lyrics1 = LyricsEntity(
            songId = "test_song",
            languageCode = "fr",
            content = "Paroles de test",
            timingData = null,
            isSynced = false
        )
        val lyrics2 = LyricsEntity(
            songId = "test_song",
            languageCode = "en",
            content = "Test lyrics",
            timingData = null,
            isSynced = false
        )
        dao.insertLyrics(lyrics1)
        dao.insertLyrics(lyrics2)
        
        // When
        val languages = dao.getAvailableLanguages("test_song")
        
        // Then
        assertEquals(setOf("fr", "en"), languages.toSet())
    }
    
    @Test
    fun deleteLyrics() = runTest {
        // Given
        val lyrics = LyricsEntity(
            songId = "test_song",
            languageCode = "fr",
            content = "Test lyrics",
            timingData = null,
            isSynced = false
        )
        dao.insertLyrics(lyrics)
        val lyricsWithId = dao.getLyrics("test_song", "fr")
        
        // When
        dao.deleteLyrics(lyricsWithId)
        val retrieved = dao.getLyrics("test_song", "fr")
        
        // Then
        assertNull(retrieved)
    }
    
    @Test
    fun deleteTranslation() = runTest {
        // Given
        val lyrics = LyricsEntity(
            songId = "test_song",
            languageCode = "fr",
            content = "Paroles de test",
            timingData = null,
            isSynced = false
        )
        dao.insertLyrics(lyrics)
        val lyricsWithId = dao.getLyrics("test_song", "fr")
        
        val translation = LyricsTranslationEntity(
            lyricsId = lyricsWithId.id,
            targetLanguageCode = "en",
            translatedContent = "Test lyrics"
        )
        dao.insertTranslation(translation)
        
        // When
        dao.deleteTranslation(translation)
        val retrieved = dao.getTranslation(lyricsWithId.id, "en")
        
        // Then
        assertNull(retrieved)
    }
    
    @Test
    fun cascadeDeleteLyricsAndTranslations() = runTest {
        // Given
        val lyrics = LyricsEntity(
            songId = "test_song",
            languageCode = "fr",
            content = "Paroles de test",
            timingData = null,
            isSynced = false
        )
        dao.insertLyrics(lyrics)
        val lyricsWithId = dao.getLyrics("test_song", "fr")
        
        val translation = LyricsTranslationEntity(
            lyricsId = lyricsWithId.id,
            targetLanguageCode = "en",
            translatedContent = "Test lyrics"
        )
        dao.insertTranslation(translation)
        
        // When
        dao.deleteLyrics(lyricsWithId)
        val retrievedTranslation = dao.getTranslation(lyricsWithId.id, "en")
        
        // Then
        assertNull(retrievedTranslation)
    }
} 