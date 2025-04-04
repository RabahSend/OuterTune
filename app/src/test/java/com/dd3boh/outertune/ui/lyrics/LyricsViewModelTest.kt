package com.dd3boh.outertune.ui.lyrics

import com.dd3boh.outertune.db.DatabaseDao
import com.dd3boh.outertune.db.entities.LyricsEntity
import com.dd3boh.outertune.db.entities.LyricsTranslationEntity
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LyricsViewModelTest {
    
    private lateinit var viewModel: LyricsViewModel
    private lateinit var databaseDao: DatabaseDao
    private val testDispatcher = StandardTestDispatcher()
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        databaseDao = mockk()
        viewModel = LyricsViewModel(databaseDao)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }
    
    @Test
    fun `initial state is correct`() = runTest {
        assertNull(viewModel.currentLyrics.value)
        assertEquals(emptyList(), viewModel.availableLanguages.value)
        assertEquals(emptyMap(), viewModel.translations.value)
        assertFalse(viewModel.editMode.value)
    }
    
    @Test
    fun `setCurrentSong updates lyrics and available languages`() = runTest {
        // Given
        val songId = "test_song"
        val lyrics = LyricsEntity(
            songId = songId,
            languageCode = "fr",
            content = "Test lyrics",
            timingData = null,
            isSynced = false
        )
        val languages = listOf("fr", "en")
        
        coEvery { databaseDao.getLyrics(songId, any()) } returns lyrics
        every { databaseDao.getAvailableLanguages(songId) } returns languages
        every { databaseDao.getTranslations(any()) } returns emptyList()
        
        // When
        viewModel.setCurrentSong(songId)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        assertEquals(lyrics, viewModel.currentLyrics.value)
        assertEquals(languages, viewModel.availableLanguages.value)
    }
    
    @Test
    fun `setSelectedLanguage updates current lyrics`() = runTest {
        // Given
        val songId = "test_song"
        val frLyrics = LyricsEntity(
            songId = songId,
            languageCode = "fr",
            content = "Paroles de test",
            timingData = null,
            isSynced = false
        )
        val enLyrics = LyricsEntity(
            songId = songId,
            languageCode = "en",
            content = "Test lyrics",
            timingData = null,
            isSynced = false
        )
        
        coEvery { databaseDao.getLyrics(songId, "fr") } returns frLyrics
        coEvery { databaseDao.getLyrics(songId, "en") } returns enLyrics
        every { databaseDao.getAvailableLanguages(songId) } returns listOf("fr", "en")
        every { databaseDao.getTranslations(any()) } returns emptyList()
        
        // When
        viewModel.setCurrentSong(songId)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.setSelectedLanguage("en")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        assertEquals(enLyrics, viewModel.currentLyrics.value)
    }
    
    @Test
    fun `toggleEditMode switches edit state`() = runTest {
        // Given
        assertFalse(viewModel.editMode.value)
        
        // When
        viewModel.toggleEditMode()
        
        // Then
        assertTrue(viewModel.editMode.value)
        
        // When
        viewModel.toggleEditMode()
        
        // Then
        assertFalse(viewModel.editMode.value)
    }
    
    @Test
    fun `saveLyrics calls database insert`() = runTest {
        // Given
        val lyrics = LyricsEntity(
            songId = "test_song",
            languageCode = "fr",
            content = "Test lyrics",
            timingData = null,
            isSynced = false
        )
        coEvery { databaseDao.insertLyrics(lyrics) } just Runs
        
        // When
        viewModel.saveLyrics(lyrics)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        coVerify { databaseDao.insertLyrics(lyrics) }
    }
    
    @Test
    fun `saveTranslation creates and inserts translation entity`() = runTest {
        // Given
        val songId = "test_song"
        val lyrics = LyricsEntity(
            id = 1,
            songId = songId,
            languageCode = "fr",
            content = "Paroles de test",
            timingData = null,
            isSynced = false
        )
        val translation = LyricsTranslationEntity(
            lyricsId = 1,
            targetLanguageCode = "en",
            translatedContent = "Test lyrics"
        )
        
        coEvery { databaseDao.getLyrics(songId, "fr") } returns lyrics
        coEvery { databaseDao.insertTranslation(any()) } just Runs
        every { databaseDao.getAvailableLanguages(songId) } returns listOf("fr")
        every { databaseDao.getTranslations(any()) } returns emptyList()
        
        // When
        viewModel.setCurrentSong(songId)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.saveTranslation("en", "Test lyrics")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        coVerify { databaseDao.insertTranslation(match { 
            it.lyricsId == translation.lyricsId &&
            it.targetLanguageCode == translation.targetLanguageCode &&
            it.translatedContent == translation.translatedContent
        }) }
    }
    
    @Test
    fun `deleteLyrics removes current lyrics`() = runTest {
        // Given
        val songId = "test_song"
        val lyrics = LyricsEntity(
            songId = songId,
            languageCode = "fr",
            content = "Test lyrics",
            timingData = null,
            isSynced = false
        )
        
        coEvery { databaseDao.getLyrics(songId, any()) } returns lyrics
        coEvery { databaseDao.deleteLyrics(lyrics) } just Runs
        every { databaseDao.getAvailableLanguages(songId) } returns listOf("fr")
        every { databaseDao.getTranslations(any()) } returns emptyList()
        
        // When
        viewModel.setCurrentSong(songId)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.deleteLyrics()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        coVerify { databaseDao.deleteLyrics(lyrics) }
    }
    
    @Test
    fun `deleteTranslation removes specific translation`() = runTest {
        // Given
        val songId = "test_song"
        val lyrics = LyricsEntity(
            id = 1,
            songId = songId,
            languageCode = "fr",
            content = "Paroles de test",
            timingData = null,
            isSynced = false
        )
        val translation = LyricsTranslationEntity(
            lyricsId = 1,
            targetLanguageCode = "en",
            translatedContent = "Test lyrics"
        )
        
        coEvery { databaseDao.getLyrics(songId, "fr") } returns lyrics
        coEvery { databaseDao.getTranslation(1, "en") } returns translation
        coEvery { databaseDao.deleteTranslation(translation) } just Runs
        every { databaseDao.getAvailableLanguages(songId) } returns listOf("fr", "en")
        every { databaseDao.getTranslations(any()) } returns listOf(translation)
        
        // When
        viewModel.setCurrentSong(songId)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.deleteTranslation("en")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        coVerify { databaseDao.deleteTranslation(translation) }
    }
} 