package com.dd3boh.outertune.ui.lyrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dd3boh.outertune.db.DatabaseDao
import com.dd3boh.outertune.db.entities.LyricsEntity
import com.dd3boh.outertune.db.entities.LyricsTranslationEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LyricsViewModel @Inject constructor(
    private val databaseDao: DatabaseDao
) : ViewModel() {
    
    private val _currentSongId = MutableStateFlow<String?>(null)
    private val _selectedLanguage = MutableStateFlow("fr")
    private val _editMode = MutableStateFlow(false)
    
    val currentLyrics: StateFlow<LyricsEntity?> = combine(
        _currentSongId,
        _selectedLanguage
    ) { songId, language ->
        songId?.let { id ->
            databaseDao.getLyrics(id, language)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )
    
    val availableLanguages: StateFlow<List<String>> = _currentSongId
        .filterNotNull()
        .map { songId ->
            databaseDao.getAvailableLanguages(songId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    val translations: StateFlow<Map<String, String>> = combine(
        currentLyrics,
        _selectedLanguage
    ) { lyrics, _ ->
        lyrics?.id?.let { lyricsId ->
            databaseDao.getTranslations(lyricsId)
                .associate { it.targetLanguageCode to it.translatedContent }
        } ?: emptyMap()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )
    
    val editMode: StateFlow<Boolean> = _editMode.asStateFlow()
    
    fun setCurrentSong(songId: String) {
        _currentSongId.value = songId
    }
    
    fun setSelectedLanguage(languageCode: String) {
        _selectedLanguage.value = languageCode
    }
    
    fun toggleEditMode() {
        _editMode.value = !_editMode.value
    }
    
    fun saveLyrics(lyrics: LyricsEntity) {
        viewModelScope.launch {
            databaseDao.insertLyrics(lyrics)
        }
    }
    
    fun saveTranslation(
        targetLanguageCode: String,
        translatedContent: String
    ) {
        viewModelScope.launch {
            currentLyrics.value?.id?.let { lyricsId ->
                val translation = LyricsTranslationEntity(
                    lyricsId = lyricsId,
                    targetLanguageCode = targetLanguageCode,
                    translatedContent = translatedContent
                )
                databaseDao.insertTranslation(translation)
            }
        }
    }
    
    fun deleteLyrics() {
        viewModelScope.launch {
            currentLyrics.value?.let { lyrics ->
                databaseDao.deleteLyrics(lyrics)
            }
        }
    }
    
    fun deleteTranslation(languageCode: String) {
        viewModelScope.launch {
            currentLyrics.value?.id?.let { lyricsId ->
                databaseDao.getTranslation(lyricsId, languageCode)?.let { translation ->
                    databaseDao.deleteTranslation(translation)
                }
            }
        }
    }
} 