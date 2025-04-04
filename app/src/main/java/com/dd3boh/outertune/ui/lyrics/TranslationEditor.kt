package com.dd3boh.outertune.ui.lyrics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dd3boh.outertune.R
import com.dd3boh.outertune.db.entities.LyricsEntity
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationEditor(
    lyrics: LyricsEntity?,
    availableLanguages: List<String>,
    translations: Map<String, String>,
    onSaveTranslation: (String, String) -> Unit,
    onDeleteTranslation: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTargetLanguage by remember { mutableStateOf<String?>(null) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var translationContent by remember { mutableStateOf("") }
    var isTranslating by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Liste des traductions existantes
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(availableLanguages) { languageCode ->
                if (languageCode != lyrics?.languageCode) {
                    TranslationItem(
                        languageCode = languageCode,
                        content = translations[languageCode] ?: "",
                        onDelete = { onDeleteTranslation(languageCode) }
                    )
                }
            }
        }
        
        // Bouton pour ajouter une nouvelle traduction
        OutlinedButton(
            onClick = { showLanguageDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Ajouter une traduction")
        }
    }
    
    // Dialog de sélection de langue
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Sélectionner une langue") },
            text = {
                LazyColumn {
                    items(TranslateLanguage.getAllLanguages()) { languageCode ->
                        if (languageCode !in availableLanguages && languageCode != lyrics?.languageCode) {
                            ListItem(
                                headlineContent = { Text(getLanguageName(languageCode)) },
                                modifier = Modifier.clickable {
                                    selectedTargetLanguage = languageCode
                                    showLanguageDialog = false
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
    
    // Dialog d'édition de traduction
    if (selectedTargetLanguage != null) {
        AlertDialog(
            onDismissRequest = { selectedTargetLanguage = null },
            title = { Text("Traduire en ${getLanguageName(selectedTargetLanguage!!)}") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Texte original
                    Text(
                        text = "Texte original :",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(lyrics?.content ?: "")
                    
                    // Zone de traduction
                    OutlinedTextField(
                        value = translationContent,
                        onValueChange = { translationContent = it },
                        label = { Text("Traduction") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isTranslating
                    )
                    
                    // Bouton de traduction automatique
                    if (!isTranslating) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    isTranslating = true
                                    lyrics?.content?.let { content ->
                                        try {
                                            val translatedText = translateText(
                                                content,
                                                lyrics.languageCode,
                                                selectedTargetLanguage!!
                                            )
                                            translationContent = translatedText
                                        } catch (e: Exception) {
                                            // Handle error
                                        }
                                    }
                                    isTranslating = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Translate, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Traduction automatique")
                        }
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedTargetLanguage?.let { language ->
                            onSaveTranslation(language, translationContent)
                            selectedTargetLanguage = null
                            translationContent = ""
                        }
                    },
                    enabled = translationContent.isNotBlank() && !isTranslating
                ) {
                    Text("Sauvegarder")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        selectedTargetLanguage = null
                        translationContent = ""
                    }
                ) {
                    Text("Annuler")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TranslationItem(
    languageCode: String,
    content: String,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = getLanguageName(languageCode),
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Supprimer")
                }
            }
            
            Spacer(Modifier.height(8.dp))
            Text(content)
        }
    }
}

private suspend fun translateText(
    text: String,
    sourceLanguage: String,
    targetLanguage: String
): String {
    val options = TranslatorOptions.Builder()
        .setSourceLanguage(sourceLanguage)
        .setTargetLanguage(targetLanguage)
        .build()
    
    val translator = Translation.getClient(options)
    
    return try {
        translator.downloadModelIfNeeded().await()
        translator.translate(text).await()
    } finally {
        translator.close()
    }
}

private fun getLanguageName(languageCode: String): String {
    return when (languageCode) {
        "fr" -> "Français"
        "en" -> "English"
        "es" -> "Español"
        "de" -> "Deutsch"
        "it" -> "Italiano"
        "pt" -> "Português"
        "ru" -> "Русский"
        "ja" -> "日本語"
        "ko" -> "한국어"
        "zh" -> "中文"
        else -> languageCode
    }
} 